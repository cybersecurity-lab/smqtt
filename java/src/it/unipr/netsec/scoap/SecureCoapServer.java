package it.unipr.netsec.scoap;

import java.io.IOException;

import org.zoolu.util.Bytes;
import org.zoolu.util.Random;
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
import io.ipstack.coap.message.CoapRequest;
import io.ipstack.coap.message.CoapResponse;
import io.ipstack.coap.server.CoapServer;
import io.ipstack.mqtt.MqttClient;
import io.ipstack.mqtt.MqttClientListener;
import io.ipstack.mqtt.PahoClient;


/** Enhanced CoAP client with end-to-end (E2E) security.
 */
public class SecureCoapServer extends CoapServer {

	/** Verbose mode */
	public static boolean VERBOSE= false;
	
	private void log(String str) {
		DefaultLogger.log(LoggerLevel.INFO,null,this.getClass().getSimpleName()+"("+clientId+"): "+str);
	}

	
	public static int GKD_TYPE= 1; // 1=static, 2=update, 3=slotted

	/** Id for communicating with the GKD server */
	String clientId= "client-"+Random.nextInt();
	
	/** Long-term server secret key */
	byte[] serverKey;
	
	GKDClient gkdClient;

	private MqttClient mqttClient= null;
	
	private JoinResponse joinResp= null;

	private long joinTimeout= 5000;
	
	
	public SecureCoapServer(String clientId, byte[] clientKey, String broker, String username, String passwd) throws IOException {
		super();
		intGKD(clientId,clientKey,broker,username,passwd);
	}

	
	public SecureCoapServer(int local_port, String clientId, byte[] clientKey, String broker, String username, String passwd) throws IOException {
		super(local_port);
		intGKD(clientId,clientKey,broker,username,passwd);
	}
	
	
	private void intGKD(String clientId, byte[] clientKey, String broker, String username, String passwd) throws IOException {
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
	
	
	@Override
	public boolean respond(CoapRequest req, CoapResponse resp) {
		log("respond(): "+resp.getCodeAsString());
		var payload= resp.getPayload();
		if (payload!=null) {
			if (VERBOSE) log("request(): encrypting the request payload");
			byte[] key= null;
			try {
				 key= getGroupKey(req.getRequestUriPath().toString());
				var cipher= new AuthenticatedEncryption(key);
				payload= cipher.encrypt(payload);
				resp.setPayload(payload);
			}
			catch (Exception e) {
				e.printStackTrace();
				if (VERBOSE) log("request(): encryption error: possible wrong group key: "+Bytes.toHex(key));
				//throw new IOException("request(): Encryption error: possible wrong group key: "+Bytes.toHex(key),e);
				return false;
			}
		}
	    return super.respond(req,resp);
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
				if (VERBOSE) log("processReceivedMessage(): JOIN RESPONSE: group="+joinResp.group+(joinResp.intBegin>=0? ", interval=["+joinResp.intBegin+"-"+(joinResp.intBegin+joinResp.intLen-1)+"]" : "")+", key-material="+joinResp.key);
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
