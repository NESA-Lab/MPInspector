package mqtt.parser.header.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import mqtt.parser.avps.*;
import mqtt.parser.header.impl.*;

import io.netty.buffer.Unpooled;
import mqtt.parser.avps.Will;
import mqtt.parser.header.impl.Connect;

public abstract class MQMessage implements ProtocolMessage
{
	public static final String JSON_MESSAGE_TYPE_PROPERTY_NAME = "packet";

	@JsonIgnore
	public abstract int getLength();

	@JsonProperty(JSON_MESSAGE_TYPE_PROPERTY_NAME)
	public abstract mqtt.parser.avps.MessageType getType();

	public abstract void processBy(MQDevice device);

	@JsonIgnore
	public Protocol getProtocol()
	{
		return Protocol.MQTT;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static class Builder
	{
		public ConnectBuilder connect()
		{
			return new ConnectBuilder();
		}

		public SubscribeBuilder subscribe()
		{
			return new SubscribeBuilder();
		}

		public UnsubscribeBuilder unsubscribe()
		{
			return new UnsubscribeBuilder();
		}

		public PublishBuilder publish()
		{
			return new PublishBuilder();
		}

		public PubackBuilder puback()
		{
			return new PubackBuilder();
		}

		public PubrecBuilder pubrec()
		{
			return new PubrecBuilder();
		}

		public PubrelBuilder pubrel()
		{
			return new PubrelBuilder();
		}

		public PubcompBuilder pubcomp()
		{
			return new PubcompBuilder();
		}

		public ConnackBuilder connack()
		{
			return new ConnackBuilder();
		}

		public SubackBuilder suback()
		{
			return new SubackBuilder();
		}

		public UnsubackBuilder unsuback()
		{
			return new UnsubackBuilder();
		}

		public static class ConnectBuilder
		{
			private String username;
			private String password;
			private String clientID;
			private Boolean isClean;
			private Integer keepalive;
			private Will will;

			public ConnectBuilder username(String username)
			{
				this.username = username;
				return this;
			}

			public ConnectBuilder password(String password)
			{
				this.password = password;
				return this;
			}

			public ConnectBuilder clientID(String clientID)
			{
				this.clientID = clientID;
				return this;
			}

			public ConnectBuilder cleanSession()
			{
				this.isClean = true;
				return this;
			}

			public ConnectBuilder keepalive(int keepalive)
			{
				this.keepalive = keepalive;
				return this;
			}

			public ConnectBuilder will(String topic, int qos, byte[] content, boolean retain)
			{
				this.will = new Will(new Topic(new Text(topic), QoS.valueOf(qos)), content, retain);
				return this;
			}

			public Connect build()
			{
				return new Connect(username, password, clientID, isClean, keepalive, will);
			}
		}

		public static class SubscribeBuilder
		{
			private Integer packetID;
			private List<Topic> topics = new ArrayList<>();

			public SubscribeBuilder addTopic(String name, int qos)
			{
				topics.add(new Topic(new Text(name), QoS.valueOf(qos)));
				return this;
			}

			public SubscribeBuilder packetID(int packetID)
			{
				this.packetID = packetID;
				return this;
			}

			public Subscribe build()
			{
				return new Subscribe(packetID, topics.toArray(new Topic[topics.size()]));
			}
		}

		public static class UnsubscribeBuilder
		{
			private Integer packetID;
			private List<Text> topics = new ArrayList<>();

			public UnsubscribeBuilder addTopic(String name)
			{
				topics.add(new Text(name));
				return this;
			}

			public UnsubscribeBuilder packetID(int packetID)
			{
				this.packetID = packetID;
				return this;
			}

			public Unsubscribe build()
			{
				return new Unsubscribe(packetID, topics.toArray(new Text[topics.size()]));
			}
		}

		public static class PublishBuilder
		{
			private Integer packetID;
			private Topic topic;
			private byte[] content;
			private boolean dup;
			private boolean retain;

			public PublishBuilder packetID(int packetID)
			{
				this.packetID = packetID;
				return this;
			}

			public PublishBuilder topic(String name, int qos)
			{
				this.topic = new Topic(new Text(name), QoS.valueOf(qos));
				return this;
			}

			public PublishBuilder content(byte[] content)
			{
				this.content = content;
				return this;
			}

			public PublishBuilder duplicate()
			{
				this.dup = true;
				return this;
			}

			public PublishBuilder retain()
			{
				this.retain = true;
				return this;
			}

			public Publish build()
			{
				return new Publish(packetID, topic, Unpooled.copiedBuffer(content), retain, dup);
			}
		}

		public static class PubackBuilder
		{
			private Integer packetID;

			public PubackBuilder packetID(int packetID)
			{
				this.packetID = packetID;
				return this;
			}

			public Puback build()
			{
				return new Puback(packetID);
			}
		}

		public static class PubrecBuilder
		{
			private Integer packetID;

			public PubrecBuilder packetID(int packetID)
			{
				this.packetID = packetID;
				return this;
			}

			public Pubrec build()
			{
				return new Pubrec(packetID);
			}
		}

		public static class PubrelBuilder
		{
			private Integer packetID;

			public PubrelBuilder packetID(int packetID)
			{
				this.packetID = packetID;
				return this;
			}

			public Pubrel build()
			{
				return new Pubrel(packetID);
			}
		}

		public static class PubcompBuilder
		{
			private Integer packetID;

			public PubcompBuilder packetID(int packetID)
			{
				this.packetID = packetID;
				return this;
			}

			public Pubcomp build()
			{
				return new Pubcomp(packetID);
			}
		}

		public static class ConnackBuilder
		{
			private boolean sessionPresent;
			private ConnackCode code;

			public Connack build()
			{
				return new Connack(sessionPresent, code);
			}

			public ConnackBuilder sessionPresent()
			{
				this.sessionPresent = true;
				return this;
			}

			public ConnackBuilder code(ConnackCode code)
			{
				this.code = code;
				return this;
			}
		}

		public static class SubackBuilder
		{
			private Integer packetID;
			private List<SubackCode> codes = new ArrayList<>();

			public Suback build()
			{
				return new Suback(packetID, codes);
			}

			public SubackBuilder packetID(int packetID)
			{
				this.packetID = packetID;
				return this;
			}

			public SubackBuilder addCode(SubackCode code)
			{
				codes.add(code);
				return this;
			}
		}

		public static class UnsubackBuilder
		{
			private Integer packetID;

			public Unsuback build()
			{
				return new Unsuback(packetID);
			}

			public UnsubackBuilder packetID(int packetID)
			{
				this.packetID = packetID;
				return this;
			}
		}
	}
}
