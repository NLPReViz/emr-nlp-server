/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.pitt.cs.nih.backend.featureVector;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import edu.pitt.cs.nih.backend.utils.Util;

/**
 *
 * @author Phuong Pham <a href="mailto:phuongpham@cs.pitt.edu">phuongpham@cs.pitt.edu</a>
 */
public abstract class LibSVMFileFormat {
    public static String svmLightDelimiter = " ";
    public static String svmLightFeatureValueDelimiter = ":";
    public static String newLine = "\n";
    
    /**
     * Create feature vectors for the dataset.
     * <p>
     * Using the MPQA structure.
     * 
     * @param fn_globalFeatureVector
     * @param dataFolder
     * @return
     * @throws Exception
     */
    protected abstract FeatureVector getFullDSFeatureVector(String fn_globalFeatureVector,
            String dataFolder) throws Exception;
    
    public void createLearningFileFromFeatureVector(FeatureVector unNormalizedFeatureVector,
            String fn_featureVector, String fn_index, boolean includeBiasFeature,
            String fn_globalFeatureVector) throws Exception {

        System.out.println("createLearningFileFromFeatureVector entry");

        HashMap<String, List<String>> instanceRationaleMap = new HashMap<>();
        // create instance feature vector (sparse format)
        // create contrastive feature vector = instance feature vector - rationale feature vector
        List<Map.Entry<String, HashMap<Integer, Double>>> normalizedFeatureVector = new ArrayList<>();
        AbstractMap.SimpleEntry<String, HashMap<Integer, Double>> item;
        String instanceID;
        HashMap<Integer, Double> sparseInstanceFeature;
        int instanceIndex;
        System.gc();
        for (int i = 0; i < unNormalizedFeatureVector.m_InstanceID.length; i++) {
            instanceID = unNormalizedFeatureVector.m_InstanceID[i];
            if (isRationaleInstance(instanceID)) {
                System.out.println("FeatureVector for rationale " + instanceID);
                instanceIndex = getOriginalInstanceIndex(instanceID,
                        normalizedFeatureVector);
                System.out.println("instanceIndex got");
                sparseInstanceFeature = normalizedFeatureVector.get(instanceIndex).getValue();
                System.out.println("sparseFeature got");
                item = new AbstractMap.SimpleEntry<>(instanceID,
                        getContrastFeature(unNormalizedFeatureVector.m_FeatureVector[i],
                            sparseInstanceFeature, includeBiasFeature));
                System.out.println("contrastFeature got");
                if (item.getValue().size() > 0) { // only add if x_ij \neq 0
                    instanceRationaleMap.get(
                            normalizedFeatureVector.get(instanceIndex).getKey()).add(instanceID);
                    System.out.println("instanceRationale got");
                    normalizedFeatureVector.add(item);
                }
                else {
                    System.out.println("Report:" + instanceID + " does not have pseudo instances");
                }

            } else {
                // System.out.println("FeatureVector for " + instanceID);
                instanceRationaleMap.put(instanceID, new ArrayList<String>());
                item = new AbstractMap.SimpleEntry<>(instanceID,
                        getInstanceFeature(unNormalizedFeatureVector.m_FeatureVector[i],
                        includeBiasFeature));
                normalizedFeatureVector.add(item);
            }
        }
//        // normalize feature vectors
//        double normalizedValue;
//        for (String originalInstanceID : instanceRationaleMap.keySet()) {
//            instanceIndex = getItemIndex(originalInstanceID,
//                    normalizedFeatureVector);
//            normalizedValue = normalizeInstanceFeatureVector(
//                    normalizedFeatureVector.get(instanceIndex).getValue(),
//                    includeBiasFeature);
//            for (String rationaleInstanceID : instanceRationaleMap.get(originalInstanceID)) {
//                instanceIndex = getItemIndex(rationaleInstanceID,
//                        normalizedFeatureVector);
//                // normalize contrastitve using instance's average value
//                normalizeContrastiveVector(normalizedValue,
//                        normalizedFeatureVector.get(instanceIndex).getValue(),
//                        includeBiasFeature);
//                
////                // normalize contrastive using contrastive's average value, because
////                // contrastive example doesn't have bias feature, we always pass false here
////                normalizeInstanceFeatureVector(
////                        normalizedFeatureVector.get(instanceIndex).getValue(),
////                        false);
//            }
//        }
        System.out.println("begin create libsvm format");
        // save the index file and the feature vector file
        // using LibSVM format
        StringBuilder indexSB = new StringBuilder();
        StringBuilder featureVectorSB = new StringBuilder();        
        List<Integer> indexList;
        int featureIndex;
        for(int i = 0; i < normalizedFeatureVector.size(); i++) {
            instanceID = normalizedFeatureVector.get(i).getKey();
            System.out.println("FeatureVector for " + instanceID);
            // index file
            indexSB.append(instanceID);
            indexSB.append(",");
            indexSB.append(Integer.toString(i));
            indexSB.append(newLine);
            
            // feature vector file
            // target
            // pseudo instances having the same target label with original instances
            featureVectorSB.append(getClassValue(instanceID));
//            // Colon data, pseudo instances have the opposite target label
//            if(isRationaleInstance(instanceID)) {
//                featureVectorSB.append(getTheOtherClassValue(getClassValue(instanceID)));
//            }
//            else {
//                featureVectorSB.append(getClassValue(instanceID));
//            }
            
            // feature values
            // sort the feature index ascending
            item = (AbstractMap.SimpleEntry<String, HashMap<Integer, Double>>) normalizedFeatureVector.get(i);
            indexList = new ArrayList<Integer>(item.getValue().keySet());
            Collections.sort(indexList);
            for(int iIndex = 0; iIndex < indexList.size(); iIndex++) {
                featureIndex = (int) indexList.get(iIndex);
                featureVectorSB.append(svmLightDelimiter);
                featureVectorSB.append(Integer.toString(featureIndex));
                featureVectorSB.append(svmLightFeatureValueDelimiter);
                featureVectorSB.append(Double.toString(item.getValue().get(featureIndex)));
            }
            featureVectorSB.append(newLine);
        }
        
        // save index file
        Util.saveTextFile(fn_index, indexSB.toString());
        // save feature vector file
        Util.saveTextFile(fn_featureVector, featureVectorSB.toString());

        System.out.println("createLearningFileFromFeatureVector exit");
    }
    
    /**
     * False: -1
     * True: 1
     * @param instanceID
     * @return
     * @throws Exception 
     */
    protected abstract String getClassValue(String instanceID) throws Exception;
    
    protected abstract boolean isRationaleInstance(String instanceID);
    
    /**
     * Look up the original instance feature vector index of a rationale feature vector  
     * in the data set.
     * <p>
     * Make sure that the original ID is a <b>substring</b> of the rationale ID.
     * 
     * @param rationaleIntanceID
     * @param dataSet
     * @return
     * @throws Exception 
     */
    protected int getOriginalInstanceIndex(String rationaleIntanceID,
            List<Map.Entry<String, HashMap<Integer, Double>>> dataSet)
            throws Exception {
        int index = -1;
        String instanceID;
        for(int i = 0; i < dataSet.size(); i++) {
            instanceID = dataSet.get(i).getKey();
            if(rationaleIntanceID.contains(instanceID) &&
                    ! isRationaleInstance(instanceID)) {
                index = i;
                break;
            }
        }
        return index;
    }
    
    /**
     * Create the sparseVector for a pseudo instance 
     * = original instance feature vector - contrastive feature vector.
     * <p>
     * contrastive feature vector = original instance feature vector - 
     * rationale feature vector.
     * 
     * @param unnormalizedFeature The contrastive feature vector
     * @param instanceVector The original instance feature vector
     * @param includeBiasFeature
     * @return
     * @throws Exception 
     */
//    protected HashMap<Integer, Double> getContrastFeature(int[] unnormalizedFeature,
    protected HashMap<Integer, Double> getContrastFeature(HashMap<Integer, Double> unnormalizedFeature,
            HashMap<Integer, Double> instanceVector, boolean includeBiasFeature)
            throws Exception {
        HashMap<Integer, Double> sparseVector = new HashMap<>();    
//        // we do not include the bias feature for contrastive example because it = 0.0
//        // however, in the Colon case, we would like to include it. Because 
//        // we use the contrastive instance, not the pseudo instance in our 
//        // training data set
//        // REMEMBER turn this off when work with the MR data set
//        if(includeBiasFeature) { // svmlight start feature count at 1 not 0
//            sparseVector.put(1, 1.0);
//        }
        int index, offset;
        
//        // visualization debug
//        String fn_globalFeature = Util.getOSPath(new String[] {
//            Util.getExecutingPath(), "data", "ColonoscopyData", "globalFeatureVector.txt"});
//        String[] featureSpace = Util.loadList(fn_globalFeature);
        
        // first approach (single rationale & merge all)
        // x_ij = x_i XOR v_ij
        // v_ij is the pseudo instance, which is the original instance remove rationale span
        // x_ij is what is left after subtract x_i by v_ij
        offset = includeBiasFeature ? - 2 : - 1; // svmlight start feature count at 1 not 0
        for(int i : instanceVector.keySet()) {
            index = i + offset;             
            // sparse vector
            if(index > 0 && !unnormalizedFeature.containsKey(index)) {
                sparseVector.put(i, 1.0);
            }
        }
//        System.out.println();
        
//        // second approach (merge all + flip)
//        // keep the contrastive instance = pseudo instance
//        // x_ij = v_ij (x_ij is the pseudo instance, v_ij is the contrastive example, while r_ij is the highlighted span)
//        offset = includeBiasFeature ? 2 : 1;
//        for(int i : unnormalizedFeature.keySet()) {
//                index = i + offset; // svmlight start feature count at 1 not 0
////                sparseVector.put(index, (double)unnormalizedFeature[i]);
//                sparseVector.put(index, 1.0);
//        }
        
        return sparseVector;
    }
    
    /**
     * Create the sparseVector for a normal instance
     * 
     * @param unnormalizedFeature
     * @param includeBiasFeature
     * @return
     * @throws Exception 
     */
//    protected HashMap<Integer, Double> getInstanceFeature(int[] unnormalizedFeature,
    protected HashMap<Integer, Double> getInstanceFeature(HashMap<Integer, Double> unnormalizedFeature,
            boolean includeBiasFeature) throws Exception {
        HashMap<Integer, Double> sparseVector = new HashMap<>();
        
        if(includeBiasFeature) { // svmlight start feature count at 1 not 0
            sparseVector.put(1, 1.0);
        }
        
        int index;
        int offset = includeBiasFeature ? 2 : 1;
        for(int i : unnormalizedFeature.keySet()) {
                index = i + offset; // svmlight start feature count at 1 not 0
                sparseVector.put(index, 1.0);
        }
        
        return sparseVector;
    }
    
    protected int getItemIndex(String instanceID,
            List<Map.Entry<String, HashMap<Integer, Double>>> dataSet) throws Exception {
        int index = -1;
        for(int i = 0; i < dataSet.size(); i++) {
            if(dataSet.get(i).getKey().equals(instanceID)) {
                index = i;
                break;
            }
        }
        
        return index;
    }
    
    /**
     * Normalize a sparse vector into unit length.
     * 
     * @param sparseVector
     * @param includeBiasFeature
     * @return
     * @throws Exception 
     */
    protected double normalizeInstanceFeatureVector(HashMap<Integer, Double> sparseVector,
            boolean includeBiasFeature) throws Exception {
        double normalizedValue = 0;
        // get the vector's norm value
//        for(Integer index : sparseVector.keySet()) {
////            normalizedValue += sparseVector.get(index) * sparseVector.get(index);
//            normalizedValue += sparseVector.get(index);
//            normalizedValue += 1;        
//        }
        normalizedValue = sparseVector.keySet().size(); // each non-zero item = 1.0
        if(includeBiasFeature) {
            normalizedValue -= 1; // the original instance feature vector always has bias feature = 1.0
        }
        
        // square root of the normalizedValue
        normalizedValue = Math.sqrt(normalizedValue);
        // normalize the sparse vector
        for(Integer index : sparseVector.keySet()) {
//            normalizedValue += sparseVector.get(index) * sparseVector.get(index);
            sparseVector.put(index, sparseVector.get(index) / normalizedValue);
        }
        if(includeBiasFeature) {
            sparseVector.put(1, 1.0); // recover the bias feature
        }
        
        return normalizedValue;
    }
    
    /**
     * Normalize the contrastive vector using its instance vector's normalized value.
     * <p>
     * Because the bias feature (if any) in the contrastive vector is always = 0.0, 
     * in a sparse vector, it does not exist. Basically, we do not care for it here.
     * 
     * @param normalizedValue
     * @param sparseVector
     * @param includeBiasFeature
     * @throws Exception 
     */
    protected void normalizeContrastiveVector(double normalizedValue,
            HashMap<Integer, Double> sparseVector, boolean includeBiasFeature) throws Exception {
        for(Integer index : sparseVector.keySet()) {
            sparseVector.put(index, sparseVector.get(index) / normalizedValue);
        }
//        // we do not include the bias feature for contrastive example because it = 0.0
//        // however, in the Colon case, we would like to include it. Because 
//        // we use the contrastive instance, not the pseudo instance in our 
//        // training data set
//        // REMEMBER turn this off when work with the MR data set
//        if(includeBiasFeature) {
//            sparseVector.put(1, 1.0); // recover the bias feature
//        }
    }
    
    /**
     * Add C, C_contrast, divide \mu for each feature vector in the train set.
     * 
     * @param instanceIndexList
     * @param indexList
     * @param fn_featureIn
     * @param fn_featureOut
     * @param C
     * @param C_contrast
     * @param mu
     * @throws Exception 
     */
    protected void createDSTrainSet(List<Integer> instanceIndexList, String[] indexList,
            String fn_featureIn, String fn_featureOut, double C, double C_contrast,
            double mu) throws Exception {
        // build the feature training file
        StringBuilder sb = new StringBuilder();
        String[] featureList = Util.loadList(fn_featureIn, newLine);
        int index;
        String instanceID;
        
        // try using different costs for different target classes
        boolean differentClassCost = false;
        double C_class, C_neg;        
        // get the class ratio in the training set
        double ratio = getPosNegInstanceRatio(instanceIndexList, indexList);
        // for example
        // # pos = 10; # neg = 5 => ratio = 2;
        // cost neg = ratio * C = 2 C (means it costly to make a
        // mistake on neg class
        C_neg = ratio * C;
        
        for(int i = 0; i < instanceIndexList.size(); i++) {
            index = instanceIndexList.get(i);
            instanceID = indexList[index];
            instanceID = instanceID.substring(0, instanceID.indexOf(","));
            if(isRationaleInstance(instanceID)) {
                sb.append(createContrastFVString(featureList[index], C_contrast, mu));
            }
            else {
//                sb.append(createInstanceFVString(featureList[index], C));
                
                // if we use different costs for different classes and this is 
                // a negative instance (positive instances use C as cost)
                if(differentClassCost && getClassValue(instanceID).equals("-1")) {
                    C_class = C_neg;
                }
                else {
                    C_class = C;
                }
                sb.append(createInstanceFVString(featureList[index], C_class));
            }
            sb.append(newLine);
        }
        Util.saveTextFile(fn_featureOut, sb.toString());
    }

    /**
     * Get the ratio positive instance / negative instance of the training set.
     * 
     * @param indexList
     * @param indexList
     * @return
     * @throws Exception 
     */
    protected double getPosNegInstanceRatio(List<Integer> instanceIndexList, String[] indexList) throws Exception {
        double posCount = 0.0;
        String instanceID;
        int index;
        for(int i = 0; i < instanceIndexList.size(); i++) {
            index = instanceIndexList.get(i);
            instanceID = indexList[index];
            instanceID = instanceID.substring(0, instanceID.indexOf(","));
            if(getClassValue(instanceID).equals("+1"))
                posCount++;
        }
        
        return posCount / (instanceIndexList.size() - posCount);
    }
    
    /**
     * Divide contrast feature vector to \mu and append C_contrast cost for x_{ij}.
     * 
     * @param contrastFV
     * @param C_contrast
     * @param mu
     * @return
     * @throws Exception 
     */
    protected String createContrastFVString(String contrastFV, double C_contrast,
            double mu) throws Exception {
        return createContrastFVString(contrastFV, mu) + svmLightDelimiter // divide x_ij - v_ij by \mu
                + "cost:" + Double.toString(C_contrast);
    }
    
    /**
     * Divide contrast feature vector to \mu
     * 
     * @param contrastFV
     * @param mu
     * @return
     * @throws Exception
     */
    protected String createContrastFVString(String contrastFV,
    		double mu) throws Exception {
    	return contrastFV.substring(0, 2) + svmLightDelimiter // get the instance label
                + divideContrastFVToMu(contrastFV, mu);
    }
    
    /**
     * Create the new contrast feature vector string from the old one by dividing by \mu.
     * 
     * @param contrastFV
     * @param mu
     * @return
     * @throws Exception 
     */
    protected String divideContrastFVToMu(String contrastFV, double mu)
            throws Exception {
        // create a list of index:value items
        List<Map.Entry<String,Double>> featureVector = extractFeatureVector(contrastFV);
        // divide by \mu and create feature vector string
        Map.Entry<String, Double> item;
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < featureVector.size(); i++) {
            item = featureVector.get(i);
            sb.append(item.getKey());
            sb.append(svmLightFeatureValueDelimiter);
            sb.append(Double.toString(item.getValue() / mu));
            sb.append(svmLightDelimiter);
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Convert a feature vector string into list of index:value.
     * 
     * @param featureVectorStr
     * @return
     * @throws Exception 
     */
    protected List<Map.Entry<String,Double>> extractFeatureVector(
            String featureVectorStr) throws Exception {
        List<Map.Entry<String,Double>> featureVector = new ArrayList<>();
        String[] itemStrList = featureVectorStr.split(svmLightDelimiter);
        String[] itemStr;
        Map.Entry<String, Double> item;
        for(int i = 1; i < itemStrList.length; i++) {
            itemStr = itemStrList[i].split(svmLightFeatureValueDelimiter);
            item = new AbstractMap.SimpleEntry<String, Double>(itemStr[0], Double.parseDouble(itemStr[1]));
            featureVector.add(item);
        }
        
        return featureVector;
    }
    
    /**
     * Append C cost for x_i.
     * 
     * @param instanceFV
     * @param C
     * @return
     * @throws Exception 
     */
    protected String createInstanceFVString(String instanceFV, double C) 
            throws Exception {
        return instanceFV.trim() + svmLightDelimiter + "cost:" + Double.toString(C);
    }
    
    public String printBinaryVerbalVector(String featureVectorStr, 
            String[] globalFeatureVector, boolean biasFeature) throws Exception {
        String[] featurePairList = featureVectorStr.split(svmLightDelimiter);
        int offset = biasFeature ? -2 : -1; // svmLight format start at 1 while java start array at 0
        
        StringBuilder sb = new StringBuilder();
        int featureIndex;
        String[] featureValue;
        for(int i = 1; i < featurePairList.length; i++) { //skip the instance label
            try {                
                featureValue = featurePairList[i].split(svmLightFeatureValueDelimiter);
                featureIndex = Integer.parseInt(featureValue[0]);
                // just for safe
                if(featureIndex + offset < 0) {
                    continue;
                }
                sb.append(globalFeatureVector[featureIndex + offset]).append(" ");
            }
            catch (NumberFormatException e){
                // must be cost feature, do nothing
            }
        }
        return sb.toString();
    }
    
    public int getNumberHighlightSV(String fn_trainSet, String fn_model, String fn_trainSetIndex)
            throws Exception {
        String[] trainSetInstanceList = Util.loadList(fn_trainSet);
        int[][] trainSetFeatureVectorIndex = getTrainSetFeatureVectorIndex(trainSetInstanceList);
        
        String[] modelInstanceList = Util.loadList(fn_model);
        int[][] modelFeatureVectorIndex = getModelFeatureVectorIndex(modelInstanceList);
        
        String[][] trainSetIDList = Util.loadTable(fn_trainSetIndex);
        
        // check each SVs in the model
        int numberHighlightSV = 0;
        String instanceID;
        for(int i = 0; i < modelFeatureVectorIndex.length; i++) {
            instanceID = getInstanceIDFromModel(modelFeatureVectorIndex[i], trainSetIDList,
                    trainSetFeatureVectorIndex);
            if(isRationaleInstance(instanceID)) {
                numberHighlightSV++;
            }
        }
        
        return numberHighlightSV;
    }
    
    public double getRatioHighlightSV(String fn_trainSet, String fn_model,
            String fn_trainSetIndex) throws Exception {
        String[] trainSetInstanceList = Util.loadList(fn_trainSet);
        int[][] trainSetFeatureVectorIndex = getTrainSetFeatureVectorIndex(trainSetInstanceList);
        
        String[] modelInstanceList = Util.loadList(fn_model);
        int[][] modelFeatureVectorIndex = getModelFeatureVectorIndex(modelInstanceList);
        
        String[][] trainSetIDList = Util.loadTable(fn_trainSetIndex);
        
        // check each SVs in the model
        int numberHighlightSV = 0;
        String instanceID;
        for(int i = 0; i < modelFeatureVectorIndex.length; i++) {
            instanceID = getInstanceIDFromModel(modelFeatureVectorIndex[i], trainSetIDList,
                    trainSetFeatureVectorIndex);
            if(isRationaleInstance(instanceID)) {
                numberHighlightSV++;
            }
        }
        
        return numberHighlightSV * 1.0 / modelFeatureVectorIndex.length;
    }
    
    public String getVerbalPseudoInstanceSV(String fn_trainSet, String fn_model,
            String fn_trainSetIndex, String[] globalFeatureVector,
            boolean biasFeature) throws Exception {
        String[] trainSetInstanceList = Util.loadList(fn_trainSet);
        int[][] trainSetFeatureVectorIndex = getTrainSetFeatureVectorIndex(trainSetInstanceList);
        
        String[] modelInstanceList = Util.loadList(fn_model);
        int[][] modelFeatureVectorIndex = getModelFeatureVectorIndex(modelInstanceList);
        
        String[][] trainSetIDList = Util.loadTable(fn_trainSetIndex);
        
        // check each SVs in the model
        LibSVMFileFormat svmFileFormat = new ColonoscopyDS_SVMLightFormat();
        StringBuilder sb = new StringBuilder();
        String instanceID;
        StringBuilder featureVectorEntry;
        int offsetInModelFile = modelInstanceList.length - modelFeatureVectorIndex.length;
        for(int i = 0; i < modelFeatureVectorIndex.length; i++) {
            instanceID = getInstanceIDFromModel(modelFeatureVectorIndex[i], trainSetIDList,
                    trainSetFeatureVectorIndex);
            if(isRationaleInstance(instanceID)) {
                sb.append(instanceID).append(" [");
                // append alpha value
                sb.append(modelInstanceList[i + offsetInModelFile].split(svmLightDelimiter)[0]).append("] : ");
                featureVectorEntry = new StringBuilder("-1 ");// -1 is the null label value
                // in modelFeatureVectorIndex we don't have label value entry (first entry)
                // but in printBinaryVerbalVector we count (not use) label value entry
                for(int j = 0; j < modelFeatureVectorIndex[i].length; j++) {
                    featureVectorEntry.append(modelFeatureVectorIndex[i][j]).append(" ");
                }
                sb.append(svmFileFormat.printBinaryVerbalVector(
                        featureVectorEntry.toString().trim(), globalFeatureVector,
                        biasFeature)).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    protected String getInstanceIDFromModel(int[] svFeatureVectorIndex,
            String[][] trainSetIDList, int[][] trainSetInstanceList) throws Exception {
        
        String instanceID = "";
        int[] trainSetInstanceIndex;
        boolean match;
        for(int i = 0; i < trainSetInstanceList.length; i++) {
            trainSetInstanceIndex = trainSetInstanceList[i];
            if(svFeatureVectorIndex.length != trainSetInstanceIndex.length) {
                continue;
            }
            match = true;
            for(int j = 0; j < trainSetInstanceIndex.length; j++) {
                if(svFeatureVectorIndex[j] != trainSetInstanceIndex[j]) {
                    match = false;
                    break;
                }
            }
            if(match) {
                instanceID = trainSetIDList[i][0];
                break;
            }
        }
        
        return instanceID;
    }
    
    protected int[][] getTrainSetFeatureVectorIndex(String[] trainSetInstanceList) throws Exception {
        String[] instanceFeatureValueList;
        int[][] trainSetFeatureVectorIndex = new int[trainSetInstanceList.length][];
        
        for(int i = 0; i < trainSetInstanceList.length; i++) {
            instanceFeatureValueList = trainSetInstanceList[i].split("\\s");
            trainSetFeatureVectorIndex[i] = new int[instanceFeatureValueList.length - 2];
            for(int j = 1; j < instanceFeatureValueList.length - 1; j++) {                
                trainSetFeatureVectorIndex[i][j - 1] = Integer.parseInt(instanceFeatureValueList[j].split(":")[0]);
            }
        }
        
        return trainSetFeatureVectorIndex;
    }
    
    protected int getModelStartLine(String[] modelInstanceList) throws Exception {
        int modelStartLine = 0;
        Pattern pattern = Pattern.compile("[a-zA-Z]+");
        while(modelStartLine < modelInstanceList.length) {
            if(!pattern.matcher(modelInstanceList[modelStartLine]).find()) {
                break;
            }
            else {
                modelStartLine++;
            }                
        }
        
        return modelStartLine;
    }
    
    protected int[][] getModelFeatureVectorIndex(String[] modelInstanceList) throws Exception {
        String[] instanceFeatureValueList;        
        
        int modelStartLine = getModelStartLine(modelInstanceList);
        
        int[][] trainSetFeatureVectorIndex = new int[modelInstanceList.length - modelStartLine][];
        
        for(int i = modelStartLine; i < modelInstanceList.length; i++) {
            instanceFeatureValueList = modelInstanceList[i].split("\\s");
            trainSetFeatureVectorIndex[i - modelStartLine] = new int[instanceFeatureValueList.length - 2];
            for(int j = 1; j < instanceFeatureValueList.length - 1; j++) {
                trainSetFeatureVectorIndex[i - modelStartLine][j - 1] = Integer.parseInt(instanceFeatureValueList[j].split(":")[0]);
            }
        }
        
        return trainSetFeatureVectorIndex;
    }
    
    public int getNumberHighlightInstance(String fn_index) throws Exception {
        String[][] instanceIDList = Util.loadTable(fn_index);
        int numberHighlightInstance = 0;
        for(String[] instanceID : instanceIDList) {
            if(isRationaleInstance(instanceID[0])) {
                numberHighlightInstance++;
            }
        }
        
        return numberHighlightInstance;
    }
    
    public double getRatioHighlightInstance(String fn_index) throws Exception {
        String[][] instanceIDList = Util.loadTable(fn_index);
        int numberHighlightInstance = 0;
        for(String[] instanceID : instanceIDList) {
            if(isRationaleInstance(instanceID[0])) {
                numberHighlightInstance++;
            }
        }
        
        return numberHighlightInstance * 1.0 / instanceIDList.length;
    }
    
    /**
     * Get number of origin and pseudo instances that are on edge (\xi_i = 0).
     * We use the observation what if a SV has alpha between (0, C), then it has 
     * \xi_i = 0.
     * <p>
     * A heuristic is that C and C_contrast are pretty far in our interested model 
     * (hyper-parameter). Then we don't need to check which SV is pseudo here. 
     * A proper check would be done when needed.
     * 
     * @param fn_model
     * @param C
     * @param C_contrast
     * @param classLabel
     * @return
     * @throws Exception 
     */
    public int getNumSVOnEdge(String fn_model, double C, double C_contrast,
            String classLabel, String fn_trainSet, String fn_trainSetIndex) throws Exception {
        String[] trainSetInstanceList = Util.loadList(fn_trainSet);
        int[][] trainSetFeatureVectorIndex = getTrainSetFeatureVectorIndex(trainSetInstanceList);
        
        String[] modelInstanceList = Util.loadList(fn_model);
        int modelStartLine = getModelStartLine(modelInstanceList);
        int[][] modelFeatureVectorIndex = getModelFeatureVectorIndex(modelInstanceList);
        
        String[][] trainSetIDList = Util.loadTable(fn_trainSetIndex);
        
        double classLabelVal = Double.parseDouble(classLabel);
        double alpha;
        int originSVOnEdgeCount = 0;
        int originSVNotOnEdgeCount = 0;
        int pseudoSVOnEdgeCount = 0;
        int pseudoSVNotOnEdgeCount = 0;
        String instanceID;
        for(int i = modelStartLine; i < modelInstanceList.length; i++) {
            alpha = Double.parseDouble(modelInstanceList[i].split(svmLightDelimiter)[0]);
            // round up to 3 numbers after decimal point
            alpha = Math.round(alpha * 1000.0) / 1000.0;
            instanceID = getInstanceIDFromModel(modelFeatureVectorIndex[i - modelStartLine],
                    trainSetIDList, trainSetFeatureVectorIndex);
            if(alpha * classLabelVal > 0) { // get instances from the interested class
                alpha = Math.abs(alpha);
                if(isRationaleInstance(instanceID)) {
                    if(alpha == C_contrast) {
                        pseudoSVNotOnEdgeCount++;
                    }
                    else {
                        pseudoSVOnEdgeCount++;
                    }
                }
                else {
                    if(alpha == C) {
                        originSVNotOnEdgeCount++;
                    }
                    else {
                        originSVOnEdgeCount++;
                    }
                }
            }
        }
        
        System.out.println("origin SVs on edge [" + classLabel + "]=" + originSVOnEdgeCount);
        System.out.println("origin SVs not on edge [" + classLabel + "]=" + originSVNotOnEdgeCount);
        System.out.println("pseudo SVs on edge [" + classLabel + "]=" + pseudoSVOnEdgeCount);
        System.out.println("pseudo SVs not on edge [" + classLabel + "]=" + pseudoSVNotOnEdgeCount);
        
        return originSVOnEdgeCount + pseudoSVOnEdgeCount;
    }
    
    /**
     * Get the other class value, apply for binary classification SVM only.
     * 
     * @param classValue
     * @return
     * @throws Exception 
     */
    public String getTheOtherClassValue(String classValue) throws Exception {
        return classValue.equals("-1") ? "+1" : "-1";
    }
}
