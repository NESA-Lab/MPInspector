package mpinspector.mplearner.mqtt;

import java.io.IOException;

import mpinspector.mplearner.LearningConfig;

public class MPConfig extends LearningConfig{
	String alphabet;
	String target;
	String host;
	//int port;
	String userName;
	String passWord;
	String clientId;
	
	
	String clientCrtFilePath;
	String caFilePATH;
	String ClientKeyFilePAth;

	String localport;
	public String subTopic;
	String pubTopic;
	String pubMsg;
	
	String type;
	String tlsversion;
	String version;   //mqttversion
	
	
	boolean restart;   //not used
	boolean console_output;  //not used
	int timeout;
	int aliveInterval;
	int delaytime;
	String adaptertype;
	
	public MPConfig(String filename) throws IOException{
		super(filename);
	}
	public MPConfig(LearningConfig config) {
		super(config);
	}
	
	@Override
	public void loadProperties() {
		super.loadProperties();

		if(properties.getProperty("alphabet") != null)
			alphabet = properties.getProperty("alphabet");
		
		if(properties.getProperty("type") != null)
			type = properties.getProperty("type");
		
		if(properties.getProperty("target").equalsIgnoreCase("client") || properties.getProperty("target").equalsIgnoreCase("server")) {
			target = properties.getProperty("target").toLowerCase();
		}
		else {
			//System.out.print("target in mqtt config is" + properties.getProperty("target"));
			target = "mqttconfigempty";
		}
		
		
		
		if(properties.getProperty("version") != null)
			version = properties.getProperty("version");
		else
			version = "";
		
		if(properties.getProperty("host") != null)
			host = properties.getProperty("host");
		
		if(properties.getProperty("adaptertype") != null)
			adaptertype = properties.getProperty("adaptertype");
		
//		if(properties.getProperty("port") != null)
//			port = Integer.parseInt(properties.getProperty("port"));
//		
		if(properties.getProperty("userName") != null)
			userName = properties.getProperty("userName");
		else
			userName = "";
		
		if(properties.getProperty("passWord") != null)
			passWord = properties.getProperty("passWord");
		else
			passWord = "";

		if(properties.getProperty("clientCrtFilePath") != null)
			clientCrtFilePath = properties.getProperty("clientCrtFilePath");
		else
			clientCrtFilePath = "clientCrtFilePathnotexist";
		
		if(properties.getProperty("caFilePATH") != null)
			caFilePATH = properties.getProperty("caFilePATH");
		else
			caFilePATH = "caFilePATHnotexist";
		
		if(properties.getProperty("ClientKeyFilePAth") != null)
			ClientKeyFilePAth = properties.getProperty("ClientKeyFilePAth");
		else
			ClientKeyFilePAth = "ClientKeyFilePAthnotexist";
		
		if(properties.getProperty("clientId") != null)
			clientId = properties.getProperty("clientId");
		else
			clientId = "noid";
		
		if(properties.getProperty("subtopic") != null)
		{
			subTopic = properties.getProperty("subtopic");
			//System.out.println("subtopic: in config" + subTopic);
		}
		else
			subTopic = "nutopic";
		
		if(properties.getProperty("pubMsg") != null)
		{
			pubMsg = properties.getProperty("pubMsg");
			//System.out.println("subtopic: in config" + subTopic);
		}
		else
			pubMsg = "empty";

		if(properties.getProperty("pubtopic") != null)
		{
			pubTopic = properties.getProperty("pubtopic");
			//System.out.println("subtopic: in config" + subTopic);
		}
		else
			pubTopic = "nutopic";		
		
		if(properties.getProperty("console_output") != null)
			console_output = Boolean.parseBoolean(properties.getProperty("console_output"));
		else
			console_output = false;
		
		if(properties.getProperty("restart") != null)
			restart = Boolean.parseBoolean(properties.getProperty("restart"));
		else
			restart = false;
		
		if(properties.getProperty("timeout") != null) {
			timeout = Integer.parseInt(properties.getProperty("timeout"));
		}
		if(properties.getProperty("delaytime") != null) {
			delaytime = Integer.parseInt(properties.getProperty("delaytime"));
		}
		
		if(properties.getProperty("aliveInterval") != null) {
			aliveInterval = Integer.parseInt(properties.getProperty("aliveInterval"));
		}
		
		if(properties.getProperty("tlsversion") != null)
			tlsversion = properties.getProperty("tlsversion");
		
		
		if(properties.getProperty("localport") != null)
			localport = properties.getProperty("localport");
	}

}
