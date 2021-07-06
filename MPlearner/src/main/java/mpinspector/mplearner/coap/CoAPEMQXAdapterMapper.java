package mpinspector.mplearner.coap;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.elements.exception.ConnectorException;


public class CoAPEMQXAdapterMapper {
	static Logger logcoap = Logger.getLogger("coapalioutput.log");
	FileHandler fileHandler;

    String result = null;
	boolean haveclient = false;
	String type = null;
	String outAction = "Empty";
	
	
	// CoAP client
    private CoapClient coapClient = new CoapClient();
    String host ="empty";
    String clientId = "empty";
    String username = "empty";
    String password = "empty";
    String subtopic = "empty";
    String pubtopic = "empty";
    String pubMsg = "empty";
    //String serverURI = "coap://"+ip+subtopic+"?c="+clientId +"&u="+username + "&p="+password ;
   //xx.xxx.xxx.xxx：2322/mqtt/topic1？c=xxx&u=xxx&p=xxx
	
	public void initLog() {
		try {
			fileHandler = new FileHandler("alicoap.log");
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	fileHandler.setLevel(Level.INFO);
        fileHandler.setFormatter(new Formatter() {//an anonymous class
        SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss S");
            
        public String format(LogRecord record) {
        	return format.format(record.getMillis()) +" " + record.getSourceClassName() +" "+ record.getSourceMethodName() + "\n" + record.getLevel() + ": " +" " + record.getMessage() +"\n";
        }
        });
        
        logcoap.addHandler(fileHandler);
        logcoap.setUseParentHandlers(false);
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
	
		
		haveclient = false;
		logcoap.info("haveclinet is " + haveclient);
		if(haveclient) {
			
		}else {
			setNewClient(); 
			//initClient();   
			haveclient = true;
		}
		
		System.out.println(result);
		
		
	}
	
	/**
     * init a coap client
     * 
     * @param productKey 
     * @param deviceName 
     * @param deviceSecret
     */
    public String publish() {
        try {
        	//String outAction = null;
            // authentication uri，/auth
        	String puburi = "coap://"+host+pubtopic+"?c="+clientId +"&u="+username + "&p="+password ;
        	System.out.println(puburi);
        	// only support put
            Request request = new Request(Code.PUT, Type.CON);
            request.setURI(puburi);
            
            request.setPayload(pubMsg);
            System.out.println(pubMsg);
            CoapResponse response;

           
            response = coapClient.advanced(request);
            if(response != null) {
	        	System.out.println(Utils.prettyPrint(response));
	        	 // resolve the response
	            outAction = response.getCode().name();
	        }else {
	            System.out.println("there is no response for this put operation");
	            outAction = "EmptyResponse";
	        }          
            
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
     * subscribe a topic
     * 
     * @param topic 
     * @param payload 
     */
    public String subscribe() {
        try {
            //uri，/topic/${topic}
        	
        	String suburi = "coap://"+host+subtopic+"?c="+clientId +"&u="+username + "&p="+password ;
        	System.out.println(suburi);
        	// only support PUT
            Request request = new Request(Code.GET, Type.CON);
            request.setURI(suburi);
           // request.addMessageObserver(observer);
           // request.setPayload("get," + new Date().toString());
            CoapResponse response;
    		//response = request.waitForResponse(10000);

          
           // System.out.println(Utils.prettyPrint(response));

            // resolve the response
            String result = null;
            response = coapClient.advanced(request);
			if(response != null) {
				System.out.println(Utils.prettyPrint(response));
	            outAction = response.getCode().name();
			}else {
			    System.out.println("there is no response for this put operation");
			    outAction = "EmptyResponse";
			    
			}
            
           // System.out.println("payload: " + result);
           // System.out.println();
            
        } catch (ConnectorException e) {
            e.printStackTrace();
            outAction = "CONNECTOREXCEPTION";
        } catch (IOException e) {
            e.printStackTrace();
            outAction = "PUBLISHIOEXCEPTION";
        } 
        return outAction;
    }
    
    public String shutdown() {
    	coapClient.shutdown();
	
	    return "SHUTDOWNSUCCESS";
    }

  
	
	public static void main(String[] args) throws Exception{
		String ans = null;
		System.out.println( "Hello World!" );
        Scanner input = new Scanner(System.in);
        System.out.println("Hello World!");
        CoAPEMQXAdapterMapper coap = new CoAPEMQXAdapterMapper();
		   /****************************************
         * coap connection test
         ***************************************/
        boolean flag = true;

		
       // mqtt.setNewClient();
        while(flag) {
        	System.out.println( "enter a number: 0 for reset, 1 for PUBLISH, 2 for SUBSCRIBE, 3 for shutdown");
        	int choose= input.nextInt();
        	System.out.println( "you choose " + choose );
        	switch(choose) {
        	case 0:
        		coap.reset();
        		continue;
        	case 1:
        		ans = coap.processSymbol("PUBLISH");
        		logcoap.info( "the answer is " + ans );
        		System.out.println( "the answer is " + ans );
        		continue;
        	case 2:
        		ans = coap.processSymbol("SUBSCRIBE");
        		logcoap.info( "the answer is " + ans );
        		continue;
        	case 3:
        		ans = coap.processSymbol("SHUTDOWN");
        		logcoap.info( "the answer is " + ans );
        		continue;
        	default:
        		flag = false;
        	}
        	
        }
        
       
        System.out.println("stop loop");
        //client.sendDisconnect();
        //client.close();
    
		
	}

	public String processSymbol(String symbol) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
				String inAction = symbol;
				System.out.println("**********************************processing the input" + inAction);
				logcoap.info("**********************************processing the input" + inAction);
				logcoap.info("haveclinet is " + haveclient);
				if(haveclient) {
					
				}else {
					setNewClient(); // 
					//initClient();  
					haveclient = true;
				}
				
				try{
					if(inAction.equals("PUBLISH")) {
						return publish();
					}else if(inAction.equals("SUBSCRIBE"))
					{
						return subscribe();
					}else if(inAction.equals("SHUTDOWN")) {
						return shutdown();
					}
//					else if(inAction.equals("PUBREC")) { //
//						return receivePublish();
//					}

					else {
						System.out.println("Unknown input symbol (" + inAction + ")...");
						throw new RuntimeException("Unknown input Symbol (" + inAction + ")...");
						
					}
				} catch (Exception e) {
					e.printStackTrace();
					logcoap.warning( e.toString());
					return "UNKNOWEXCEPTION";
				}	
		
	}

	public void setType(String type) {
		// TODO Auto-generated method stub
		this.type = type;
	}

	public void setHost(String HOST) {
		// TODO Auto-generated method stub
		this.host = HOST;
	}

	public void setUsername(String userName2) {
		// TODO Auto-generated method stub
		this.username = userName2;
	}

	public void setPassword(String passWord2) {
		// TODO Auto-generated method stub
		this.password = passWord2;
	}

	public void setClientID(String clientId2) {
		// TODO Auto-generated method stub
		this.clientId = clientId2;
	}

	public void setSubTopic(String subTopic2) {
		// TODO Auto-generated method stub
		this.subtopic = subTopic2;
	}

	public void setPubTopic(String pubTopic2) {
		// TODO Auto-generated method stub
		this.pubtopic = pubTopic2;
	}

	public void setPubMsg(String pubMsg) {
		// TODO Auto-generated method stub
		this.pubMsg = pubMsg;
	}
}
