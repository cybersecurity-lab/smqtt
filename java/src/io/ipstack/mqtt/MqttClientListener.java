package io.ipstack.mqtt;


public interface MqttClientListener {

	public void onSubscribing(MqttClient client, String topic, int qos);

	public void onPublishing(MqttClient client, String topic, int qos, byte[] payload);

	public void onMessageArrived(MqttClient client, String topic, int qos, byte[] payload);

	public void onConnectionLost(MqttClient client, Throwable cause);

}
