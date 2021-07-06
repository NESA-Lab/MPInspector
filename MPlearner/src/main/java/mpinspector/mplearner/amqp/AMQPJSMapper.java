package mpinspector.mplearner.amqp;

import java.io.BufferedReader;
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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class AMQPJSMapper {
	static Logger logmqtt = Logger.getLogger("logmqtt.log");
	FileHandler fileHandler;
    InputStream is = null;
    BufferedReader br = null;
    String result = null;
	boolean haveclient = false;
	String type = null;
	String outAction;
	String host = "platform/xx.yml";
	String localport = "9123";
			//"http://127.0.0.1:9126/amqp/";
	
	
	public void initLog() {
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
	
	public void setNewClient() {
		logmqtt.info("Start a new client and load the configuration ");
		//String para = null;
	//	logmqtt.info("type = "+ type );
		//if(type.contains("mqttazure") ) {
			logmqtt.info("the testhost is"+host);
			///mqtt/initClient/?filename=platforms/azure/dev1.yml
			String result = AMQPJSMapper.get("http://127.0.0.1:"+localport+"/amqp/init?filename="+host);
			System.out.println("http://127.0.0.1:"+localport+"/amqp/init?filename="+host);
			System.out.println("set new client result is "+ result);
			logmqtt.info("A new client is built");
	}
	
	public void reset() {  //reset done
		logmqtt.info("haveclinet is " + haveclient);
		logmqtt.info("start reset ");
		if(haveclient) {
			
		}else {
			setNewClient(); // 
			//initClient();   //
			haveclient = true;
		}
		logmqtt.info("send reset ");
		String result = AMQPJSMapper.get("http://127.0.0.1:"+localport+"/amqp/reset");
		System.out.println("http://127.0.0.1:"+localport+"/amqp/reset");
		System.out.println("response from result is "+ result);
//		System.out.println(result);
		String tmp = "opened";
		while(!(result.contains(tmp))) {
			System.out.println("response from result is "+ result+" try reset again");
			result = AMQPJSMapper.get("http://127.0.0.1:"+localport+"/amqp/reset");
			//System.out.println(result);
		}
		logmqtt.info("reset finished and success ");
	}
	
	
	public String sendHeader3()
	{
		logmqtt.info("Start send header with 3 ");
		String result = AMQPJSMapper.get("http://127.0.0.1:"+localport+"/amqp/send/header/3");
		return processResult("HEADER3",result);
	}
	public String sendSASLInit()
	{
		logmqtt.info("Start send SASL Init ");
		String result = AMQPJSMapper.get("http://127.0.0.1:"+localport+"/amqp/send/sasl_init");
		return processResult("SASLINIT",result);
	}
	
	
	
	public String sendHeader0() {
		logmqtt.info("Start send header with 0 ");
		String result = AMQPJSMapper.get("http://127.0.0.1:"+localport+"/amqp/send/header/0");
		return processResult("HEADER0", result);
	}
	
	
	public String sendOpen() {
		logmqtt.info("Start send open ");
		String result = AMQPJSMapper.get("http://127.0.0.1:"+localport+"/amqp/send/open");
		return processResult("OPEN",result);
	}
	
	
	public String sendBegin() {
		logmqtt.info("Start send begin ");
		String result = AMQPJSMapper.get("http://127.0.0.1:"+localport+"/amqp/send/begin");
		return processResult("BEGIN", result);
	}
	
	
	
	public String sendAttach() {
		logmqtt.info("Start send attach ");
		String result = AMQPJSMapper.get("http://127.0.0.1:"+localport+"/amqp/send/attach");
		System.out.println(result);
		return processResult("ATTACH", result);

//		logmqtt.info("The result from connect is  "+result);
//		if(result.contains("connack")) {
//			return "CONNACK";
//		}else if (result.contains("closed")){
//			return "ConnectionClosed";
//		}else {
//			return result;
//		}
		//return outAction;
	}
	
	
	public String sendTransfer() {
		logmqtt.info("Start send transfer ");
		String result = AMQPJSMapper.get("http://127.0.0.1:"+localport+"/amqp/send/transfer");
		return processResult("TRANSFER", result);
	}
	
	
	public String sendDetach() {
		logmqtt.info("Start send detach ");
		String result = AMQPJSMapper.get("http://127.0.0.1:"+localport+"/amqp/send/detach");
		return processResult("DETACH", result);
		//return outAction;
	}
	
	
	public String sendEnd() {
		logmqtt.info("Start send end ");
		String result = AMQPJSMapper.get("http://127.0.0.1:"+localport+"/amqp/send/end");
		return processResult("END",result);
	}
	
	public String sendClose() {
		logmqtt.info("Start send close ");
		String result = AMQPJSMapper.get("http://127.0.0.1:"+localport+"/amqp/send/close");
		return processResult("CLOSE",result);
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
			logmqtt.info("when perform get result is Request exception" + e.toString());
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
	
	public static void main(String[] args) throws Exception{
		String ans = null;
		System.out.println( "Hello World!" );
        Scanner input = new Scanner(System.in);
        System.out.println("Hello World!");
        AMQPJSMapper amqp = new AMQPJSMapper();
		   /****************************************
         * Test AMQP connection
         ***************************************/
        boolean flag = true;
       // mqtt.setNewClient();
        while(flag) {
        	System.out.println( "enter a number: 0 for reset, 1 for header3, 2 for sasl_init, 3 for header0, 4 for open, 5 for begin, 6 for attach, 7 for transfer, 8 for detach, 9 for end, 10 for close" );
        	int choose= input.nextInt();
        	System.out.println( "you choose " + choose );
        	switch(choose) {
        	case 0:
        		amqp.reset();
        		continue;
        	case 1:
        		ans = amqp.processSymbol("HEADER3");
        		logmqtt.info( "the answer is " + ans );
        		System.out.println( "the answer is " + ans );
        		continue;
        	case 2:
        		ans = amqp.processSymbol("SASLINIT");
        		logmqtt.info( "the answer is " + ans );
        		continue;
        	case 3:
        		ans = amqp.processSymbol("HEADER0");
        		logmqtt.info( "the answer is " + ans );
        		continue;
        	case 4:
        		ans = amqp.processSymbol("OPEN");
        		logmqtt.info( "the answer is " + ans );
        		continue;
        	case 5:
        		ans = amqp.processSymbol("BEGIN");
        		logmqtt.info( "the answer is " + ans );
        		continue;
        	case 6:
        		ans = amqp.processSymbol("ATTACH");
        		logmqtt.info( "the answer is " + ans );
        		continue;
        	case 7:
        		ans = amqp.processSymbol("TRANSFER");
        		logmqtt.info( "the answer is " + ans );
        		continue;
        	case 8:
        		ans = amqp.processSymbol("DETACH");
        		logmqtt.info( "the answer is " + ans );
        		continue;
        	case 9:
        		ans = amqp.processSymbol("END");
        		logmqtt.info( "the answer is " + ans );
        		continue;
        	case 10:
        		ans = amqp.processSymbol("CLOSE");
        		logmqtt.info( "the answer is " + ans );
        		continue;
        	default:
        		flag = false;
        	}
        	
        }
        
       
        System.out.println("stop loop");
        //client.sendDisconnect();
        //client.close();
    

//		mqtt.setNewClient();
//		mqtt.reset();
//		mqtt.sendConnect();
		
	}

	public String processSymbol(String symbol) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
				String inAction = symbol;
				System.out.println("**********************************Processing the input " + inAction);
				logmqtt.info("**********************************Processing the input " + inAction);
				logmqtt.info("haveclinet is " + haveclient);
				if(haveclient) {
					
				}else {
					setNewClient(); // 
					//initClient();   
					haveclient = true;
				}
				
				try{
					if(inAction.equals("HEADER3")) {
						outAction = sendHeader3();
						logmqtt.info( "the answer is " + outAction );
						return outAction;
					}else if(inAction.equals("SASLINIT"))
					{
						outAction = sendSASLInit();
						logmqtt.info( "the answer is " + outAction );
						return outAction;
					}else if(inAction.equals("HEADER0")) {
						outAction = sendHeader0();
						logmqtt.info( "the answer is " + outAction );
						return outAction;
					}else if(inAction.equals("OPEN")) {
						outAction = sendOpen();
						logmqtt.info( "the answer is " + outAction );
						return outAction;
					}
					else if(inAction.equals("BEGIN")) { 
						return sendBegin();
					}
					else if(inAction.equals("ATTACH")) { 
						outAction = sendAttach();
						logmqtt.info( "the answer is " + outAction );
						return outAction;
					}
					else if(inAction.equals("TRANSFER")) { 
						outAction = sendTransfer();
						logmqtt.info( "the answer is " + outAction );
						return outAction;
					}
					else if(inAction.equals("DETACH")) { 
						outAction = sendDetach();
						logmqtt.info( "the answer is " + outAction );
						return outAction;
					}
					else if(inAction.equals("END")) { 
						outAction = sendEnd();
						logmqtt.info( "the answer is " + outAction );
						return outAction;
					}
					else if(inAction.equals("CLOSE")) { 
						outAction = sendClose();
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
	
	
	String processResult(String input, String result) {
		String result_refined = "Empty";
		System.out.println(result);
		logmqtt.info("The result from " +input+" is  "+result);
		JSONParser parser = new JSONParser();
		try {
			JSONObject resultjson = (JSONObject) parser.parse(result);
			//System.out.println(resultjson);
			result_refined = (String) resultjson.get("type");
			if(result_refined.contains("header")) {
				long tmp = (long) resultjson.get("protocol_id");
				result_refined = result_refined + String.valueOf(tmp);
			}
			else if(result_refined.contains("open")) {
				
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println(result_refined);
		return result_refined;
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
