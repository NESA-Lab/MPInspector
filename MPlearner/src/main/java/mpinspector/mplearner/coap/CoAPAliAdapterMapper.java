package mpinspector.mplearner.coap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.text.SimpleDateFormat;
import java.util.Scanner;

import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomUtils;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionNumberRegistry;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.elements.exception.ConnectorException;

import com.alibaba.fastjson.JSONObject;



public class CoAPAliAdapterMapper {
	static Logger logmqtt = Logger.getLogger("mqttoutput.log");
	FileHandler fileHandler;

    String result = null;
	boolean haveclient = false;
	String type = null;
	String outAction = "Empty";
	String host = "empty";
	String username = "empty";
	String password = "empty";
	String pubtopic = "empty";
	String pubmsg = "empty";
	
	
	// ===================user needs to write start===========================
    // region ID，
    //private static String regionId = "";
    // productKey
    //private static String productKey = "";
    // deviceName
    //private static String deviceName = "";
    // deviceSecret
   // private static String deviceSecret = "";
    // ===================user needs to write end===========================

    // identify MAC algorithm: HmacMD5 HmacSHA1, need to be same with signature
    private static final String HMAC_ALGORITHM = "hmacsha1";

    // CoAP address，port is 5682
    
    //private static String serverURI = "coap://" + productKey +  ".coap." + regionId + ".link.aliyuncs.com:5682";

    // Topic for send messages; user can customize his topic in console; the authority of device operation should be chosen to publish
    //private static String updateTopic = "/" + productKey + "/" + deviceName + "/user/update";
    /////user/get
   // private static String getTopic = "/"+productKey+"/"+deviceName+"/user/get";

    // token option
    private static final int COAP2_OPTION_TOKEN = 2088;
    // seq option
    private static final int COAP2_OPTION_SEQ = 2089;

    // encryption algorithm sha256
    private static final String SHA_256 = "SHA-256";

    private static final int DIGITAL_16 = 16;
    private static final int DIGITAL_48 = 48;
    private CoapClient coapClient;
   

    // token is valid in seven days
    private String token = "***";
    private String random = "***";
    @SuppressWarnings("unused")
    private long seqOffset = 0;
	
	public void initLog() {
		try {
			fileHandler = new FileHandler("testmqtt_alicoap.log");
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
	
	public void setNewClient() {
		 // CoAP client
	    coapClient = new CoapClient();
		String result=null;
		System.out.println(result);
	}
	
	public void reset() {
		//System.out.println(coapClient.toString());
		if(haveclient) {
			coapClient.shutdown();
		}
		token = "***";
	    random = "***";
	    seqOffset = 0;
		
		haveclient = false;
		logmqtt.info("haveclinet is " + haveclient);
		if(haveclient) {
			
		}else {
			setNewClient(); // 
			//initClient();   //
			haveclient = true;
		}
		
		System.out.println(result);
		
		
	}
	
	/**
     * init coap client
     * 
     * @param productKey 
     * @param deviceName 
     * @param deviceSecret 
     */
    public String connect() {
        try {
        	//String outAction = null;
            // authenticate uri，/auth
            String uri = host + "/auth";

            // only support post
            Request request = new Request(Code.POST, Type.CON);

            // option set
            OptionSet optionSet = new OptionSet();
            optionSet.addOption(new Option(OptionNumberRegistry.CONTENT_FORMAT, MediaTypeRegistry.APPLICATION_JSON));
            optionSet.addOption(new Option(OptionNumberRegistry.ACCEPT, MediaTypeRegistry.APPLICATION_JSON));
            request.setOptions(optionSet);

            // authentication uri set
            request.setURI(uri);

            // authentication request payload
            request.setPayload(CoAPAliEncryption.authBody(CoAPAliEncryption.getproductkey(host), username, password));

            // send authentication request
            CoapResponse response = coapClient.advanced(request);
           // System.out.println(Utils.prettyPrint(response));
            //System.out.println();
           
            // resolve the response
            JSONObject json = JSONObject.parseObject(response.getResponseText());
            token = json.getString("token");
            random = json.getString("random");
            seqOffset = json.getLongValue("seqOffset");
            if(response.getCode().name().contains("CONTENT"))
            	outAction = "CONNACK";
            else outAction = response.getCode().name();
            
        } catch (ConnectorException e) {
            e.printStackTrace();
            outAction = "CONNECTOREXCEPTION";
        } catch (IOException e) {
            e.printStackTrace();
            outAction = "CONNECTIOEXCEPTION";
        }
        return outAction;
    }

    /**
     * publish message
     * 
     * @param topic 
     * @param payload 
     */
    public String publish() {
        try {
            // publish uri，/topic/${topic}
        	
            String uri = host + "/topic" + pubtopic;

            // AES encrypt seq，seq=RandomUtils.nextInt()
            String shaKey = CoAPAliEncryption.encod(password + "," + random);
            byte[] keys = Hex.decodeHex(shaKey.substring(DIGITAL_16, DIGITAL_48));
            byte[] seqBytes = CoAPAliEncryption.encrypt(String.valueOf(RandomUtils.nextInt()).getBytes(StandardCharsets.UTF_8), keys);

            // only support post method
            Request request = new Request(CoAP.Code.POST, CoAP.Type.CON);

            // option set
            OptionSet optionSet = new OptionSet();
            optionSet.addOption(new Option(OptionNumberRegistry.CONTENT_FORMAT, MediaTypeRegistry.APPLICATION_JSON));
            optionSet.addOption(new Option(OptionNumberRegistry.ACCEPT, MediaTypeRegistry.APPLICATION_JSON));
            optionSet.addOption(new Option(COAP2_OPTION_TOKEN, token));
            optionSet.addOption(new Option(COAP2_OPTION_SEQ, seqBytes));
            request.setOptions(optionSet);

            // publish uri
            request.setURI(uri);

            // publish payload
            request.setPayload(CoAPAliEncryption.encrypt(pubmsg.getBytes(StandardCharsets.UTF_8), keys));

            // send message
            CoapResponse response = coapClient.advanced(request);
           // System.out.println(Utils.prettyPrint(response));

            // resolve the reponse
            String result = null;
            if (response.getPayload() != null) {
                result = new String(CoAPAliEncryption.decrypt(response.getPayload(), keys));
            }
           // System.out.println("payload: " + result);
           // System.out.println();
            if(response.getCode().name().contains("CONTENT"))
            	outAction = "PUBACK";
            else outAction = response.getCode().name();
        } catch (ConnectorException e) {
            e.printStackTrace();
            outAction = "CONNECTOREXCEPTION";
        } catch (IOException e) {
            e.printStackTrace();
            outAction = "PUBLISHIOEXCEPTION";
        } catch (DecoderException e) {
            e.printStackTrace();
            outAction = "CRYPTOERROR";
        }
        return outAction;
    }
    
    public String shutdown() {
    	coapClient.shutdown();
		token = "***";
	    random = "***";
	    seqOffset = 0;
	    return "SHUTDOWNSUCCESS";
    }

    
	
	public static void main(String[] args) throws Exception{
		String ans = null;
		System.out.println( "Hello World!" );
        Scanner input = new Scanner(System.in);
        System.out.println("Hello World!");
        CoAPAliAdapterMapper coap = new CoAPAliAdapterMapper();
		   /****************************************
         * Test coap connection
         ***************************************/
        boolean flag = true;

		
       // mqtt.setNewClient();
        while(flag) {
        	System.out.println( "enter a number: 0 for reset, 1 for connect, 2 for publish, 3 for shutdown");
        	int choose= input.nextInt();
        	System.out.println( "you choose " + choose );
        	switch(choose) {
        	case 0:
        		coap.reset();
        		continue;
        	case 1:
        		ans = coap.processSymbol("CONNECT");
        		logmqtt.info( "the answer is " + ans );
        		System.out.println( "the answer is " + ans );
        		continue;
        	case 2:
        		ans = coap.processSymbol("PUBLISH");
        		logmqtt.info( "the answer is " + ans );
        		continue;
        	case 3:
        		ans = coap.processSymbol("SHUTDOWN");
        		logmqtt.info( "the answer is " + ans );
        		continue;
        	default:
        		flag = false;
        	}
        	
        }
        
       
        System.out.println("stop loop");
	}

	public String processSymbol(String symbol) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
				String inAction = symbol;
				System.out.println("**********************************processing the input" + inAction);
				logmqtt.info("**********************************processing the input" + inAction);
				logmqtt.info("haveclinet is " + haveclient);
				if(haveclient) {
					
				}else {
					setNewClient(); 
					//initClient();   
					haveclient = true;
				}
				
				try{
					if(inAction.equals("CONNECT")) {
						//return connect(productKey, deviceName, deviceSecret);
						return connect();
					}else if(inAction.equals("PUBLISH"))
					{
						//return publish(updateTopic, "hello coap".getBytes(StandardCharsets.UTF_8));
						return publish();
					}else if(inAction.equals("SHUTDOWN")) {
						return shutdown();
					}
//					else if(inAction.equals("PUBREC")) { 
//						return receivePublish();
//					}

					else {
						System.out.println("Unknown input symbol (" + inAction + ")...");
						throw new RuntimeException("Unknown input Symbol (" + inAction + ")...");
						
					}
				} catch (Exception e) {
					e.printStackTrace();
					logmqtt.warning( e.toString());
					return "UNKNOWEXCEPTION";
				}	
		
	}

	public void setType(String type) {
		// TODO Auto-generated method stub
		this.type = type;
	}

	public void setHost(String hOST2) {
		// TODO Auto-generated method stub
		this.host = hOST2;
	}

	public void setUsername(String userName2) {
		// TODO Auto-generated method stub
		this.username = userName2;
	}

	public void setPassword(String passWord2) {
		// TODO Auto-generated method stub
		this.password = passWord2;
	}


	public void setPubTopic(String pubTopic2) {
		// TODO Auto-generated method stub
		this.pubtopic = pubTopic2;
	}

	public void setPubMsg(String pubMsg2) {
		// TODO Auto-generated method stub
		this.pubmsg = pubMsg2;
	}

}
