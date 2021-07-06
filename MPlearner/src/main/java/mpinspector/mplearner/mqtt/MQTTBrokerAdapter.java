package mpinspector.mplearner.mqtt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Scanner;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.mobius.software.mqtt.parser.MQParser;
import com.mobius.software.mqtt.parser.avps.QoS;
import com.mobius.software.mqtt.parser.avps.Text;
import com.mobius.software.mqtt.parser.avps.Topic;
import com.mobius.software.mqtt.parser.avps.Will;
import com.mobius.software.mqtt.parser.header.api.MQMessage;
import com.mobius.software.mqtt.parser.header.impl.Connect;
import com.mobius.software.mqtt.parser.header.impl.Disconnect;
import com.mobius.software.mqtt.parser.header.impl.Publish;
import com.mobius.software.mqtt.parser.header.impl.Subscribe;
import com.mobius.software.mqtt.parser.header.impl.Unsubscribe;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;


//not used
//discard


public class MQTTBrokerAdapter {
	OutputStream output_socket;
	InputStream input_socket;
	Socket socket_tcp= null;
	SSLSocket socket_tls = null;
	Byte[] packet = null;
	
	// Message variants
	String username = "tmp";
	String password = "tmp";
	String clientID = "clientid_tmp";
	boolean cleanSession = false;
	int keepalive = 20;
	Text topicName = new Text("command///req/#");
	Topic topic = Topic.valueOf(topicName, QoS.AT_LEAST_ONCE);
	String content = "message";
	boolean retain = false;
	Will will = new Will(topic, content.getBytes(), retain);
	String clientCrtFilePath = "iothub.crt";
	String tlsversion = "TLSv1.2";
	int connectTimeout=100;
	Integer packetID = 0;
	
	//outAction
	String outAction = "Empty";
	
	
	void connectSSLSocket() {
		SSLSocketFactory sslsf;
		try {
			sslsf = getSSLSocket(clientCrtFilePath,tlsversion);
			socket_tls = (SSLSocket) sslsf.createSocket("mqtt.bosch-iot-hub.com", 8883);
			System.out.println("TLS socket connected");
			//socket_tls.connect(new InetSocketAddress("mqtt.bosch-iot-hub.com", 8883), connectTimeout);
			input_socket = socket_tls.getInputStream();
			output_socket = socket_tls.getOutputStream();	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	String sendConnect() {
		try {
			if(socket_tls.isClosed()) {
				System.out.println("Socket closed before connect " );
			}else {
			
			Connect connect = new Connect(username, password, clientID, cleanSession, keepalive, null);
			// Encode message
			ByteBuf encoded = MQParser.encode(connect);
			byte[] send_packet = new byte[encoded.readableBytes()];
			encoded.readBytes(send_packet);
			String sends = new String(send_packet);
			output_socket.write(send_packet);
			System.out.println("Send Connect " + byte2Hex(send_packet));
			//int length = input_socket.read();
			//System.out.println("length " + length);
			byte[] bytes = new byte[4];  
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		//	input_socket.read(bytes,0,4);
		//	System.out.println("get connack response " + byte2Hex(bytes));
			//MQMessage connack = MQParser.decode(Unpooled.wrappedBuffer(bytes));
			//System.out.println("connack result is " + connack.toString());
			//connack isSessionPresent  getReturnCode
			
			Topic topic = Topic.valueOf(topicName, QoS.AT_LEAST_ONCE);
			Topic[] topics = new Topic[1];
			topics[0] = topic;
			Subscribe sub =new Subscribe(packetID,topics);
			
			ByteBuf encoded_sub = MQParser.encode(sub);
			byte[] send_sub = new byte[encoded_sub.readableBytes()];
			encoded_sub.readBytes(send_sub);
			output_socket.write(send_sub); 
			System.out.println("Send Subscribe " + byte2Hex(send_sub));
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			byte[] bytes_sub = new byte[10];  
			//bytes_sub[0] = 1;
			input_socket.read(bytes_sub);
			System.out.println("get suback response " + byte2Hex(bytes_sub));
			//MQMessage suback = MQParser.decode(Unpooled.wrappedBuffer(bytes_sub));
			
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return outAction;		
	}
	
	String sendDisconnect() {
		try {
			Disconnect disconnect = new Disconnect();//
			ByteBuf encoded_disc = MQParser.encode(disconnect); //
			byte[] send_disc= new byte[encoded_disc.readableBytes()];//
			encoded_disc.readBytes(send_disc);
			output_socket.write(send_disc);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return outAction;
	}
	
	String sendSubscribe() {
		try {
			if(socket_tls.isClosed()) {
				System.out.println("Socket closed befor connect " );
			}else {
			Topic topic = Topic.valueOf(topicName, QoS.AT_LEAST_ONCE);
			Topic[] topics = new Topic[1];
			topics[0] = topic;
			Subscribe sub =new Subscribe(packetID,topics);
			
			ByteBuf encoded_sub = MQParser.encode(sub);
			byte[] send_sub = new byte[encoded_sub.readableBytes()];
			encoded_sub.readBytes(send_sub);
			output_socket.write(send_sub); 
			System.out.println("Send Subscribe " + byte2Hex(send_sub));
			byte[] bytes_sub = new byte[5];  
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			input_socket.read(bytes_sub);
			System.out.println("get suback response " + byte2Hex(bytes_sub));
			//MQMessage suback = MQParser.decode(Unpooled.wrappedBuffer(bytes_sub));
			packetID = packetID +1;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return outAction;
		
	}
	
	
	String sendUnSubscribe() {
		try {
			Text[] topicNames = new Text[1];
			topicNames[0] = topicName;
			Unsubscribe unsub =new Unsubscribe(packetID,topicNames);
			
			ByteBuf encoded_sub = MQParser.encode(unsub);
			byte[] send_sub = new byte[encoded_sub.readableBytes()];
			encoded_sub.readBytes(send_sub);
			output_socket.write(send_sub);
			
			byte[] bytes = new byte[4];  
			input_socket.read(bytes);
			System.out.println("unsuback get response " + byte2Hex(bytes));
			MQMessage unsuback = MQParser.decode(Unpooled.wrappedBuffer(bytes));
			packetID = packetID + 1;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return outAction;
		
	}
	
	
	String sendPublish() {
		try {
			Topic topic = Topic.valueOf(topicName, QoS.AT_LEAST_ONCE);
			//String content_tmp = "hello my test";
			ByteBuf content=Unpooled.copiedBuffer("hello my test", CharsetUtil.UTF_8);;
			// Publish(Topic topic, ByteBuf content, boolean retain, boolean dup)
			Publish pub =new Publish(topic,content,false,false);
			
			ByteBuf encoded_pub = MQParser.encode(pub);
			byte[] send_pub = new byte[encoded_pub.readableBytes()];
			encoded_pub.readBytes(send_pub);
			output_socket.write(send_pub); 
			
			byte[] bytes = new byte[4];  
			input_socket.read(bytes);
			System.out.println("puback get response " + byte2Hex(bytes));
			MQMessage puback = MQParser.decode(Unpooled.wrappedBuffer(bytes));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return outAction;
	}
	
	
	void reset() {
		try {
			if(socket_tls != null) {
				socket_tls.close();
			}
			connectSSLSocket();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	//void sendPacket
	
	//void communicated (send and read)
	
	void closeSSLSocket() {
		try {
			socket_tls.close();
			System.out.println("TLS socket closed");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	
	
	/**************************************
	 * 
	 ******************************************/
	 public SSLSocketFactory getSSLSocket(String crtPath, String tlsversion) throws Exception {		
			// CA certificate is used to authenticate server
			CertificateFactory cAf = CertificateFactory.getInstance("X.509");
			FileInputStream caIn = new FileInputStream(crtPath);
			X509Certificate ca = (X509Certificate) cAf.generateCertificate(caIn);
			KeyStore caKs = KeyStore.getInstance("JKS");
			caKs.load(null, null);
			caKs.setCertificateEntry("ca-certificate", ca);
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
			tmf.init(caKs);    		
			
			// finally, create SSL socket factory
			SSLContext context = SSLContext.getInstance(tlsversion);
			context.init(null, tmf.getTrustManagers(), new SecureRandom());
	 
			return context.getSocketFactory();
		}
	 
		private static String byte2Hex(byte[] bytes){
			StringBuffer stringBuffer = new StringBuffer();
			String temp = null;
			for (int i=0;i<bytes.length;i++){
				temp = Integer.toHexString(bytes[i] & 0xFF);
				if (temp.length()==1){
					stringBuffer.append("0");
					}
				stringBuffer.append(temp);
				}
			return stringBuffer.toString();
			}
	 
	 public static void main(String[] args) throws Exception{
		 MQTTBrokerAdapter mqttAdapter = new MQTTBrokerAdapter();
		 /****************************************
	         * conneetion test
	         ***************************************/
		 	Scanner input = new Scanner(System.in);
	        System.out.println("Hello World!");
	        boolean flag = true;
	       // flag = false;
	       // mqtt.setNewClient();
	        while(flag) {
	        	System.out.println( "enter a number: 0 for reset, 1 for Connect, 2 for Subscribe, 3 for unsubscribe, 4 for disconnect, 5 for PUBLISH" );
	        	int choose= input.nextInt();
	        	System.out.println( "you choose " + choose );
	        	switch(choose) {
	        	case 0:
	        		mqttAdapter.reset();
	        		continue;
	        	case 1:
	        		mqttAdapter.sendConnect();
	        		continue;
	        	case 2:
	        		mqttAdapter.sendSubscribe();
	        		continue;
	        	case 3:
	        		mqttAdapter.sendUnSubscribe();
	        		continue;
	        	case 4:
	        		mqttAdapter.sendDisconnect();
	        		continue;
	        	case 5:
	        		mqttAdapter.sendPublish();
	        		continue;
	        	default:
	        		flag = false;
	        	}
	        	
	        }
	        
	        System.out.println("stopped");
	        //client.sendDisconnect();
	        //client.close();
	        
	        
	        /**************************
	         * Java JS File test
	         ******************************/
	        testJSFile();
	 }

	private static void testJSFile() {
		// TODO Auto-generated method stub
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("javascript");
		try {
			engine.eval(readJSFile());
			Invocable inv = (Invocable) engine;
			Object res = inv.invokeFunction("convert", new String[] {"505041", "D"});
			System.out.println("res: " + res);
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static String readJSFile() {
		// TODO Auto-generated method stub
		StringBuffer script = new StringBuffer();
		File file = new File("iot prtocol project\\mqtt client js\\test.js");
		FileReader fileReader;
		try {
			fileReader = new FileReader(file);
			BufferedReader bufferreader = new BufferedReader(fileReader);
			String tmpString = null;
			while((tmpString = bufferreader.readLine())!=null) {
				script.append(tmpString).append(" \n");
			}
			bufferreader.close();
			fileReader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return script.toString();
	}
	
	
	 
	 
	 
}
