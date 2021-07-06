package mqtt.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import mqtt.parser.avps.MessageType;
import mqtt.parser.exceptions.MalformedMessageException;
import mqtt.parser.header.api.MQMessage;
import mqtt.parser.header.impl.Connack;
import mqtt.parser.header.impl.Connect;
import mqtt.parser.header.impl.Disconnect;
import mqtt.parser.header.impl.Pingreq;
import mqtt.parser.header.impl.Pingresp;
import mqtt.parser.header.impl.Puback;
import mqtt.parser.header.impl.Pubcomp;
import mqtt.parser.header.impl.Publish;
import mqtt.parser.header.impl.Pubrec;
import mqtt.parser.header.impl.Pubrel;
import mqtt.parser.header.impl.Suback;
import mqtt.parser.header.impl.Subscribe;
import mqtt.parser.header.impl.Unsuback;
import mqtt.parser.header.impl.Unsubscribe;



public class MQJsonParser {

	private ObjectMapper mapper = new ObjectMapper();
	
	public MQJsonParser() {
		this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		this.mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	}
	
	public byte[] encode(MQMessage message) throws JsonProcessingException {
		String json = this.mapper.writeValueAsString(message);
		return json.getBytes();
	}
	
	public String jsonString(MQMessage message) throws JsonProcessingException {
		return new String(this.encode(message));
	}
	
	public MQMessage decode(byte[] data) throws Exception {
		String json = new String(data);
		ObjectNode node = this.mapper.readValue(json, ObjectNode.class);
		if (node.has(MQMessage.JSON_MESSAGE_TYPE_PROPERTY_NAME)) {
			JsonNode packetProperty = node.get(MQMessage.JSON_MESSAGE_TYPE_PROPERTY_NAME);
			MessageType packet = MessageType.valueOf(packetProperty.asInt());
			switch (packet) {
			case CONNECT:
				return mapper.readValue(json, Connect.class);
			case CONNACK: 
				return mapper.readValue(json, Connack.class);
			case PUBLISH: 
				return mapper.readValue(json, Publish.class);
			case PUBACK: 
				return mapper.readValue(json, Puback.class);
			case PUBREC: 
				return mapper.readValue(json, Pubrec.class);
			case PUBREL: 
				return mapper.readValue(json, Pubrel.class);
			case PUBCOMP: 
				return mapper.readValue(json, Pubcomp.class);
			case SUBSCRIBE: 
				return mapper.readValue(json, Subscribe.class);
			case SUBACK: 
				return mapper.readValue(json, Suback.class);
			case UNSUBSCRIBE: 
				return mapper.readValue(json, Unsubscribe.class);
			case UNSUBACK: 
				return mapper.readValue(json, Unsuback.class);
			case PINGREQ: 
				return mapper.readValue(json, Pingreq.class);
			case PINGRESP: 
				return mapper.readValue(json, Pingresp.class);
			case DISCONNECT: 
				return mapper.readValue(json, Disconnect.class);
			default:
				throw new MalformedMessageException("Wrong packet type while decoding message from json.");
			}
		} 
		return null;
	}
	
	public MQMessage messageObject(String json) throws Exception {
		return this.decode(json.getBytes());
	}

}
