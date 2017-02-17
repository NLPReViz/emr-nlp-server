/**
 * 
 */
package io.github.nlpreviz.nlp.featureVector;

import io.github.nlpreviz.nlp.utils.TextUtil;
import io.github.nlpreviz.nlp.utils.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * @author Phuong Pham
 * An inherited class of FeatureSet
 * This class create n-gram features
 */
public class FeatureSetNGram extends FeatureSet {
	protected String[] m_FullFeatureList;

	public FeatureSetNGram(boolean removeStopWord, boolean removePunctuation,
            boolean stemWord, boolean caseSensitive, boolean removeHeaderFooter)
                throws Exception {
            
            super(removeStopWord, removePunctuation, stemWord, caseSensitive,
                    removeHeaderFooter);
        }	

	/**
	 * Get ngram feature vector
	 * <p>
	 * This method scan through the dataset to get a global feature vector. Then 
	 * it discards all features of which occurrence less than the threshold. Then, it 
	 * saves the global feature vector. Finally, feature vector of each instance is 
	 * built and added into a FeatureVector object.
	 * 
	 * @param ngram
	 * @param binary Binary ngram or frequency ngram
	 * @param threshold
	 * @param fn_GlobalFeatureVector file name used to save the global feature vector for 
	 * each data set
	 * @return A FeatureVector object contains all instance feature vectors in the dataset
	 * @throws Exception
	 */
	public FeatureVector getFeatureVector(int ngram, int threshold,
			boolean binary, String fn_GlobalFeatureVector) throws Exception {
		// get the global feature vector only
		String[] globalFeatureVector = getGlobalFeatureVectorFromText(ngram, threshold, binary);
		// save the global feature vector
		Util.saveList(fn_GlobalFeatureVector, globalFeatureVector);
		
		// load the global feature vector
		globalFeatureVector = Util.loadList(fn_GlobalFeatureVector);
		FeatureVector featureVector = getInstanceFeatureVectorWithoutUnknownNgram(
				globalFeatureVector, ngram, binary);
		
		return featureVector;
	}
        
        /**
         * Default setting for getFeatureVectorFromGlobalFeatureVector.
         * 
         * @param globalFeatureVector
         * @return
         * @throws Exception 
         */
        public FeatureVector getFeatureVectorFromGlobalFeatureVector(String[] globalFeatureVector) throws Exception {
            int ngram = 1;
            boolean binaryFeature = true;
            FeatureVector featureVector = getInstanceFeatureVectorWithoutUnknownNgram(
                    globalFeatureVector, ngram, binaryFeature);

            return featureVector;
        }
        
        /**
	 * Get ngram feature vector
	 * <p>
	 * This method scan through the dataset to get a global feature vector from a 
         * pre-built global feature vector
	 * 
	 * @param ngram
	 * @param binary Binary ngram or frequency ngram
	 * @param threshold
	 * @param globalFeatureVector a string array of feature names
	 * @return A FeatureVector object contains all instance feature vectors in the dataset
	 * @throws Exception
	 */
	public FeatureVector getFeatureVectorFromGlobalFeatureVector(int ngram, int threshold,
			boolean binary, String[] globalFeatureVector) throws Exception {

            FeatureVector featureVector = getInstanceFeatureVectorWithoutUnknownNgram(
				globalFeatureVector, ngram, binary);
		
		return featureVector;
	}
        
        /**
         * Get global feature vector from the data set
         * 
         * @param ngram
         * @param threshold
         * @param binary
         * @return
         * @throws Exception 
         */
        public String[] getGlobalFeatureVector(int ngram, int threshold, boolean binary) throws Exception {
            return getGlobalFeatureVectorFromText(ngram, threshold, binary);
        }
        
        /**
         * Default setting for global feature vector.
         * 
         * @return
         * @throws Exception 
         */
        public String[] getGlobalFeatureVector() throws Exception {
            int ngram = 1;
            int threshold = 1;
            boolean binary = true;
            return getGlobalFeatureVectorFromText(ngram, threshold, binary);
        }
        
	
	/**
	 * Get ngram global feature vector
	 * <p>
	 * This method scan through the dataset to get a global feature vector. Then 
	 * it discards all features of which occurrence less than the threshold.
	 * 
	 * @param ngram
	 * @param threshold
	 * @param binary
	 * @return
	 * @throws Exception
	 */
	public String[] getGlobalFeatureVectorFromText(int ngram, int threshold,
			boolean binary) throws Exception {
		// list of features in the dataset
		HashMap<String, Integer> datasetFeature = new HashMap<String, Integer>();
		// list of features in each instance
//		HashMap<String, HashMap<String, Integer>> instanceLocalFeatures = 
//				new HashMap<String, HashMap<String, Integer>>();

		for(String instanceID : m_Instances.keySet()) {
			HashMap<String, Integer> localFeature = 
					getInstanceLocalFeatures(instanceID, ngram);
//			instanceLocalFeatures.put(instanceID, localFeature);
			// append the local feature to the global (dataset) feature
			for(String feature : localFeature.keySet()) {
				int count = localFeature.get(feature);
				if(datasetFeature.containsKey(feature)) {
					count += datasetFeature.get(feature);
				}
				datasetFeature.put(feature, count);
			}
		}

		// filter global feature set by threshold
		ArrayList<String> filteredFeature = new ArrayList<String> ();
		for(String feature : datasetFeature.keySet()) {
			if(datasetFeature.get(feature) >= threshold)
				filteredFeature.add(feature);
		}
		// sort the global feature vector
		Collections.sort(filteredFeature);
		return filteredFeature.toArray(new String[filteredFeature.size()]);
	}
	
	/**
	 * Build n-gram vectors of instances from the global feature vector. 
	 * We only need to count the frequency of each feature in each instance.
	 *  
	 * @param featureList The global feature vector
	 * @param ngram N-gram
	 * @param binary Binary feature or frequency feature
	 * @return A feature vector object contains feature vectors of all instances 
	 * of this feature set object
	 * @throws Exception
	 */
	public FeatureVector getInstanceFeatureVectorWithoutUnknownNgram(String[] featureList, 
			int ngram, boolean binary) throws Exception{
		// initialize feature vectors for dataset
		FeatureVector featureVectors = new FeatureVector();
		// try to sort the instance IDs, can use without sort for speeding up
				ArrayList<String> instanceIDList = new ArrayList<String>(
						m_Instances.keySet());
				Collections.sort(instanceIDList);
				String[] instanceIDSortedList = instanceIDList.toArray(
						new String[m_Instances.size()]);
		// create feature vector table
		// row = an instance
		// col = a feature
		featureVectors.setFeature_InstanceIDList(featureList, 
				instanceIDSortedList);
		// scan through each document
		int nInstances = featureVectors.m_InstanceID.length;
		for(int instanceCount = 0; instanceCount < nInstances; 
				instanceCount++) {
			String instanceID = instanceIDSortedList[instanceCount];
			// scan through this instance, get the n-gram feature vector of this instance
			HashMap<String, Integer> instanceFeature = 
					getInstanceLocalFeatures(instanceID, ngram);
			// match to the total feature vector
			for(int featureCount = 0; featureCount < featureVectors.m_Feature.length; 
					featureCount++) {
				String feature = featureVectors.m_Feature[featureCount];
				// if the report has this feature
				if(instanceFeature.containsKey(feature)) {
					featureVectors.setFeatureValue(instanceCount, featureCount,
							binary ? 1 : instanceFeature.get(feature));
				}
			}
//			// feedback
//			System.out.println("completed feature vector of document " + 
//					String.format("%d", instanceCount + 1) + "/" + String.format("%d", nInstances));
		}
		return featureVectors;
	}
	
	/**
	 * Get all n-gram features in the document.
	 * <p>
	 * Scan through each sentence and extract ngrams
	 * 
	 * @param instanceID
	 * @param ngram
	 * @return A HashMap <ngram, count>
	 * @throws Exception
	 */
	private HashMap<String, Integer> getInstanceLocalFeatures(String instanceID, 
			int ngram) throws Exception {
		HashMap<String, Integer> featureList = new HashMap<String, Integer>();
		ArrayList<String[]> unigramList = getInstanceUnigramList(instanceID);
		for(String[] line : unigramList) {			
			for(int wordCount = 0; wordCount < line.length - ngram + 1; wordCount++) {
				String[] ngramList = new String[ngram]; 
				System.arraycopy(line, wordCount, ngramList, 0, ngram);
				String key = Util.joinString(ngramList, " ");
				// skip empty terms
				if(key.equals(""))
					continue;
				if(featureList.containsKey(key)) {
					featureList.put(key, featureList.get(key) + 1);
				}
				else {
					featureList.put(key, 1);
				}
			}
		}
		return featureList;
	}

	/**
	 * Get unigrams in each sentence of the document.
	 * <p>
	 * From the list of unigrams in each sentence, we can extract any 
	 * ngram we want in the document. The method returns an array of 
	 * which each element is a sentence in a document and the element 
	 * itself is an array of unigrams in the sentence.
	 * <p>
	 * We use Stanford parser to get words (unigrams) in each sentence. 
	 * A simpler approach would split each sentence by whitespace.
         * <p>
         * By default, we will preprocess the text in this step. If do not want preprocess, 
         * simply turn off all control of the Preprocess object.
	 * 
	 * @param instanceID
	 * @return An array (1) of which each element is an array (2) of unigrams in 
	 * in each sentence.
	 * @throws Exception
	 */
	protected ArrayList<String[]> getInstanceUnigramList(String instanceID) throws Exception {
		ArrayList<String[]> featureList = new ArrayList<String[]>();
		String text = getInstance(instanceID);

//		// simple approach: split by whitespaces
		// featureList = TextUtil.extractWordsByToken(text, "\\s");

		// NLP approach: Stanford parser
		featureList = TextUtil.extractWordsByStandfordParser(text);

		// preprocess tokens in the document
		m_preprocess.process(featureList);

		return featureList;
	}
        
        public static FeatureSetNGram createFeatureSetNGram() throws Exception {
            boolean removeStopWord = true;
            boolean removePunctuation = true;
            boolean stemWord = true;
            boolean caseSensitive = false;
            boolean removeHeaderFooter = false;
            
            return new FeatureSetNGram(removeStopWord, removePunctuation, stemWord, caseSensitive, removeHeaderFooter);
        }
}
