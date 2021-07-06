package main.java.mpinspector;

public class MQTTPacket {
	private String name;
	private byte[] content;
	
	public MQTTPacket() {
		super();
	}
	
	public String getName() {
		return name;
	}
	public byte[] getContent() {
		return content;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setContent(byte[] content) {
		this.content = content;
	}

}
