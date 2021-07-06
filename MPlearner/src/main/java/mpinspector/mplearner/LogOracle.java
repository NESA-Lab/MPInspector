package mpinspector.mplearner;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.annotation.ParametersAreNonnullByDefault;

import de.learnlib.api.MembershipOracle;
import de.learnlib.api.MembershipOracle.MealyMembershipOracle;
import de.learnlib.api.Query;
import de.learnlib.logging.LearnLogger;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;

@ParametersAreNonnullByDefault
public class LogOracle<I, D> implements MealyMembershipOracle<I, D> {
	
	
	//identifying mealylogOracle
	public static class MealyLogOracle<I, O> extends LogOracle<I, O> {
		public MealyLogOracle(StateLearnerSUL<I, O> sul, LearnLogger logger, boolean combine_query) {
			super(sul, logger, combine_query);
		}
	}
	
	LearnLogger logger;
	StateLearnerSUL<I,D> sul;
	boolean combine_query = false;
	Logger loglearner = Logger.getLogger("logmqtt.log");
	FileHandler fileHandler;
	public LogOracle(StateLearnerSUL<I,D> sul, LearnLogger logger, boolean combine_query) {
		this.sul = sul;
		this.logger = logger;
		this.combine_query = combine_query;
		
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
        
        loglearner.addHandler(fileHandler);
        loglearner.setUseParentHandlers(false);
        
	}
	
	public Word<D> answerQueryCombined(Word<I> prefix, Word<I> suffix){
		Word<I> query = prefix.concat(suffix);
		Word<D> response = null;
		Word<D> responsePrefix = null;
		Word<D> responseSuffix = null;
		
		try {
			this.sul.pre();
			response = this.sul.stepWord(query);
			
			if(query.length() != response.length()) {
				throw new RuntimeException("Received number of output symbols not equal to number of input symbols (" + query.length() + " input symbols vs " + response.length() + " output symbols)");
			}
			
			responsePrefix = response.subWord(0, prefix.length());
			responseSuffix = response.subWord(prefix.length(), response.length());
			
			logger.logQuery("[" + prefix.toString() + "|" + suffix.toString() + "/" + responsePrefix.toString() + "|" + responseSuffix.toString() + "]") ;	
			loglearner.info("The query result："+"[" + prefix.toString() + "|" + suffix.toString() + "/" + responsePrefix.toString() + "|" + responseSuffix.toString() + "]");
		}
		finally {
			sul.post();
		}
		
		return responseSuffix;
	}
	
	public Word<D> answerQuerySteps(Word<I> prefix, Word<I> suffix) {
		WordBuilder<D> wbPrefix = new WordBuilder<>(prefix.length());
		WordBuilder<D> wbSuffix = new WordBuilder<>(suffix.length());

		this.sul.pre();
		try {
			// Prefix: Execute symbols, only log output
			for(I sym : prefix) {
				wbPrefix.add(this.sul.step(sym));
			}
			
			// Suffix: Execute symbols, outputs constitute output word
			for(I sym : suffix) {
				wbSuffix.add(this.sul.step(sym));
			}
		
	    	logger.logQuery("[" + prefix.toString() + " | " + suffix.toString() +  " / " + wbPrefix.toWord().toString() + " | " + wbSuffix.toWord().toString() + "]");
	    	loglearner.info("The query result is ："+"[" + prefix.toString() + " | " + suffix.toString() +  " / " + wbPrefix.toWord().toString() + " | " + wbSuffix.toWord().toString() + "]");
		
		}
		finally {
			sul.post();
		}

		return wbSuffix.toWord();
    }
	
	@Override
	public Word<D> answerQuery(Word<I> prefix, Word<I> suffix){
		if(combine_query) {
			return answerQueryCombined(prefix, suffix);
		}else {
			return answerQuerySteps(prefix, suffix);
		}
	}
	
	@Override
    @SuppressWarnings("unchecked")
	public Word<D> answerQuery(Word<I> query) {
		return answerQuery((Word<I>)Word.epsilon(), query);
    }

    @Override
    public MembershipOracle<I, Word<D>> asOracle() {
    	return this;
    }
    
    @Override
	public void processQueries(Collection<? extends Query<I, Word<D>>> queries) {
		for (Query<I,Word<D>> q : queries) {
			Word<D> output = answerQuery(q.getPrefix(), q.getSuffix());
			q.answer(output);
		}
	}

}
