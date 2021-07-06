package mpinspector.mplearner.mqtt;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import mpinspector.mplearner.StateLearnerSUL;
import mpinspector.mplearner.amqp.AMQPJSMapper;
import mpinspector.mplearner.coap.CoAPAliAdapterMapper;
import mpinspector.mplearner.coap.CoAPEMQXAdapterMapper;
import mpinspector.mplearner.coap.CoAPJSMapper;
import net.automatalib.words.impl.SimpleAlphabet;

public class TestSUL implements StateLearnerSUL<String, String>{
	
	SimpleAlphabet<String> alphabet;
	MQTTAdapterMapper mqtt1;
	MQTTJSMapper mqttjs;
	AMQPJSMapper amqpjs;
	CoAPAliAdapterMapper coapali;
	CoAPEMQXAdapterMapper coapemqx;
	/**************************************
	 *perform MP protocol
	 ******************************************/
	MQTTAdapterMapper mqtt;
	String type;  //used to distinguish mqttaws and mqttbosch
	public  String HOST;  
    public static final String TOPIC1 = "command///req/#";
    public static final String TOPIC2 = "temperature";
    private String clientid; 
    String adaptertype;
    private String userName;    //not neccessary
    private String passWord;  //not neccessary
    
    String tlsversion;
    public String clientCrtFilePath;
    String caFilePATH;
	String ClientKeyFilePAth;
	String localport;
    
    public int timeout;
    public int aliveInterval;
    int delaytime = 5000;  //query delaytime
    
	String subTopic;
	String pubTopic;
	
	String version;  // mqtt version
	Logger logmqtt = Logger.getLogger("logmqtt.log");
	FileHandler fileHandler;
	
	
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
        fileHandler.setFormatter(new Formatter() {
        	SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss S");
            
            public String format(LogRecord record) {
            	return format.format(record.getMillis()) +" " + record.getSourceClassName() +" "+ record.getSourceMethodName() + "\n" + record.getLevel() + ": " +" " + record.getMessage() +"\n";
            }
        });
        
        logmqtt.addHandler(fileHandler);
        logmqtt.setUseParentHandlers(false);
	}
	
	
	public TestSUL(MPConfig config) throws Exception {
		type = config.type;  //distingusish mqttaws and mqttbosch
		
		//mqtt config
		alphabet = new SimpleAlphabet<String> (Arrays.asList(config.alphabet.split(" ")));
		HOST = config.host;
		timeout = config.timeout;
		delaytime = config.delaytime;
		aliveInterval = config.aliveInterval;
		clientid = config.clientId;
		userName = config.userName;
		passWord = config.passWord;
		localport = config.localport;
		tlsversion = config.tlsversion;
		clientCrtFilePath = config.clientCrtFilePath;
		caFilePATH = config.caFilePATH;
		ClientKeyFilePAth = config.ClientKeyFilePAth;
		subTopic = config.subTopic;
		pubTopic = config.pubTopic;
		
		
		adaptertype = config.adaptertype;
		
		//build 
		if(adaptertype.contains("mqttjs")) {
			mqttjs = new MQTTJSMapper();
			//set mqttjs
			mqttjs.initLog();
			mqttjs.setHost(HOST);
			mqttjs.setType(type);
			mqttjs.setLocalPort(localport);
			
			//test log
			System.out.println("host: in "+ adaptertype + " sul is " + HOST);
			System.out.println("type: in "+ adaptertype + " sul is " + config.type);
			System.out.println("localport: in "+ adaptertype + " sul is " + config.localport);
			
		}else if(adaptertype.contains("amqpjs")){
			amqpjs = new AMQPJSMapper();
			amqpjs.initLog();
			amqpjs.setHost(HOST);
			amqpjs.setType(type);
			amqpjs.setLocalPort(localport);
			
			//test log
			System.out.println("host: in "+ adaptertype + " sul is " + HOST);
			System.out.println("type: in "+ adaptertype + " sul is " + config.type);
			System.out.println("localport: in "+ adaptertype + " sul is " + config.localport);
			
			
		}else if(adaptertype.contains("coapemqx")) {
			coapemqx = new CoAPEMQXAdapterMapper();
			coapemqx.initLog();
			coapemqx.setHost(HOST);
			coapemqx.setType(adaptertype);
			coapemqx.setUsername(config.userName);
			coapemqx.setPassword(config.passWord);
			coapemqx.setClientID(config.clientId);
			coapemqx.setSubTopic(subTopic);
			coapemqx.setPubTopic(pubTopic);
			coapemqx.setPubMsg(config.pubMsg);
			
			
			//test log
			System.out.println("host: in "+ adaptertype + " sul is " + HOST);
			System.out.println("type: in "+ adaptertype + " sul is " + config.type);
			System.out.println("username: in "+ adaptertype + " sul is " + config.userName);
			System.out.println("clientId: in "+ adaptertype + " sul is " + config.clientId);
			System.out.println("password: in "+ adaptertype + " sul is " + config.passWord);
			System.out.println("subtopic: in "+ adaptertype + " sul is " + config.subTopic);
			System.out.println("username: in "+ adaptertype + " sul is " + config.pubTopic);
			
		}
		
		
	else if(adaptertype.contains("coapali")) {
		coapali = new CoAPAliAdapterMapper();
		coapali.initLog();
		coapali.setHost(HOST);
		coapali.setType(adaptertype);
		coapali.setUsername(config.userName);
		coapali.setPassword(config.passWord);
		//coapali.setSubTopic(subTopic);
		coapali.setPubTopic(pubTopic);
		coapali.setPubMsg(config.pubMsg);
		
		
		//test log
		System.out.println("host: in "+ adaptertype + " sul is " + HOST);
		System.out.println("type: in "+ adaptertype + " sul is " + config.type);
		System.out.println("username: in "+ adaptertype + " sul is " + config.userName);
		System.out.println("clientId: in "+ adaptertype + " sul is " + config.clientId);
		System.out.println("password: in "+ adaptertype + " sul is " + config.passWord);
		System.out.println("username: in "+ adaptertype + " sul is " + config.pubTopic);
		
	}
		else if(adaptertype.contains("mqttjava")){
			mqtt1= new MQTTAdapterMapper();
			mqtt1.initLog();
			// target setting server or client
			mqtt1.setTarget(config.target);
			
			//host and port setting
			mqtt1.setHost(config.host);
			mqtt1.setSubTopic(subTopic);
			mqtt1.setPubTopic(pubTopic);
			mqtt1.setPubMsg(config.pubMsg);
			
			mqtt1.setType(config.type);
			mqtt1.setUsername(config.userName);
			mqtt1.setPassword(config.passWord);
			mqtt1.setClientID(config.clientId);
			
			mqtt1.setTlsversion(tlsversion);
			mqtt1.setClientCrtFilePAth(config.clientCrtFilePath);
			mqtt1.setCaFilePATH(config.caFilePATH);
			mqtt1.setClientKeyFilePAth(config.ClientKeyFilePAth);
			
			mqtt1.setRequireRestart(config.restart);
			
			mqtt1.setTimeout(timeout);
			mqtt1.setAliveInterval(aliveInterval);
			
			
			//test log
			System.out.println("target: in "+ adaptertype + " sul is " + config.target);
			System.out.println("host: in "+ adaptertype + " sul is " + config.host);
			System.out.println("subtopic: in "+ adaptertype + " sul is " + subTopic);
			System.out.println("pubtopic: in "+ adaptertype + " sul is " + pubTopic);
			System.out.println("pubMsg: in "+ adaptertype + " sul is " + config.pubMsg);
			
			System.out.println("adaptertype: in "+ adaptertype + " sul is " + config.adaptertype);
			System.out.println("type: in "+ adaptertype + " sul is " + config.type);
			System.out.println("userName: in "+ adaptertype + " sul is " + config.userName);
			System.out.println("passWord: in "+ adaptertype + " sul is " + config.passWord);
			System.out.println("clientId: in "+ adaptertype + " sul is " + config.clientId);
			
			System.out.println("tlsversion: in "+ adaptertype + " sul is " + tlsversion);
			System.out.println("clientCrtFilePath: in "+ adaptertype + " sul is " + config.clientCrtFilePath);
			System.out.println("caFilePATH: in "+ adaptertype + " sul is " + config.caFilePATH);
			System.out.println("ClientKeyFilePAth: in "+ adaptertype + " sul is " + config.ClientKeyFilePAth);
			
			System.out.println("restart: in "+ adaptertype + " sul is " + config.restart);
			System.out.println("timeout: in "+ adaptertype + " sul is " + timeout);
			System.out.println("aliveInterval: in "+ adaptertype + " sul is " + aliveInterval);
		}
		
	}
	public SimpleAlphabet<String> getAlphabet() {
		return alphabet;
	}	

	
	
	// A step function, choose string from alphbet，the letter in the string matches output letter
	@Override
	public String step(String symbol) {
		String result = null;
		try {
			Thread.sleep(delaytime);
			logmqtt.info("**********************************Processing input " + symbol);
			if(adaptertype.contains("mqttjs")) {
				result = mqttjs.processSymbol(symbol);
			}else if(adaptertype.contains("amqpjs")){
				result = amqpjs.processSymbol(symbol);
			}else if(adaptertype.contains("mqttjava")){
				result = mqtt1.processSymbol(symbol);
			}else if(adaptertype.contains("coapemqx")) {
				result = coapemqx.processSymbol(symbol);
			}else if(adaptertype.contains("coapali"))
			{
				result = coapali.processSymbol(symbol);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	// override reset
	@Override
	public void pre() {
		try {
			if(adaptertype.contains("mqttjs")) {
				mqttjs.reset();
			}else if(adaptertype.contains("amqpjs")){
				amqpjs.reset();
			}
			else if(adaptertype.contains("mqttjava")) {
				mqtt1.reset();
			}else if(adaptertype.contains("coapemqx")) {
				coapemqx.reset();
			}else if(adaptertype.contains("coapali"))
			{
				coapali.reset();	
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public void post() {
	}

	

}
