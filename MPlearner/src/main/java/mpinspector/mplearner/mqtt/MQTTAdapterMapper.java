package mpinspector.mplearner.mqtt;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;



import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;

import javax.net.ssl.KeyManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.Security;

public class MQTTAdapterMapper {

	String type;
	String input;
	String target;
	String host;
	int timeout;
	int aliveInterval;
	String subTopic;
	String pubTopic;
	String pubMsg;
	
	String clientId;
	String userName;
	String passWord;
	
	String tlsversion;
	String clientCrtFilePath;  //certfile
	String caFilePath;  //ca file
	String clientKeyFilePath;//certkey
	
	
	MqttAsyncClient client;
	String outAction = "Empty";
	private IMqttToken connectToken;
	boolean restart;  //not used
	boolean newstart = true;
	boolean haveclient = false ;
	//int port;
	MqttConnectOptions options;
	
	Logger logmqtt = Logger.getLogger("logmqtt.log");
	FileHandler fileHandler;
	
	ClientComms comms;

	

	

	void initLog() {
		try {
			fileHandler = new FileHandler("output_server\\logmqtt.log");
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	fileHandler.setLevel(Level.INFO);
        fileHandler.setFormatter(new Formatter() {//set the log format
        	SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss S");
            
            public String format(LogRecord record) {
            	return format.format(record.getMillis()) +" " + record.getSourceClassName() +" "+ record.getSourceMethodName() + "\n" + record.getLevel() + ": " +" " + record.getMessage() +"\n";
            }
        });
        
        logmqtt.addHandler(fileHandler);
        logmqtt.setUseParentHandlers(false);
	}
	/**************************************
	 * init client
	 ******************************************/	
	public void setNewClient() {
		
		//client = new ClientMQTT(host, timeout, clientId, userName, passWord, clientCrtFilePath);
		try {
			client = new MqttAsyncClient(host, clientId,new MemoryPersistence());
			logmqtt.info("Set a new client ");
			haveclient = true;
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			logmqtt.warning(e.toString());
		}
		
	}

	/**************************************
	 * reset mqtt client
	 ******************************************/	
	public void reset() {
//		// TODO Auto-generated method stub
		logmqtt.info("Reset MQTT client");
		logmqtt.info("newstart is " + newstart);
		if(newstart) {
			newstart = false;
			haveclient = false;
		}else {
			try {
				//System.out.println("close client");
				sendUnsubscribe();
				sendDisconnect();
				sendDisconnect();
				client.close();
				
	
				System.out.println("close Client success ");
				//sendDisconnect();
				
				setNewClient(); // set a new client
				initClient();
				
				haveclient = true;
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				logmqtt.warning( e.toString());
			} 
		}
	}


	/**************************************
	 * init option
	 ******************************************/	
    public void initClient() {  
		  
        try {
	        options = new MqttConnectOptions();  
	        
	        options.setCleanSession(false);  
	       // System.out.println("timeout is " + timeout 
	        options.setConnectionTimeout(timeout);  //bosch, aws is 60, ali is 30
	        logmqtt.info("timeout is "  + timeout);
	        options.setKeepAliveInterval(aliveInterval);  //bosch aws is 20, ali is 60
	        logmqtt.info("aliveInterval is "  + aliveInterval);
	        //handler the SSL connection and TLS connection
	        if(tlsversion.contains("tls")) {
	        	//exist cafile
	        	if(!(caFilePath.equals("empty")) && clientCrtFilePath.equals("empty") && clientKeyFilePath.equals("empty")) {
	        		options.setSocketFactory(getSSLSocket(caFilePath,tlsversion));
	        	}else if(!(caFilePath.equals("empty")) && !(clientCrtFilePath.equals("empty")) && !(clientKeyFilePath.equals("empty"))){
	        		options.setSocketFactory(SslUtil.getSocketFactory(caFilePath,clientCrtFilePath,clientKeyFilePath,tlsversion));
	        	}
	        }
	        if(userName.equals("empty") && passWord.equals("empty")) {
	        	
	        }else {
	        	options.setUserName(userName);  
		        options.setPassword(passWord.toCharArray());
	        }

	        logmqtt.info("initClient success");
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			logmqtt.warning("initClient fail" + e.toString());
			e.printStackTrace();
//			outAction = "initFail";
//			return outAction;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logmqtt.warning("initClient fail" + e.toString());
//			outAction = "initFail";
//			return outAction;
			e.printStackTrace();
		}  
    }
	

	/**************************************
	 *connection and mapper
	 ******************************************/
	public String processSymbol(String symbol) {
		// TODO Auto-generated method stub
		String inAction = symbol;
		System.out.println("**********************************Processing the input " + inAction);
		logmqtt.info("**********************************Processing the input " + inAction);
		if(haveclient) {
			
		}else {
			setNewClient(); 
			initClient();
		}
		initClient();
		try{
			if(inAction.equals("CONNECT")) {
				return sendConnect();
			}else if(inAction.equals("SUBSCRIBE"))
			{
				return sendSubscribe();
			}else if(inAction.equals("UNSUBSCRIBE")) {
				return sendUnsubscribe();
			}else if(inAction.equals("DISCONNECT")) {
				return sendDisconnect();
			}
//			else if(inAction.equals("PUBREC")) { 
//				return sendDisconnect();
//			}
			else if(inAction.equals("PUBLISH")) { 
				return sendPublish();
			}
			else {
				System.out.println("Unknown input symbol (" + inAction + ")...");
				throw new RuntimeException("Unknown input Symbol (" + inAction + ")...");
			}
		} catch (Exception e) {
			logmqtt.warning( e.toString());
			return outAction;
		}	
	}








	/**************************************
	 * load config parameters
	 ******************************************/
	public void setTarget(String target) {
		// TODO Auto-generated method stub
		this.target = target;
	}
	public void setType(String type) {
		// TODO Auto-generated method stub
		this.type = type;
	}
	public void setTimeout(int timeout) {
		// TODO Auto-generated method stub
		this.timeout = timeout;
	}
	public void setAliveInterval(int aliveInterval) {
		this.aliveInterval = aliveInterval;
	}
	public void setHost(String host2) {
		// TODO Auto-generated method stub
		this.host = host2;
	}
	public void setRequireRestart(boolean restart) {
		// TODO Auto-generated method stub
		this.restart = restart;	
	}
	public void setUsername(String userName) {
		// TODO Auto-generated method stub
		this.userName = userName;
	}
	public void setPassword(String passWord) {
		// TODO Auto-generated method stub
		this.passWord = passWord;
	}
	public void setClientID(String clientId) {
		// TODO Auto-generated method stub
		this.clientId = clientId;
	}
	
	
	public void setTlsversion(String tlsversion) {
		// TODO Auto-generated method stub
		this.tlsversion = tlsversion;
	}
	public void setClientCrtFilePAth(String clientCrtFilePath) {
		// TODO Auto-generated method stub
		this.clientCrtFilePath = clientCrtFilePath;
	}
	
	public void setCaFilePATH(String caFilePath) {
		// TODO Auto-generated method stub
		this.caFilePath = caFilePath;
		System.out.println( caFilePath);
	}
	public void setClientKeyFilePAth(String clientKeyFilePath) {
		// TODO Auto-generated method stub
		this.clientKeyFilePath = clientKeyFilePath;
	}
	
	
	public void setSubTopic(String subTopic) {
		// TODO Auto-generated method stub
		this.subTopic = subTopic;
	}
	public void setPubTopic(String pubTopic) {
		// TODO Auto-generated method stub
		this.pubTopic = pubTopic;
	}
	public void setPubMsg(String pubMsg) {
		// TODO Auto-generated method stub
		this.pubMsg = pubMsg;
	}
	
	
	
	
	/**************************************
	 * Functions to process the sending action
	 ******************************************/
    String sendConnect() {
    	try {
    		
    		//System.out.println("client" + client.toString());
    		//client.connect(options);; 
    		//System.out.println("connect option is " + options);
    		//System.out.println("clientId " + client.getClientId());
    		connectToken = client.connect(options); 
    		connectToken.waitForCompletion();
    		logmqtt.info("Successfully send CONNECT packet");
    		logmqtt.info("sendConnect success: connectToken is  "+ connectToken.toString());

    		outAction = "CONNACK";
    		//return outAction;
    	} catch (Exception e) {
    		//e.printStackTrace();
    		logmqtt.warning("sendConnect fail: connectToken.getResponse() is "+ connectToken.getResponse()); 	
    		logmqtt.warning( e.toString());
    		//while(e.)
    		if(client.isConnected()) {
	    		outAction = "AlreadyCONNECTED";
	    	}else {
	    		outAction = "ConnectionClosed";
	    	}
	    	
    	}
    	System.out.println("The Processing result is " + outAction);
    	return outAction;
    }
    
    
    String sendUnsubscribe() {
    	try {
    		System.out.println("Start sending UNSUBSCRIBE packet");
    		logmqtt.info("Start sending UNSUBSCRIBE packet");
    		//System.out.println("client" + client.toString());
    		IMqttToken token = client.unsubscribe(subTopic);
			token.waitForCompletion();
			logmqtt.info("UNSUBSCRIBE token "+ token.getResponse());
    		outAction = "UNSUBACK";
//            	return outAction;
    		logmqtt.info("Successfully send UNSUBSCRIBE packet");
		} catch (MqttSecurityException e) {
			// TODO Auto-generated catch block
			logmqtt.warning( e.toString());
			if(client.isConnected()) {
				outAction = "UNSUBACKFail";
			}else {
				outAction = "ConnectionClosed";
			}
    		//return outAction;
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			logmqtt.warning( e.toString());
			if(client.isConnected()) {
				outAction = "UNSUBACKFail";
			}else {
				outAction = "ConnectionClosed";
			}
    		//return outAction;
		} 
    	System.out.println("The Processing result is " + outAction);
    	return outAction;
    }
    
    String sendSubscribe(){
    	try {
    		
            int Qos  = 1;  
            if(client.isConnected()) {
            	logmqtt.info("client is connected ");
            	// MqttDeliveryToken token = client.subscribe(topic1, Qos); 
            	logmqtt.info("Client SUBSCRIBE topic "+ subTopic);
                logmqtt.info("Sending SUBSCRIBE packet");
                //client.setCallback(new PushCallback());
                IMqttToken token = client.subscribe(subTopic, Qos);
                token.waitForCompletion();
                logmqtt.info("subscribe token "+ token.getResponse());
    			//logmqtt.info("sendSubscribe token" + connectToken.toString());
    			//logmqtt.info("sendSubscribe success");
                System.out.println("subscribe token --"+ token.getResponse() +"--"+ (token.getGrantedQos())[0]);
                //client.subscribe(subTopic, Qos);
                if((token.getGrantedQos())[0] != 1) {
                	outAction = "SUBACKFail";
                }else {
                	//logmqtt.info("sendSubscribe token" + connectToken.toString());
        			outAction = "SUBACK";
        			logmqtt.info("Successfully send SUBSCRIBE packet");
                }
    			
        		//return outAction;
            }else {
            	logmqtt.info("client connection closed");
            	outAction = "ConnectionClosed";
            	//return outAction;
            }
           
    	} catch (Exception e) {
    		logmqtt.info("sendSubscribe fail");
    		logmqtt.warning(e.toString());
    		if(client.isConnected()) {
				outAction = "SUBACKFail";
			}else {
				outAction = "ConnectionClosed";
			}
    		//return outAction;
    	}
    	System.out.println("The Processing result is " + outAction);
    	return outAction;
    }
    

    
    String sendDisconnect() {
    	try {
    		//System.out.println("client" + client.toString());
    		//if(client != null && client.isConnected()) {
    		Boolean connectflag = false;
    		if(client.isConnected()) {
    			connectflag = true;
    		}
    		IMqttToken token = client.disconnect();
    		token.waitForCompletion();
    		logmqtt.info("message is disconnected completely! "  
                        + token.isComplete() + " "+ token.toString());
    			//clientStatus.disconnect();
    			//Thread.sleep(10000);
    		logmqtt.info("Successfully send DISCONNECT packet");
    		//}
			if(client.isConnected()) {
				logmqtt.info("sendDisconnect fail: client is connected ");
				outAction = "DISCONNECTFail";
	    		//return outAction;
			}else if(connectflag){
				logmqtt.info("sendDisconnect success: client is disconnected ");
				outAction = "DISCONNACK";
	    		//return outAction;
			}else {
				logmqtt.info("client is disconnected before");
				outAction = "ConnectionClosed";
			}
			
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			logmqtt.warning(e.toString());
			if(e.toString().equals("client not connected (32104)") ||e.toString().equals("client disconnected (32101)")) {
    			outAction = "ConnectionClosed";
    		}else {
    			//outAction = "SUBSCRIBEFail"
    			outAction = "DISCONNECTFail";
    		}
		}
    	System.out.println("The Processing result is " + outAction);
		return outAction;
    }
    
    //
    String sendPublish() {
    	MqttMessage message = new MqttMessage("Hello my test".getBytes());
    	message.setQos(1);  
        message.setRetained(false);
        try {
        	IMqttDeliveryToken token = client.publish(pubTopic, message);
        	token.waitForCompletion();
        	logmqtt.info("message is published completely! "  
                     + token.isComplete() + " "+ token.toString());
			outAction = "PUBACK";
			
		} catch (MqttPersistenceException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			logmqtt.warning(e.toString());
			outAction = "PUBACKFail";
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			if(e.toString().equals("client not connected (32104)") ||e.toString().equals("client disconnected (32101)")) {
    			outAction = "ConnectionClosed";
    		}else {
    			//outAction = "SUBSCRIBEFail"
    			outAction = "PUBLISHFail";
    		}
			//e.printStackTrace();
			logmqtt.warning(e.toString());
		}
        System.out.println("The Processing result is " + outAction);
    	return outAction;
    }
    
    //receive PUBLISH packet
    void receivePublish() {
    	
    }
    

    
 
	/**************************************
	 * ssl lib, a crt file
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
	 
	 
		/**************************************
		 * ssl lib, two crt file, a key and a password
		 ******************************************/

     public static SSLSocketFactory getSocketFactory(final String caCrtFile, final String crtFile, final String keyFile,
                                                     final String password) throws Exception {
         Security.addProvider(new BouncyCastleProvider());

         // load CA certificate
         PEMReader reader = new PEMReader(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(caCrtFile)))));
         X509Certificate caCert = (X509Certificate)reader.readObject();
         reader.close();

         // load client certificate
         reader = new PEMReader(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(crtFile)))));
         X509Certificate cert = (X509Certificate)reader.readObject();
         reader.close();

         // load client private key
         reader = new PEMReader(
                 new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(keyFile)))),
                 new PasswordFinder() {
                     @Override
                     public char[] getPassword() {
                         return password.toCharArray();
                     }
                 }
         );
         KeyPair key = (KeyPair)reader.readObject();
         reader.close();

         // CA certificate is used to authenticate server
         KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
         caKs.load(null, null);
         caKs.setCertificateEntry("ca-certificate", caCert);
         TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
         tmf.init(caKs);

         // client key and certificates are sent to server so it can authenticate us
         KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
         ks.load(null, null);
         ks.setCertificateEntry("certificate", cert);
         ks.setKeyEntry("private-key", key.getPrivate(), password.toCharArray(), new java.security.cert.Certificate[]{cert});
         KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
         kmf.init(ks, password.toCharArray());

         // finally, create SSL socket factory
         SSLContext context = SSLContext.getInstance("TLSv1");
         context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

         return context.getSocketFactory();
     }
	 

     
     
	

}
