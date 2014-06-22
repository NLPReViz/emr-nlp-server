/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.pitt.cs.nih.backend.feedback;

import edu.pitt.cs.nih.backend.featureVector.ColonoscopyDS_SVMLightFormat;
import edu.pitt.cs.nih.backend.featureVector.FeatureVector;
import edu.pitt.cs.nih.backend.featureVector.WekaDataSet;
import edu.pitt.cs.nih.backend.utils.Util;
import edu.pitt.cs.nih.backend.utils.XMLUtil;
import emr_vis_nlp.ml.LibSVMPredictor;
import frontEnd.serverSide.controller.Storage_Controller;
import frontEnd.serverSide.model.FeedbackSpan_Model;
import frontEnd.serverSide.model.Feedback_Model;
import frontEnd.serverSide.model.TextSpan_Model;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weka.core.Instances;

/**
 *
 * @author Phuong Pham <a href="mailto:phuongpham@cs.pitt.edu">phuongpham@cs.pitt.edu</a>
 */
/**
 * @author Phuong Pham
 *
 */
public class FileTextCreateInitialDS {
	
	/**
	 * Intialize session management file and feedback file from the default initial list
	 * modelList.0..xml
	 * 
	 * @param fn_modelList
	 * @throws Exception
	 */
	public void initializeFeedbackFile(String fn_modelList,
			String fn_instanceIDList) throws Exception {
//		initializeFeedbackFileWeka(XMLUtil.getModelFnFromXMLList(fn_modelList),
//		XMLUtil.getReportIDFromXMLList(fn_instanceIDList));
		initializeFeedbackFileLibSVM(XMLUtil.getModelFnFromXMLList(fn_modelList),
				XMLUtil.getReportIDFromXMLList(fn_instanceIDList));
	}
	
	/**
     * create initial session manager and feedback file from training files of a model list
     * 
     * @param modelFnList
     * @throws Exception
     */
    public void initializeFeedbackFileLibSVM(List<String> modelFnList,
    		List<String> instanceIDList) throws Exception {
    	String userID = ""; // default user is ""
    	String sessionID = "0"; // default sessionID = "1"
    	ColonoscopyDS_SVMLightFormat svm = new ColonoscopyDS_SVMLightFormat();
    	
    	// initialize the session manager file
    	if(Util.fileExists(Storage_Controller.getSessionManagerFn())) {
    		Util.deleteFile(Storage_Controller.getSessionManagerFn());
    	}
    	createSessionEntries(modelFnList, sessionID, userID,
    			Storage_Controller.getSessionManagerFn());
    	
    	// initialize the feedback file
    	if(Util.fileExists(Storage_Controller.getFeedbackFn())) {
    		Util.deleteFile(Storage_Controller.getFeedbackFn());
    	}
    	createFeedbackEntries(modelFnList, sessionID, userID,
    			Storage_Controller.getFeedbackFn(), instanceIDList);

    	// clear learning folder
    	Util.clearFolder(Storage_Controller.getTrainingFileFolder());
    	// initialize learning files from modelList linked with feedback file
    	svm.createLearningFileFromSession(sessionID, userID);
    	// create dev set
    	createLearningFileFromFn(Util.getOSPath(new String[]{
    					Storage_Controller.getDocumentListFolder(), "devIDList.xml"}));
    	
    	// clear model folder
    	String feedbackFileName = Storage_Controller.getFeedbackFn();
    	String fn_sessionManager = Storage_Controller.getSessionManagerFn();
    	String learningFolder = Storage_Controller.getTrainingFileFolder();
    	String docsFolder = Storage_Controller.getDocsFolder();
    	String modelFolder = Storage_Controller.getModelFolder();
    	String featureWeightFolder = Storage_Controller.getWeightFolder();
    	String globalFeatureName = Storage_Controller.getGlobalFeatureVectorFn();
    	String xmlPredictorFolder = Storage_Controller.getModelListFolder();
    	
    	Util.clearFolder(modelFolder);
    	// update the current (0..) model
    	TextFileFeedbackManagerLibSVM feedbackManager = new TextFileFeedbackManagerLibSVM(feedbackFileName,
    			fn_sessionManager, learningFolder, docsFolder, modelFolder, featureWeightFolder,
    			globalFeatureName, xmlPredictorFolder);
    	feedbackManager.setUserID(userID);
    	feedbackManager.updateModels();
    	
    	
//    	String[] fnList;
//    	// remove additional weight files
//    	fnList = Util.loadFileList(Storage_Controller.getWeightFolder());
//    	for(int i = 0; i < fnList.length; i++) {
//    		if(fnList[i].charAt(0) != '0') { // e.g. 1.1.biopsy.weight.csv
//    			Util.deleteFile(Util.getOSPath(new String[]{
//    					Storage_Controller.getWeightFolder(), fnList[i]}));
//    		}
//    	}
//    	
//    	// remove additional model files
//    	fnList = Util.loadFileList(Storage_Controller.getModelFolder());
//    	for(int i = 0; i < fnList.length; i++) {
//    		if(fnList[i].charAt(0) != '0') { // e.g. 1.1.biopsy.weight.csv
//    			Util.deleteFile(Util.getOSPath(new String[]{
//    					Storage_Controller.getModelFolder(), fnList[i]}));
//    		}
//    	}
//    	
//    	// remove additional model list
//    	fnList = Util.loadFileList(Storage_Controller.getModelListFolder());
//    	for(int i = 0; i < fnList.length; i++) {
//    		int index = fnList[i].indexOf(".");
//    		if(!fnList[i].substring(index + 1, index + 2).equals("0")) { // e.g. modelList.1.1.xml
//    			Util.deleteFile(Util.getOSPath(new String[]{
//    					Storage_Controller.getModelListFolder(), fnList[i]}));
//    		}
//    	}
    }
	
    /**
     * create initial session manager and feedback file from training files of a model list
     * 
     * @param modelFnList
     * @throws Exception
     */
    public void initializeFeedbackFileWeka(List<String> modelFnList,
    		List<String> instanceIDList) throws Exception {
    	String userID = ""; // default user is ""
    	String sessionID = "0"; // default sessionID = "1"
    	
    	// reset current files if existed
    	if(Util.fileExists(Storage_Controller.getSessionManagerFn())) {
    		Util.deleteFile(Storage_Controller.getSessionManagerFn());
    	}
    	if(Util.fileExists(Storage_Controller.getFeedbackFn())) {
    		Util.deleteFile(Storage_Controller.getFeedbackFn());
    	}
    	
    	// initialize the session manager file
    	createSessionEntries(modelFnList, sessionID, userID,
    			Storage_Controller.getSessionManagerFn());
    	
    	// initialize the feedback file
//    	createFeedbackEntries(modelFnList, sessionID, userID, Storage_Controller.getFeedbackFn(),
//    			Storage_Controller.getTrainingFileFolder(), Storage_Controller.getDocsFolder());
    	createFeedbackEntries(modelFnList, sessionID, userID,
    			Storage_Controller.getFeedbackFn(), instanceIDList);
    	
    	String[] fnList;
    	// remove additional learning files
    	fnList = Util.loadFileList(Storage_Controller.getTrainingFileFolder());
    	for(int i = 0; i < fnList.length; i++) { // e.g. 0..any-adenoma.feedbackSet.arff, 0..any-adenoma.instanceSet.arff
    		if(fnList[i].contains("feedbackSet") ||
    				fnList[i].contains("instanceSet")) {
    			Util.deleteFile(Util.getOSPath(new String[]{
    					Storage_Controller.getTrainingFileFolder(), fnList[i]}));
    		}
    	}
    	
    	// remove additional weight files
    	fnList = Util.loadFileList(Storage_Controller.getWeightFolder());
    	for(int i = 0; i < fnList.length; i++) {
    		if(fnList[i].charAt(0) != '0') { // e.g. 1.1.biopsy.weight.csv
    			Util.deleteFile(Util.getOSPath(new String[]{
    					Storage_Controller.getWeightFolder(), fnList[i]}));
    		}
    	}
    	
    	// remove additional model files
    	fnList = Util.loadFileList(Storage_Controller.getModelFolder());
    	for(int i = 0; i < fnList.length; i++) {
    		if(fnList[i].charAt(0) != '0') { // e.g. 1.1.biopsy.weight.csv
    			Util.deleteFile(Util.getOSPath(new String[]{
    					Storage_Controller.getModelFolder(), fnList[i]}));
    		}
    	}
    	
    	// remove additional model list
    	fnList = Util.loadFileList(Storage_Controller.getModelListFolder());
    	for(int i = 0; i < fnList.length; i++) {
    		int index = fnList[i].indexOf(".");
    		if(!fnList[i].substring(index + 1, index + 2).equals("0")) { // e.g. modelList.1.1.xml
    			Util.deleteFile(Util.getOSPath(new String[]{
    					Storage_Controller.getModelListFolder(), fnList[i]}));
    		}
    	}
    }
    
    /**
     * Initialize the session file. Store all classifiers and all entries are valid
     * 
     * @param modelFnList
     * @param sessionID
     * @param userID
     * @param fn_sessionMeta
     * @throws Exception
     */
    protected void createSessionEntries(List<String> modelFnList, String sessionID,
            String userID, String fn_sessionMeta) throws Exception {
        StringBuilder sessionText = new StringBuilder();
        for(String modelInList : modelFnList) {        	
            sessionText.append(sessionID).append(",").append(userID).append(",");
            sessionText.append( Storage_Controller.getVarIdFromFn(modelInList));
            sessionText.append(",active,valid\n");
        }
        // overwrite the current session meta file
        Util.saveTextFile(fn_sessionMeta, sessionText.toString());
    }
    
    /**
     * From the initialIDList, extract instanceID, instanceClass, text span to create 
     * initial instance feedback.
     * 
     * @param modelFnList
     * @param sessionID
     * @param userID
     * @param fn_feedbackMeta
     * @param learningFolder
     * @param docsFolder
     * @throws Exception
     */
    protected void createFeedbackEntries(List<String> modelFnList, String sessionID,
            String userID, String fn_feedbackMeta, List<String> instanceIDList)
            		throws Exception {
        String requestID, varID, spanEnd, instanceID, instanceClass;        
        WekaDataSet wekaDS = new WekaDataSet();
        int iCount = 0;
        StringBuilder feedbackText = new StringBuilder(); 
        Map<String,String> classValueMap;
        
        requestID = new SimpleDateFormat("yyyyMMddHHmmss").format(
                Calendar.getInstance().getTime());
        
        for(String modelInList : modelFnList) {
        	varID = Storage_Controller.getVarIdFromFn(modelInList);
        	classValueMap = ColonoscopyDS_SVMLightFormat.getClassMap(varID);
        	
        	for(int i = 0; i < instanceIDList.size(); i++) {
        		instanceID = instanceIDList.get(i);        		
        		instanceClass = classValueMap.get(instanceID).equals("1") ?
        				"True" : "False";
        		
        		feedbackText.append(iCount++).append(",").append(sessionID);
        		feedbackText.append(",").append(userID).append(",").append(requestID);
        		feedbackText.append(",").append(instanceID).append(",");
        		feedbackText.append(varID).append(",0,0"); // instance level affects the whole document, no need for span
        		feedbackText.append(",create,-1,").append(instanceClass).append("\n");
        	}
        }
        // overwrite the current feedback meta file
        Util.saveTextFile(fn_feedbackMeta, feedbackText.toString());
    }
    
    
    /**
     * Report colonoscopy report and pathology report of a docId.
     * 
     * @param docsFolder
     * @param docId
     * @return
     * @throws Exception
     */
    protected String getReportText(String docsFolder, String docId) 
            throws Exception {
    	StringBuilder sb = new StringBuilder(Util.loadTextFile(
    			Util.getOSPath(new String[]{docsFolder, docId,
    					Storage_Controller.getColonoscopyReportFn()})));
    	
        if(Util.fileExists(Util.getOSPath(new String[] {docsFolder,
        		docId, Storage_Controller.getPathologyReportFn()}))) {
            sb.append("\n").append(Util.loadTextFile(Util.getOSPath(new String[] 
                {docsFolder, docId, Storage_Controller.getPathologyReportFn()})));
        }
        
        return sb.toString();
    }
    
    
    /**
     * Verify if initial feedback file contain correct instanceID and class label
     * 
     * @throws Exception
     */
    public void validateFeedbackInstanceClass() throws Exception {
    	String fn_feedback = Util.getOSPath(new String[]{Storage_Controller.getBaseFolder(), "feedback", "feedback.txt"});
    	String[] feedbackList = Util.loadList(fn_feedback);
    	String[] feedbackEntryList;
    	String instanceID, instanceClass, varID;
    	
    	String labelFolder = Util.getOSPath(new String[]{Storage_Controller.getBaseFolder(), "labels"});    	
    	String[] labelFnList = Util.loadFileList(labelFolder);
    	HashMap<String, HashMap<String,String>> varLabelMap = new HashMap<>();
    	for(int i = 0; i < labelFnList.length; i++) {
    		String[][] labelTable = Util.loadTable(Util.getOSPath(new String[]{labelFolder, labelFnList[i]}));
    		HashMap<String, String> instanceLabelMap = new HashMap<>();
    		for(int j = 1; j < labelTable.length; j++) {
    			instanceLabelMap.put(labelTable[j][0], labelTable[j][1]);
    		}
    		varLabelMap.put(labelFnList[i].substring(labelFnList[i].indexOf("-") + 1, labelFnList[i].indexOf(".")), instanceLabelMap);
    	}
    	
    	
    	for(int i = 0; i < feedbackList.length; i++) {
    		feedbackEntryList = feedbackList[i].split(",");
    		instanceID = feedbackEntryList[4];
    		varID = feedbackEntryList[5];
    		instanceClass = feedbackEntryList[10];
    		
    		System.out.println(varID + "," + instanceID + "," + varLabelMap.get(varID).get(instanceID) + "," + instanceClass);
    		if((varLabelMap.get(varID).get(instanceID).equals("1") && instanceClass.equals("False"))
    				|| (varLabelMap.get(varID).get(instanceID).equals("0") && instanceClass.equals("True"))) {
    			System.out.println("Error: instance" + instanceID + " var=" + varID);
    			break;
    		}
    	}
    	
    	System.out.println("Done");
    }
    
    /**
     * Test case: show that add new feedback is good, can discriminate between add and change; it is safe 
     * to delete (reject a feedback session) and keep on new feedback session
     * 
     * Init session and feedback
     * add1: 0003, 0004, 0005 biopsy -> it's good to add new feedback
     * add2: 0003 cecum (add) asa (add); 0004 biopsy (change) -> can differentiate add and change
     * delete + add3: 0003 cecum (add); 0005 biopsy (change) asa (add) -> it's safe to reject a feedback and move on new one
     * 
     */
    
    /**
     * Add feedback and observed session management file + feedback file
     * Add 2 instance level feedbacks and 4 span level feedbacks
     * 
     * @param userID
     * @return
     * @throws Exception
     */
    public List<Feedback_Model> addFeedBack1(String userID) throws Exception {
        // create a batch of feedback
        ArrayList<Feedback_Model> batch = new ArrayList<>();
        
        // first var value
        Feedback_Model varValue = new Feedback_Model();
        varValue.setRequestId(getRequestID());
        varValue.setDocId("0001");
        varValue.setVariableName("biopsy");
        varValue.setDocValue("True");
        batch.add(varValue);
        // first highlight
        FeedbackSpan_Model highlightSpan = new FeedbackSpan_Model();
        TextSpan_Model span = new TextSpan_Model();
        span.setStart(0);
        span.setEnd(10);
        highlightSpan.setDocId("0001");
        highlightSpan.setVariableName("biopsy");
        highlightSpan.setDocValue("True");
        highlightSpan.setRequestId(getRequestID());
        highlightSpan.setSpan(span);
        highlightSpan.setSpan(span);
        batch.add(highlightSpan);
        
        // second var value
        varValue = new Feedback_Model();
        varValue.setRequestId(getRequestID());
        varValue.setDocId("0004");
        varValue.setVariableName("biopsy");
        varValue.setDocValue("True");
        batch.add(varValue);
        
        // third var value
        varValue = new Feedback_Model();
        varValue.setRequestId(getRequestID());
        varValue.setDocId("0005");
        varValue.setVariableName("biopsy");
        varValue.setDocValue("True");
        batch.add(varValue);
        
        return batch;
	}
    
    
    public List<Feedback_Model> addFeedBack2(String userID) throws Exception {
        // create a batch of feedback
        ArrayList<Feedback_Model> batch = new ArrayList<>();
        
        // first var value
        Feedback_Model varValue = new Feedback_Model();
        varValue.setRequestId(getRequestID());
        varValue.setDocId("0001");
        varValue.setVariableName("cecum");
        varValue.setDocValue("True");
        batch.add(varValue);
        
        // second var value
        varValue = new Feedback_Model();
        varValue.setRequestId(getRequestID());
        varValue.setDocId("0001");
        varValue.setVariableName("asa");
        varValue.setDocValue("True");
        batch.add(varValue);
        
        // third var value
        varValue = new Feedback_Model();
        varValue.setRequestId(getRequestID());
        varValue.setDocId("0009");
        varValue.setVariableName("biopsy");
        varValue.setDocValue("False");
        batch.add(varValue);
        FeedbackSpan_Model highlightSpan = new FeedbackSpan_Model();
        TextSpan_Model span = new TextSpan_Model();
        span.setStart(0);
        span.setEnd(10);
        highlightSpan.setDocId("0009");
        highlightSpan.setVariableName("biopsy");
        highlightSpan.setDocValue("True");
        highlightSpan.setRequestId(getRequestID());
        highlightSpan.setSpan(span);
        highlightSpan.setSpan(span);
        batch.add(highlightSpan);
        
        return batch;
	}

    public List<Feedback_Model> addFeedBack3(String userID) throws Exception {
        // create a batch of feedback
        ArrayList<Feedback_Model> batch = new ArrayList<>();
        
        // first var value
        Feedback_Model varValue = new Feedback_Model();
        varValue.setRequestId(getRequestID());
        varValue.setDocId("0001");
        varValue.setVariableName("cecum");
        varValue.setDocValue("False");
        batch.add(varValue);
        FeedbackSpan_Model highlightSpan = new FeedbackSpan_Model();
        TextSpan_Model span = new TextSpan_Model();
        span.setStart(0);
        span.setEnd(10);
        highlightSpan.setDocId("0001");
        highlightSpan.setVariableName("cecum");
        highlightSpan.setDocValue("True");
        highlightSpan.setRequestId(getRequestID());
        highlightSpan.setSpan(span);
        highlightSpan.setSpan(span);
        batch.add(highlightSpan);
        
        // second var value
        varValue = new Feedback_Model();
        varValue.setRequestId(getRequestID());
        varValue.setDocId("0011");
        varValue.setVariableName("biopsy");
        varValue.setDocValue("False");
        batch.add(varValue);
        highlightSpan = new FeedbackSpan_Model();
        span = new TextSpan_Model();
        span.setStart(0);
        span.setEnd(10);
        highlightSpan.setDocId("0011");
        highlightSpan.setVariableName("biopsy");
        highlightSpan.setDocValue("True");
        highlightSpan.setRequestId(getRequestID());
        highlightSpan.setSpan(span);
        highlightSpan.setSpan(span);
        batch.add(highlightSpan);
        
//        // third var value
//        varValue = new Feedback_Model();
//        varValue.setRequestId(getRequestID());
//        varValue.setDocId("0005");
//        varValue.setVariableName("asa");
//        varValue.setDocValue("True");
//        batch.add(varValue);
        
        return batch;
	}
    
    protected String getRequestID() throws Exception {
    	return new SimpleDateFormat("yyyyMMddHHmmss").format(
                Calendar.getInstance().getTime());
    }
    
    /**
     * Create a sparse dataset from reportID list
     * 
     * @param reportIDList
     * @param varIDList
     * @param outputFolder
     * @param labelsFolder
     * @throws Exception
     */
    public void createWekaDataSet(List<String> reportIDList, String[] varIDList,
    		String outputFolder, String labelsFolder) throws Exception {
    	boolean removeStopWord = true;
		boolean removePunctuation = true;
		boolean stemWord = true;
		boolean caseSensitive = true;
		boolean removeHeaderFooter = true;
		
		String docsFolder = Storage_Controller.getDocsFolder();
		String fn_colonoscopyReport = Storage_Controller.getColonoscopyReportFn();
		String fn_pathologyReport = Storage_Controller.getPathologyReportFn();
		
		FeatureVector testSetFV = WekaDataSet.getTestSetFeatureVector(reportIDList,
				docsFolder, fn_colonoscopyReport, fn_pathologyReport,
				removeStopWord, removePunctuation, stemWord, caseSensitive, removeHeaderFooter);
		
		for(int iVar = 0; iVar < varIDList.length; iVar++) {
			HashMap<String, String> classMap = ColonoscopyDS_SVMLightFormat.getClassMap(varIDList[iVar]);
			
			StringBuilder sb = new StringBuilder();
			// header
			sb.append("@relation Weka_test_set\n");
			sb.append("@attribute \"[ReportID]\" string\n");
			for (int i = 0; i < testSetFV.m_Feature.length; i++) {
				String feature = testSetFV.m_Feature[i];
				sb.append("@attribute \"f_")
						.append(feature.replaceAll("\\s", "_"))
						.append("_f\" {0, 1}\n");
			}
			// class label is also an atrribute
			sb.append("@attribute \"[classLabel]\" {0, 1}\n");
			// adding data
			sb.append("@data\n");
			for (int i = 0; i < testSetFV.m_InstanceID.length; i++) {
				sb.append("{0 ").append(testSetFV.m_InstanceID[i]).append(",");
				List<Integer> nonZeroIndices = new ArrayList<>(
						testSetFV.m_FeatureVector[i].keySet());
				Collections.sort(nonZeroIndices);
				for (int iFeature = 0; iFeature < nonZeroIndices.size(); iFeature++) {
					// 2 assumptions here:
					// 1/ the first feature index is instanceID, and Weka sparse
					// format count feature index from 0 -> + 1
					// 2/ use binary feature, no need to access the "count"
					// value in m_FeatureVector, add 1 by default
					sb.append(
							Integer.toString(nonZeroIndices.get(iFeature) + 1))
							.append(" 1, ");
				}
				// add class value for each test instance
				sb.append(testSetFV.m_Feature.length + 1).append(" ")
					.append(classMap.get(testSetFV.m_InstanceID[i]))
					.append("}\n");
			}

			String fn_arff = Util.getOSPath(new String[]{outputFolder,
					varIDList[iVar] + ".arff"});
			Util.saveTextFile(fn_arff, sb.toString());
		}
    }
    
    /**
     * Create dev set for testing
     * 
     * @throws Exception
     */
    public void createLearningFileFromFn(String fn_xml) throws Exception {
    	List<String> instanceIDList = XMLUtil.getReportIDFromXMLList(fn_xml);
    	
    	String[] varIDList = new String[] {"any-adenoma",
    		      "appendiceal-orifice", "asa", "biopsy", "cecum", "ileo-cecal-valve",
    		      "indication-type", "informed-consent", "nursing-report", "prep-adequateNo",
    		      "prep-adequateNot", "prep-adequateYes", "proc-aborted", "withdraw-time"};
    	
    	ColonoscopyDS_SVMLightFormat libSVM = new ColonoscopyDS_SVMLightFormat();
    	
    	String varID, fn_featureVector, fn_index;
    	FeatureVector fv;
    	String docsFolder = Storage_Controller.getDocsFolder();
    	String fn_globalFeatureVector = Storage_Controller.getGlobalFeatureVectorFn();
    	boolean includeBiasFeature = true;
    	
    	for(int iVar = 0; iVar < varIDList.length; iVar++) {
    		varID = varIDList[iVar];
    		fn_featureVector = Storage_Controller.getDevFeatureFn(varID);
    		fn_index = Storage_Controller.getDevIndexFn(varID);
    		
    		libSVM.setClassValueMap(varID);
    		fv = libSVM.getFeatureVectorFromReportList(fn_globalFeatureVector, docsFolder,
    				instanceIDList);
    		libSVM.createLearningFileFromFeatureVector(fv, fn_featureVector, fn_index,
    				includeBiasFeature, fn_globalFeatureVector);
    	}
    }
    
    public void evaluateOnDevSet(String fn_modelListXML) throws Exception {
    	List<String> modelFnList = XMLUtil.getModelFnFromXMLList(fn_modelListXML);
    	String varID, fn_testFeature;
    	String fn_prediction = Storage_Controller.getPredictionFn();
    	LibSVMPredictor libSVM = new LibSVMPredictor();
    	String[] svmTestParams = new String[] {Storage_Controller.getLibSVMPath(),
                "", "", fn_prediction};
    	double[][] predictionMatrix;
    	int[][] confusionMatrix;
    	double[] perfMeasures;
    	
    	for(int iVar = 0; iVar < modelFnList.size(); iVar++) {
    		varID = Storage_Controller.getVarIdFromFn(modelFnList.get(iVar));
    		fn_testFeature = Storage_Controller.getDevFeatureFn(varID);
    		svmTestParams[1] = fn_testFeature;
    		svmTestParams[2] = Storage_Controller.getModelFn(modelFnList.get(iVar));
    		
    		predictionMatrix = libSVM.predict((Object)svmTestParams);
    		
    		confusionMatrix = libSVM.getConfusionMatrix(fn_testFeature, predictionMatrix);
    		
    		System.out.print(varID + ",");
    		perfMeasures = libSVM.getPerformanceMeasure(confusionMatrix, 0);
    		System.out.print(perfMeasures[0] + ",");
    		System.out.print(perfMeasures[1] + ",");
    		System.out.print(perfMeasures[2] + ",");
    		System.out.print(perfMeasures[3] + ",");
    		perfMeasures = libSVM.getPerformanceMeasure(confusionMatrix, 1);
    		System.out.print(perfMeasures[0] + ",");
    		System.out.print(perfMeasures[1] + ",");
    		System.out.print(perfMeasures[2] + ",");
    		System.out.println(perfMeasures[3] + ",");
    	}
    }
}
