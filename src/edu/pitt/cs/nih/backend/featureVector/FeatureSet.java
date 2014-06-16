/**
 *
 */
package edu.pitt.cs.nih.backend.featureVector;

import java.util.HashMap;

import edu.pitt.cs.nih.backend.utils.Util;

/**
 * @author Phuong Pham This is the generic class of feature used by machine
 * learning algorithm At this moment, this class has the following inherited
 * classes: NGram
 */
public class FeatureSet {

    /**
     * Preprocessor will preprocess input text
     */
    protected Preprocess m_preprocess;
    /**
     * A list of instances. Each instance is an instanceID, and the file path to
     * the preprocessed text file. We load this file whenever needed
     */
    public HashMap<String, String> m_Instances;

//    public FeatureSet() {
//        m_preprocess = new Preprocess(true, true, false, true, false);
//    }
//
//    /**
//     * Initialize a feature set with default options: remove stop words, remove
//     * punctuation, no stem words, case-insensitive
//     */
//    public FeatureSet(String preprocessedDataDir) {
//        // different values of preprocessing
////		boolean removeStopWord = true;
////		boolean removePunctuation = true;
////		boolean stemWord = false;
////		boolean excludeHeaderFooter = true;
////		boolean caseSensitive = false;
//        m_preprocess = new Preprocess(true, true, false, true, false);
//    }

    /**
     * Initialize a feature set
     * <p>
     * A feature set contains feature vectors for a dataset. This object is
     * initialized with different options of preprocessing
     *
     * @param removeStopWord Remove stop words or not. True: remove; False:
     * otherwise
     * @param removePunctuation Remove punctuation or not. True: remove; False:
     * otherwise
     * @param stemWord Stem words in the input text or not. True: stem words;
     * False: otherwise
     * @param caseSensitive The input text is case sensitive or not. True:
     * sensitive; False: insensitive (upper -> lower)
     * @throws Exception
     */
    public FeatureSet(boolean removeStopWord, boolean removePunctuation,
            boolean stemWord, boolean caseSensitive, boolean removeHeaderFooter)
            throws Exception {
        // content of dataset (all data points)
        m_Instances = new HashMap<>();

        m_preprocess = new Preprocess(removeStopWord, removePunctuation,
                stemWord, removeHeaderFooter, caseSensitive);
    }    

    /**
     * Get the text value of an instance (not the file name)
     *
     * @param instanceID
     * @return
     * @throws Exception
     */
    public String getInstance(String instanceID) throws Exception {
        return m_Instances.get(instanceID);
    }
    
    /**
     * Add text to the object.
     * <p>
     * All header, footer extraction must be done before this step. 
     * The values passed here are report content only, not header or footer.
     * 
     * @param instanceID
     * @param instanceValues
     * @param docType
     * @param preprocessing
     * @throws Exception 
     */
    public void addInstance(String instanceID, String[] instanceValues, MLInstanceType docType) throws Exception {
        String instanceValue = "";
        // add report's content
        if (docType == MLInstanceType.COLONREPORTONLY || docType == MLInstanceType.COLONREPORTANDPATHOLOGYREPORT) {
            instanceValue = instanceValues[0] + "\n";
        }
        // add pathology's content if any
        if ((docType == MLInstanceType.PATHOLOGYREPORTONLY || docType == MLInstanceType.COLONREPORTANDPATHOLOGYREPORT)
                && !instanceValues[1].equals("")) {
            instanceValue += instanceValues[1];
        }
        
        m_Instances.put(instanceID, instanceValue.trim());
    }

    /**
     * Save the feature set table (instanceID, text filename) into a file
     * <p>
     * The file is in CSV format. This method will save the current dataset into
     * a file. Later, we can load it, not need to recreate the dataset and walk
     * through the preprocessing stage.
     *
     * @param fileName
     * @throws Exception
     */
    public void saveFeatureSet(String fileName) throws Exception {
        String[][] featureSetTable = new String[m_Instances.size()][2];
        // convert hash map into 2-dimension array
        int instanceCount = 0;
        for (String instanceID : m_Instances.keySet()) {
            featureSetTable[instanceCount][0] = instanceID;
            featureSetTable[instanceCount][1] = m_Instances.get(instanceID);
            instanceCount++;
        }
        // save the table into CSV file
        Util.saveTable(fileName, featureSetTable);
    }

    /**
     * Load instanceID and text fileName into the object
     *
     * @param fileName
     * @throws Exception
     */
    public void loadFeatureSet(String fileName) throws Exception {
        String[][] featureSetTable = Util.loadTable(fileName);
        m_Instances = new HashMap<String, String>();

        for (int instanceCount = 0; instanceCount < featureSetTable.length;
                instanceCount++) {
            m_Instances.put(featureSetTable[instanceCount][0],
                    featureSetTable[instanceCount][1]);
        }
    }

    /**
     * Create class label file (CSV format) for the input dataset.
     * <p>
     * According to the Movie Review dataset, if the instance ID starts with pos
     * -> class label = 1, else if instance ID starts with neg 0 -> class label
     * = 0
     *
     * @param fn_DataSet
     * @param fn_ClassLabel
     * @throws Exception
     */
    public void createDSClassLabel(String fn_DataSet, String fn_ClassLabel)
            throws Exception {
        String[][] dsFeatureVectorTable = Util.loadTable(fn_DataSet);
        String[][] featureVectorTable = new String[dsFeatureVectorTable.length + 1][dsFeatureVectorTable[0].length];
        // set header row
        featureVectorTable[0][0] = "[ReportID]";
        featureVectorTable[0][1] = "[classLabel]";
        // get class label based on instanceID
        for (int i = 0; i < dsFeatureVectorTable.length; i++) {
            featureVectorTable[i + 1][0] = dsFeatureVectorTable[i][0];
            if (dsFeatureVectorTable[i][0].indexOf("pos") > -1) {
                featureVectorTable[i + 1][1] = "1";
            } else {
                featureVectorTable[i + 1][1] = "0";
            }
        }
        // save the class label feature vector
        Util.saveTable(fn_ClassLabel, featureVectorTable);
    }
    
     /**
     * Indicate which kind of report we should extract
     */
    public static enum MLInstanceType {
        COLONREPORTONLY,
        PATHOLOGYREPORTONLY,
        COLONREPORTANDPATHOLOGYREPORT
    }
    
    // check if a string is in form of a number or a date
    public static boolean isNumberOrDate(String featureName) {
        return featureName.matches("\\d+") || 
                featureName.matches("\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}");
    }
}
