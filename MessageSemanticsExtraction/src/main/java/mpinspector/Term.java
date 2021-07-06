package main.java.mpinspector;

import java.util.List;

public class Term {
	private String name_term;
	private List<String> value_term;
	
	public Term() {
		super();
	}
	
	public void setValueList(List<String> value_term) {
		this.value_term = value_term;
	}
	
	public void addValueList(String value) {
		this.value_term.add(value);
	}
	
	public void setTermName(String name_term) {
		this.name_term = name_term;
	}
	
	public List<String> getValueList(){
		return this.value_term;
	}
	
	public String getTermName() {
		return this.name_term;
	}
}
