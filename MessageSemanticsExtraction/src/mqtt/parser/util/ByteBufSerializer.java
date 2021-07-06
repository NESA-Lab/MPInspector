package mqtt.parser.util;

import java.io.IOException;

import io.netty.buffer.ByteBuf;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class ByteBufSerializer extends StdSerializer<ByteBuf> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ByteBufSerializer() { 
        this(null); 
    } 
 
    public ByteBufSerializer(Class<ByteBuf> s) {
        super(s); 
    }

	@Override
	public void serialize(ByteBuf value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		byte[] bytes = new byte[value.readableBytes()];
		value.readBytes(bytes);
		gen.writeBinary(bytes);
	}

}
