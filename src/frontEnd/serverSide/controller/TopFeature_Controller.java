/**
 * 
 */
package frontEnd.serverSide.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import edu.pitt.cs.nih.backend.utils.TextUtil;
import frontEnd.serverSide.model.FeatureWeight;

/**
 * @author Phuong Pham
 *
 */
public class TopFeature_Controller {
	public TopFeature_Controller(){}
	
	public void extractMatchedUnigram(List<FeatureWeight> featureWeightList,
			List<List<String[]>> allTokenList) throws Exception {
		List<String[]> tokenList = allTokenList.get(0);
		List<String[]> stemmedList = allTokenList.get(1);
		
		FeatureWeight fw;
		List<Set<String>> matchedList = new ArrayList<>();
		Set<String> matchedListPerFeature;
		for(int iFeature = 0; iFeature < featureWeightList.size(); iFeature++) {
			matchedListPerFeature = new TreeSet<>();
			matchedList.add(matchedListPerFeature);
		}
		
		for(int iSentence = 0; iSentence < stemmedList.size(); iSentence++) {
			for(int iWord = 0; iWord < stemmedList.get(iSentence).length; iWord++) {
//				System.out.print(stemmedList.get(iSentence)[iWord] + "/" + tokenList.get(iSentence)[iWord]);
				for(int iFeature = 0; iFeature < featureWeightList.size(); iFeature++) {
					fw = featureWeightList.get(iFeature);
//					System.out.println(iWord + ": " + fw.getTerm() + "/" + featureWeightList.size());					
					if(stemmedList.get(iSentence)[iWord].equals(fw.getTerm().toLowerCase())) {
						matchedList.get(iFeature).add(tokenList.get(iSentence)[iWord]);
//						System.out.print("*");
						break; // all features are different, matched this feature will terminate the others
					}
				}
//				System.out.println();
			}
		}
		
		for(int iFeature = 0; iFeature < featureWeightList.size(); iFeature++) {
			fw = featureWeightList.get(iFeature);
			fw.setMatchedList(new ArrayList<>(matchedList.get(iFeature)));
		}
	}
	
	/**
	 * Tokenize a document.
	 * Output is 2 list: (1) original word tokens; (2) stemmed word tokens
	 * 
	 * @param document
	 * @return
	 * @throws Exception
	 */
	public List<List<String[]>> getStemmedTokenList(String document) throws Exception {
		List<List<String[]>> allTokenList = new ArrayList<>();
		allTokenList.add(TextUtil.extractWordsByStandfordParser(document));
		List<String[]> stemmedTokenList = new ArrayList<>();
		
//		Set<String> unigramList = new TreeSet<>();
		
		for(int iSentence = 0; iSentence < allTokenList.get(0).size(); iSentence++) {
			String[] wordList = new String[allTokenList.get(0).get(iSentence).length];
			for(int iWord = 0; iWord < wordList.length; iWord++) {
				wordList[iWord] = TextUtil.stemWord(allTokenList.get(0).get(iSentence)[iWord]).toLowerCase();
//				unigramList.add(wordList[iWord]);
			}
			stemmedTokenList.add(wordList);
		}
		allTokenList.add(stemmedTokenList);
		
//		System.out.println("original text");
//		for(String unigram : unigramList) {
//			System.out.println(unigram);
//		}
		
		return allTokenList;
	}
}
