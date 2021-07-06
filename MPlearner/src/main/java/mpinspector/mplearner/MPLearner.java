package mpinspector.mplearner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.codec.digest.DigestUtils;

import de.learnlib.acex.analyzers.AcexAnalyzers;
import de.learnlib.algorithms.dhc.mealy.MealyDHC;
import de.learnlib.algorithms.kv.mealy.KearnsVaziraniMealy;
import de.learnlib.algorithms.lstargeneric.mealy.ExtensibleLStarMealyBuilder;
import de.learnlib.algorithms.malerpnueli.MalerPnueliMealy;
import de.learnlib.algorithms.rivestschapire.RivestSchapireMealy;
import de.learnlib.algorithms.ttt.mealy.TTTLearnerMealy;

import de.learnlib.api.EquivalenceOracle;
import de.learnlib.api.LearningAlgorithm;
import de.learnlib.counterexamples.AcexLocalSuffixFinder;
import de.learnlib.eqtests.basic.RandomWordsEQOracle.MealyRandomWordsEQOracle;
import de.learnlib.eqtests.basic.WMethodEQOracle;
import de.learnlib.eqtests.basic.WpMethodEQOracle;
import de.learnlib.logging.LearnLogger;
import de.learnlib.oracles.CounterOracle.MealyCounterOracle;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.oracles.SULOracle;
import de.learnlib.statistics.Counter;
import de.learnlib.statistics.SimpleProfiler;
import mpinspector.mplearner.LogOracle.MealyLogOracle;
import mpinspector.mplearner.ModifiedWMethodEQOracle.MealyModifiedWMethodEQOracle;
import mpinspector.mplearner.mqtt.MPConfig;
import mpinspector.mplearner.mqtt.TestSUL;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.util.graphs.dot.GraphDOT;
import net.automatalib.words.Word;
import net.automatalib.words.impl.SimpleAlphabet;
import de.learnlib.cache.mealy.MealyCacheOracle;



public class MPLearner {
	LearningConfig config;                     //learner configuration
	boolean combine_query = false;
	SimpleAlphabet<String> alphabet;  //input alphabet
	
	//Membership query
	StateLearnerSUL<String, String> sul; //
	SULOracle<String, String> memOracle;  // membership oracle
	MealyLogOracle<String, String>logMemOracle; //a log class, customized 
	MealyCounterOracle<String,String> statesMemOracle; //have to calculate real queries that interact with the system, oracle is used to count,
	MealyCacheOracle<String, String> cachedMemOracle; //cache membership oracle
	MealyCounterOracle<String, String> statesCachedMemOracle;  //calculate the numbers of membership query in the cache 
	LearningAlgorithm<MealyMachine<?,String,?,String>, String, Word<String>> learningAlgorithm;
	//membership query algoritm
	
	//Equivenlence query 
	SULOracle<String,String> eqOracle;
	MealyLogOracle<String, String> logEqOracle;
	MealyCounterOracle<String, String> statesEqOracle;
	MealyCacheOracle<String, String> cachedEqOracle;
	MealyCounterOracle<String, String> statesCachedEqOracle;
	EquivalenceOracle<MealyMachine<?,String,?,String>, String, Word<String>> equivalenceAlgorithm;
	//eq query algorithm
	Logger loglearner = Logger.getLogger("logmqtt.log");
	FileHandler fileHandler;
	
	public MPLearner(LearningConfig config) throws Exception{
		this.config = config;
		try {
			fileHandler = new FileHandler(config.output_dir+"logmqtt.log");
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
        
        loglearner.addHandler(fileHandler);
        loglearner.setUseParentHandlers(false);
		
		Path path = Paths.get(config.output_dir);
		if(Files.notExists(path)) {
			Files.createDirectories(path);
		}
		
		configureLogging(config.output_dir);
		
		LearnLogger log = LearnLogger.getLogger(MPLearner.class.getSimpleName());
		
		
		//MQTTConfig mqttConfig = new MQTTConfig(config);
		sul =new TestSUL(new MPConfig(config));
		//System.out.println("subtopic: in MQTTLearner" + mqttConfig.subTopic);
		//sul and alphabet
		
		log.info("Using "+ config.type+" SUL");
		alphabet = ((TestSUL)sul).getAlphabet();
		//load learning algorithm / membership query algorithm
		loadLearnAlgorithm(config.learning_algorithm,alphabet,sul);
		//load equivelence algorithm
		loadEquivalenceAlgorithm(config.eqtest,alphabet,sul);
	}
	
	
	/**************************************
	 * load membership query algorithm
	 ******************************************/
	
	public void loadLearnAlgorithm(String algorithm, SimpleAlphabet<String> alphabet, StateLearnerSUL<String,String> sul) throws Exception {
		//
		//memOracle = new SULOracle<String, String>(sul);
		//add a logging oracle
		logMemOracle = new MealyLogOracle<String, String>(sul, LearnLogger.getLogger("Learning_queries"), combine_query);
		//counter oracle to calculate the queries sent to the SUL
		statesMemOracle = new MealyCounterOracle<String,String>(logMemOracle,"Membershio queries to SUL");
		//cache oracle to prevent duplicate queries to the SUL
		//cachedMemOracle = MealyCacheOracle.createDAGCacheOracle(alphabet, stateMemOracle);
		statesCachedMemOracle = new MealyCounterOracle<String, String>(statesMemOracle, "membership queries to cache");
		
		
		//
		//active model learning algorithm：ADT，DHC,DT,KV,L*,NL*,TTT,  
		switch(algorithm.toLowerCase()) {
		//L* cen generate mealy and DFA
		case "lstar":
			learningAlgorithm = new ExtensibleLStarMealyBuilder<String, String>().withAlphabet(alphabet).withOracle(statesCachedMemOracle).create();
			break;
			 	
		case "dhc":
			learningAlgorithm = new MealyDHC<String, String>(alphabet, statesCachedMemOracle);
			break;
			
		case "kv":
			learningAlgorithm = new KearnsVaziraniMealy<String, String>(alphabet, statesCachedMemOracle, true, AcexAnalyzers.BINARY_SEARCH);
			break;
			
		case "ttt":
			AcexLocalSuffixFinder suffixFinder = new AcexLocalSuffixFinder(AcexAnalyzers.BINARY_SEARCH, true, "Analyzer");
			learningAlgorithm = new TTTLearnerMealy<String, String>(alphabet, statesCachedMemOracle, suffixFinder);
			break;
		case "mp":
			learningAlgorithm = new MalerPnueliMealy<String, String>(alphabet, statesCachedMemOracle);
			break;
			
		case "rs":
			learningAlgorithm = new RivestSchapireMealy<String, String>(alphabet, statesCachedMemOracle);
			break;
	


		default:
			throw new Exception("Unknown learning algorithm " + config.learning_algorithm);
		}
		
	}
	/**************************************
	 * load equivalence algorithm
	 ******************************************/
	//equivelence query algorithm random words，random walk， w-method，w-method-random，wp，wp-ramdom
	public void loadEquivalenceAlgorithm(String algorithm, SimpleAlphabet<String> alphabet, StateLearnerSUL<String, String> sul) throws Exception {
		//
		//eqOracle = new SULOracle<String, String>(sul);
		// Create the equivalence oracle
		// Add a logging oracle
		logEqOracle =new MealyLogOracle<String, String>(sul, LearnLogger.getLogger("equivalence_queries"), combine_query);
		//cache oracle to count the numbers of query
		statesEqOracle = new MealyCounterOracle<String, String>(logEqOracle, "equivalence queries to SUL");
		//用
		statesCachedEqOracle = new MealyCounterOracle<String, String>(statesEqOracle, "equivalence queries to cache");
		// Instantiate the selected equivalence algorithm
		switch(algorithm.toLowerCase()) {
			case "wmethod":
				equivalenceAlgorithm = new WMethodEQOracle.MealyWMethodEQOracle<String, String>(config.max_depth, statesCachedEqOracle);
				break;
				
			//new algorithm
			case "modifiedwmethod":
				equivalenceAlgorithm = new MealyModifiedWMethodEQOracle<String, String>(config.max_depth, statesCachedEqOracle);
				break;
				
			case "wpmethod":
				equivalenceAlgorithm = new WpMethodEQOracle.MealyWpMethodEQOracle<String, String>(config.max_depth, statesCachedEqOracle);
				break;
				
			case "randomwords":
				equivalenceAlgorithm = new MealyRandomWordsEQOracle<String, String>(statesCachedEqOracle, config.min_length, config.max_length, config.nr_queries, new Random(config.seed));
				break;
				
			default:
				throw new Exception("Unknown equivalence algorithm " + config.eqtest);
				}	
	
	}
	
	/**************************************
	 * start learning, core steps
	 * @throws Exception 
	 ******************************************/
	public void learn() throws Exception {
		LearnLogger log = LearnLogger.getLogger(MPLearner.class.getSimpleName());   
		log.info( "Using learning algorithm " + learningAlgorithm.getClass().getSimpleName());
		log.info("Using equivalence algorithm " + equivalenceAlgorithm.getClass().getSimpleName());
		log.info("Start Learning");
		
		loglearner.info( "Using learning algorithm " + learningAlgorithm.getClass().getSimpleName());
		loglearner.info("Using equivalence algorithm " + equivalenceAlgorithm.getClass().getSimpleName());
		loglearner.info("Start Learning");
		
		SimpleProfiler.start("Total time");
		List<List<String>> HypotheisIndex = new ArrayList<List<String>>();
		boolean learning = true;
		Counter round = new Counter("Round","");
		round.increment();
		log.logPhase("Starting round" + round.getCount());
		loglearner.info("Starting round" + round.getCount());
		
		SimpleProfiler.start("Learning");
		learningAlgorithm.startLearning();  //start learning, return a hypothesis
		SimpleProfiler.stop("Learning");
		//start with membership query which has an interface processQueries 
		MealyMachine<?, String, ?, String> hypothesis = learningAlgorithm.getHypothesisModel();
		loglearner.info("stop first round learning.");
		//boolean breakflag = false;
		while(learning) {
			//store the output
			writeDotModel(hypothesis, alphabet, config.output_dir + "/hypothesis_" + round.getCount() + ".dot");
			if(round.getCount() > 1) {
				for (int i = 0; i < HypotheisIndex.size(); i++) {
					String BeComparedFile = HypotheisIndex.get(i).get(0);
					File file1 = new File(config.output_dir + "/hypothesis_" + round.getCount() + ".dot");
					File file2 = new File(BeComparedFile);
					if (file1.length() == file2.length()) {
			            System.out.println("+++++++++ unequal +++++++++++++");
			            InputStream fileStream1 = new FileInputStream(file1);
				        InputStream fileStream2 = new FileInputStream(file2);
				        byte[] file_1 = new byte[fileStream1.available()];
				        byte[] file_2 = new byte[fileStream2.available()];
				        if(isSameFiles(file_1, file_2)) {
				        	HypotheisIndex.get(i).add(config.output_dir + "/hypothesis_" + round.getCount() + ".dot");
				        	if(HypotheisIndex.get(i).size()>=config.threshold)
				        	{
				        		//if there is no counter example stop learning
								learning = false;
								//store
								writeDotModel(hypothesis, alphabet, config.output_dir + "/learnedModel.dot");
								//breakflag = true;
								break;
				        	}
				        }else {
				        	List<String> tmpList = new ArrayList<String>();
				        	tmpList.add(config.output_dir + "/hypothesis_" + round.getCount() + ".dot");
				        	HypotheisIndex.add(tmpList);
				        }
					}else {
						List<String> tmpList = new ArrayList<String>();
			        	tmpList.add(config.output_dir + "/hypothesis_" + round.getCount() + ".dot");
			        	HypotheisIndex.add(tmpList);
					}
				}
			}else {
				List<String> SameHypothesisList = new ArrayList<String>();
				SameHypothesisList.add(config.output_dir + "/hypothesis_" + round.getCount() + ".dot");
				HypotheisIndex.add(SameHypothesisList);
			}
			
			if(learning == false) {
				break;
			}
			
			//if two file is the same stop learning  here can add customized count
//			if(round.getCount() >1) {
//				 File file1 = new File(config.output_dir + "/hypothesis_" + round.getCount() + ".dot");
//			     File file2 = new File(config.output_dir + "/hypothesis_" + (round.getCount()-1)+ ".dot");
//			        if (file1.length() == file2.length()) {
//			            System.out.println("+++++++++ unequal +++++++++++++");
//			            InputStream fileStream1 = new FileInputStream(file1);
//				        InputStream fileStream2 = new FileInputStream(file2);
//				        byte[] file_1 = new byte[fileStream1.available()];
//				        byte[] file_2 = new byte[fileStream2.available()];
//				        if(isSameFiles(file_1, file_2)) {
//				        	//if there is no counter example stop learning
//							learning = false;
//							//store
//							writeDotModel(hypothesis, alphabet, config.output_dir + "/learnedModel.dot");
//							break;
//				        }
//				        
//			        }
//			}
			
			loglearner.info("Start Searching for counter-example using equivalence");
			
			SimpleProfiler.start("Searching for counter-example");
			
			DefaultQuery<String, Word<String>> counterExample = equivalenceAlgorithm.findCounterExample(hypothesis, alphabet);
			SimpleProfiler.stop("Searching for counter-example");
			loglearner.info("Stop Searching for counter-example using equivalence");
			if(counterExample == null) {
				
				learning = false;
				//
				loglearner.info("No counterexample found and finish learning");
				writeDotModel(hypothesis, alphabet, config.output_dir + "/learnedModel.dot");
				//writeAutModel(hypothesis, alphabet, config.output_dir + "/learnedModel.aut");
			}else {
				//find the counter example which is used for refine the hypothesis
				log.logCounterexample("Counter-example found " + counterExample.toString());
				loglearner.info("Counter-example found " + counterExample.toString());			
				log.logPhase("Counter example is found" );
				round.increment();
				log.logPhase("Starting round " + round.getCount());
				loglearner.info("Starting round " + round.getCount());
				log.logPhase("start refinehypothesis !" );
				
				SimpleProfiler.start("Learning");
				
				//refine
				boolean refined = learningAlgorithm.refineHypothesis(counterExample);
				SimpleProfiler.stop("Learning");
				loglearner.info("stop refinehypothesis.");
				hypothesis = learningAlgorithm.getHypothesisModel();
				loglearner.info("get new hypothesis.");
				

				if(!refined) { 
					learning = false;
					log.logPhase("No refinement effected by counterexample! #############");
					loglearner.info("No refinement effected by counterexample! #############");
					//SimpleProfiler.stop("Learning");
					log.logPhase("######################################stop learning");
					loglearner.info("######################################stop learning");
					writeDotModel(hypothesis, alphabet, config.output_dir + "/learnedModel.dot");
				}else {
					//refined and a new round learning 
					log.logPhase("hypothesis is refined refineHypothesis(counterExample) ###");
				}
				
			}
		}
		
		SimpleProfiler.stop("Total time");
		// Output statistics
		log.info( "-------------------------------------------------------");
		log.info( SimpleProfiler.getResults());
		log.info( round.getSummary());
		log.info(statesMemOracle.getStatisticalData().getSummary());
		log.info( statesCachedMemOracle.getStatisticalData().getSummary());
		log.info(statesEqOracle.getStatisticalData().getSummary());
		log.info(statesCachedEqOracle.getStatisticalData().getSummary());
		log.info( "States in final hypothesis: " + hypothesis.size());
	}
	
	
	/**************************************
	 * output dot file
	 ******************************************/
	public static void writeDotModel(MealyMachine<?, String, ?, String> model, SimpleAlphabet<String> alphabet, String filename) throws IOException, InterruptedException {
		// Write output to dot-file
		File dotFile = new File(filename);
		PrintStream psDotFile = new PrintStream(dotFile);
		GraphDOT.write(model, alphabet, psDotFile);//
		psDotFile.close();
		
		//TODO Check if dot is available
		
		// Convert .dot to .pdf
		Runtime.getRuntime().exec("dot -Tpdf -O " + filename);
	}
	
	/**************************************
	 * 
	 ******************************************/
	public void configureLogging(String output_dir) throws SecurityException, IOException {
		LearnLogger loggerLearnlib = LearnLogger.getLogger("de.learnlib");
		loggerLearnlib.setLevel(Level.ALL);
		FileHandler fhLearnlibLog = new FileHandler(output_dir + "/learnlib.log");
		loggerLearnlib.addHandler(fhLearnlibLog);
		fhLearnlibLog.setFormatter(new SimpleFormatter());
		
		LearnLogger loggerLearner = LearnLogger.getLogger(MPLearner.class.getSimpleName());
		loggerLearner.setLevel(Level.ALL);
		FileHandler fhLearnerLog = new FileHandler(output_dir + "/learner.log");
		loggerLearner.addHandler(fhLearnerLog);
		fhLearnerLog.setFormatter(new SimpleFormatter());
		loggerLearner.addHandler(new ConsoleHandler());
		
		LearnLogger loggerLearningQueries = LearnLogger.getLogger("learning_queries");
		loggerLearningQueries.setLevel(Level.ALL);
		FileHandler fhLearningQueriesLog = new FileHandler(output_dir + "/learning_queries.log");
		loggerLearningQueries.addHandler(fhLearningQueriesLog);
		fhLearningQueriesLog.setFormatter(new SimpleFormatter());
		loggerLearningQueries.addHandler(new ConsoleHandler());		

		LearnLogger loggerEquivalenceQueries = LearnLogger.getLogger("equivalence_queries");
		loggerEquivalenceQueries.setLevel(Level.ALL);
		FileHandler fhEquivalenceQueriesLog = new FileHandler(output_dir + "/equivalence_queries.log");
		loggerEquivalenceQueries.addHandler(fhEquivalenceQueriesLog);
		fhEquivalenceQueriesLog.setFormatter(new SimpleFormatter());
		loggerEquivalenceQueries.addHandler(new ConsoleHandler());	
	}
	
	
	
	
	
	/**************************************
	 * test main function (test for config)
	 ******************************************/

//	public static void main(String[] args) throws Exception {
//		
//		//inputL: file name
//		LearningConfig config = new LearningConfig("F:\\javaworkspace\\mplearner\\example\\serveramqp.properties");
//		
//		
//		MPLearner learner = new MPLearner(config);
//		
//		//test if the config is correctly loaded
//		
//		
//		learner.learn();
//	}
	
	
	
	
	/**************************************
	 * main function
	 ******************************************/
	public static void main(String[] args) throws Exception {
		if(args.length < 1) {
			System.err.println("Invalid number of parameters");
			System.exit(-1);
		}
		
		//learning config
		LearningConfig config = new LearningConfig(args[0]);
		
		MPLearner learner = new MPLearner(config);
		
		//test if the config is correctly loaded
		
		learner.learn();
		
		//learner.semanticextraction();
	}
	
	
	
	/**
     * Verify if the two files are the same 
     * @return boolean true 
     * @throws IOException
     */
    private static boolean isSameFiles(byte[] fileByte1, byte[] fileByte2) {
        String firstFileMd5 = DigestUtils.md5Hex(fileByte1);
        String secondFileMd5 = DigestUtils.md5Hex(fileByte2);
        if (firstFileMd5.equals(secondFileMd5)) {
            System.out.println("---- equals ------ md5 " + firstFileMd5);
            return true;
        } else {
            System.out.println(firstFileMd5 + " is firstFileMd5 ++ unequal ++ secondFileMd5 = " + secondFileMd5);
            return false;
        }
    }
	
}
