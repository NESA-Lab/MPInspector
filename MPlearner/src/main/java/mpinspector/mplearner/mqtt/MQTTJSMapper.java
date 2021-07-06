package mpinspector.mplearner.mqtt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;



public class MQTTJSMapper {
	static Logger logmqtt = Logger.getLogger("mqttoutput.log");
	FileHandler fileHandler;
 
    String result = null;
	boolean haveclient = false;
	String type = null;
	String outAction;
	String host = "platforms/xxx/xxx.yml";
	private String localport = "9123";
	
	void initLog() {
		try {
			fileHandler = new FileHandler("testmqtt.log");
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
		logmqtt.info("Start a new client and load the configuration ");
		//String para = null;
		logmqtt.info("type = "+ type );
		//if(type.contains("mqttazure") ) {
			System.out.println("http://127.0.0.1:"+localport+"/mqtt/initClient/?filename="+host);
			///mqtt/initClient/?filename=platforms/azure/dev1.yml
			//String link = "http://127.0.0.1:"+localport+"/mqtt/initClient/?filename="+host;
			String result = MQTTJSMapper.get("http://127.0.0.1:"+localport+"/mqtt/initClient/?filename="+host);
			System.out.println(result);
			logmqtt.info("A new client is built ");

	}
	
	public void reset() {
		logmqtt.info("haveclinet is " + haveclient);
		logmqtt.info("start reset ");
		if(haveclient) {
			
		}else {
			setNewClient(); // 
			//initClient();   //
			haveclient = true;
		}
		logmqtt.info("send reset ");
		System.out.println("http://127.0.0.1:"+localport+"/mqtt/reset");
		String result = MQTTJSMapper.get("http://127.0.0.1:"+localport+"/mqtt/reset");
		System.out.println("response from result is "+ result);
		System.out.println(result);
		String tmp = "ok";
		while(!(result.contains(tmp))) {
			System.out.println("response from result is "+ result+" try reset again");
			result = MQTTJSMapper.get("http://127.0.0.1:"+localport+"/mqtt/reset");
			//System.out.println(result);
		}
		logmqtt.info("reset finished and success ");
	}
	
	public String sendConnect() {
		logmqtt.info("Start connect ");
		String result = MQTTJSMapper.get("http://127.0.0.1:"+localport+"/mqtt/connect");
		System.out.println(result);
		logmqtt.info("The result from connect is  "+result);
		if(result.contains("connack")) {
			return "CONNACK";
		}else if (result.contains("closed")){
			return "ConnectionClosed";
		}else {
			return result;
		}
		
	}
	
	public String sendSubscribe() {
		logmqtt.info("Start subscribe ");
		System.out.println("http://127.0.0.1:"+localport+"/mqtt/subscribe");
		String result = MQTTJSMapper.get("http://127.0.0.1:"+localport+"/mqtt/subscribe");
		System.out.println(result);
		logmqtt.info("The result from subscribe is  "+result);
		if(result.contains("success")) {
			return "SUBACK";
		}else if (result.contains("closed")){
			return "ConnectionClosed";
		}else {
			return result;
		}
	}
	
	public String sendUnsubscribe() {
		logmqtt.info("Start subscribe ");
		String result = MQTTJSMapper.get("http://127.0.0.1:"+localport+"/mqtt/unsubscribe");
		System.out.println(result);
		logmqtt.info("The result from unsubscribe is  "+result);
		if(result.contains("success")) {
			return "UNSUBACK";
		}else if (result.contains("closed")){
			return "ConnectionClosed";
		}else {
			return result;
		}
	}
	
	public String sendPublish() {
		logmqtt.info("Start publish ");
		String result = MQTTJSMapper.get("http://127.0.0.1:"+localport+"/mqtt/publish");
		System.out.println(result);
		logmqtt.info("The result from publish is  "+result);
		if(result.contains("success")){
			return "PUBACK";
		}else if (result.contains("closed")){
			return "ConnectionClosed";
		}else {
			return result;
		}
		
	}
	
	public String sendDisconnect() {
		logmqtt.info("Start disconnect ");
		String result = MQTTJSMapper.get("http://127.0.0.1:"+localport+"/mqtt/disconnect");
		System.out.println(result);
		logmqtt.info("The result from disconnect is  "+result);
		if(result.contains("success")) {
			return "DISCONNACK";
		}else if (result.contains("closed")){
			return "ConnectionClosed";
		}else {
			return result;
		}
		
	}
	
	public static String get(String url){
		String result = "";
		HttpGet get = new HttpGet(url);
		try{
			CloseableHttpClient httpClient = HttpClients.createDefault();
			
			HttpResponse response = httpClient.execute(get);
			result = getHttpEntityContent(response);
			
			if(response.getStatusLine().getStatusCode()!=HttpStatus.SC_OK){
				result = "Server exception";
				logmqtt.info("when perform get result is Server exception" );
			}
		} catch (Exception e){
			System.out.println("Request exception");
			logmqtt.info("when perform get result is Request exception " + e.toString());
			throw new RuntimeException(e);
		} finally{
			get.abort();
		}
		return result;
	}
	
	public static String getHttpEntityContent(HttpResponse response) throws UnsupportedOperationException, IOException{
		String result = "";
		HttpEntity entity = response.getEntity();
		if(entity != null){
			InputStream in = entity.getContent();
			BufferedReader br = new BufferedReader(new InputStreamReader(in, "utf-8"));
			StringBuilder strber= new StringBuilder();
			String line = null;
			while((line = br.readLine())!=null){
				strber.append(line+'\n');
			}
			br.close();
			in.close();
			result = strber.toString();
		}
		
		return result;
		
	}
	
	
	
	//test for service
//	public static void main(String[] args) throws Exception{
//		String ans = null;
//		System.out.println( "Hello World!" );
//        Scanner input = new Scanner(System.in);
//        System.out.println("Hello World!");
//        MQTTJSMapper mqtt = new MQTTJSMapper();
//		   /****************************************
//         * Test MQTT connection
//         ***************************************/
//        boolean flag = true;
//       // mqtt.setNewClient();
//        while(flag) {
//        	System.out.println( "enter a number: 0 for reset, 1 for Connect, 2 for Subscribe, 3 for unsubscribe, 4 for disconnect, 5 for PUBLISH" );
//        	int choose= input.nextInt();
//        	System.out.println( "you choose " + choose );
//        	switch(choose) {
//        	case 0:
//				mqtt.reset();
//        		continue;
//        	case 1:
//        		ans = mqtt.processSymbol("CONNECT");
//        		logmqtt.info( "the answer is " + ans );
//        		System.out.println( "the answer is " + ans );
//        		continue;
//        	case 2:
//        		ans = mqtt.processSymbol("SUBSCRIBE");
//        		logmqtt.info( "the answer is " + ans );
//        		continue;
//        	case 3:
//        		ans = mqtt.processSymbol("UNSUBSCRIBE");
//        		logmqtt.info( "the answer is " + ans );
//        		continue;
//        	case 4:
//        		ans = mqtt.processSymbol("DISCONNECT");
//        		logmqtt.info( "the answer is " + ans );
//        		continue;
//        	case 5:
//        		ans = mqtt.processSymbol("PUBLISH");
//        		logmqtt.info( "the answer is " + ans );
//        		continue;
//        	default:
//        		flag = false;
//        	}
//        	
//        }
//        System.out.println("stop loop");
//        //client.sendDisconnect();
//        //client.close();
////		mqtt.setNewClient();
////		mqtt.reset();
////		mqtt.sendConnect();
//		
//	}
	
	
	
	
	//MQTTExpireTest
	public static void main(String[] args) throws Exception{
		String ansbefore = null;
		String ansafter = null;
		boolean connectcheck = false;
		boolean subscribecheck = false;
		boolean publishcheck = false;
		System.out.println( "Hello World!" );
        Scanner input = new Scanner(System.in);
        System.out.println("Hello World!");
        MQTTJSMapper mqtt = new MQTTJSMapper();
		   /****************************************
         * Test MQTT Expiretime
         * update the config in JS client file
         * 1 min
         ***************************************/
        //Test connect, replay the connect request
        mqtt.reset();
        ansbefore = mqtt.processSymbol("CONNECT");
        mqtt.reset();
        Thread.sleep(60000);
        ansafter = mqtt.processSymbol("CONNECT");
        //Test subscribe, replay the subscribe request
        if(ansbefore == ansafter) {
        	connectcheck = false;
        }else {
        	connectcheck = true;
        }
        
        //Test subscribe
        mqtt.reset();
        ansbefore = mqtt.processSymbol("SUBSCRIBE");
        Thread.sleep(60000);
        ansafter = mqtt.processSymbol("SUBSCRIBE");
        if(ansbefore == ansafter) {
        	subscribecheck = false;
        }else {
        	subscribecheck = true;
        }
        
        //Test publish
        mqtt.reset();
        ansbefore = mqtt.processSymbol("PUBLISH");
        Thread.sleep(60000);
        ansafter = mqtt.processSymbol("PUBLISH");
        if(ansbefore == ansafter) {
        	publishcheck = false;
        }else {
        	publishcheck = true;
        }
        
        /******************
         * Store the result into txt
         *******************/
        try {
	    	String savefilepath = "checktimevalueresult.txt";
			File file1 = new File(savefilepath);
			if (!file1.exists()) {
		        System.out.println("File is not exist");
		        file1.createNewFile();
		    }
			FileWriter fileWritter = new FileWriter(file1,false);
			fileWritter.write("connectcheck="+connectcheck+",subscribecheck="+subscribecheck+"publishcheck="+publishcheck);
			fileWritter.close();
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        //Test publish, replay the subscribe request
        
	}
	
	
	

	String processSymbol(String symbol) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
				String inAction = symbol;
				System.out.println("**********************************Processing the input " + inAction);
				logmqtt.info("**********************************Processing the input " + inAction);
				logmqtt.info("haveclinet is " + haveclient);
				if(haveclient) {
					
				}else {
					setNewClient(); 
					//initClient();   
					haveclient = true;
				}
				
				try{
					if(inAction.equals("CONNECT")) {
						outAction = sendConnect();
						logmqtt.info( "the answer is " + outAction );
						return outAction;
					}else if(inAction.equals("SUBSCRIBE"))
					{
						outAction = sendSubscribe();
						logmqtt.info( "the answer is " + outAction );
						return outAction;
					}else if(inAction.equals("UNSUBSCRIBE")) {
						outAction = sendUnsubscribe();
						logmqtt.info( "the answer is " + outAction );
						return outAction;
					}else if(inAction.equals("DISCONNECT")) {
						outAction = sendDisconnect();
						logmqtt.info( "the answer is " + outAction );
						return outAction;
					}
//					else if(inAction.equals("PUBREC")) { //
//						return receivePublish();
//					}
					else if(inAction.equals("PUBLISH")) { 
						outAction = sendPublish();
						logmqtt.info( "the answer is " + outAction );
						return outAction;
					}
					else {
						System.out.println("Unknown input symbol (" + inAction + ")...");
						throw new RuntimeException("Unknown input Symbol (" + inAction + ")...");
					}
				} catch (Exception e) {
					e.printStackTrace();
					logmqtt.warning( e.toString());
					return outAction;
				}	
		
	}

	public void setType(String type) {
		// TODO Auto-generated method stub
		this.type = type;
	}

	public void setHost(String host2) {
		// TODO Auto-generated method stub
		this.host = replaceBlank(host2);
	}
	
	public void setLocalPort(String localport) {
		// TODO Auto-generated method stub
		this.localport  = replaceBlank(localport);
	}
	
	
	public static String replaceBlank(String str) {
		String dest = "";
		if (str!=null) {
			Pattern p = Pattern.compile("\\s*|\t|\r|\n");
			Matcher m = p.matcher(str);
			dest = m.replaceAll("");
		}
		return dest;
	}

}
