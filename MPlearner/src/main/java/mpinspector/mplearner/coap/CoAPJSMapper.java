package mpinspector.mplearner.coap;

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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class CoAPJSMapper {
	static Logger logcoapjs = Logger.getLogger("coapjsoutput.log");
	FileHandler fileHandler;
	// 
    InputStream is = null;
    BufferedReader br = null;
    String result = null;
	boolean haveclient = false;
	String type = null;
	String outAction;
	String host = "platforms/xxx/xxx.yml";
	String localport = "9123";
	
	public void initLog() {
		try {
			fileHandler = new FileHandler("testcoap.log");
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
        
        logcoapjs.addHandler(fileHandler);
        logcoapjs.setUseParentHandlers(false);
	}
	
	public void setNewClient() {
		String result = CoAPJSMapper.get("http://127.0.0.1:"+localport+"/coap/init?filename="+host);
		System.out.println(result);
	}
	
	public void reset() {
		logcoapjs.info("haveclinet is " + haveclient);
		if(haveclient) {
			
			
		}else {
			setNewClient(); 
			//initClient();   
			haveclient = true;
		}
		String result = CoAPJSMapper.get("http://127.0.0.1:"+localport+"/coap/reset");
		System.out.println(result);
		
		
	}
	public String sendGETCON() {
		
		String result = CoAPJSMapper.get("http://127.0.0.1:"+localport+"/coap/send/GET?type=con");
		System.out.println(result);
		String ans = null;
		
		JSONParser parser = new JSONParser();
		try {
			JSONObject responsejson = (JSONObject) parser.parse(result);
			JSONObject messagejsson = (JSONObject) responsejson.get("message");
			ans = (String) messagejsson.get("code");
			if(ans.contains("2.05")) {
				outAction = "GETACK";
			}else {
				outAction = "GETRC"+ans;
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		logcoapjs.info("The result from GETCON is  "+ outAction);
		return outAction;
		
	}
	
	public String sendPOSTCON() {
		String result = CoAPJSMapper.get("http://127.0.0.1:"+localport+"/coap/send/POST?type=con");
		System.out.println(result);
		String ans = null;
		
		JSONParser parser = new JSONParser();
		try {
			JSONObject responsejson = (JSONObject) parser.parse(result);
			JSONObject messagejsson = (JSONObject) responsejson.get("message");
			ans = (String) messagejsson.get("code");
			if(ans.contains("2.05")) {
				outAction = "POSTACK";
			}else {
				outAction = "POSTRC"+ans;
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		logcoapjs.info("The result from POSTCON is  "+ outAction);
		return outAction;
		
	}
	
	public String sendDELETECON() {
		String result = CoAPJSMapper.get("http://127.0.0.1:"+localport+"/coap/send/DELETE?type=con");
		System.out.println(result);
		String ans = null;
		
		JSONParser parser = new JSONParser();
		try {
			JSONObject responsejson = (JSONObject) parser.parse(result);
			JSONObject messagejsson = (JSONObject) responsejson.get("message");
			ans = (String) messagejsson.get("code");
			if(ans.contains("2.05")) {
				outAction = "DELETEACK";
			}else {
				outAction = "DELETERC"+ans;
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		logcoapjs.info("The result from DELETECON is  "+ outAction);
		return outAction;
	}
	
	public String sendPUTCON() {
		String result = CoAPJSMapper.get("http://127.0.0.1:"+localport+"/coap/send/PUT?type=con");
		System.out.println(result);
		String ans = null;
		
		JSONParser parser = new JSONParser();
		try {
			JSONObject responsejson = (JSONObject) parser.parse(result);
			JSONObject messagejsson = (JSONObject) responsejson.get("message");
			ans = (String) messagejsson.get("code");
			if(ans.contains("2.05")) {
				outAction = "PUTACK";
			}else {
				outAction = "PUTRC"+ans;
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		logcoapjs.info("The result from PUTCON is  "+ outAction);
		return outAction;
		
	}
	
	public static String get(String url){
		String result = "";
		HttpGet get = new HttpGet(url);
		try{
			CloseableHttpClient httpClient = HttpClients.createDefault();
			
			HttpResponse response = httpClient.execute(get);
			result = getHttpEntityContent(response);
			
			if(response.getStatusLine().getStatusCode()!=HttpStatus.SC_OK){
				result = "server exception";
				logcoapjs.info("when perform get result is server exception" );
			}
		} catch (Exception e){
			System.out.println("request exception");
			logcoapjs.info("when perform get result is request exception " + e.toString());
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
        CoAPJSMapper coapjs = new CoAPJSMapper();
		   /****************************************
         * test connection
         ***************************************/
        boolean flag = true;
       // mqtt.setNewClient();
        while(flag) {
        	System.out.println( "enter a number: 0 for reset, 1 for Connect, 2 for Subscribe, 3 for unsubscribe, 4 for disconnect, 5 for PUBLISH" );
        	int choose= input.nextInt();
        	System.out.println( "you choose " + choose );
        	switch(choose) {
        	case 0:
				coapjs.reset();
        		continue;
        	case 1:
        		ans = coapjs.processSymbol("CONNECT");
        		logcoapjs.info( "the answer is " + ans );
        		System.out.println( "the answer is " + ans );
        		continue;
        	case 2:
        		ans = coapjs.processSymbol("SUBSCRIBE");
        		logcoapjs.info( "the answer is " + ans );
        		continue;
        	case 3:
        		ans = coapjs.processSymbol("UNSUBSCRIBE");
        		logcoapjs.info( "the answer is " + ans );
        		continue;
        	case 4:
        		ans = coapjs.processSymbol("DISCONNECT");
        		logcoapjs.info( "the answer is " + ans );
        		continue;
        	case 5:
        		ans = coapjs.processSymbol("PUBLISH");
        		logcoapjs.info( "the answer is " + ans );
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
				System.out.println("**********************************Processing the input" + inAction);
				logcoapjs.info("**********************************Processing the input" + inAction);
				logcoapjs.info("haveclinet is " + haveclient);
				if(haveclient) {
					
				}else {
					setNewClient(); 
					//initClient();   
					haveclient = true;
				}
				
				try{
					if(inAction.equals("GETCON")) {
						return sendGETCON();
					}else if(inAction.equals("POSTCON"))
					{
						return sendPOSTCON();
					}else if(inAction.equals("PUTCON")) {
						return sendPUTCON();
					}else if(inAction.equals("DELETECON")) {
						return sendDELETECON();
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
					logcoapjs.warning( e.toString());
					return outAction;
				}	
		
	}

	public void setType(String type) {
		// TODO Auto-generated method stub
		this.type = type;
	}

	public void setHost(String host2) {
		// TODO Auto-generated method stub
		this.host = host2;
	}
	
	public void setLocalPort(String localport) {
		this.localport = localport;
	}




}
