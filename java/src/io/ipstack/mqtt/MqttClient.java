package io.ipstack.mqtt;

import java.io.IOException;

/** MQTT Client.
 */
public interface MqttClient {
	
	/** Default broker port */
	public static int DEFAULT_BROKER_PORT=1883;

	
	/** Gets the client identifier.
	 * @return the identifier */
	public String getId();

	/** Gets the broker URL.
	 * @return the URL */
	public String getBrokerUrl();

	/** Connect the client. 
	 * @throws IOException */
	public void connect() throws IOException;
	
	/** Disconnect the client. 
	 * @throws IOException */
	public void disconnect() throws IOException;
  
	/** Publish / send a message to an MQTT server
	 * @param topic_name the name of the topic to publish to
	 * @param qos the quality of service to delivery the message at (0,1,2)
	 * @param payload the set of bytes to send to the MQTT server
	 * @throws IOException */
	public void publish(String topic_name, int qos, byte[] payload) throws IOException;

	/** Subscribe to a topic on an MQTT server
	 * Once subscribed this method waits for the messages to arrive from the server
	 * that match the subscription. It continues listening for messages until the enter key is
	 * pressed.
	 * @param topic_name to subscribe to (can be wild carded)
	 * @param qos the maximum quality of service to receive messages at for this subscription
	 * @throws IOException */
	public void subscribe(String topic_name, int qos) throws IOException;
	
}
