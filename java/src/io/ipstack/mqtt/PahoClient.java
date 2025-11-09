package io.ipstack.mqtt;

import java.io.IOException;
import java.sql.Timestamp;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.zoolu.util.log.DefaultLogger;
import org.zoolu.util.log.LoggerLevel;


/** MQTT PahoClient.
 */
public class PahoClient implements io.ipstack.mqtt.MqttClient {
	
	/** Verbose mode */
	public static boolean VERBOSE= false;
	
	/** Prints a message. */
	private void log(String str) {
		DefaultLogger.log(LoggerLevel.INFO,null,toString()+": "+str);
	}

	
	private org.eclipse.paho.client.mqttv3.MqttClient client;
	private String client_id;
	private String broker_url;
	private MqttConnectOptions con_opt;
	private boolean clean;
	private String username;	
	private String password;
	private MqttClientListener listener;
	
	
	/** Creates a new client
	 * @param client_id the client id to connect with
	 * @param broker_url the url of the server to connect to
	 * @param username the username to connect with
	 * @param password the password for the user
	 * @param listener client listener
	 */
	public PahoClient(String client_id, String broker_url, String username, String password, final MqttClientListener listener) {
		this.client_id= client_id;
		this.broker_url= broker_url;
		this.clean= true;
		this.password= password;
		this.username= username;
		this.listener= listener;
	 	//This sample stores in a temporary directory... where messages temporarily
		// stored until the message has been delivered to the server.
		//..a real application ought to store them somewhere
		// where they are not likely to get deleted or tampered with
		//String tmpDir = System.getProperty("java.io.tmpdir");
		//MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);
		if (VERBOSE) log("PahoClient()");
		String tempdir="./tmp/";
		String temp=""+client_id+broker_url;
		for (byte c : temp.getBytes()) if ((c>='a' && c<='z') || (c>='A' && c<='Z') || (c>='0' && c<='9')) tempdir+=(char)c;
		MqttDefaultFilePersistence dataStore= new MqttDefaultFilePersistence(tempdir);

		try {
			// Construct an MQTT blocking mode client
			client=new org.eclipse.paho.client.mqttv3.MqttClient(this.broker_url,client_id,dataStore);

			// Set this wrapper as the callback handler
			client.setCallback(new MqttCallback() {
				@Override
				public void connectionLost(Throwable cause) {
					if (listener!=null) listener.onConnectionLost(PahoClient.this,cause);
				}
				@Override
				public void messageArrived(String topic, MqttMessage message) throws Exception {
					if (listener!=null) listener.onMessageArrived(PahoClient.this,topic,message.getQos(),message.getPayload());
				}
				@Override
				public void deliveryComplete(IMqttDeliveryToken token) {
					if (listener!=null) {
						try {
							MqttMessage msg=token.getMessage();
							int id=msg!=null? msg.getId() : -1;
							//listener.onDeliveryComplete(id);
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			});

		} catch (MqttException e) {
			e.printStackTrace();
			if (VERBOSE) log("Unable to set up client: "+e.toString());
			System.exit(1);
		}
	}
	
	@Override
	public String getBrokerUrl() {
		return broker_url;
	}

	@Override
	public String getId() {
		return client_id;
	}

	@Override
	public void connect() throws IOException  {
		try {
			if (VERBOSE) log("Connecting to "+broker_url + " with client ID "+client.getClientId());
			// Construct the connection options object that contains connection parameters
			// such as cleanSession and LWT
			con_opt=new MqttConnectOptions();
			con_opt.setCleanSession(clean);
			if(password!=null) {
			  con_opt.setPassword(password.toCharArray());
			}
			if(username!=null) {
			  con_opt.setUserName(username);
			}
			client.connect(con_opt);
			if (VERBOSE) log("Connected");   	
		}
		catch (MqttException e) {
			throw new IOException("Connect error",e);
		}
	}
	
	@Override
	public void disconnect() throws IOException {
		try {
			client.disconnect();
			if (VERBOSE) log("Disconnected");
		}
		catch (MqttException e)
		{
			throw new IOException("Disconnect error",e);
		}
	}
  
	@Override
	public void publish(String topic_name, int qos, byte[] payload) throws IOException {
		try {
			String time = new Timestamp(System.currentTimeMillis()).toString();
			if (VERBOSE) log("Publishing at: "+time+ " to topic='"+topic_name+"' qos="+qos+" datalen="+payload.length);

			// Create and configure a message
	   		MqttMessage message = new MqttMessage(payload);
			message.setQos(qos);

			// Send the message to the server, control is not returned until
			// it has been delivered to the server meeting the specified
			// quality of service.
			if (listener!=null) listener.onPublishing(this,topic_name,qos,payload);
			client.publish(topic_name,message);
		}
		catch (MqttException e) {
			throw new IOException("Publish failure",e);
		}
	}

	@Override
	public void subscribe(String topic_name, int qos) throws IOException {
		// Subscribe to the requested topic
		// The QoS specified is the maximum level that messages will be sent to the client at.
		// For instance if QoS 1 is specified, any messages originally published at QoS 2 will
		// be downgraded to 1 when delivering to the client but messages published at 1 and 0
		// will be received at the same level they were published at.
		try {
			if (VERBOSE) log("Subscribing to topic \""+topic_name+"\" qos "+qos);
			if (listener!=null) listener.onSubscribing(this,topic_name,qos);
			client.subscribe(topic_name, qos);
		}
		catch (MqttException e) {
			throw new IOException("Subscribe failure",e);
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"["+client_id+">"+broker_url.split(":")[2]+"]";
	}
	
}
