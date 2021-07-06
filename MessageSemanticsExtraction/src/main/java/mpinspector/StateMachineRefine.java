package main.java.mpinspector;

import java.util.List;
import java.util.Map;


// discard this file  as we use python script to finish this interface
public class StateMachineRefine {
	Map<String, List<String>> packets_abterms;
	String file_to_be_refined = "empty.dot";
	boolean tlsflag = false;
	
	
	public void StateMachineRefine() {
		// open raw dot file
		
		
		//delete useless transistions
		
		
		//if tls flag 
		
		
		
		//for provision
		
		
		
		//add the terms 
	}

	public void setMap(Map<String, List<String>> packets_abterms2) {
		// TODO Auto-generated method stub
		this.packets_abterms = packets_abterms2;
	}

	public void setfilename(String string) {
		// TODO Auto-generated method stub
		this.file_to_be_refined = string;
	}

	public void setTlsFlag(boolean b) {
		// TODO Auto-generated method stub
		this.tlsflag = b;
	}

}
