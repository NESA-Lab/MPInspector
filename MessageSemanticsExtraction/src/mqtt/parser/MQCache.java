package mqtt.parser;

import mqtt.parser.avps.MessageType;
import mqtt.parser.header.api.MQMessage;

public interface MQCache
{
	MQMessage borrowMessage(MessageType type);

	MQMessage borrowCopy(MQMessage message);
	
	void returnMessage(MQMessage message);
}
