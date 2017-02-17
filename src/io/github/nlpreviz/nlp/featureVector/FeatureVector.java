/**
 *
 */
package io.github.nlpreviz.nlp.featureVector;

import io.github.nlpreviz.nlp.utils.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * @author Phuong Pham
 *
 */
public class FeatureVector {

    /**
     * Should be an table of int but get ready for TF-IDF feature vector, should
     * be a 2-dimensional table
     * <p>
     * - row: datapoints
     * <p>
     * - col: attributes
     */
//    public int[][] m_FeatureVector;
    // sparse vector implementation
    public HashMap<Integer, Double>[] m_FeatureVector;
    /**
     * List of features (name of each feature)
     */
    public String[] m_Feature;
    /**
     * List of instance IDs
     */
    public String[] m_InstanceID;

    public FeatureVector() {
    }

    /**
     * Assign a list of feature names
     *
     * @param featureList
     * @throws Exception
     */
    public void setFeatureList(String[] featureList) throws Exception {
        m_Feature = featureList;
    }

    /**
     * Assign a list of instance IDs
     *
     * @param instanceIDList
     * @throws Exception
     */
    public void setInstanceIDList(String[] instanceIDList) throws Exception {
        m_InstanceID = instanceIDList;
    }

    /**
     * Create a feature vector table
     * <p>
     * The table has
     * <p>
     * - row : instances
     * <p>
     * - col : features
     *
     * @param featureList
     * @param instanceIDList
     * @throws Exception
     */    
	public void setFeature_InstanceIDList(String[] featureList, String[] instanceIDList) throws Exception {
        setFeatureList(featureList);
        setInstanceIDList(instanceIDList);
//        m_FeatureVector = new int[m_InstanceID.length][m_Feature.length];
        m_FeatureVector = createSparsedFeatureVectorTable(m_InstanceID.length);
    }
    
    protected HashMap<Integer, Double>[] createSparsedFeatureVectorTable(int rowNum) {
        @SuppressWarnings("unchecked")
		HashMap<Integer, Double>[] featureVectorTable = new HashMap[rowNum];
        for(int i = 0; i < featureVectorTable.length; i++) {
            featureVectorTable[i] = new HashMap<>();
        }
        return featureVectorTable;
    }

    /**
     * Set value for a cell in the feature vector table
     *
     * @param row Instance
     * @param col Feature
     * @param value New value
     * @throws Exception
     */
//    public void setFeatureValue(int row, int col, int value) throws Exception {
    public void setFeatureValue(int row, int col, double value) throws Exception {
//        m_FeatureVector[row][col] = value;
        m_FeatureVector[row].put(col, value);
    }

    /**
     * Get value of a cell in the feature vector table
     *
     * @param row Instance
     * @param col Feature
     * @return
     * @throws Exception
     */
//    public int getFeatureValue(int row, int col) throws Exception {
    public double getFeatureValue(int row, int col) throws Exception {
//        return m_FeatureVector[row][col];
        double value;
        if(m_FeatureVector[row].containsKey(col)) {
            value = m_FeatureVector[row].get(col);
        }
        else {
            value = 0.0;
        }
        
        return value;
    }

    /**
     * Merge 2 feature vector from 2 different feature sets of the same dataset.
     * Only instances in this object are kept. Instances which are in
     * featureVector but not in this object will be skipped.
     *
     * @param featureVector A feature vector we want to merge with the current
     * feature vector
     * @return the new merged feature vector
     */
    public FeatureVector merge(FeatureVector featureVector) {
        FeatureVector newFeatureVector = new FeatureVector();
        // the same data set
        newFeatureVector.m_InstanceID = m_InstanceID.clone();
        // merge feature list
        // feature list of this instance first
        // feature list of instance featureVector later
        newFeatureVector.m_Feature = new String[m_Feature.length + featureVector.m_Feature.length];
        System.arraycopy(m_Feature, 0, newFeatureVector.m_Feature, 0, m_Feature.length);
        System.arraycopy(featureVector.m_Feature, 0, newFeatureVector.m_Feature, m_Feature.length,
                featureVector.m_Feature.length);
        
        // merge feature vector, need to match instanceID
//        newFeatureVector.m_FeatureVector =
//                new int[newFeatureVector.m_InstanceID.length][newFeatureVector.m_Feature.length];
        newFeatureVector.m_FeatureVector = createSparsedFeatureVectorTable(
                newFeatureVector.m_InstanceID.length);
        // scan through each data point
        boolean found;
        for (int i = 0; i < m_InstanceID.length; i++) {
            // instanceID index is the same as this instance
            // search the corresponding instance in featureVector object
            found = false;
            for (int j = 0; j < featureVector.m_InstanceID.length; j++) {
                String instanceID = featureVector.m_InstanceID[j];
                // match instanceID, then merge feature vector
                if (instanceID.equals(newFeatureVector.m_InstanceID[i])) {
//                    // old approach: densed feature vector with int array
//                    System.arraycopy(m_FeatureVector[i], 0, newFeatureVector.m_FeatureVector[i], 0,
//                            m_FeatureVector[i].length);
//                    System.arraycopy(featureVector.m_FeatureVector[j], 0, newFeatureVector.m_FeatureVector[i],
//                            m_FeatureVector[i].length, featureVector.m_FeatureVector[j].length);
                    
                    // new approach: sparse vector with HashMap<Integer, Double> (index, value)
                    // set feature value from this feature vector
                    for(int iThisFV : m_FeatureVector[i].keySet()) {
                        newFeatureVector.m_FeatureVector[i].put(iThisFV, m_FeatureVector[i].get(iThisFV));
                    }
                    // set feature value from featureVector object
                    // append feature vector table -> need an offset = length of this feature vector space
                    for(int iFeatureVectorFV : featureVector.m_FeatureVector[j].keySet()) {
                        newFeatureVector.m_FeatureVector[i].put(
                                iFeatureVectorFV + m_Feature.length,
                                featureVector.m_FeatureVector[j].get(iFeatureVectorFV));
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println(newFeatureVector.m_InstanceID[i] + " is not found in class label. Counting at " + Integer.toString(i));
            }
        }
        return newFeatureVector;
    }

    /**
     * Convert the feature vector table into a 2-dimension (table) String array
     *
     * @return A 2-dimension array
     * @throws Exception
     */
    @Deprecated
    public String[][] toTable() throws Exception {
        String[][] featureVectors = new String[m_InstanceID.length + 1][m_Feature.length + 1];
        featureVectors[0][0] = "[ReportID]";
        System.arraycopy(m_Feature, 0, featureVectors[0], 1, m_Feature.length);
        for (int i = 1; i < m_InstanceID.length + 1; i++) {
            featureVectors[i][0] = m_InstanceID[i - 1];
            for (int j = 1; j < m_Feature.length + 1; j++) {
                // deal with NGram first, change to float later with TF_IDF
//                // only applicable for densed feature vector
//                featureVectors[i][j] = String.format("%d", (int) m_FeatureVector[i - 1][j - 1]);
                featureVectors[i][j] = String.format("%f", getFeatureValue(i, j));
            }
        }
        return featureVectors;
    }

    /**
     * Convert a 2-dimension (table) String array into the feature vector table
     * for this object instance
     *
     * @param table
     */
    @Deprecated
    public void loadTable(String[][] table) {
        m_InstanceID = new String[table.length - 1];
        m_Feature = new String[table[0].length - 1];
//        m_FeatureVector = new int[m_InstanceID.length][m_Feature.length];
        m_FeatureVector = createSparsedFeatureVectorTable(m_InstanceID.length);
        // load feature list
        for (int i = 1; i < m_Feature.length + 1; i++) {
            m_Feature[i - 1] = table[0][i];
        }
        // load instanceID list and feature vector
        for (int i = 1; i < m_InstanceID.length + 1; i++) {
            m_InstanceID[i - 1] = table[i][0];
            for (int j = 1; j < m_Feature.length + 1; j++) {
                // deal with NGram first, change to float later with TF_IDF
//                m_FeatureVector[i - 1][j - 1] = Integer.parseInt(table[i][j]);
                double value = Double.parseDouble(table[i][j]);
                if(value != 0) {
                    m_FeatureVector[i - 1].put(j - 1, value);
                }
            }
        }
    }

    /**
     * Save the feature vector table in CSV format
     *
     * @param filename
     * @throws Exception
     */
    @Deprecated
    public void saveCSV(String filename) throws Exception {
        String[][] featureVectors = toTable();
        Util.saveTable(filename, featureVectors);
    }

    /**
     * Load a CSV file into the feature vector table
     *
     * @param filename
     * @throws Exception
     */
    @Deprecated
    public void loadCSV(String filename) throws Exception {
        String[][] table = Util.loadTable(filename);
        loadTable(table);
    }

    /**
     * Save the feature vector table as ARFF format
     * <p>
     * We need to modify this function as needed for each feature vector set.
     * Because ARFF need to explicitly declare attribute types
     *
     * @param filename
     * @param description A short description for this dataset. Used in Weka
     * (optional)
     * @throws Exception
     */
    public void saveAsARFF(String filename, String description) throws Exception {
        // use StringBuilder object for large string
        StringBuilder text = new StringBuilder();
        if(description.equals("")) description = "Classifcation problem";
        text.append("% Author: Phuong Pham\n@relation " + description.replaceAll("\\s", "_") + "\n");

        // reportID, attr1, attr2, ... @data, attr1_val, attr2_val, ...

        // adding attribute list
        // using ngram binary, all attributes are indicator {0, 1}
        // the class label of this problem is a binary variable
        // all attribute are binary but class label and the first feature: reportID
        text.append("@attribute \"[ReportID]\" string\n");
        for (int i = 0; i < m_Feature.length - 1; i++) {
            String feature = m_Feature[i];
            // in case feature name is a special character
            // we create a prefix f_ to make sure all attribute names start with an alphabet
            text.append("@attribute \"f_").append(feature.replaceAll("\\s", "_")).append("_f\" {0, 1}\n");
//            text += "@attribute \"f_" + feature.replaceAll("\\s", "_") + "_f\" numeric\n";
        }

        // class label is also an atrribute
        text.append("@attribute \"").append(m_Feature[m_Feature.length - 1]).append("\" {0, 1}\n");
//		text += "@attribute \"" + m_Feature[m_Feature.length - 1] + "\" {-1, 0, 1}\n";

        // adding data
        text.append("@data\n");
//        double[] featureVector = new double[m_Feature.length];
        for (int i = 0; i < m_InstanceID.length; i++) {
        	// sparsed format
        	text.append("{0 ").append(m_InstanceID[i]).append(",");
        	List<Integer> nonZeroIndices = new ArrayList<>(m_FeatureVector[i].keySet());
        	Collections.sort(nonZeroIndices);
        	for(int iFeature = 0; iFeature < nonZeroIndices.size(); iFeature++) {
        		// 2 assumptions here: 
        		// 1/ the first feature index is instanceID, and Weka sparse format count feature index from 0 -> + 1
        		// 2/ use binary feature, no need to access the "count" value in m_FeatureVector, add 1 by default
        		text.append(Integer.toString(nonZeroIndices.get(iFeature) + 1)).append(" 1, "); 
        	}
        	// verify if the class label value is 0 or not
            // if it is zero, we don't have that index in the feature vector
            if(!m_FeatureVector[i].containsKey(m_Feature.length - 1)) {
                text.append(m_Feature.length).append(" 0}\n");
            }
            else {
                // remove the last 2 characters , and whitespace
                text.setLength(text.length() - 2);
                text.append("}\n");
            }
        	
            System.out.println("Completed instance " + Integer.toString(i + 1));
        }
        Util.saveTextFile(filename, text.toString());
    }
}
