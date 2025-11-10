package it.unipr.netsec.scoap;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import org.zoolu.util.Bytes;
import org.zoolu.util.SystemUtils;
import org.zoolu.util.Timer;
import org.zoolu.util.TimerListener;
import org.zoolu.util.json.Json;
import org.zoolu.util.log.DefaultLogger;
import org.zoolu.util.log.LoggerLevel;

import it.unipr.netsec.smqtt.gkd.GKDClient;
import it.unipr.netsec.smqtt.gkd.GKDServer;
import it.unipr.netsec.smqtt.gkd.message.AuthenticatedEncryption;
import it.unipr.netsec.smqtt.gkd.message.JoinRequest;
import it.unipr.netsec.smqtt.gkd.message.JoinResponse;
import it.unipr.netsec.smqtt.gkd.method.SlottedGKDClient;
import it.unipr.netsec.smqtt.gkd.method.StaticGKDClient;
import it.unipr.netsec.smqtt.gkd.method.UpdateGKDClient;
import io.ipstack.coap.blockwise.BlockwiseTransactionClient;
import io.ipstack.coap.blockwise.BlockwiseTransactionClientListener;
import io.ipstack.coap.client.CoapClient;
import io.ipstack.coap.client.CoapResponseHandler;
import io.ipstack.coap.message.CoapRequest;
import io.ipstack.coap.message.CoapRequestMethod;
import io.ipstack.coap.message.CoapResponse;
import io.ipstack.coap.provider.CoapURI;
import io.ipstack.mqtt.MqttClient;
import io.ipstack.mqtt.MqttClientListener;
import io.ipstack.mqtt.PahoClient;


/** Enhanced CoAP client with end-to-end (E2E) security.
 */
public class SecureCoapClient extends CoapClient {

	/** Verbose mode */
	public static boolean VERBOSE= false;
	
	private void log(String str) {
		DefaultLogger.log(LoggerLevel.INFO,null,this.getClass().getSimpleName()+"("+clientId+"): "+str);
	}

	
	public static int GKD_TYPE= 1; // 1=static, 2=update, 3=slotted

	/** Id for communicating with the GKD server */
	String clientId;
	
	/** Long-term client secret key */
	byte[] clientKey;
	
	GKDClient gkdClient;

	private MqttClient mqttClient;
	
	//private String joinLock= "JOIN_LOCK";
	
	private JoinResponse joinResp= null;

	private long joinTimeout= 5000;

	
	public SecureCoapClient(String clientId, byte[] clientKey, String broker, String username, String passwd) throws IOException {
		super();
		initGKD(clientId,clientKey,broker,username,passwd);
	}

	
	public SecureCoapClient(int localPort, String clientId, byte[] clientKey, String broker, String username, String passwd) throws IOException {
		super(localPort);
		initGKD(clientId,clientKey,broker,username,passwd);
	}

	
	public SecureCoapClient(DatagramSocket socket, String clientId, byte[] clientKey, String broker, String username, String passwd) throws IOException {
		super(socket);
		initGKD(clientId,clientKey,broker,username,passwd);
	}
	
	private void initGKD(String clientId, byte[] clientKey, String broker, String username, String passwd) throws IOException {
		this.clientId= clientId;
		mqttClient= new PahoClient(clientId,"tcp://"+broker,username,passwd,new MqttClientListener() {

			@Override
			public void onSubscribing(MqttClient client, String topic, int qos) {
			}

			@Override
			public void onPublishing(MqttClient client, String topic, int qos, byte[] payload) {
			}

			@Override
			public void onMessageArrived(MqttClient client, String topic, int qos, byte[] payload) {
				processMqttMessage(topic,qos,payload);
			}

			@Override
			public void onConnectionLost(MqttClient client, Throwable cause) {
			}
		});

		switch (GKD_TYPE) {
			case 1: gkdClient= new StaticGKDClient(clientId,clientKey); break;
			case 2: gkdClient= new UpdateGKDClient(clientId,clientKey); break;
			case 3: gkdClient= new SlottedGKDClient(clientId,clientKey); break;
		}
		if (gkdClient==null) throw new IOException("unsupported GKD type: "+GKD_TYPE);

		mqttClient.connect();
		SystemUtils.sleep(1000);
		mqttClient.subscribe(GKDServer.TOPIC_GKD+"/+/"+clientId,GKDServer.DEFAULT_QOS);
	}
	
	
	/**
	 * @param topic
	 * @param payload
	 */
	private void processMqttMessage(String topic, int qos, byte[] payload) {
		try {
			if (VERBOSE) log("processReceivedMessage(): topic="+topic+", len="+payload.length+"B");
			var topicPath= topic.split("/");
			if (topicPath[0].equals(GKDServer.TOPIC_GKD)) {
				if (!topicPath[1].equals(String.valueOf(GKD_TYPE))) throw new IOException("Wrong GKD type: "+topicPath[1]);
				if (!topicPath[2].equals(clientId)) throw new IOException("Wrong client id: "+topicPath[2]);
				// else
				var body= new String(payload);
				joinResp= Json.fromJSON(body,JoinResponse.class);
				if (VERBOSE) log("processReceivedMessage(): JOIN RESPONSE: group="+joinResp.group+", expires="+joinResp.expires+"s, key-material="+joinResp.key);
				synchronized (mqttClient) {
					mqttClient.notifyAll();					
				}
			}
			else {
				if (VERBOSE) log("processReceivedMessage(): unknown topic '+topic+': message discarded");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void request(CoapRequest req, InetSocketAddress serverSoaddr, final CoapResponseHandler respHandler) {
		var resourceName= req.getRequestUriPath();
		// check whether a valid key for the target resource is already available, otherwise request a new one
		byte[] key= getGroupKey(resourceName);
		if (key==null) {
			if (VERBOSE) log("request(): no valid group key found: request discarded");
			// NOTE: SHOULD THROW AN EXCEPTION
			return;
		}
		// else
		var payload= req.getPayload();
		// encrypt the payload
		if (payload!=null) {
			if (VERBOSE) log("request(): encrypting the request payload");
			try {
				var cipher= new AuthenticatedEncryption(key);
				payload= cipher.encrypt(payload);
			}
			catch (Exception e) {
				e.printStackTrace();
				if (VERBOSE) log("request(): encryption error: possible wrong group key: "+Bytes.toHex(key));
				//throw new IOException("request(): Encryption error: possible wrong group key: "+Bytes.toHex(key),e);
				return;
			}
			req.setPayload(payload);
		}
		var secureRespHandler= new CoapResponseHandler() {
			@Override
			public void onRequestFailure(CoapRequest arg0) {
				respHandler.onRequestFailure(arg0);
			}
			@Override
			public void onResponse(CoapRequest req, CoapResponse resp) {
				var payload= resp.getPayload();
				// decrypt the payload
				if (payload!=null) {
					if (VERBOSE) log("request(): decrypting the request payload");
					try {
						var cipher= new AuthenticatedEncryption(key);
						payload= cipher.decrypt(payload);
					}
					catch (Exception e) {
						e.printStackTrace();
						if (VERBOSE) log("request(): decryption error: possible wrong group key: "+Bytes.toHex(key));
						//throw new IOException("request(): Encryption error: possible wrong group key: "+Bytes.toHex(key),e);
						return;
					}
					resp.setPayload(payload);						
				}
				respHandler.onResponse(req,resp);
			}
		};
		super.request(req,serverSoaddr,secureRespHandler);
	}


	
	@Override
	public CoapResponse request(CoapRequestMethod method, CoapURI resourceUri, int format, byte[] payload) {
		var resourceName= resourceUri.getPath().toString();
		// check whether a valid key for the target resource is already available, otherwise request a new one
		byte[] key= getGroupKey(resourceName);
		if (key==null) {
			if (VERBOSE) log("request(): no valid group key found: request discarded");
			// NOTE: SHOULD THROW AN EXCEPTION
			return null;
		}
		// else
		// encrypt the payload
		if (payload!=null) {
			if (VERBOSE) log("request(): encrypting the request payload");
			try {
				var cipher= new AuthenticatedEncryption(key);
				payload= cipher.encrypt(payload);
			}
			catch (Exception e) {
				e.printStackTrace();
				if (VERBOSE) log("request(): encryption error: possible wrong group key: "+Bytes.toHex(key));
				//throw new IOException("request(): Encryption error: possible wrong group key: "+Bytes.toHex(key),e);
				return null;
			}
		}
		var coapResp= super.request(method,resourceUri,format,payload);
		payload= coapResp.getPayload();
		// decrypt the payload
		if (payload!=null) {
			if (VERBOSE) log("request(): decrypting the request payload");
			try {
				var cipher= new AuthenticatedEncryption(key);
				payload= cipher.decrypt(payload);
			}
			catch (Exception e) {
				e.printStackTrace();
				if (VERBOSE) log("request(): decryption error: possible wrong group key: "+Bytes.toHex(key));
				//throw new IOException("request(): Encryption error: possible wrong group key: "+Bytes.toHex(key),e);
				return null;
			}
			coapResp.setPayload(payload);
		}
		return coapResp;
	}
	
	private byte[] getGroupKey(String resourceName) {
		byte[] key= gkdClient.getGroupKey(resourceName);
		if (key!=null) {
			if (VERBOSE) log("request(): group key already available: "+Bytes.toHex(key));
			return key;
		}
		else {
			if (VERBOSE) log("request(): group key not found: trying to join the group");
			try {
				gkdClient.join(resourceName,(JoinRequest join)->mqttClient.publish(GKDServer.TOPIC_GKD+"/"+GKD_TYPE+"/"+GKDServer.TOPIC_JOIN,GKDServer.DEFAULT_QOS,Json.toJSON(join).getBytes()));
				// TODO
				
				if (joinTimeout>0) {
					new Timer(joinTimeout,new TimerListener() {
						@Override
						public void onTimeout(Timer t) {
							synchronized (mqttClient) {
								mqttClient.notifyAll();
							}						
						}
					}).start();
				}
				// wait for join response, and get key
				synchronized (mqttClient) {
					try { mqttClient.wait(); } catch (InterruptedException e) {}
					if (joinResp!=null) {
						gkdClient.handleJoinResponse(joinResp);
						key= gkdClient.getGroupKey(resourceName);
					}			
				}
				return key;
			}
			catch (Exception e) {
				e.printStackTrace();
				// NOTE: SHOULD THROW AN EXCEPTION
				return null;
			}
		}
	}

}
