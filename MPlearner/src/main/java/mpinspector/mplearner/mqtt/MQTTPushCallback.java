package mpinspector.mplearner.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTPushCallback implements MqttCallback {

	@Override
	public void connectionLost(Throwable cause) {
		// TODO Auto-generated method stub
		System.out.println("Disconnected, can be reconnected");  
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		// TODO Auto-generated method stub
		System.out.println("Receive message : " + message.toString());
        System.out.println("Receive message topic : " + topic);  
        System.out.println("Receive message qos : " + message.getQos());  
        System.out.println("Receive message order : " + message.getId()); 
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// TODO Auto-generated method stub
		System.out.println("deliveryComplete---------" + token.isComplete());  
	}
	public void onConnected() {
    	System.out.println("consuming : successfully connect");
    }

}
