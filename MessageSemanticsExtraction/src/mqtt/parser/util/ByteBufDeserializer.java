package mqtt.parser.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class ByteBufDeserializer extends StdDeserializer<ByteBuf> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ByteBufDeserializer() {
        this(null);
    }
 
    public ByteBufDeserializer(Class<ByteBuf> vc) {
        super(vc);
    }

	@Override
	public ByteBuf deserialize(JsonParser parser, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		
		ObjectCodec codec = parser.getCodec();
        JsonNode node = codec.readTree(parser);

		return Unpooled.wrappedBuffer(node.binaryValue());
	}
	
}
