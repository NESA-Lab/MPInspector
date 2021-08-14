package main.java.mpinspector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;



import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

import mqtt.parser.header.impl.*;
import mqtt.parser.MQParser;
import mqtt.parser.avps.Topic;
import mqtt.parser.header.api.MQMessage;

public class MQTTSemantics {
	/********************
	 * handle MQTT packet in binary file
	 * output: an semantics map
	 ******************/
	static String platformtype = "bosch";
	
	public void setPlatformtype(String platformtype) {
		this.platformtype = platformtype;
	}

	public static void main( String[] args ) //args0: platform
    {
		MQTTSemantics semantic = new MQTTSemantics();
		// semantic.setPlatformtype("gcp");
		// tuya  aws  gcp  alitls  alitcp azure bosch
		semantic.setPlatformtype(args[0]);
		//load the traffic file 
		//String path_filedir = "iot prtocol project\\trafficanalysis_mqtt\\"+semantic.platformtype+"\\";
		//String path_filedir = "mediaresultFile"+semantic.platformtype+"\\";
		String path_filedir = "../traffic_logs/"+semantic.platformtype+"/";
		String output_dir = "../traffic_analysis/"+semantic.platformtype+"/";
		//load the txt file of a platform
		//String text_filedir = "mediaresultFile\\automation customized crypto function\\"+semantic.platformtype+".txt";
		//String text_filedir = "iot prtocol project\\automation customized crypto function\\"+semantic.platformtype+".txt";
		//load coap file
		//String code_filedir = "iot prtocol project\\automation customized crypto function\\"+semantic.platformtype+".java";
		//String code_filedir = "mediaresultFile\\"+semantic.platformtype+".java";
    	
		//handle the state machine
		StateMachineRefine refinedot = null;
		
		
		//calculate the time
		long starttime =System.currentTimeMillis();
		
		
		List<MQTTPacket> packets = new ArrayList<MQTTPacket>();
		List<MQTTPacket> packets_in_a_log = new ArrayList<MQTTPacket>();
    	//List<Term> terms = new ArrayList<Term>();
		
		
		//
    	Map<String, List<String>> con_map = new HashMap<String, List<String>>();
    	Map<String, List<String>> conack_map = new HashMap<String, List<String>>();
    	Map<String, List<String>> sub_map = new HashMap<String, List<String>>();
    	Map<String, List<String>> suback_map = new HashMap<String, List<String>>();
    	Map<String, List<String>> unsub_map = new HashMap<String, List<String>>();
    	Map<String, List<String>> unsuback_map = new HashMap<String, List<String>>();
    	Map<String, List<String>> pub_map = new HashMap<String, List<String>>();
    	Map<String, List<String>> puback_map = new HashMap<String, List<String>>();
    	
    	
    	List<Map> packets_terms = new ArrayList<Map>();
    	packets_terms.add(con_map);
    	packets_terms.add(conack_map);
    	packets_terms.add(sub_map);
    	packets_terms.add(suback_map);
    	packets_terms.add(unsub_map);
    	packets_terms.add(unsuback_map);
    	packets_terms.add(pub_map);
    	packets_terms.add(puback_map);
    	
    	Map<String, List<String>> packets_abterms = new HashMap<String, List<String>>();
    	Map<String, JSONObject> encrypt_terms = new HashMap<>(); 
    	List<String> packet_names = new ArrayList<String>();
    	packet_names.add("CONNECT");
    	packet_names.add("CONNACK");
    	packet_names.add("SUBSCRIBE");
    	packet_names.add("SUBACK");
    	packet_names.add("UNSUBSCRIBE");
    	packet_names.add("UNSUBACK");
    	packet_names.add("PUBLISH");
    	packet_names.add("PUBACK");
    	
    	Map<String, List<String>> abwords_map = new HashMap<String, List<String>>();
    	//store the relationship betweem terms and words  password -> jwt(iat, exp, aud)
    	//topic -> v9, devices, v12, qos
    	Map<String, String> raw_words = new HashMap<String,String>();
    	Map<String, Object> raw_values = new HashMap<String,Object>();
    	//Map<String, String> words_map = new HashMap<String,String>();
    	//abwords_map: a Map <String, List<String>> a map, terms to word
		//example <password, <(jwt<a,b,c>,tokenkey,alg)>>;
    	//raw_words, a Map <String, String> a map, words to related value
    	
    	//example: <a, 1223845>, <b,123456>
//    	Map<String,String> packet_words_map = new HashMap<String,String>();
    	//packet_words_map string-string
    	//output: connect-(username,password(),clientID())
    
    	File file = new File(path_filedir);
    	String logs[];
    	logs = file.list();
    	for(int i =0; i< logs.length; i++) {
    		//System.out.println(logs[i]);
    		if(logs[i].contains(".log")) {
    			String path = path_filedir + logs[i];
    			MQTTparsertest(path, packets); //extract all packets
    		}
    	}
    	
    	
    	//packets is a list of all packets extracted from different packets
    	packets.forEach(packet->{   		
			ByteBuf temp = null;
			temp = Unpooled.wrappedBuffer(packet.getContent());  //byte[] -> ByteBuf
			MQMessage decoded = MQParser.decode(temp);  //extract MQTT packet
			String terms_raw = decoded.toString();
			
			
			// System.out.println("single packet is: " + terms_raw);
			/**
			 * connect terms: username, password, clientID, isClean, keepalive, will
			 * un/subscribe terms: packetID, topics
			 * publish terms: packetID, topics, content, dup, retain
			 * puback terms: packetID
			 * connack terms: sessionPresent, code
			 * un/suback terms: packetID, code
			 */
			//if the packet is connect 
			switch(packet.getName()) {
			case "CONNECT":
				if(con_map.containsKey("username")) {
					List<String> con_list = con_map.get("username");
					raw_values.put("username", con_list.get(0));
					if(platformtype.contains("tuya")) {
						raw_words.put(con_list.get(0), "username");
					}
					if(con_list.contains(((Connect) decoded).getUsername())) {
						
					}else {
						con_list.add(((Connect) decoded).getUsername());
						con_map.put("username",con_list);
						
					}
				}else {
					//new MQTTSemantic().getTermsMap(terms_raw, con_map);
					List<String> con_list = new ArrayList<String>();
					con_list.add(((Connect) decoded).getUsername());
					con_map.put("username",con_list);
				}
				if(con_map.containsKey("password")) {
					// System.out.println("password con_list:" + con_list);
					List<String> con_list = con_map.get("password");
					raw_values.put("password", con_list.get(0));
					if(con_list.contains(((Connect) decoded).getPassword())) {
						//bosch refind
				        if(platformtype.contains("bosch")||platformtype.contains("tuya")) {
				        	raw_words.put(con_list.get(0),"password");
				        }
					}else {
						con_list.add(((Connect) decoded).getPassword());
						con_map.put("password",con_list);
//						if(platformtype.contains("tuya")) {
//				        	raw_words.put(con_list.get(0),"password");
//				        }
					}
				}else {
					List<String> con_list = new ArrayList<String>();
					con_list.add(((Connect) decoded).getPassword());
					con_map.put("password",con_list);
				}
				if(con_map.containsKey("clientID")) {
					List<String> con_list = con_map.get("clientID");
					raw_values.put("clientID", con_list.get(0));
					if(con_list.contains(((Connect) decoded).getClientID())) {
						//clientId refine
				        if(platformtype.contains("azure")||platformtype.contains("aws")||platformtype.contains("bosch")||platformtype.contains("tuya")) {
				        	raw_words.put(con_list.get(0),"clientID");
				        }
					}else {
						con_list.add(((Connect) decoded).getClientID());
						con_map.put("clientID",con_list);
					}
				}else {
					List<String> con_list = new ArrayList<String>();
					con_list.add(((Connect) decoded).getClientID());
					con_map.put("clientID",con_list);
				}
				if(con_map.containsKey("keepalive")) {
					List<String> con_list = con_map.get("keepalive");
					int keepalive = ((Connect) decoded).getKeepalive();
					if(con_list.contains(String.valueOf(keepalive))) {
						
					}else {
						con_list.add(String.valueOf(keepalive));
						con_map.put("keepalive",con_list);
					}
				}else {
					List<String> con_list = new ArrayList<String>();
					int keepalive = ((Connect) decoded).getKeepalive();
					con_list.add(String.valueOf(keepalive));
					con_map.put("keepalive",con_list);
				}
				if(con_map.containsKey("protocolLevel")) {
					List<String> con_list = con_map.get("protocolLevel");
					int protocollevel = ((Connect) decoded).getProtocolLevel();
					if(con_list.contains(String.valueOf(protocollevel))) {
						
					}else {
						con_list.add(String.valueOf(protocollevel));
						con_map.put("protocolLevel",con_list);
						for(String level:con_list) {
							raw_words.put(level, "protocolLevel");
						}
					}
					
				}else {
					List<String> con_list = new ArrayList<String>();
					int protocollevel = ((Connect) decoded).getProtocolLevel();
					con_list.add(String.valueOf(protocollevel));
					con_map.put("protocolLevel",con_list);
				}
				if(con_map.containsKey("cleanSession")) {
					List<String> con_list = con_map.get("cleanSession");
					String cleansession = null;
					if(((Connect) decoded).isCleanSession()) {
						cleansession = "true";
					}else {
						cleansession = "false";
					}
					if(con_list.contains(cleansession)) {
						
					}else {
						con_list.add(String.valueOf(cleansession));
						con_map.put("cleanSession",con_list);
					}
				}else {
					List<String> con_list = new ArrayList<String>();
					String cleansession = null;
					if(((Connect) decoded).isCleanSession()) {
						cleansession = "true";
					}else {
						cleansession = "false";
					}
					con_list.add(String.valueOf(cleansession));
					con_map.put("cleanSession",con_list);
				}
				if(((Connect) decoded).isWillFlag()) {
					if(con_map.containsKey("will")) {
						List<String> con_list = con_map.get("will");
						if(con_list.contains(((Connect) decoded).getWill().toString())) {
							
						}else {
							con_list.add(((Connect) decoded).getWill().toString());
							con_map.put("will",con_list);
						}
					}else {
						//new MQTTSemantic().getTermsMap(terms_raw, con_map);
						List<String> con_list = new ArrayList<String>();
						con_list.add(((Connect) decoded).getWill().toString());
						con_map.put("will",con_list);
					}
				}
				break;
			case "CONNACK":
				new MQTTSemantics().getTermsMap(terms_raw, conack_map);
				break;
			case "SUBSCRIBE":
				new MQTTSemantics().getTermsMap(terms_raw, sub_map);
				break;
			case "SUBACK":
				if(suback_map.containsKey("getPacketID()")) {
					List<String> con_list = suback_map.get("getPacketID()");
					if(con_list.contains(((Suback) decoded).getPacketID().toString())) {
						
					}else {
						con_list.add(((Suback) decoded).getPacketID().toString());
						suback_map.put("getPacketID()",con_list);
					}
				}else {
					List<String> con_list = new ArrayList<String>();
					con_list.add(((Suback) decoded).getPacketID().toString());
					suback_map.put("getPacketID()",con_list);
				}
				String tmpcode = ((Suback) decoded).getReturnCodes().toString();
				if(tmpcode.contains("QOS")) {
				if(suback_map.containsKey("qos")) {
					List<String> con_list = suback_map.get("qos");
					//getReturnCodes() result: [ACCEPTED_QOS0]
					//String tmpcode = ((Suback) decoded).getReturnCodes().toString();
					//tmp_code example: [ACCEPTED_QOS0]
					
					String tmp_qos = tmpcode.replace("[", "").replace("]", "").split("_")[1];
					
					//System.out.println("tmp_qos is " + tmp_qos);
					if(con_list.contains(tmp_qos)) {
						
					}else {
						con_list.add(tmp_qos);
						suback_map.put("qos",con_list);
					}
				}else {
					List<String> con_list = new ArrayList<String>();
					//getReturnCodes() result: [ACCEPTED_QOS0]
					//String tmpcode = ((Suback) decoded).getReturnCodes().toString();
					//tmp_code example: [ACCEPTED_QOS0]
					String tmp_qos = tmpcode.replace("[", "").replace("]", "").split("_")[1];
					//System.out.println("tmp_qos is " + tmp_qos);
					con_list.add(tmp_qos);
					suback_map.put("qos",con_list);
				}}
				break;
			case "UNSUBSCRIBE":
				new MQTTSemantics().getTermsMap(terms_raw, unsub_map);
				break;
			case "UNSUBACK":
				new MQTTSemantics().getTermsMap(terms_raw, unsuback_map);
				break;
			case "PUBLISH":
				//new MQTTSemantic().getTermsMap(terms_raw, pub_map);
				if(pub_map.containsKey("topic")) {				
					List<String> con_list = pub_map.get("topic");
					Topic topic = ((Publish) decoded).getTopic();
					raw_values.put("topic", con_list);
					if(con_list.contains(topic.toString())) {
						
					}else {
						con_list.add(topic.toString());
						pub_map.put("topic",con_list);
					}
				}else {
					//new MQTTSemantic().getTermsMap(terms_raw, con_map);
					List<String> con_list = new ArrayList<String>();
					Topic topic = ((Publish) decoded).getTopic();
					con_list.add(topic.toString());
					pub_map.put("topic",con_list);
				}
				if(pub_map.containsKey("payload")) {
					List<String> con_list = pub_map.get("payload");
					ByteBuf content = ((Publish) decoded).getContent();
					String content_str = convertByteBufToString(content);
					raw_values.put("payload", con_list);
					if(con_list.contains(content_str)) {
						if(platformtype.contains("alitcp")||platformtype.contains("aws")||platformtype.contains("bosch")||platformtype.contains("gcp")) {
							raw_words.put(content_str, "payload");
						}
					}else {
						con_list.add(content_str);
						if(platformtype.contains("tuya")) raw_words.put(content_str, "payload");
						pub_map.put("payload",con_list);
					}
				}else {
					//new MQTTSemantic().getTermsMap(terms_raw, con_map);
					List<String> con_list = new ArrayList<String>();
					ByteBuf content = ((Publish) decoded).getContent();
					con_list.add(convertByteBufToString(content));
					pub_map.put("payload",con_list);
				}
				if(pub_map.containsKey("retain")) {
					List<String> con_list = pub_map.get("retain");
					String retain = null;
					if(((Publish) decoded).isRetain()) {
						retain = "true";
					}else {
						retain = "false";
					}
					if(con_list.contains(retain)) {
						if(platformtype.contains("bosch")) {
							raw_words.put(retain, "retain");
						}
					}else {
						con_list.add(String.valueOf(retain));
						pub_map.put("retain",con_list);
					}
					
				}else {
					List<String> con_list = new ArrayList<String>();
					String retain = null;
					if(((Publish) decoded).isRetain()) {
						retain = "true";
					}else {
						retain = "false";
					}
					con_list.add(String.valueOf(retain));
					pub_map.put("retain",con_list);
				}
				if(pub_map.containsKey("dup")) {
					List<String> con_list = pub_map.get("dup");
					String dup = null;
					if(((Publish) decoded).isDup()) {
						dup = "true";
					}else {
						dup = "false";
					}
					if(con_list.contains(dup)) {
						
					}else {
						con_list.add(String.valueOf(dup));
						pub_map.put("dup",con_list);
					}
				}else {
					List<String> con_list = new ArrayList<String>();
					String dup = null;
					if(((Publish) decoded).isDup()) {
						dup = "true";
					}else {
						dup = "false";
					}
					con_list.add(String.valueOf(dup));
					pub_map.put("dup",con_list);
				}
				break;
			case "PUBACK":
				new MQTTSemantics().getTermsMap(terms_raw, puback_map);
				break;
			default:
				break;
			}
			
			//general extraction
			
    	});
    	//end packets.forEach(packet->{ 
    	
//    	System.out.println("con_map is " + con_map);
//    	System.out.println("conack_map is " + conack_map);
//    	System.out.println("pub_map is " + pub_map);
//    	System.out.println("suback_map is " + suback_map);
//    	System.out.println("unsuback_map is " + unsuback_map);
//    	System.out.println("puback_map is " + puback_map);
    	
    	
    	System.out.println("packets_terms is " + packets_terms);
    	
    	
    	//delete duplicate terms
    	for(int i=0; i<packets_terms.size();i++) {
    		new MQTTSemantics().getChangedTerms(packets_terms.get(i));
    		@SuppressWarnings("unchecked")
			List<String>list1=new ArrayList<String>(packets_terms.get(i).keySet());
    		//System.out.println(packet_names.get(i));
    		packets_abterms.put(packet_names.get(i),list1);
    	}
    	System.out.println("packets_abterms is *****\n"+packets_abterms);
    	
    	
    	
    	/*
    	 * get word
    	 * packets_in_a_log is a list of packets from a log
    	 *  extract the basi message parameters and their value
    	 * 
    	 */
    	Map<String, List<String>> terms_map_in_a_log = new HashMap<String, List<String>>();
    	String path = path_filedir + logs[1];  
    	MQTTparsertest(path, packets_in_a_log);
    	packets_in_a_log.forEach(packet_in_a_log->{
    		ByteBuf temp = null;
			temp = Unpooled.wrappedBuffer(packet_in_a_log.getContent());  //byte[] to ByteBuf
			MQMessage decoded = MQParser.decode(temp);  //decode message
			String terms_raw_in_a_log = decoded.toString();	
			//System.out.println("packet is " +terms_raw_in_a_log);
			String protocolname = decoded.getType().name();
			if(protocolname == "CONNECT") {
				//System.out.println("protocol name is " + protocolname);
				if(terms_map_in_a_log.containsKey(protocolname+"->username")) {
					List<String> con_list = terms_map_in_a_log.get(protocolname+"->username");
					con_list.add(((Connect) decoded).getUsername());
					terms_map_in_a_log.put(protocolname+"->username",con_list);
				}else {
					//new MQTTSemantic().getTermsMap(terms_raw, con_map);
					List<String> con_list = new ArrayList<String>();
					con_list.add(((Connect) decoded).getUsername());
					terms_map_in_a_log.put(protocolname+"->username",con_list);
				}
				if(terms_map_in_a_log.containsKey(protocolname+"->password")) {
					List<String> con_list = terms_map_in_a_log.get(protocolname+"->password");
					con_list.add(((Connect) decoded).getPassword());
					terms_map_in_a_log.put(protocolname+"->password",con_list);
				}else {
					List<String> con_list = new ArrayList<String>();
					con_list.add(((Connect) decoded).getPassword());
					terms_map_in_a_log.put(protocolname+"->password",con_list);
				}
				if(terms_map_in_a_log.containsKey(protocolname+"->clientID")) {
					List<String> con_list = terms_map_in_a_log.get(protocolname+"->clientID");
					con_list.add(((Connect) decoded).getClientID());
					terms_map_in_a_log.put(protocolname+"->lientID",con_list);
				}else {
					List<String> con_list = new ArrayList<String>();
					con_list.add(((Connect) decoded).getClientID());
					terms_map_in_a_log.put(protocolname+"->clientID",con_list);
				}
				if(terms_map_in_a_log.containsKey(protocolname+"->keepalive")) {
					List<String> con_list = terms_map_in_a_log.get(protocolname+"->keepalive");
					int keepalive = ((Connect) decoded).getKeepalive();
					con_list.add(String.valueOf(keepalive));
					terms_map_in_a_log.put(protocolname+"->keepalive",con_list);
				}else {
					List<String> con_list = new ArrayList<String>();
					int keepalive = ((Connect) decoded).getKeepalive();
					con_list.add(String.valueOf(keepalive));
					terms_map_in_a_log.put(protocolname+"->keepalive",con_list);
				}
				if(terms_map_in_a_log.containsKey(protocolname+"->protocolLevel")) {
					List<String> con_list = terms_map_in_a_log.get(protocolname+"->protocolLevel");
					int protocollevel = ((Connect) decoded).getProtocolLevel();
					con_list.add(String.valueOf(protocollevel));
					terms_map_in_a_log.put(protocolname+"->protocolLevel",con_list);
				}else {
					List<String> con_list = new ArrayList<String>();
					int protocollevel = ((Connect) decoded).getProtocolLevel();
					con_list.add(String.valueOf(protocollevel));
					terms_map_in_a_log.put(protocolname+"->protocolLevel",con_list);
				}
				if(terms_map_in_a_log.containsKey(protocolname+"->cleanSession")) {
					List<String> con_list = terms_map_in_a_log.get(protocolname+"->cleanSession");
					String cleansession = null;
					if(((Connect) decoded).isCleanSession()) {
						cleansession = "true";
					}else {
						cleansession = "false";
					}
					con_list.add(String.valueOf(cleansession));
					terms_map_in_a_log.put(protocolname+"->cleanSession",con_list);
				}else {
					List<String> con_list = new ArrayList<String>();
					String cleansession = null;
					if(((Connect) decoded).isCleanSession()) {
						cleansession = "true";
					}else {
						cleansession = "false";
					}
					con_list.add(String.valueOf(cleansession));
					terms_map_in_a_log.put(protocolname+"->cleanSession",con_list);
				}
				if(((Connect) decoded).isWillFlag()) {
					if(terms_map_in_a_log.containsKey(protocolname+"->will")) {
						List<String> con_list = terms_map_in_a_log.get(protocolname+"->will");
						con_list.add(((Connect) decoded).getWill().toString());
						terms_map_in_a_log.put(protocolname+"->will",con_list);
					}else {
						//new MQTTSemantic().getTermsMap(terms_raw, con_map);
						List<String> con_list = new ArrayList<String>();
						con_list.add(((Connect) decoded).getWill().toString());
						terms_map_in_a_log.put(protocolname+"->will",con_list);
					}
				}	
		}else if(protocolname == "PUBLISH") {
			//System.out.println("protocol name is " + protocolname);
			//new MQTTSemantic().getTermsMap(terms_raw, pub_map);
			if(terms_map_in_a_log.containsKey(protocolname+"->topic")) {
				List<String> con_list = terms_map_in_a_log.get(protocolname+"->topic");
				Topic topic = ((Publish) decoded).getTopic();
				con_list.add(topic.toString());
				terms_map_in_a_log.put(protocolname+"->topic",con_list);
			}else {
				//new MQTTSemantic().getTermsMap(terms_raw, con_map);
				List<String> con_list = new ArrayList<String>();
				Topic topic = ((Publish) decoded).getTopic();
				con_list.add(topic.toString());
				terms_map_in_a_log.put(protocolname+"->topic",con_list);
			}
			if(terms_map_in_a_log.containsKey(protocolname+"->payload")) {
				List<String> con_list = terms_map_in_a_log.get(protocolname+"->payload");
				ByteBuf content = ((Publish) decoded).getContent();
				con_list.add(convertByteBufToString(content));
				terms_map_in_a_log.put(protocolname+"->payload",con_list);
			}else {
				//new MQTTSemantic().getTermsMap(terms_raw, con_map);
				List<String> con_list = new ArrayList<String>();
				ByteBuf content = ((Publish) decoded).getContent();
				con_list.add(convertByteBufToString(content));
				terms_map_in_a_log.put(protocolname+"->payload",con_list);
			}
			if(terms_map_in_a_log.containsKey(protocolname+"->retain")) {
				List<String> con_list = terms_map_in_a_log.get(protocolname+"->retain");
				String retain = null;
				if(((Publish) decoded).isRetain()) {
					retain = "true";
				}else {
					retain = "false";
				}
				con_list.add(String.valueOf(retain));
				terms_map_in_a_log.put(protocolname+"->retain",con_list);
			}else {
				List<String> con_list = new ArrayList<String>();
				String retain = null;
				if(((Publish) decoded).isRetain()) {
					retain = "true";
				}else {
					retain = "false";
				}
				con_list.add(String.valueOf(retain));
				terms_map_in_a_log.put(protocolname+"->retain",con_list);
			}
			if(terms_map_in_a_log.containsKey(protocolname+"->dup")) {
				List<String> con_list = terms_map_in_a_log.get(protocolname+"->dup");
				String dup = null;
				if(((Publish) decoded).isDup()) {
					dup = "true";
				}else {
					dup = "false";
				}
				con_list.add(String.valueOf(dup));
				terms_map_in_a_log.put(protocolname+"->dup",con_list);
			}else {
				List<String> con_list = new ArrayList<String>();
				String dup = null;
				if(((Publish) decoded).isDup()) {
					dup = "true";
				}else {
					dup = "false";
				}
				con_list.add(String.valueOf(dup));
				terms_map_in_a_log.put(protocolname+"->dup",con_list);
			}
		}else {
			getTermsMapinaLog(packet_in_a_log,terms_raw_in_a_log,terms_map_in_a_log);
		}
    	});
    	
    	System.out.println("*****************terms_map_in_a_log " + terms_map_in_a_log);
    	
    	
    	
    	
    	// after decode the message then extract parameters' semantics
    	//search the terms_map_in_a_log, compare the value with crypto function feature
    	//update the map
    	System.out.println("*****************start crptyo match");
    	int id_key = 0;
    	Iterator<Map.Entry<String, List<String>>> it = terms_map_in_a_log.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, List<String>> entry = it.next();
			String key_tmp = entry.getKey();
			List<String> value_tmp = entry.getValue();
			
			//System.out.println(value_tmp.get(0));
			//System.out.println(key_tmp);
			if(value_tmp.get(0)==null) {
				System.out.println("null and exit");
				continue;
			}
			
			
			//crypto function match, 
			//input is  key_tmp is a parameter from a message, value is the value of the parameter
			// abwords_map parameter and semantics
			// raw_words value to semantics 
			if(getCryLibQuery(key_tmp, value_tmp.get(0), abwords_map, raw_words,encrypt_terms) != 0) {
				System.out.println("function matched!");
			}else {
				// topic special process
				if(entry.getKey().contains("topic")) {
					List<String> words = new ArrayList<String>();
					String refine_topic = value_tmp.get(0).replace("[", "").replace("]", "");
					//System.out.println("topic refine：" + entry.getKey()+" and topic is"+refine_topic);
					//if there is a #  consider topic has only one paramerer
//					if(refine_topic.contains("#")|| refine_topic.contains("+") || !(refine_topic.contains("/"))) {
//						if(refine_topic.contains(":")) { //only one parameter with qos
//							String qos = refine_topic.split(":")[1];
//							refine_topic = refine_topic.split(":")[0];
//							raw_words.put(qos, "qos");
//						}
//						//refine topic is exited in raw_map
//					}else {
					if(refine_topic.contains(":")) { //if there is qos
						String qos = refine_topic.split(":")[1];
						refine_topic = refine_topic.split(":")[0];
						raw_words.put(qos, "qos");
						words.add("qos");
					}
					//update abwords_map without qos
					if(refine_topic.contains("/") && !(refine_topic.contains("#")&& refine_topic.contains("+"))) {
						String[] topic_words = refine_topic.split("/");
						for(int i = 0; i< topic_words.length;i++) {
							if(topic_words[i].length()!=0) {
								String word_ab = null;
								if(raw_words.containsKey(topic_words[i])) {
									word_ab=raw_words.get(topic_words[i]);
								}else {
									word_ab = "V"+id_key;
									raw_words.put(topic_words[i],word_ab);
									id_key = id_key+1;
								}
								words.add(word_ab);
							}
						}
					}else {
						if(raw_words.containsKey(refine_topic)) {
							words.add(raw_words.get(refine_topic));
						}else {
							words.add("V"+id_key);
							raw_words.put(refine_topic, "V"+id_key);
							id_key = id_key+1;
						}
					}
					abwords_map.put(entry.getKey(), words);
				}

				//check and split the value / & |
		    	//abwords_map <clientID, <v1,v2,v3>>
		    	//words_map<v1,xxxx>, <v2,wofeojfe>
				else if(value_tmp.get(0).contains("/")) {
					List<String> words = new ArrayList<String>();
					System.out.println("should be split further：" + value_tmp.get(0));
					String[] words_value = value_tmp.get(0).split("/");
					for(int i = 0; i< words_value.length;i++) {
						if(words_value[i].length()!=0) {
							String word_ab = null;
							if(raw_words.containsKey(words_value[i])) {
								word_ab=raw_words.get(words_value[i]);
							}else {
								word_ab = "V"+id_key;
								raw_words.put(words_value[i],word_ab);
								id_key = id_key+1;
							}
							words.add(word_ab);
							
						}
					}
					abwords_map.put(entry.getKey(), words);
					
				}else if(value_tmp.get(0).contains("&")) {
					List<String> words = new ArrayList<String>();
					System.out.println("should be split further：" + value_tmp.get(0));
					String[] words_value = value_tmp.get(0).split("&");
					for(int i = 0; i< words_value.length;i++) {
						if(words_value[i].length()!=0) {
							String word_ab = null;
							if(raw_words.containsKey(words_value[i])) {
								word_ab=raw_words.get(words_value[i]);
							}else {
								word_ab = "V"+id_key;
								raw_words.put(words_value[i],word_ab);
								id_key = id_key+1;
							}
							words.add(word_ab);
							
						}
					}
					abwords_map.put(entry.getKey(), words);
					
				}else if(value_tmp.get(0).contains("|")) {
					
					//System.out.println("$$$$##### is " + value_tmp.get(0));
					List<String> words = new ArrayList<String>();
					System.out.println("should be split further：" + value_tmp.get(0));
					String[] words_value = value_tmp.get(0).split("\\|");
					for(int i = 0; i< words_value.length;i++) {
						if(words_value[i].length()!=0) {
							String word_ab = null;
							if(raw_words.containsKey(words_value[i])) {
								word_ab=raw_words.get(words_value[i]);
							}else {
								word_ab = "V"+id_key;
								raw_words.put(words_value[i],word_ab);
								id_key = id_key+1;
							}
							words.add(word_ab);
							
						}
					}
					abwords_map.put(entry.getKey(), words);
				}else if(value_tmp.get(0).contains("@")) {
					//System.out.println("$$$$##### is " + value_tmp.get(0));
					List<String> words = new ArrayList<String>();
					System.out.println("should be split further：" + value_tmp.get(0));
					String[] words_value = value_tmp.get(0).split("@");
					for(int i = 0; i< words_value.length;i++) {
						if(words_value[i].length()!=0) {
							String word_ab = null;
							if(raw_words.containsKey(words_value[i])) {
								word_ab=raw_words.get(words_value[i]);
							}else {
								word_ab = "V"+id_key;
								raw_words.put(words_value[i],word_ab);
								id_key = id_key+1;
							}
							words.add(word_ab);
							
						}
					}
					abwords_map.put(entry.getKey(), words);
				}
			}
			
		}
		System.out.println("abwords_map is ****\n"+abwords_map.toString());
		
		//// ali refine
        if(platformtype.contains("ali")  ) {
        	//System.out.println("abwords_map clientID ****\n"+abwords_map.get("CONNECT->ClientID").toString());
        	String value1 = abwords_map.get("CONNECT->clientID").get(0);
        	abwords_map.get("CONNECT->clientID").set(0, "deviceId");
        	String value2 = abwords_map.get("CONNECT->username").get(0);
        	String value3 = abwords_map.get("CONNECT->username").get(1);
        	abwords_map.get("CONNECT->username").set(0, "deviceName");
        	abwords_map.get("CONNECT->username").set(1, "productKey");
        	
        	//update others
        	Iterator iter = abwords_map.entrySet().iterator();
        	while(iter.hasNext()) {
        		Map.Entry entry = (Map.Entry) iter.next();
            	List<String> tmp_list = (List<String>) entry.getValue();
            	for (int i = 0; i < tmp_list.size(); i++) {
                    if(tmp_list.get(i).contains(value1)) {
                    	tmp_list.set(i, "deviceId");
                    }else if(tmp_list.get(i).contains(value2)) {
                    	tmp_list.set(i, "deviceName");
                    }else if(tmp_list.get(i).contains(value3)) {
                    	tmp_list.set(i, "productKey");
                    }
                }
        	}
        	
        	System.out.println("abwords_map of ali is ****\n"+abwords_map.toString());
        	raw_words.put("mpinspector","deviceId");
        	for(String key:raw_words.keySet()) {
        		if(raw_words.get(key).contentEquals("V4")) {
        			raw_words.put(key,"deviceName");
        			break;
        		}
        		
        	}
        	for(String key:raw_words.keySet()) {
        		if(raw_words.get(key).contentEquals("V3")) {
        			raw_words.put(key,"productKey");
        			break;
        		}
        		
        	}
    
        }
        
		
		System.out.println("raw_words is ****\n"+raw_words.toString());
		//System.out.println("the terms map in a log is \n"+ terms_map_in_a_log);
		
    	//crypto function match

		//Map<String, List<String>> packets_abterms = new HashMap<String, List<String>>();
		//refine 一下
		//{CONNACK=[], SUBACK=[getPacketID(), returnCodes], UNSUBSCRIBE=[topics, getPacketID()], 
		//CONNECT=[password, clientID], UNSUBACK=[], PUBLISH=[getPacketID(), topic, content], 
		//PUBACK=[getPacketID()], SUBSCRIBE=[topics, getPacketID()]}
		
		//packet_words_map string-string
    	//final result，connect-(username,password(),clientID())
		
		
		/*********************************************************
		 * Result is stored in packets_abterms  use abwords_map update packets_abterms
		*********************************************************/
		System.out.println("the final abterms_map in a log is *********************\n"+ packets_abterms);
		Iterator iter = packets_abterms.entrySet().iterator();
		//{CONNECT->clientID=[V0, aud, V2, V3, V4, V5, V6, V7], SUBSCRIBE->topics=[V6, V7, V8, V9, qos], 
		//CONNECT->password=[JWT(token(aud, exp, iat), tokenKey, RS256)], 
		//PUBLISH->topic=[V6, V7, V10, qos]}
        while (iter.hasNext()) {
        	Map.Entry entry = (Map.Entry) iter.next();
        	List<String> terms_list = (List<String>) entry.getValue();
        	//System.out.println("the terms_list is *********************\n"+ terms_list);
        	if(terms_list.size()>0) {
        		//System.out.println("the terms_list is *********************\n"+ terms_list);
        		for(int i=0;i<terms_list.size();i++) {
        			String terms = terms_list.get(i);
        			String mapkey_abwords = entry.getKey()+"->"+terms;
        			//System.out.println("the mapkey_abwords is *********************\n"+ mapkey_abwords);
        			if(abwords_map.containsKey(mapkey_abwords)) {
        				String words = terms+"(";
        				List<String> words_list = abwords_map.get(mapkey_abwords);
        				for(int j=0;j<words_list.size();j++) {
        					if(j==words_list.size()-1) {
        						words=words+words_list.get(j)+")";
        					}else {
        						words=words+words_list.get(j)+",";
        					}
        				}
        				terms_list.set(i, words);
        			}
        		}
        		
        	}
        	packets_abterms.put((String) entry.getKey(), terms_list);
        	
        }
        
        
        
        
        
        long endtime = System.currentTimeMillis();
        System.out.println("the time consumption："+(endtime-starttime)+" milionseconds");
        System.out.println("the final packets_abterms in a log is *********************\n"+ packets_abterms);
        
        
        /******************
         * Store the "packets_abterms" result into txt
         *******************/
        
        writeMapListintoFile(packets_abterms, output_dir,"parameter.txt");
        writeMapListintoFile(abwords_map, output_dir,"abwords_map.txt");
        writeMapStringintoFile(raw_words, output_dir,"raw_words.txt");
        
        /******************
         * Combine the parameter and raw_words
         *******************/
        writeFinalResult(packets_abterms,abwords_map,raw_words,encrypt_terms,terms_map_in_a_log,output_dir,"result.json",semantic.platformtype);
        writeRawResult(raw_values,output_dir,"raw.json",semantic.platformtype);
//        writeMapListintoFile(packets_abterms, "iot prtocol project\\trafficanalysis\\"+semantic.platformtype+"parameter.txt");
//        writeMapListintoFile(abwords_map, "projects\\iot prtocol project\\trafficanalysis\"+semantic.platformtype+"abwords_map.txt");
//        writeMapStringintoFile(raw_words, "projects\\iot prtocol project\\trafficanalysis\\"+semantic.platformtype+"raw_words.txt");

//        refinedot.setMap(packets_abterms);
//        refinedot.setTlsFlag(false);
//        refinedot.StateMachineRefine();
    	
    }
    
	
	

	
	/*
	 * input: a string of a packet
	 * output: a terms map that stores the changed terms and values.
	 */
	public static void getTermsMapinaLog(MQTTPacket packet_in_a_log, String terms_raw, Map<String, List<String>> terms_map) {
		//System.out.println(terms_raw);
//		Map<String, List<String>> terms_map = new HashMap<String, List<String>>();
//		terms_map = input_map;
		Pattern p = Pattern.compile("\\s*|\t|\r|\n");
		Matcher m = p.matcher(terms_raw);
		terms_raw = m.replaceAll("");
		
		/*
		 * extract all equivalence relations
		 */
		if(terms_raw.indexOf("[") == -1) {
			System.out.println("no equations");
		}else {
			String terms_string = terms_raw.substring(terms_raw.indexOf("[")+1, terms_raw.lastIndexOf("]"));
			//System.out.println(terms_string);
			String[] tmp_equations = terms_string.split(",");
			List<String> equations = new ArrayList<String>();
			int ei = 0;
			for(int i = 0; i< tmp_equations.length; i++) {
				//System.out.println(tmp_equations[i]);
				if(tmp_equations[i].contains("="))
				{
					equations.add(tmp_equations[i]);
					ei++;
				}else {
					equations.set(ei-1, equations.get(ei-1).concat(",").concat(tmp_equations[i]));
				}
			}

			/*
			 *update the map with the extracted equivalence relations
			 */
			for (int i = 0; i < equations.size(); i++) {
				//System.out.println( packet_in_a_log.getName()+"->"+ "equations: "+ equations.get(i));
				int indexeq = equations.get(i).indexOf("=");
				String key_tmp = packet_in_a_log.getName()+"->"+equations.get(i).substring(0,indexeq);
				String value_tmp = equations.get(i).substring(indexeq+1);
				if(terms_map.containsKey(key_tmp)) {
					
				}else {
					List<String> value_list = new ArrayList<String>();
					value_list.add(value_tmp);
					terms_map.put(key_tmp, value_list);
				}
			}
			
		}

	}
	
	
	
	/*
	 * input: a string of a packet
	 * output: a terms map that stores the changed terms and values.
	 */
	public void getTermsMap(String terms_raw, Map<String, List<String>> terms_map){
		//System.out.println(terms_raw);
//		Map<String, List<String>> terms_map = new HashMap<String, List<String>>();
//		terms_map = input_map;
		Pattern p = Pattern.compile("\\s*|\t|\r|\n");
		Matcher m = p.matcher(terms_raw);
		terms_raw = m.replaceAll("");
		
		/*
		 * extract all equivalence relations
		 */
		if(terms_raw.indexOf("[") == -1) {
			System.out.println("no equations");
		}else {
			String terms_string = terms_raw.substring(terms_raw.indexOf("[")+1, terms_raw.lastIndexOf("]"));
			//System.out.println(terms_string);
			String[] tmp_equations = terms_string.split(",");
			List<String> equations = new ArrayList<String>();
			int ei = 0;
			for(int i = 0; i< tmp_equations.length; i++) {
				//System.out.println(tmp_equations[i]);
				if(tmp_equations[i].contains("="))
				{
					equations.add(tmp_equations[i]);
					ei++;
				}else {
					equations.set(ei-1, equations.get(ei-1).concat(",").concat(tmp_equations[i]));
				}
			}

			/*
			 *update the map with the extracted equivalence relations
			 */
			for (int i = 0; i < equations.size(); i++) {
					//System.out.println( equations.get(i));
				int indexeq = equations.get(i).indexOf("=");
				String key_tmp = equations.get(i).substring(0,indexeq);
				String value_tmp = equations.get(i).substring(indexeq+1);
				if(terms_map.containsKey(key_tmp)) {
					List<String> value_list = (List<String>) terms_map.get(key_tmp);
					if(value_list.contains(value_tmp)) {
						
					}else {
						value_list.add(value_tmp);
						terms_map.put(key_tmp, value_list);
					}
				}else {
					List<String> value_list = new ArrayList<String>();
					value_list.add(value_tmp);
					terms_map.put(key_tmp, value_list);
				}
			}
			
		}	
	}
	
	public static void getChangedTerms(Map<String, List<String>> terms_map) {
		/*
		 * delete the same terms	
		 */
		Iterator<Map.Entry<String, List<String>>> it = terms_map.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, List<String>> entry = it.next();
			List<String> value_tmp = entry.getValue();
			if(entry.getKey().contains("PacketID") || entry.getKey().contains("qos")) {
			
			}else {
				if(value_tmp.size()<=1) {
					it.remove();
				}
			}
	}
	}
	
	
	
	/* algorithm lib
	 * input: A string 
	 * output:a formalized representation
	 * exp:    	 
	 * result: (jwt(iat, exp, aud), tokenkey, alg)
	*/
	private static int getCryLibQuery(String key, String value, Map<String, List<String>> abwords_map, Map<String,String> raw_words,Map<String,JSONObject> encrypt_terms) {
		// TODO Auto-generated method stub
		
		//JWT match
		int id_lkey = 0;
		Pattern jwtp = Pattern.compile("ey[A-Za-z0-9_\\/+-]*\\.[A-Za-z0-9._\\/+-]*");
		//System.out.println("value is "+ value);
		Matcher jwtm = jwtp.matcher(value);
		//System.out.println(m.toString());
		//System.out.println("value is "+ value);
		//System.out.println("key is "+ key);
		boolean jwtb = jwtm.matches();
		//System.out.println(b);
		
		Pattern base64p = Pattern.compile("/^\\s*data:(?:[a-z]+\\/[a-z0-9-+.]+(?:;[a-z-]+=[a-z0-9-]+)?)?(?:;base64)?,([a-z0-9!$&',()*+;=\\-._~:@\\/?%\\s]*?)\\s*$/i");
		Matcher base64m = base64p.matcher(value);
		boolean base64b = base64m.matches();
		//String platformtype = this.platformtype;
		if(base64b && platformtype.contains("bosch")) {
			//update terms-map
			//value = "h(skpwd)";
			List<String> func = new ArrayList<String>();
			func.add("h(skpwd)");
			abwords_map.put(key, func); //password, h(secret key)
			
			return 1;
		}else if(platformtype.contains("tuya") && key.contains("payload")){
			List<String> func = new ArrayList<String>();
			func.add("senc(content,clientID,timestamp)skDev");
			func.add("md5(senc(content,clientID,timestamp)skDev)skDev");
			abwords_map.put(key, func); //password, h(secret key)
			
			
			return 1;
		}else if(platformtype.contains("ali") && key.contains("password")){
			List<String> func = new ArrayList<String>();
			func.add("hmac(deviceId,deviceName,productKey)skDev");
			abwords_map.put(key, func); //password, h(secret key)
			return 1;
		}
		else if(jwtb) {                          //JWT
			//decode jwt function
			String jwt = jwtm.group(0);
			Base64 base64 = new Base64();
			//JSONObject accountIdsJson = new JSONObject();
			String header = jwt.split("\\.")[0];
			String payload = jwt.split("\\.")[1];
			String sign = jwt.split("\\.")[2];
			String decodedHeader = new String(base64.decode(header.getBytes()));
			String decodedPayload = new String(base64.decode(payload.getBytes()));
			
            try {
            	JSONParser parser = new JSONParser();
				JSONObject headerjson = (JSONObject) parser.parse(decodedHeader);
				JSONObject payloadJson = (JSONObject) parser.parse(decodedPayload);
				String type = (String) headerjson.get("typ");
				String algorithm = (String) headerjson.get("alg");
				List<String> func = new ArrayList<String>();
				String token = payloadJson.keySet().toString().replace("[", "").replace("]", "");	
				func.add(type +"(token("+ token+"), tokenKey, "+algorithm +")");
				//update terms-map
				abwords_map.put(key, func); //password, jwt(token(a,b,c),tokenKey,algorithm)
				
				//token can get a,b,c three parameter's semantics，update raw_words
				 Iterator iter = payloadJson.entrySet().iterator();
			        while (iter.hasNext()) {
			            Map.Entry entry = (Map.Entry) iter.next();
			            String tmp_key = entry.getKey().toString();
			            String tmp_value = entry.getValue().toString();
			            //System.out.println(tmp_key);
			            //System.out.println(tmp_value);
			            //refine abwords_map
			            if(raw_words.containsKey(tmp_value)) {
			            	//update abwords username(x,n,b) terms->words
			            	Iterator abwords_iter = abwords_map.entrySet().iterator();
			            	while(abwords_iter.hasNext()) {
			            		Map.Entry<String, List<String>> abwords_entry = (Entry<String, List<String>>) abwords_iter.next();
			            		 List<String> words_tmp_list = abwords_entry.getValue();
			            		 String key_abwords_map = abwords_entry.getKey();
			            		 for(int i=0;i<words_tmp_list.size();i++) {
			            			 if(words_tmp_list.get(i).contains(raw_words.get(tmp_value))) {
			            				 String tttmp = null;
			            				 tttmp = words_tmp_list.get(i).replaceAll(raw_words.get(tmp_value), tmp_key);
			            				 words_tmp_list.set(i, tttmp);
			            			 }	 
			            		 }
			            		 abwords_map.put(key_abwords_map,words_tmp_list);
			            	}
			            }
			            //finish refine
			            raw_words.put(tmp_value,tmp_key);
			        }
			        JSONObject encryptTermObj = new JSONObject();
		        	encryptTermObj.putAll(headerjson);
		        	encryptTermObj.putAll(payloadJson);
					encrypt_terms.put("gcp->password", encryptTermObj);
			        	
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
			return 1;
		}else if(value.contains("SharedAccessSignature")){   //SAS match
			System.out.println(value);
			List<String> func = new ArrayList<String>();
			String sas_raw = value.replace("SharedAccessSignature ", "");
			String[] sas_equations = sas_raw.split("&");
			//System.out.println(sas_equations);
			List<String> equations = new ArrayList<String>();
			String para = "sas(";
			int ei = 0;
			for(int i = 0; i< sas_equations.length; i++) {
				//System.out.println(tmp_equations[i]);
				//System.out.println(sas_equations[i]);
				String eq_key = sas_equations[i].split("=")[0];
				String eq_value = getURLDecoderString(sas_equations[i].split("=")[1]);
				//System.out.println(eq_value);
				if(i !=0) {
					para = para +","+ eq_key;
				}else {
					para = para + eq_key;
				}
				
				if(raw_words.containsKey(eq_value)) {
					System.out.println("sas existed value "+eq_value);
					//update abwords username(x,n,b) terms->words
	            	Iterator abwords_iter = abwords_map.entrySet().iterator();
	            	while(abwords_iter.hasNext()) {
	            		Map.Entry<String, List<String>> abwords_entry = (Entry<String, List<String>>) abwords_iter.next();
	            		 List<String> words_tmp_list = abwords_entry.getValue();
	            		 String key_abwords_map = abwords_entry.getKey();
	            		 for(int j=0;j<words_tmp_list.size();j++) {
	            			 if(words_tmp_list.get(j).contains(raw_words.get(eq_value))) {
	            				 String tttmp = null;
	            				 tttmp = words_tmp_list.get(j).replaceAll(raw_words.get(eq_value), eq_key);
	            				 words_tmp_list.set(j, tttmp);
	            			 }	 
	            		 }
	            		 abwords_map.put(key_abwords_map,words_tmp_list);
	            	}
				}
				//finish refine
	            raw_words.put(eq_value,eq_key);
			}
			para= para+")";
			//System.out.println(para);
			func.add(para);
			abwords_map.put(key, func);
			return 1;
		}
		
		else if(!(value.equals(getURLDecoderString(value))) && !(key.contains("topic"))){  // if url encode
			System.out.println("the url encode " + value);
			System.out.println("the url decode " + getURLDecoderString(value));
			String url = value;
			String paras_raw =null;
			String[] paras;
			List<String> func = new ArrayList<String>();
			List<String> functmp = new ArrayList<String>();
			//if ？ exist  split &，
			if(value.contains("?")) {
				url =value.split("[?]")[0];
				paras_raw = value.split("[?]")[1];
				//update abwords username(x,n,b) terms->words
				if(paras_raw.contains("&")) {
					paras = paras_raw.split("&");
					for(int i =0;i<paras.length;i++) {
						String eq_key = paras[i].split("=")[0];
						String eq_value = paras[i].split("=")[1];
						functmp.add(eq_key);
						if(raw_words.containsKey(eq_value)) {
							//update abwords username(x,n,b) terms->words
			            	Iterator abwords_iter = abwords_map.entrySet().iterator();
			            	while(abwords_iter.hasNext()) {
			            		Map.Entry<String, List<String>> abwords_entry = (Entry<String, List<String>>) abwords_iter.next();
			            		 List<String> words_tmp_list = abwords_entry.getValue();
			            		 String key_abwords_map = abwords_entry.getKey();
			            		 for(int j=0;j<words_tmp_list.size();j++) {
			            			 if(words_tmp_list.get(j).contains(raw_words.get(eq_value))) {
			            				 String tttmp = null;
			            				 tttmp = words_tmp_list.get(j).replaceAll(raw_words.get(eq_value), eq_key);
			            				 words_tmp_list.set(j, tttmp);
			            			 }	 
			            		 }
			            		 abwords_map.put(key_abwords_map,words_tmp_list);
			            	}
						}
						 //finish refine
			            raw_words.put(eq_value,eq_key);
					}
				}else {
					if(raw_words.containsKey(paras_raw)){
						functmp.add(raw_words.get(paras_raw));
					}else {
						String tmp_key = "L"+id_lkey;
						functmp.add(tmp_key);
						raw_words.put(paras_raw, tmp_key);
						id_lkey = id_lkey+1;
					}
					
				}
				System.out.println(url);
				if(raw_words.containsKey(url)) {
					functmp.add(raw_words.get(url));
				}else {
					functmp.add("resourceurl");
					raw_words.put(url, "resourceurl");
				}
				String tmp = functmp.get(0);
				for(int j=1;j<functmp.size();j++) {
					tmp = tmp+","+ functmp.get(j);
				}
				//tmp = tmp+")";
				func.add(tmp);
				
			}else {
				if(raw_words.containsKey(value)) {
					func.add(raw_words.get(value));
				}else {
					func.add("resourceurl");
					raw_words.put(value, "resourceurl");
				}
			}
			abwords_map.put(key, func);
			return 1;
		}else {
			return 0;
		}
			
	}

	/*
	 * input: a path and a list of packets
	 * output: a list of packets in a log  (connect, subscribe, unsubscribe, puback,...)
	 */
	
	private static void MQTTparsertest(String path, List<MQTTPacket> packets) {
		// TODO Auto-generated method stub
		try
		{
			byte[] byte_array = getContent(path);
			String bittemp = byte2Hex(byte_array); 
			//check the message type and split
			int point = 0;
			int remainlength = 0;
			while(point != byte_array.length) {
				byte[] packet = null;
				String msgtype ="";

				if(byte_array[point] == 0x10) {
//					System.out.println("Client -- > Server： CONNECT");
					msgtype = "CONNECT";
				}else if(byte_array[point] == 0x20) {
//					System.out.println("Server-- > Client： CONNACK");
					msgtype = "CONNACK";
//					System.out.println("remian length is " + remainlength);	
				}else if((byte_array[point] & 0xF0) == 0x80) {
//					System.out.println("Client -- > Server： SUBSCRIBE");
					msgtype = "SUBSCRIBE";
				}else if ((byte_array[point] & 0xF0) == 0x30) {
//					System.out.println("Client -- > Server： PUBLISH");
					msgtype = "PUBLISH";
				}else if ((byte_array[point] & 0xF0) == 0x90) {
//					System.out.println("Client -- > Server： SUBACK");
					msgtype = "SUBACK";
				}else if ((byte_array[point] & 0xF0) == 0x40) {
//					System.out.println("Client -- > Server： PUBACK");
					msgtype = "PUBACK";
				}else if ((byte_array[point] & 0xF0) == 0xe0) {
//					System.out.println("Client -- > Server： DISCONNECT");
					msgtype = "DISCONNECT";
				}else if ((byte_array[point] & 0xF0) == 0xc0) {
//					System.out.println("Client -- > Server： PINGREQ");
					msgtype = "PINGREQ";
				}else if ((byte_array[point] & 0xF0) == 0xd0) {
//					System.out.println("Client -- > Server： PINGBACK");
					msgtype = "PINGBACK";
				}else if ((byte_array[point] & 0xF0) == 0x70) {
//					System.out.println("Client -- > Server： PUBCOMP");
					msgtype = "PUBCOMP";
				}else if ((byte_array[point] & 0xF0) == 0x50) {
//					System.out.println("Client -- > Server： PUBREC");
					msgtype = "PUBREC";
				}else if ((byte_array[point] & 0xF0) == 0x60) {
//					System.out.println("Client -- > Server： PUBREL");
					msgtype = "PUBREL";
				}else if ((byte_array[point] & 0xF0) == 0xa0) {
//					System.out.println("Client -- > Server： UNSUBSCRIBE");
					msgtype = "UNSUBSCRIBE";
				}else if ((byte_array[point] & 0xF0) == 0xb0) {
//					System.out.println("Client -- > Server： UNSUBACK");
					msgtype = "UNSUBACK";
				}
				point = point + 1;
				//System.out.println("point is " + point);
				int multiplier = 1;
				short digit;
				int loops = 0;
				remainlength = 0;
				do {
					digit = byte_array[point];
					remainlength += (digit & 127) * multiplier;
					multiplier *= 128;
					loops ++;
					point ++;
				}while((digit & 128) != 0 && loops <4);
				
				if(loops == 4 && (digit & 128) !=0)
				{
					 throw new DecoderException("remaining length exceeds 4 digits (" + msgtype + ')');
				}else {				
					packet = hexToByte(bittemp.substring((point-loops-1)*2,(point+remainlength)*2));
					point = point + remainlength;
					MQTTPacket packet_tmp = new MQTTPacket();
					packet_tmp.setName(msgtype);
					packet_tmp.setContent(packet);
					packets.add(packet_tmp);	
					
				}			
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}
	
	
	/**
	 * url decode
	 * @param str
	 * @return
	 */
	public static String getURLDecoderString(String str) {
        String result = "";
        if (null == str) {
            return "";
        }
        try {
            result = java.net.URLDecoder.decode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

	/**
	 * 
	 * <p>Title: getContent</p>  
	 * <p>Description:read file and get byte[] </p>  
	 * @param filePath
	 * @return byte arrat
	 * @throws IOException
	 */
	@SuppressWarnings("resource")
	public static byte[] getContent(String filePath) throws IOException { 
        File file = new File(filePath);  
        long fileSize = file.length();  
        if (fileSize > Integer.MAX_VALUE) {  
        	System.out.println("file too big...");  
            return null;  
        }  
        FileInputStream fi = new FileInputStream(file);  
        byte[] buffer = new byte[(int) fileSize];  
        int offset = 0;  
        int numRead = 0;  
        while (offset < buffer.length  
        && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {  
            offset += numRead;  
        }  
        // ensure all the data has been read 
        if (offset != buffer.length) {  
        	throw new IOException("Could not completely read file "  
                    + file.getName());  
        }  
        fi.close();  
        return buffer;  
    }

	
	/**
	 * Byte to Bit
	 */
	public static String byteToBit(byte b) {
	 return "" +(byte)((b >> 7) & 0x1) + 
	 (byte)((b >> 6) & 0x1) + 
	 (byte)((b >> 5) & 0x1) + 
	 (byte)((b >> 4) & 0x1) + 
	 (byte)((b >> 3) & 0x1) + 
	 (byte)((b >> 2) & 0x1) + 
	 (byte)((b >> 1) & 0x1) + 
	 (byte)((b >> 0) & 0x1);
	}
	/**
	 * Bit convert to Byte
	 */
	public static byte BitToByte(String byteStr) {
	 int re, len;
	 if (null == byteStr) {
	  return 0;
	 }
	 len = byteStr.length();
	 if (len != 4 && len != 8) {
	  return 0;
	 }
	 if (len == 8) {// 8 bit handle
	  if (byteStr.charAt(0) == '0') {// 
	   re = Integer.parseInt(byteStr, 2);
	  } else {// 
	   re = Integer.parseInt(byteStr, 2) - 256;
	  }
	 } else {//4 bit handle
	  re = Integer.parseInt(byteStr, 2);
	 }
	 return (byte) re;
	}
	
	
	/**
	　　* byte[] convert to hex
	　　* @param bytes
	　　* @return
	　　*/
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
	
	/**
     * hex covert to byte
     * @param hex
     * @return
     */
    public static byte[] hexToByte(String hex){
        int m = 0, n = 0;
        int byteLen = hex.length() / 2; //every two hex is a byte 
        byte[] ret = new byte[byteLen];
        for (int i = 0; i < byteLen; i++) {
            m = i * 2 + 1;
            n = m + 1;
            int intVal = Integer.decode("0x" + hex.substring(i * 2, m) + hex.substring(m, n));
            ret[i] = Byte.valueOf((byte)intVal);
        }
        return ret;
    }
	
	static Boolean expect_ch(char w, byte c) {
		if(c != w) {
			System.out.println("It is" + c + " , not " + w);
			return false;
		}
		return true;
	}
	
	static Boolean expect_ch(int w, byte c) {
		if(c != w) {
			System.out.println("It is" + c + " , not " + w);
			return false;
		}
		return true;
	}
	
	
	
	public static String convertByteBufToString(ByteBuf buf) {
	    String str;
	    if(buf.hasArray()) { // 
	        str = new String(buf.array(), buf.arrayOffset() + buf.readerIndex(), buf.readableBytes());
	    } else { 
	        byte[] bytes = new byte[buf.readableBytes()];
	        buf.getBytes(buf.readerIndex(), bytes);
	        str = new String(bytes, 0, buf.readableBytes());
	    }
	    return str;
	}
	
	

	public static void writeMapListintoFile(Map<String, List<String>> map_list,  String filepath,String filename) {
		/******************
         * Store the result into txt
         *******************/
        try {
	    	
        	File savefilepath = new File(filepath);
        	if (!savefilepath.exists()){
        		savefilepath.mkdir();
                System.out.println("Create dir: "+ savefilepath);
        	}
			File file1 = new File(filepath,filename);
			FileWriter fileWritter = new FileWriter(file1,false);
			fileWritter.write(map_list.toString());
			fileWritter.close();
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void writeMapStringintoFile(Map<String, String> map_string, String filepath,String filename) {
		/******************
         * Store the result into txt
         *******************/
        try {
        	File savefilepath = new File(filepath);
        	if (!savefilepath.exists()){
        		savefilepath.mkdir();
                System.out.println("Create dir: "+ savefilepath);
        	}
			File file1 = new File(filepath,filename);
			if (!file1.exists()) {
		        System.out.println("File does not exist");
		        file1.createNewFile();
		    }
			FileWriter fileWritter = new FileWriter(file1,false);
			fileWritter.write(map_string.toString());
			fileWritter.close();
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private static boolean isEncryptionFunction(String str) {
		if(str.contains("hmac(")||str.contains("senc(")||str.contains("JWT(")||str.contains("md5(")||str.contains("sas(")) return true;
		return false;
	}
	private static List<JSONObject> parseEncryptionTerm(Map<String, String> raw_words,String str,String platformtype) {
		String[] splitRes = str.split("\\((.)+\\)");
		JSONObject termObj= new JSONObject();
		String term = splitRes[0];
		String termKey = splitRes.length>=2?splitRes[1]:null;
		List<JSONObject> ans = new ArrayList<>();
		if(term.length()==str.length()) {
			String[] termVals = term.split(",");
			for(String termVal:termVals) {
				JSONObject termKeyValueObj = new JSONObject();
				if(raw_words.containsValue(termVal)) {
					List<String> tmp = new ArrayList<String>();
					for (String k : raw_words.keySet()) {
						if (raw_words.get(k).equals(termVal)) {
							tmp.add(k);
						}
					}
					termKeyValueObj.put(termVal,tmp);
				}else {
					termKeyValueObj.put(termVal,new ArrayList<String>());
				}
				//ali refine
				if(platformtype.contains("alitcp")&&termVal.contentEquals("deviceId")) {
					List<String> tmp = new ArrayList<String>();
					for (String k : raw_words.keySet()) {
						if (raw_words.get(k).equals("V0")) {
							tmp.add(k);
						}
					}
					termKeyValueObj.put(termVal,tmp);
				}
				ans.add(termKeyValueObj);
			}
		}else {
			String termVal = str.substring(term.length()+1, termKey==null?str.length()-1:str.length()-termKey.length()-1);
			if(isEncryptionFunction(term+"(")) {
				if(platformtype.contains("ali")&&term.contentEquals("hmac")) termObj.put("method", "hmacsha1");
				else termObj.put("method", term);
				
				termObj.put("encry_term", parseEncryptionTerm(raw_words,termVal,platformtype));
				if(termKey!=null) termObj.put("encry_key", termKey);
			}else {			
				List<JSONObject> termVals = parseEncryptionTerm(raw_words,termVal,platformtype);
				termObj.put(term, termVals.size()==1?termVals.get(0):termVals);
			}
			ans.add(termObj);
		}
		return ans;
	}
	private static void writeRawResult(Map<String,Object> raw_values,String filepath,String filename,String platformtype) {
		try {
        	File savefilepath = new File(filepath);
        	if (!savefilepath.exists()){
        		savefilepath.mkdir();
                System.out.println("Create dir: "+ savefilepath);
        	}
			File file1 = new File(filepath,filename);
			if (!file1.exists()) {
		        System.out.println("File does not exist");
		        file1.createNewFile();
		    }
			FileWriter fileWritter = new FileWriter(file1,false);
			JSONObject finalResult = new JSONObject();
			for(Map.Entry<String,Object> entry:raw_values.entrySet()) {
				if(entry.getKey().contains("username")||entry.getKey().contains("password")||entry.getKey().contains("clientID")) {
					if(entry.getValue()==null) continue;
					finalResult.put(entry.getKey(), (String)(entry.getValue()));
				}else {
					if(entry.getValue()==null) continue;
					if(entry.getKey().contains("topic")) {
						List<String> topics = new ArrayList<>();
						for(String topic:(List<String>)(entry.getValue())) {
							topics.add(topic.split(":")[0]);
						}
						finalResult.put(entry.getKey(), topics);
					}else {
						finalResult.put(entry.getKey(), (List<String>)(entry.getValue()));
					}
					
					
				}
			}
			fileWritter.write(finalResult.toString());
			
			fileWritter.close();
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private static void writeFinalResult(Map<String, List<String>> parameters,Map<String, List<String>> abwords_map,Map<String, String> raw_words,Map<String,JSONObject> encrypt_terms,Map<String,List<String>> terms_map_in_a_log,String filepath,String filename,String platformtype) {
		/******************
	     * Combine the parameter and raw_words
	     *******************/
		JSONObject finalResult = new JSONObject();
		finalResult.put("platform", platformtype);
		for(String key:parameters.keySet()) {
			List<String> values = parameters.get(key);
			List<JSONObject> msgObj = new ArrayList<>();
			for(String value:values) {
				if(Pattern.matches("(.)+\\((.)+\\)(.)*",value)) {
					JSONObject termObj= new JSONObject();
					if(!isEncryptionFunction(value)) {
						List<JSONObject> termList = new ArrayList<JSONObject>();
						String termKey = value.split("\\((.)+\\)")[0];
						if(platformtype.contentEquals("azure")&&termKey.contentEquals("username")) {
							String username = terms_map_in_a_log.get("CONNECT->username").get(0);
							JSONObject methodObj = new JSONObject();
							methodObj.put("method", "urlencode");
							termList.add(methodObj);
							String url = username.substring(0,username.indexOf("?"));
							JSONObject urlObj = new JSONObject();
							urlObj.put("url", url);
							termList.add(urlObj);
							JSONObject paramObj = new JSONObject();
							List<JSONObject> paramList = new ArrayList<>();
							String api_version = username.substring(username.indexOf("?")+1,username.indexOf("&"));
							JSONObject apiObj = new JSONObject();
							apiObj.put("api-version", api_version.substring(api_version.indexOf("=")+1));
							paramList.add(apiObj);
							String dtype = username.substring(username.indexOf("&")+1);
							JSONObject dtypeObj = new JSONObject();
							dtypeObj.put("DeviceClientType", dtype.substring(dtype.indexOf("=")+1));
							paramList.add(dtypeObj);
							paramObj.put("params", paramList);
							termList.add(paramObj);
  						}else {
							List<String> termKeyValues = abwords_map.get(key+"->"+termKey);
							for(String termKeyValue:termKeyValues) {
								JSONObject termKeyValueObj = new JSONObject();
								
								if(raw_words.containsValue(termKeyValue)) {
									List<String> tmp = new ArrayList<String>();
									for (String k : raw_words.keySet()) {
										if (raw_words.get(k).equals(termKeyValue)) {
											tmp.add(k);
										}
									}
									termKeyValueObj.put(termKeyValue,tmp);
								}else {
									termKeyValueObj.put(termKeyValue,new ArrayList<String>());
								}
								//ali refine
								if(platformtype.contains("alitcp")&&termKeyValue.contentEquals("deviceId")) {
									List<String> tmp = new ArrayList<String>();
									for (String k : raw_words.keySet()) {
										if (raw_words.get(k).equals("V0")) {
											tmp.add(k);
										}
									}
									termKeyValueObj.put(termKeyValue,tmp);
								}
								termList.add(termKeyValueObj);
							}
							
						}
						termObj.put(termKey,termList);
						msgObj.add(termObj);
					}else {
						String termKey = value.split("\\((.)+\\)")[0];
						if((platformtype+"->"+termKey).contentEquals("gcp->password")) {
							termObj.put("password", encrypt_terms.get("gcp->password"));
						}else if((platformtype+"->"+termKey).contentEquals("tuya->payload")) {
							List<String> tmp = new ArrayList<String>();
							for (String k : raw_words.keySet()) {
								if (raw_words.get(k).equals("payload")) {
									tmp.add(k);
								}
							}
							termObj.put(termKey, tmp);
						}
						else {
							List<String> termKeyValues = abwords_map.get(key+"->"+termKey);
							List<JSONObject> termKeyValuesObj = new ArrayList<>();
							for(String termKeyValue:termKeyValues) {
								termKeyValuesObj.add(parseEncryptionTerm(raw_words,termKeyValue,platformtype).get(0));
							}
							termObj.put(termKey, termKeyValuesObj);
						}
						
						msgObj.add(termObj);
					}
					
					
				}else {
					JSONObject termObj= new JSONObject();
					List<String> termList = new ArrayList<String>();
					if(raw_words.containsValue(value)) {
						for (String k : raw_words.keySet()) {
							if (raw_words.get(k).equals(value)) {
								termList.add(k);
							}
						}
					}
					//tuya refine
					if(platformtype.contains("tuya")&&value.contentEquals("username")) {
						for (String k : raw_words.keySet()) {
							if (raw_words.get(k).equals("clientID")) {
								termList.add(k);
							}
						}
					}
					
					termObj.put(value,termList);
					msgObj.add(termObj);
				}
			}
			finalResult.put(key,msgObj);
		}
		try {
        	File savefilepath = new File(filepath);
        	if (!savefilepath.exists()){
        		savefilepath.mkdir();
                System.out.println("Create dir: "+ savefilepath);
        	}
			File file1 = new File(filepath,filename);
			if (!file1.exists()) {
		        System.out.println("File does not exist");
		        file1.createNewFile();
		    }
			FileWriter fileWritter = new FileWriter(file1,false);
			fileWritter.write(finalResult.toString());
			
			fileWritter.close();
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    
}


