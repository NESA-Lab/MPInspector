package mqtt.parser.header.api;

/**
 * Mobius Software LTD
 * Copyright 2015-2016, Mobius Software LTD
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

import io.netty.buffer.ByteBuf;

import java.util.List;

import mqtt.parser.avps.ConnackCode;
import mqtt.parser.avps.SubackCode;
import mqtt.parser.avps.Text;
import mqtt.parser.avps.Topic;
import mqtt.parser.avps.Will;

public interface MQDevice
{
	void processConnect(boolean cleanSession, int keepalive, Will will);

	void processConnack(ConnackCode code, boolean sessionPresent);

	void processSubscribe(Integer packetID, Topic[] topics);

	void processSuback(Integer packetID, List<SubackCode> codes);

	void processUnsubscribe(Integer packetID, Text[] topics);

	void processUnsuback(Integer packetID);

	void processPublish(Integer packetID, Topic topic, ByteBuf content, boolean retain, boolean isDup);

	void processPuback(Integer packetID);

	void processPubrec(Integer packetID);

	void processPubrel(Integer packetID);

	void processPubcomp(Integer packetID);

	void processPingreq();

	void processPingresp();

	void processDisconnect();
}
