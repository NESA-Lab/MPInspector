package mpinspector.mplearner;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class LearningConfig {
	//hashtable store key-value
	//
	protected Properties properties;
	
	String output_dir = "output";
	
	String learning_algorithm = "lstar";
	String eqtest = "randomwords";
	String type;
	//if use W-method and wp-method
	int max_depth = 10;
	
	//used for random words
	int min_length = 5;
	int max_length = 10;
	int nr_queries = 100;
	int seed = 1;
	
	//use for learning strategy
	int threshold = 2;
	
	
	
	public LearningConfig(String filename) throws IOException{
		properties = new Properties();
		
		InputStream input = new FileInputStream(filename);
		properties.load(input);
		
		loadProperties();
	}
	
	public LearningConfig(LearningConfig config) {
		properties = config.getProperties();
		loadProperties();
	}
	
	public Properties getProperties() {
		return properties;
	}
	
	public void loadProperties() {
		if(properties.getProperty("output_dir") != null)
			output_dir = properties.getProperty("output_dir");
		
		if(properties.getProperty("learning_algorithm").equalsIgnoreCase("lstar") || properties.getProperty("learning_algorithm").equalsIgnoreCase("dhc") || properties.getProperty("learning_algorithm").equalsIgnoreCase("kv") || properties.getProperty("learning_algorithm").equalsIgnoreCase("ttt") || properties.getProperty("learning_algorithm").equalsIgnoreCase("mp") || properties.getProperty("learning_algorithm").equalsIgnoreCase("rs"))
			learning_algorithm = properties.getProperty("learning_algorithm").toLowerCase();
		
		if(properties.getProperty("eqtest") != null && (properties.getProperty("eqtest").equalsIgnoreCase("wmethod") || properties.getProperty("eqtest").equalsIgnoreCase("modifiedwmethod") || properties.getProperty("eqtest").equalsIgnoreCase("wpmethod") || properties.getProperty("eqtest").equalsIgnoreCase("randomwords")))
			eqtest = properties.getProperty("eqtest").toLowerCase();
		
		if(properties.getProperty("max_depth") != null)
			max_depth = Integer.parseInt(properties.getProperty("max_depth"));
		
		if(properties.getProperty("min_length") != null)
			min_length = Integer.parseInt(properties.getProperty("min_length"));
		
		if(properties.getProperty("max_length") != null)
			max_length = Integer.parseInt(properties.getProperty("max_length"));
		
		if(properties.getProperty("nr_queries") != null)
			nr_queries = Integer.parseInt(properties.getProperty("nr_queries"));
		
		if(properties.getProperty("threshold") != null)
			threshold = Integer.parseInt(properties.getProperty("threshold"));
		
		if(properties.getProperty("seed") != null)
			seed = Integer.parseInt(properties.getProperty("seed"));
		if(properties.getProperty("type") != null)
			type = properties.getProperty("type");
	}
	
	
}
