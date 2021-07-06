package mpinspector.mplearner;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import de.learnlib.api.EquivalenceOracle;
import de.learnlib.api.MembershipOracle;
import de.learnlib.oracles.DefaultQuery;
import net.automatalib.automata.UniversalDeterministicAutomaton;
import net.automatalib.automata.concepts.Output;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.commons.util.collections.CollectionsUtil;
import net.automatalib.util.automata.Automata;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;


public class ModifiedWMethodEQOracle<A extends UniversalDeterministicAutomaton<?, I, ?, ?, ?> & Output<I, D>, I, D>
		implements EquivalenceOracle<A, I, D> {

//refine DFA
	public static class DFAModifiedWMethodEQOracle<I> extends ModifiedWMethodEQOracle<DFA<?, I>, I, Boolean>
			implements DFAEquivalenceOracle<I> {
		
		public DFAModifiedWMethodEQOracle(int maxDepth, MembershipOracle<I, Boolean> sulOracle) {
			super(maxDepth, sulOracle);
			}
	}
//refine Mealy
	public static class MealyModifiedWMethodEQOracle<I, O> extends
		ModifiedWMethodEQOracle<MealyMachine<?, I, ?, O>, I, Word<O>> implements MealyEquivalenceOracle<I, O> {
		public MealyModifiedWMethodEQOracle(int maxDepth, MembershipOracle<I, Word<O>> sulOracle) {
			super(maxDepth, sulOracle);
		}
	}

	private int maxDepth;
	private final MembershipOracle<I, D> sulOracle;


	public ModifiedWMethodEQOracle(int maxDepth, MembershipOracle<I, D> sulOracle) {
		this.maxDepth = maxDepth;
		this.sulOracle = sulOracle;
	}
	
	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}
	
	/*
	* (non-Javadoc)
	* 
	* @see
	* de.learnlib.api.EquivalenceOracle#findCounterExample(java.lang.Object,
	* java.util.Collection)
	*/
	
	//rewrite findCounterExample
	@Override
	public DefaultQuery<I, D> findCounterExample(A hypothesis, Collection<? extends I> inputs) {
		List<Word<I>> transCover = Automata.transitionCover(hypothesis, inputs);
		List<Word<I>> charSuffixes = Automata.characterizingSet(hypothesis, inputs);

		if (charSuffixes.isEmpty())
			charSuffixes = Collections.singletonList(Word.<I> epsilon());

		WordBuilder<I> wb = new WordBuilder<>();

		DefaultQuery<I, D> query;  
		D hypOutput; 
		String output;           
		Word<I> queryWord;      
		boolean blacklisted;

		HashSet<Word<I>> blacklist = new HashSet<Word<I>>();  //build a new blacklist

		for (Word<I> trans : transCover) {  //
			query = new DefaultQuery<>(trans);
			sulOracle.processQueries(Collections.singleton(query));  
		
			hypOutput = hypothesis.computeOutput(trans);     //
			if (!Objects.equals(hypOutput, query.getOutput()))
				return query;
		
			output = query.getOutput().toString();  
		
			
			if (output.endsWith("ConnectionClosed") || output.endsWith("ConnectionClosedEOF") || output.endsWith("ConnectionClosedException")) {
				blacklist.add(trans);				
				continue;
			}
		
			//for(int start = 1; start < maxDepth; start++) {
			for (List<? extends I> middle : CollectionsUtil.allTuples(inputs, 1, maxDepth)) {
				wb.append(trans).append(middle);
				queryWord = wb.toWord();
				wb.clear();
	
				blacklisted = false;
				for(Word<I> w: blacklist) {
					if(w.isPrefixOf(queryWord)) {
						blacklisted = true;
						break;
					}
				}					
				if(blacklisted) continue;  //modifiedwmethod
	
				query = new DefaultQuery<>(queryWord);
				sulOracle.processQueries(Collections.singleton(query));
	
				hypOutput = hypothesis.computeOutput(queryWord);
	
				if (!Objects.equals(hypOutput, query.getOutput()))
					return query;
	
				output = query.getOutput().toString();
	
				if (output.endsWith("ConnectionClosed") || output.endsWith("ConnectionClosedEOF") || output.endsWith("ConnectionClosedException")) {
					
					blacklist.add(queryWord);
					continue;
				}
	
				for (Word<I> suffix : charSuffixes) {
					wb.append(trans).append(middle).append(suffix);
					queryWord = wb.toWord();
					wb.clear();
					
					
					query = new DefaultQuery<>(queryWord);
					hypOutput = hypothesis.computeOutput(queryWord);
					sulOracle.processQueries(Collections.singleton(query));
					
					if (!Objects.equals(hypOutput, query.getOutput()))
						return query;
					
					output = query.getOutput().toString();
					if (output.endsWith("ConnectionClosed") || output.endsWith("ConnectionClosedEOF") || output.endsWith("ConnectionClosedException")) {
						
						blacklist.add(queryWord);
					}
				}
			}
			//}
		}
		
		return null;
	}
}

