/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.pitt.cs.nih.backend.featureVector;

import edu.pitt.cs.nih.backend.featureVector.FeatureSet.MLInstanceType;
import edu.pitt.cs.nih.backend.simpleWS.model.Report;
import edu.pitt.cs.nih.backend.utils.Util;
import edu.pitt.cs.nih.backend.utils.XMLUtil;
import emr_vis_nlp.ml.ALearner;
import emr_vis_nlp.ml.SVMPredictor;
import frontEnd.serverSide.controller.Storage_Controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

/**
 * A dataset helper uses Weka format
 * 
 * @author phuongpham <a href="mailto:phuongpham@cs.pitt.edu">phuongpham@cs.pitt.edu</a>
 */
public class WekaDataSet extends DataSet {

    /**
     * Create a data set including input feature vector and class target value 
     * from a List<Document> object
     * 
     * @param rawData
     * @return A FeatureVector object
     */
    int default_ngram = 1;
    int default_threshold = 2;
    boolean default_binary = true;
    
    @Override
    public Object createDataSet(Object rawData) {
        String fn_globalFeatureVector = "globalFeatureVector.txt";
        try {
            fn_globalFeatureVector = ALearner.getGlobalFeatureVectorFn();
        } catch (Exception ex) {
            Logger.getLogger(WekaDataSet.class.getName()).log(Level.SEVERE, null, ex);
        }
        String targetName = "informed-consent";
        
        return createDataSet(rawData, targetName, fn_globalFeatureVector);
    }
    
    public Object createDataSet(Object rawData, String targetName,
            String fn_globalFeatureVector) {
        
        MLInstanceType instanceType = MLInstanceType.COLONREPORTANDPATHOLOGYREPORT;
        boolean removeStopWord = true;
        boolean removePunctuation = true;
        boolean stemWord = true;
        boolean caseSensitive = false;
        boolean removeHeaderFooter = true;
        
        return createDataSet(rawData, removeStopWord,
                removePunctuation, stemWord, caseSensitive, removeHeaderFooter,
                default_ngram, default_threshold, default_binary,
                instanceType, targetName, fn_globalFeatureVector);
    }
    
    /**
     * Create a data including input feature vector and class target value 
     * from a List<Document> object with full parameter settings
     * 
     * @param rawData
     * @param ngram
     * @param occurrenceThreshold
     * @param binaryFeature
     * @param instanceType
     * @param preprocessingText
     * @param targetName
     * @return 
     */
    public Object createDataSet(Object rawData, boolean removeStopWord, boolean removePunctuation,
            boolean stemWord, boolean caseSensitive, boolean removeHeaderFooter, int ngram,
            int occurrenceThreshold, boolean binaryFeature, MLInstanceType instanceType,
            String targetName, String fn_globalFeatureVector) {
        
        FeatureVector inputVector = getInputVector(rawData, removeStopWord,
                removePunctuation, stemWord, caseSensitive, removeHeaderFooter, 
                ngram, occurrenceThreshold, binaryFeature, instanceType, fn_globalFeatureVector);
        FeatureVector outputVector = getOutputVector(rawData, targetName);
        
        return inputVector.merge(outputVector);
    }
    
    public FeatureVector getOutputVector(Object rawData, String targetName) {
        FeatureVector outputVector = null;
        @SuppressWarnings("unchecked")
		List<Report> documentList = (List<Report>) rawData;
        try {
            outputVector = getTargetClassFeatureVector(documentList, targetName);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        
        return outputVector;
    }
    
    public FeatureVector getInputVector(Object rawData, boolean removeStopWord, boolean removePunctuation,
            boolean stemWord, boolean caseSensitive, boolean removeHeaderFooter,
            int ngram, int occurrenceThreshold,
            boolean binaryFeature, MLInstanceType instanceType, String fn_globalFeatureVector) {
        FeatureVector dataSet = null;
        @SuppressWarnings("unchecked")
		List<Report> documentList = (List<Report>) rawData;
        try {            
            dataSet = getInputFeatureVector(documentList, removeStopWord,
                removePunctuation, stemWord, caseSensitive, removeHeaderFooter, ngram, occurrenceThreshold,
                    binaryFeature, instanceType, fn_globalFeatureVector);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        
        return dataSet;
    }
    
    public static FeatureVector getInstanceFeatureVector (String[] instanceText, 
            String[] globalFeatureVector, String docID) throws Exception {
        
        MLInstanceType instanceType = MLInstanceType.COLONREPORTANDPATHOLOGYREPORT;
        
        FeatureSetNGram featureSet = FeatureSetNGram.createFeatureSetNGram();
        featureSet.addInstance(docID, instanceText, instanceType);
        FeatureVector instanceFeatureVector = featureSet.getFeatureVectorFromGlobalFeatureVector(globalFeatureVector);
       
        return instanceFeatureVector;
    }
    
    public static Instance getInstanceObject (String[] instanceText, 
            String[] globalFeatureVector, String docID, String classValue, Instances ds) throws Exception {
        
        FeatureVector instanceFeatureVector = getInstanceFeatureVector(instanceText, 
                globalFeatureVector, docID);
        
        
        Instance instance = new Instance(globalFeatureVector.length + 2);
        instance.setDataset(ds);
        instance.setValue(0, docID);
        for(int i = 0; i < globalFeatureVector.length; i++) {
        	double value = 0;
        	if(instanceFeatureVector.m_FeatureVector[0].containsKey(i)) {
        		value = instanceFeatureVector.m_FeatureVector[0].get(i);
        	}
        	
        	instance.setValue(i + 1, value);
        }
        instance.setValue(globalFeatureVector.length + 1, classValue);
        
        return new SparseInstance(instance);
    }

    /**
     * Save a FeatureVector object to a file in ARFF format
     * 
     * @param rawData FeatureVector object
     * @param fn_ds output file name
     */
    @Override
    public void toFile(Object rawData, String fn_ds) {
        FeatureVector featureVector = (FeatureVector) rawData;
        try {
            featureVector.saveAsARFF(fn_ds, "");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public Object fromFile(String fn_ds) {
    	Instances dataSet = null;
    			
        try {
			dataSet = new Instances(new BufferedReader(new FileReader(fn_ds)));
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	return dataSet;
    }
    
    /**
     * Extract input feature vector from a List<Document> object. 
     * The output does not  contain target class for each instance. 
     * This separation will be good if we reuse the same input feature vector
     * for many target classifiers. 
     * Note that this function also save the global feature vector into the file 
     * fn_globalFeatureVector which can be used to build later learning instances
     * 
     * @param documentList
     * @param ngram
     * @param occurrenceThreshold
     * @param binaryFeature
     * @param instanceType
     * @param preprocessingText
     * @param fn_globalFeatureVector
     * @return
     * @throws Exception 
     */
    protected FeatureVector getInputFeatureVector(List<Report> documentList, 
            boolean removeStopWord, boolean removePunctuation,
            boolean stemWord, boolean caseSensitive, boolean removeHeaderFooter,
            int ngram, int occurrenceThreshold, boolean binaryFeature, 
            MLInstanceType instanceType, String fn_globalFeatureVector) throws Exception {
        FeatureSetNGram featureSet = new FeatureSetNGram(removeStopWord,
                removePunctuation, stemWord, caseSensitive, removeHeaderFooter);
        
        for(int i = 0; i < documentList.size(); i++) {
            Report document = documentList.get(i);
            
            String instanceID = document.getId();
            String[] instanceTextList = new String[2];
            // the first string is colonocopy report
            // get content only, skip header and footer
            instanceTextList[0] = Preprocess.separateReportHeaderFooter(
            		document.getColonoscopyReport())[1];
            if(document.getPathologyReport().length() > 0) {
            	instanceTextList[1] = Preprocess.separatePathologyHeaderFooter(
            			document.getPathologyReport())[1];
            }
            else {
            	instanceTextList[1] = "";
            }
            
            featureSet.addInstance(instanceID, instanceTextList, instanceType);
        }

        String[] globalFeatureVector = Util.loadList(fn_globalFeatureVector, "\n");
        return featureSet.getFeatureVectorFromGlobalFeatureVector(ngram, occurrenceThreshold,
                binaryFeature, globalFeatureVector);
    }
    
    /**
     * Get target class value of a data set
     * 
     * @param documentList
     * @param targetName
     * @return
     * @throws Exception 
     */
	protected FeatureVector getTargetClassFeatureVector(List<Report> documentList, String targetName) 
            throws Exception {
        String[][] classValueList = new String[documentList.size() + 1][2];
        // include headers
        classValueList[0][0] = "InstanceID";
        classValueList[0][1] = "[ClassValue]";
        
        for(int i = 0; i < documentList.size(); i++) {
            Report document = documentList.get(i);
            
            classValueList[i + 1][0] = document.getId();
            // trueClassConfidence == 0 => class "0" (false), otherwise "1" 
            String targetValue = document.getTrueClassConfidence(targetName) == 0 ?
            		"0" : "1";
            classValueList[i + 1][1] = targetValue;
        }
        
        FeatureVector outputVector = new FeatureVector();
        outputVector.loadTable(classValueList);
        return outputVector;
    }
    
    protected void saveAsARFF(FeatureVector inputFeatureVector, String[][] classLabelValueList, String description, String fn_arff) {
        FeatureVector classLabelVector = new FeatureVector();
        classLabelVector.loadTable(classLabelValueList);
        
        // merge the input feature vector with class target. So the class label will be the last element in each row
        FeatureVector finalVector = inputFeatureVector.merge(classLabelVector);
        try {
            finalVector.saveAsARFF(fn_arff, description);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    /**
     * Load an arff file into an Instances object.
     * Maybe used to test a predictor
     * @param fn_arff
     * @return 
     */
    public static Instances loadInstancesObjectFromFile(String fn_arff) {
        Instances instances = null;
        try {
            BufferedReader reader = new BufferedReader(
                    new FileReader(fn_arff));
            instances = new Instances(reader);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return instances;
    }
    
    /**
     * Extract feature name list from an Instances object.
     * Skip [ReportID] and [ClassValue] feature
     * 
     * @param ds
     * @return 
     */
    public static String[] getFeatureNameList(Instances ds) {
        ArrayList<String> featureNameList = new ArrayList<>();
        
        String featureName;
        for(int i = 0; i < ds.instance(0).numAttributes(); i++)
        {
            featureName = ds.attribute(i).name();
            if(featureName.equals("[ReportID]") || featureName.equals("[ClassValue]"))
                continue;
            
            featureNameList.add(featureName);
        }
        
        return featureNameList.toArray(new String[featureNameList.size()]);
    }
    
    /**
     * Create an empty data set based on the global feature vector.
     * @return
     * @throws Exception 
     */
    public static Instances createAnEmptyDataSet(String varID, String fn_globalFeatureVector) throws Exception {
        String[] featureNameSpace = Util.loadList(fn_globalFeatureVector);
        
        StringBuilder tempFileBuilder = new StringBuilder();
        StringReader strReader = null;

        String header = "% This is the Colonoscopy problem\n@relation current_working_report\n";
        header += "@attribute [ReportID] string\n";

        tempFileBuilder.append(header);

        for (String featureName : featureNameSpace) {
            String line = "@attribute \"" + featureName + "\" {0, 1}\n";
            tempFileBuilder.append(line);
        }

        String attrFooter = "@attribute \"[classLabel]\" {0, 1}\n@data\n";
        tempFileBuilder.append(attrFooter);

        strReader = new StringReader(tempFileBuilder.toString());
        
        // the result string contains header of a arff file without any data section
        return new Instances(strReader);
    }
    
    public static void saveInstancesToFile(String fn, Instances ds) throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fn));
        writer.write(ds.toString());
        writer.flush();
        writer.close();
    }
    
    public void createGlobalFeatureVectorFile(String docsFolder, 
            String fn_globalFeature) throws Exception {
        
        MLInstanceType instanceType = MLInstanceType.COLONREPORTANDPATHOLOGYREPORT;
        
        String[] instanceIDList = Util.loadSubFolderList(docsFolder);
        
        FeatureSetNGram featureSet = FeatureSetNGram.createFeatureSetNGram();
        
        String text;
        for(String instanceID : instanceIDList) {
            String[] instanceTextList = new String[2];
            // the first string is colonocopy report
            text = Util.loadTextFile(Util.getOSPath(new String[]{
                docsFolder, instanceID, "report.txt"}));
            // get the report content only
            instanceTextList[0] = Preprocess.separateReportHeaderFooter(text)[1];
            if (Util.fileExists(Util.getOSPath(new String[]{
                docsFolder, instanceID, "pathology.txt"}))) {
                text = Util.loadTextFile(Util.getOSPath(new String[]{
                    docsFolder, instanceID, "pathology.txt"}));
                // get the report content only
                instanceTextList[1] = Preprocess.separatePathologyHeaderFooter(text)[1];
            } else {
                instanceTextList[1] = "";
            }
            featureSet.addInstance(instanceID, instanceTextList, instanceType);
        }
        String[] globalFeatureVector = featureSet.getGlobalFeatureVector(
                default_ngram, default_threshold, default_binary);
        Util.saveList(fn_globalFeature, globalFeatureVector);
    }
    
    /**
     * Create a data set based on instanceIDList and variableList (quick creation based 
     * on the MPQA format).
     * <p>
     * All raw texts are stored in the docsFolder using MPQA format. While all instance labels 
     * are stored in the labelFolder (each csv file for a variable).
     * <p>
     * This function creates learning files for all variables in the variableList.
     * 
     * @param variableList
     * @param instanceIDFolder
     * @param docsFolder
     * @param labelFolder
     * @throws Exception 
     */
    public void createDataSet(String[] variableList, String instanceIDFolder,
            String docsFolder, String labelFolder, String[] globalFeatureVector,
            String outputFolder, String modelFolder) throws Exception {
//        int[] k_foldList = new int[] {5};
        int[] k_foldList = new int[] {0,1,2,3,4};
        String[] instanceIDList;
        FeatureVector inputVector;
        FeatureVector labelVector;
        SVMPredictor svm = new SVMPredictor();
        String fn_dataSetXML;
        
        MLInstanceType instanceType = MLInstanceType.COLONREPORTANDPATHOLOGYREPORT;
        
        for (String varID : variableList) {
            labelVector = new FeatureVector();
            labelVector.loadCSV(Util.getOSPath(new String[] {labelFolder,
                        "class-" + varID + ".csv"}));
            
            String text;
            for (int iFold : k_foldList) {
                instanceIDList = Util.loadList(Util.getOSPath(new String[]{
                    instanceIDFolder, varID + "-fold" + iFold + "of5-reportID-trainSet.csv"}));
                FeatureSetNGram featureSet = FeatureSetNGram.createFeatureSetNGram();

                for (String instanceID : instanceIDList) {
                    // create input vector
                    String[] instanceTextList = new String[2];
                    // the first string is colonocopy report
                    text = Util.loadTextFile(Util.getOSPath(new String[] {
                                docsFolder, instanceID, "report.txt"}));
                    instanceTextList[0] = Preprocess.separateReportHeaderFooter(text)[1];
                    if (Util.fileExists(Util.getOSPath(new String[] {
                                docsFolder, instanceID, "pathology.txt"}))) {
                        text = Util.loadTextFile(Util.getOSPath(new String[] {
                                docsFolder, instanceID, "pathology.txt"}));
                        instanceTextList[1] = Preprocess.separatePathologyHeaderFooter(text)[1];
                    } else {
                        instanceTextList[1] = "";
                    }

                    featureSet.addInstance(instanceID, instanceTextList,
                            instanceType);
                }
                inputVector = featureSet.getFeatureVectorFromGlobalFeatureVector(
                        default_ngram, default_threshold, default_binary, globalFeatureVector);
                
                // create final vector (merge input vector and labelVector
                inputVector = inputVector.merge(labelVector);
                // save file
                inputVector.saveAsARFF(get5FoldTrainingFileName(outputFolder,
                        varID, iFold), "");
                
                // begin train the model
                svm.trainModelFromFile(get5FoldTrainingFileName(outputFolder, varID, iFold));
                svm.saveModel(get5FoldModelFileName(modelFolder, varID, iFold));
                svm.loadModel(get5FoldModelFileName(modelFolder, varID, iFold));
                svm.saveFeatureWeights(globalFeatureVector,
                        get5FoldFeatureWeightFileName(modelFolder, varID, iFold));
                
                // create xml data set file to load files that are unseen by the model (test set in 5-fold)
                fn_dataSetXML = get5FoldDsXml(varID, outputFolder, iFold);
//                fn_dataSetXML = get5FoldDsXml(outputFolder, iFold);                
                if(!Util.fileExists(fn_dataSetXML)) {
                    instanceIDList = Util.loadList(Util.getOSPath(new String[]{
                            instanceIDFolder, varID + "-fold" + iFold + 
                            "of5-reportID-testSet.csv"}));
                    // make sure each instanceID is in %04d format
                    for(int i = 0; i < instanceIDList.length; i++) {
                        instanceIDList[i] = String.format("%04d", Integer.parseInt(instanceIDList[i]));
                    }
                    
                    XMLUtil.createXMLDatasetFileFromList("emr-vis-nlp_colonoscopy",
                            instanceIDList, "Document", fn_dataSetXML);
                }
            }
        }
    }
    
    public static String get5FoldModelFileName(String modelFolder, String varID,
            int iFold) throws Exception {
        return Util.getOSPath(new String[] {modelFolder,
                        varID + "-fold" + iFold + "of5.model"});
    }
    
    public static String get5FoldFeatureWeightFileName(String modelFolder,
            String varID, int iFold) throws Exception {
        return Util.getOSPath(new String[] {modelFolder,
                        varID + "-fold" + iFold + "of5.weight.csv"});
    }
    
    public static String get5FoldTrainingFileName(String trainingFolder,
            String varID, int iFold) throws Exception {
        return Util.getOSPath(new String[] {trainingFolder,
                    varID + "-fold" + iFold + "of5.arff"});
    }
    
    public static String get5FoldDsXml(String varID, String trainingFolder, int iFold)
            throws Exception {
//    public static String get5FoldDsXml(String trainingFolder, int iFold)
//            throws Exception {
        return Util.getOSPath(new String[] {trainingFolder,
                    "docList.emr-vis-nlp-" + varID + "-fold" + iFold + "of5.xml"});
//        return Util.getOSPath(new String[] {trainingFolder,
//                    "docList.emr-vis-nlp" + "-fold" + iFold + "of5.xml"});
    }
    
    /**
     * Create learning files for variables from an instanceID list.
     * 
     * @param variableList
     * @param instanceIDList
     * @param docsFolder
     * @param labelsFolder
     * @param globalFeatureVector
     * @param outputFolder
     * @throws Exception 
     */
    public void createDataSetFromIDList(String[] variableList, String[] instanceIDList,
            String docsFolder, String labelsFolder, String[] globalFeatureVector,
            String outputFolder) throws Exception {
        FeatureVector inputVector;
        FeatureVector labelVector;
        String fn_output;

        MLInstanceType instanceType = MLInstanceType.COLONREPORTANDPATHOLOGYREPORT;
        for (String varID : variableList) {
            labelVector = new FeatureVector();
            labelVector.loadCSV(Util.getOSPath(new String[]{labelsFolder,
                "class-" + varID + ".csv"}));

            String text;

            FeatureSetNGram featureSet = FeatureSetNGram.createFeatureSetNGram();

            for (String instanceID : instanceIDList) {
                // create input vector
                String[] instanceTextList = new String[2];
                // the first string is colonocopy report
                text = Util.loadTextFile(Util.getOSPath(new String[]{
                    docsFolder, instanceID, "report.txt"}));
                instanceTextList[0] = Preprocess.separateReportHeaderFooter(text)[1];
                if (Util.fileExists(Util.getOSPath(new String[]{
                    docsFolder, instanceID, "pathology.txt"}))) {
                    text = Util.loadTextFile(Util.getOSPath(new String[]{
                        docsFolder, instanceID, "pathology.txt"}));
                    instanceTextList[1] = Preprocess.separatePathologyHeaderFooter(text)[1];
                } else {
                    instanceTextList[1] = "";
                }

                featureSet.addInstance(instanceID, instanceTextList,
                        instanceType);
            }
            inputVector = featureSet.getFeatureVectorFromGlobalFeatureVector(
                    default_ngram, default_threshold, default_binary, globalFeatureVector);

            // create final vector (merge input vector and labelVector
            inputVector = inputVector.merge(labelVector);
            // save file
            fn_output = Util.getOSPath(new String[] {outputFolder, varID + "-initialTrain.arff"});
//            fn_output = Util.getOSPath(new String[] {outputFolder, varID + "-test.arff"});
            inputVector.saveAsARFF(fn_output, "");
        }
    }
    
    /**
     * Create learning files for variables from an instanceID list with filename suffix.
     * 
     * @param variableList
     * @param instanceIDList
     * @param docsFolder
     * @param labelsFolder
     * @param globalFeatureVector
     * @param outputFolder
     * @throws Exception 
     */
    public void createDataSetFromIDListWithPrefixSuffixFN(String[] variableList, String[] instanceIDList,
            String docsFolder, String labelsFolder, String[] globalFeatureVector,
            String outputFolder, String fn_prefix, String fn_suffix) throws Exception {
        FeatureVector inputVector;
        FeatureVector labelVector;
        String fn_output;

        MLInstanceType instanceType = MLInstanceType.COLONREPORTANDPATHOLOGYREPORT;
        for (String varID : variableList) {
            labelVector = new FeatureVector();
            labelVector.loadCSV(Util.getOSPath(new String[]{labelsFolder,
                "class-" + varID + ".csv"}));

            String text;

            FeatureSetNGram featureSet = FeatureSetNGram.createFeatureSetNGram();

            for (String instanceID : instanceIDList) {
                // create input vector
                String[] instanceTextList = new String[2];
                // the first string is colonocopy report
                text = Util.loadTextFile(Util.getOSPath(new String[]{
                    docsFolder, instanceID, "report.txt"}));
                instanceTextList[0] = Preprocess.separateReportHeaderFooter(text)[1];
                if (Util.fileExists(Util.getOSPath(new String[]{
                    docsFolder, instanceID, "pathology.txt"}))) {
                    text = Util.loadTextFile(Util.getOSPath(new String[]{
                        docsFolder, instanceID, "pathology.txt"}));
                    instanceTextList[1] = Preprocess.separatePathologyHeaderFooter(text)[1];
                } else {
                    instanceTextList[1] = "";
                }

                featureSet.addInstance(instanceID, instanceTextList,
                        instanceType);
            }
            inputVector = featureSet.getFeatureVectorFromGlobalFeatureVector(
                    default_ngram, default_threshold, default_binary, globalFeatureVector);

            // create final vector (merge input vector and labelVector
            inputVector = inputVector.merge(labelVector);
            // save file
            fn_output = Util.getOSPath(new String[] {outputFolder, fn_prefix + varID + fn_suffix});
            inputVector.saveAsARFF(fn_output, "");
        }
    }
    
    public static FeatureVector getTestSetFeatureVector(List<String> reportIDList, String docsFolder,
    		String fn_colonoscopyReport, String fn_pathologyReport, boolean removeStopWord,
    		boolean removePunctuation, boolean stemWord, boolean caseSensitive, boolean removeHeaderFooter)
    				throws Exception {
		FeatureSetNGram featureSet = new FeatureSetNGram(removeStopWord,
                removePunctuation, stemWord, caseSensitive, removeHeaderFooter);
		String reportID, temp;
		String[] reportText = new String[2];
		for(int i = 0; i < reportIDList.size(); i++) {
			reportID = reportIDList.get(i);
			temp = Util.loadTextFile(Util.getOSPath(new String[]{docsFolder, reportID, fn_colonoscopyReport}));
			reportText[0] = Preprocess.separateReportHeaderFooter(temp)[1];
			if(Util.fileExists(Util.getOSPath(new String[]{docsFolder, reportID, fn_pathologyReport}))) {
				temp = Util.loadTextFile(Util.getOSPath(new String[]{docsFolder, reportID, fn_pathologyReport}));
				reportText[1] = Preprocess.separatePathologyHeaderFooter(temp)[1]; 
			}
			else {
				reportText[1] = "";
			}
			featureSet.addInstance(reportID, reportText, MLInstanceType.COLONREPORTANDPATHOLOGYREPORT);
		}
		
		int ngram = 1;
		int frequency_threshold = 2;
		boolean binaryFeature = true;
		
		String[] globalFeatureVector = Util.loadList(Storage_Controller.getGlobalFeatureVectorFn());
		return featureSet.getFeatureVectorFromGlobalFeatureVector(ngram, frequency_threshold,
                binaryFeature, globalFeatureVector);
	}
}
