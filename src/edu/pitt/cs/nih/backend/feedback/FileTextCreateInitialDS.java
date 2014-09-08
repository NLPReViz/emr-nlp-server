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
import emr_vis_nlp.ml.LibLinearPredictor;
import emr_vis_nlp.ml.LibSVMPredictor;
import frontEnd.serverSide.controller.Feedback_Controller;
import frontEnd.serverSide.controller.Storage_Controller;
import frontEnd.serverSide.controller.WordTree_Controller;
import frontEnd.serverSide.model.FeedbackSpan_Model;
import frontEnd.serverSide.model.FeedbackSpan_WordTree_Model;
import frontEnd.serverSide.model.Feedback_Abstract_Model;
import frontEnd.serverSide.model.Feedback_Document_Model;
import frontEnd.serverSide.model.Feedback_WordTree_JSON_Model;
import frontEnd.serverSide.model.ReportPrediction_Model;
import frontEnd.serverSide.model.TextSpan_Model;

import java.nio.channels.SeekableByteChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Phuong Pham <a href="mailto:phuongpham@cs.pitt.edu">phuongpham@cs.pitt.edu</a>
 */
/**
 * @author Phuong Pham
 *
 */
public class FileTextCreateInitialDS {
	
	public static String[] varIDList = new String[]{
			"any-adenoma", "appendiceal-orifice", "asa", "biopsy", "cecum",
			"ileo-cecal-valve", "indication-type", "informed-consent",
			"nursing-report", "prep-adequateNo", "prep-adequateNot",
			"prep-adequateYes", "proc-aborted", "withdraw-time"};
	
	/**
	 * Initialize session management file and feedback file from the default initial list
	 * modelList.0..xml
	 * 
	 * @param fn_modelList
	 * @throws Exception
	 */
	public void initializeFeedbackFile(String fn_modelList,
			String fn_instanceIDList) throws Exception {
//		initializeFeedbackFileWeka(XMLUtil.getModelFnFromXMLList(fn_modelList),
//		XMLUtil.getReportIDFromXMLList(fn_instanceIDList));
//		initializeFeedbackFileLibSVM(XMLUtil.getModelFnFromXMLList(fn_modelList),
//				XMLUtil.getReportIDFromXMLList(fn_instanceIDList));
		initializeFeedbackLibSVM(XMLUtil.getModelFnFromXMLList(fn_modelList),
				XMLUtil.getReportIDFromXMLList(fn_instanceIDList));
	}
	
	/**
	 * Initialize session management file and feedback file with no initial 
	 * training set and no initial models (an empty data set)
	 * 
	 * @param fn_modelList
	 * @throws Exception
	 */
	public void initializeFeedbackFileEmpty() throws Exception {   	
    	// reset the feedback file
    	Util.saveTextFile(Storage_Controller.getFeedbackFn(), "");

    	// clear modelList folder
    	Util.clearFolder(Storage_Controller.getModelListFolder());
    	// add a fake model list with non-existed filenames
    	String[] sessionIDList = new String[varIDList.length];
    	Arrays.fill(sessionIDList, "-1");
    	String[] userIDList = new String[varIDList.length];
    	Arrays.fill(userIDList, "");
    	String fn_initialModelList = Util.getOSPath(new String[]{
    			Storage_Controller.getModelListFolder(),
				"modelList.-1..xml"});
    	XMLUtil.createXMLPredictor(sessionIDList, userIDList, varIDList,
    			Storage_Controller.getModelListFolder(), "0", "",
    			fn_initialModelList);
    	// reset the session manager file
		String userID = ""; // default user is ""
    	String sessionID = "0"; // default sessionID = "-1"
    	
    	if(Util.fileExists(Storage_Controller.getSessionManagerFn())) {
    		Util.deleteFile(Storage_Controller.getSessionManagerFn());
    	}
    	List<String> modelFnList = XMLUtil.getModelFnFromXMLList(fn_initialModelList);
    	createSessionEntries(modelFnList, sessionID, userID,
    			Storage_Controller.getSessionManagerFn());
    	
    	// clear learning folder
    	Util.clearFolder(Storage_Controller.getTrainingFileFolder());
    	
    	// clear model folder
    	Util.clearFolder(Storage_Controller.getModelFolder());
    	
    	// clear weight folder
    	Util.clearFolder(Storage_Controller.getWeightFolder());
    	
    	// clear word tree annotation feedback file
    	Util.saveTextFile(Storage_Controller.getWordTreeFeedbackFn(), "");
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
//    	createFeedbackEntries(sessionID, userID,
//    			Storage_Controller.getFeedbackFn(),
//    			Storage_Controller.getInitialIDFolder());
    	createFeedbackEntries(modelFnList, sessionID, userID, Storage_Controller.getFeedbackFn(), instanceIDList);

    	// clean modelList folder
    	Util.clearFolder(Storage_Controller.getModelListFolder());
    	// clear learning folder
    	Util.clearFolder(Storage_Controller.getTrainingFileFolder());
    	// initialize learning files from modelList linked with feedback file
    	svm.createLearningFileFromSession(sessionID, userID);
    	
    	String feedbackFileName = Storage_Controller.getFeedbackFn();
    	String fn_sessionManager = Storage_Controller.getSessionManagerFn();
    	String learningFolder = Storage_Controller.getTrainingFileFolder();
    	String docsFolder = Storage_Controller.getDocsFolder();
    	String modelFolder = Storage_Controller.getModelFolder();
    	String featureWeightFolder = Storage_Controller.getWeightFolder();
    	String globalFeatureName = Storage_Controller.getGlobalFeatureVectorFn();
    	String xmlPredictorFolder = Storage_Controller.getModelListFolder();
    	// clear model folder
    	Util.clearFolder(modelFolder);
    	// clear weight folder
    	Util.clearFolder(featureWeightFolder);
    	// update the current (0..) model
    	TextFileFeedbackManagerLibSVM feedbackManager = new TextFileFeedbackManagerLibSVM(feedbackFileName,
    			fn_sessionManager, learningFolder, docsFolder, modelFolder, featureWeightFolder,
    			globalFeatureName, xmlPredictorFolder);
    	feedbackManager.setUserID(userID);
    	feedbackManager.updateModels();
    	
    	// clear word tree annotation feedback file
    	Util.saveTextFile(Storage_Controller.getWordTreeFeedbackFn(), "");
    }
	
//	/**
//     * create initial session manager and feedback file from training files of a model list
//     * 
//     * @param modelFnList
//     * @throws Exception
//     */
//    public void initializeFeedbackFileLibSVM(List<String> modelFnList,
//    		List<String> instanceIDList) throws Exception {
//    	String userID = ""; // default user is ""
//    	String sessionID = "0"; // default sessionID = "1"
//    	ColonoscopyDS_SVMLightFormat svm = new ColonoscopyDS_SVMLightFormat();
//    	
//    	// initialize the session manager file
//    	if(Util.fileExists(Storage_Controller.getSessionManagerFn())) {
//    		Util.deleteFile(Storage_Controller.getSessionManagerFn());
//    	}
//    	createSessionEntries(modelFnList, sessionID, userID,
//    			Storage_Controller.getSessionManagerFn());
//    	
//    	// initialize the feedback file
//    	if(Util.fileExists(Storage_Controller.getFeedbackFn())) {
//    		Util.deleteFile(Storage_Controller.getFeedbackFn());
//    	}
//    	createFeedbackEntries(modelFnList, sessionID, userID,
//    			Storage_Controller.getFeedbackFn(), instanceIDList);
//
//    	// clean modelList folder
//    	String[] fnList = Util.loadFileList(Storage_Controller.getModelListFolder());
//    	for(int i = 0; i < fnList.length; i++) {
//    		String fnModel = fnList[i];
//    		if(!fnModel.contains("modelList.0.")) { // the initial session
//    			Util.deleteFile(Util.getOSPath(new String[]{
//    					Storage_Controller.getModelListFolder(), fnModel}));
//    		}
//    	}
//    	// clear learning folder
//    	Util.clearFolder(Storage_Controller.getTrainingFileFolder());
//    	// initialize learning files from modelList linked with feedback file
//    	svm.createLearningFileFromSession(sessionID, userID);
//    	// create dev set
//    	createLearningFileFromFn(Util.getOSPath(new String[]{
//    					Storage_Controller.getDocumentListFolder(), "devIDList.xml"}));
//    	
//    	String feedbackFileName = Storage_Controller.getFeedbackFn();
//    	String fn_sessionManager = Storage_Controller.getSessionManagerFn();
//    	String learningFolder = Storage_Controller.getTrainingFileFolder();
//    	String docsFolder = Storage_Controller.getDocsFolder();
//    	String modelFolder = Storage_Controller.getModelFolder();
//    	String featureWeightFolder = Storage_Controller.getWeightFolder();
//    	String globalFeatureName = Storage_Controller.getGlobalFeatureVectorFn();
//    	String xmlPredictorFolder = Storage_Controller.getModelListFolder();
//    	// clear model folder
//    	Util.clearFolder(modelFolder);
//    	// clear weight folder
//    	Util.clearFolder(featureWeightFolder);
//    	// update the current (0..) model
//    	TextFileFeedbackManagerLibSVM feedbackManager = new TextFileFeedbackManagerLibSVM(feedbackFileName,
//    			fn_sessionManager, learningFolder, docsFolder, modelFolder, featureWeightFolder,
//    			globalFeatureName, xmlPredictorFolder);
//    	feedbackManager.setUserID(userID);
//    	feedbackManager.updateModels();
//    	
//    	// clear word tree annotation feedback file
//    	Util.saveTextFile(Storage_Controller.getWordTreeFeedbackFn(), "");
//    	
//    	// legacy
////    	String[] fnList;
////    	// remove additional weight files
////    	fnList = Util.loadFileList(Storage_Controller.getWeightFolder());
////    	for(int i = 0; i < fnList.length; i++) {
////    		if(fnList[i].charAt(0) != '0') { // e.g. 1.1.biopsy.weight.csv
////    			Util.deleteFile(Util.getOSPath(new String[]{
////    					Storage_Controller.getWeightFolder(), fnList[i]}));
////    		}
////    	}
////    	
////    	// remove additional model files
////    	fnList = Util.loadFileList(Storage_Controller.getModelFolder());
////    	for(int i = 0; i < fnList.length; i++) {
////    		if(fnList[i].charAt(0) != '0') { // e.g. 1.1.biopsy.weight.csv
////    			Util.deleteFile(Util.getOSPath(new String[]{
////    					Storage_Controller.getModelFolder(), fnList[i]}));
////    		}
////    	}
////    	
////    	// remove additional model list
////    	fnList = Util.loadFileList(Storage_Controller.getModelListFolder());
////    	for(int i = 0; i < fnList.length; i++) {
////    		int index = fnList[i].indexOf(".");
////    		if(!fnList[i].substring(index + 1, index + 2).equals("0")) { // e.g. modelList.1.1.xml
////    			Util.deleteFile(Util.getOSPath(new String[]{
////    					Storage_Controller.getModelListFolder(), fnList[i]}));
////    		}
////    	}
//    }
	
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
        String requestID, varID, instanceID, instanceClass;        
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
     * @param modelFnList
     * @param sessionID
     * @param userID
     * @param fn_feedbackMeta
     * @param initialFolder
     * @throws Exception
     */
    protected void createFeedbackEntries(String sessionID,
            String userID, String fn_feedbackMeta, String initialFolder)
            		throws Exception {
        String requestID, instanceID, instanceClass;        
        int iCount = 0;
        StringBuilder feedbackText = new StringBuilder(); 
        Map<String,String> classValueMap;
        
        requestID = new SimpleDateFormat("yyyyMMddHHmmss").format(
                Calendar.getInstance().getTime());
        
        for(String varID : varIDList) {
        	classValueMap = ColonoscopyDS_SVMLightFormat.getClassMap(varID);
        	String[] instanceIDList = Util.loadList(Util.getOSPath(new String[]{
        			initialFolder, varID + "-id.txt"}));
        	for(int i = 0; i < instanceIDList.length; i++) {
        		instanceID = instanceIDList[i];        		
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
    
    public void createInitialIDListForSession0(String fn_reportIDXML,
    		String initialFolder) throws Exception {
    	List<String> reportIDList = XMLUtil.getReportIDFromXMLList(fn_reportIDXML);
    	Collections.sort(reportIDList);
    	String[] initialReportID = reportIDList.toArray(new String[reportIDList.size()]);
    	String idContent = Util.joinString(initialReportID, "\n");
    	for(String varID : varIDList) {
    		Util.saveTextFile(Util.getOSPath(new String[]{
    				initialFolder, varID + "-id.txt"}), idContent);    		
    	}
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
    public List<Feedback_Document_Model> addFeedBack1(String userID) throws Exception {
        // create a batch of feedback
        ArrayList<Feedback_Document_Model> batch = new ArrayList<>();
        
        // first var value
        Feedback_Document_Model varValue = new Feedback_Document_Model();
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
        varValue = new Feedback_Document_Model();
        varValue.setRequestId(getRequestID());
        varValue.setDocId("0004");
        varValue.setVariableName("biopsy");
        varValue.setDocValue("True");
        batch.add(varValue);
        
        // third var value
        varValue = new Feedback_Document_Model();
        varValue.setRequestId(getRequestID());
        varValue.setDocId("0005");
        varValue.setVariableName("biopsy");
        varValue.setDocValue("True");
        batch.add(varValue);
        
        return batch;
	}
    
    
    public List<Feedback_Document_Model> addFeedBack2(String userID) throws Exception {
        // create a batch of feedback
        ArrayList<Feedback_Document_Model> batch = new ArrayList<>();
        
        // first var value
        Feedback_Document_Model varValue = new Feedback_Document_Model();
        varValue.setRequestId(getRequestID());
        varValue.setDocId("0001");
        varValue.setVariableName("cecum");
        varValue.setDocValue("True");
        batch.add(varValue);
        
        // second var value
        varValue = new Feedback_Document_Model();
        varValue.setRequestId(getRequestID());
        varValue.setDocId("0001");
        varValue.setVariableName("asa");
        varValue.setDocValue("True");
        batch.add(varValue);
        
        // third var value
        varValue = new Feedback_Document_Model();
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

    public List<Feedback_Document_Model> addFeedBack3(String userID) throws Exception {
        // create a batch of feedback
        ArrayList<Feedback_Document_Model> batch = new ArrayList<>();
        
        // first var value
        Feedback_Document_Model varValue = new Feedback_Document_Model();
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
        varValue = new Feedback_Document_Model();
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
//    	LibSVMPredictor libSVM = new LibSVMPredictor();
    	LibLinearPredictor libSVM = new LibLinearPredictor();
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
    
    public void verifyWordTreeAnnotation() throws Exception {
//    	// print report ID contains the span (compare with word tree)    	
//    	documentIDContainsText();
//    	documentIDContainsTextFromWordTree();
    	// re-create the data set files
    	String fn_modelList = Util.getOSPath(new String[] {
				Storage_Controller.getModelListFolder(), "modelList.0..xml" });
		String fn_reportIDList = Util.getOSPath(new String[]{
				Storage_Controller.getDocumentListFolder(), "initialIDList.xml"});
		// re-create the whole dataset
    	initializeFeedbackFile(fn_modelList, fn_reportIDList);
//    	// only re-create wordtree annotation
//    	Util.saveTextFile(Storage_Controller.getWordTreeFeedbackFn(), "");
    	
    	// handle feedback
		String userID = "1";
		String feedbackFileName = Storage_Controller.getFeedbackFn();
		String fn_sessionManager = Storage_Controller.getSessionManagerFn();
		String _learningFolder = Storage_Controller.getTrainingFileFolder();
		String _docsFolder = Storage_Controller.getDocsFolder();
		String _modelFolder = Storage_Controller.getModelFolder();
		String _featureWeightFolder = Storage_Controller.getWeightFolder();
		String _globalFeatureName = Storage_Controller
				.getGlobalFeatureVectorFn();		
		String _xmlPredictorFolder = Storage_Controller.getModelListFolder();
		String _fn_wordTreeFeedback = Storage_Controller.getWordTreeFeedbackFn();
		
		TextFileFeedbackManager_LibSVM_WordTree manager = new TextFileFeedbackManager_LibSVM_WordTree(
    			feedbackFileName, fn_sessionManager, _learningFolder, _docsFolder, 
    			_modelFolder, _featureWeightFolder, _globalFeatureName, _xmlPredictorFolder,
    			_fn_wordTreeFeedback);
		manager.setUserID(userID);
		// got from the front-end
		List<Feedback_WordTree_JSON_Model> feedbackBatch = addWordTreeJSONAnnotation();
		// intermediate step, convert Feedback_WordTree_JSON_Model into Feedback_Abstract_Model
		List<Feedback_Abstract_Model> feedbackBatchBackEnd = Feedback_WordTree_JSON_Model.toFeedbackModelList(feedbackBatch);
//    	// add word tree annotation
//    	manager.saveFeedbackBatch(feedbackBatchBackEnd);
//    	manager.createLearningFiles();
		System.out.println(manager.processFeedback(feedbackBatchBackEnd));
//		// test skipped n-gram
//    	Map<String,String> spanMap = new HashMap<>();
//    	spanMap.put("selected", "(ISH & FISH) Anatomic Pathology Testing");
//    	spanMap.put("matched", "(ISH & FISH) , Molecular Anatomic Pathology , and Immunofluorescent Testing");
//    	System.out.println(manager.wordTreeSkippedNGramPatternString(spanMap));
    }
    
    protected void documentIDContainsTextFromWordTree() throws Exception {
    	String text = "biopsy";
    	String fn_xmlList = Util.getOSPath(new String[]{Storage_Controller.getDocumentListFolder(),
			"devIDList.xml"});
		List<String> reportIDList = XMLUtil.getReportIDFromXMLList(fn_xmlList);
		Map<String, Object> sentenceMap = new WordTree_Controller().getWordTree(reportIDList, text);
		List<Map<String, Object>> leftBranchList = (List<Map<String, Object>>) sentenceMap.get("lefts");
		List<String> matchSentence = new ArrayList<>();
		int count = 0;
		for(Map<String, Object> leftBranch : leftBranchList) {
			List<String> leftTokenList = (List<String>)leftBranch.get("sentence");			
//			if(leftTokenList.get(leftTokenList.size() - 1).equals("diminutive")) {
			for(int i = 0; i < leftTokenList.size(); i++) {
				if(leftTokenList.contains("using")) {
				System.out.println("[" + leftBranch.get("doc") + "]" + leftTokenList);
				++count;
				matchSentence.add((String)leftBranch.get("doc"));
				break;
				}
			}
		}
		System.out.println(count);
		Collections.sort(matchSentence);
		System.out.println(matchSentence);
    }
    
    protected List<String> documentIDContainsText() throws Exception {
    	String text = "diminutive using biopsy";
    	// get from devID list
    	String fn_xmlList = Util.getOSPath(new String[]{Storage_Controller.getDocumentListFolder(),
    			"devIDList.xml"});
    	List<String> reportIDList = XMLUtil.getReportIDFromXMLList(fn_xmlList);
    	List<String> matchIDList = new ArrayList<>();
    	Pattern pattern;
//    	pattern = Pattern.compile(text, Pattern.CASE_INSENSITIVE); // raw text compare
    	System.out.println(text.replaceAll("\\s",  ".+?"));
    	pattern = Pattern.compile(text.replaceAll("\\s",  ".+?"), Pattern.MULTILINE | Pattern.DOTALL); // skipped n-gram
    	Matcher m;
    	int count = 0;
    	String doc;
    	for(String reportID : reportIDList) {
    		doc = Util.loadTextFile(Util.getOSPath(new String[]{
    				Storage_Controller.getDocsFolder(), reportID,
    				Storage_Controller.getColonoscopyReportFn()}));
    		m = pattern.matcher(doc);
    		while(m.find()) {
    			matchIDList.add(reportID + "a");
    			System.out.println("[" + reportID + "a]" + m.group());
    			++count;
    		}
    		
    		if(Util.fileExists(Util.getOSPath(new String[]{Storage_Controller.getDocsFolder(),
    				reportID, Storage_Controller.getPathologyReportFn()}))) {
    			doc = Util.loadTextFile(Util.getOSPath(new String[]{Storage_Controller.getDocsFolder(),
    				reportID, Storage_Controller.getPathologyReportFn()}));
    			m = pattern.matcher(doc);
    			while(m.find()) {
    				matchIDList.add(reportID + "b");
    				System.out.println("[" + reportID + "b]" + m.group());
    				++count;
    			}
    		}
    	}
    	System.out.println(count);
    	Collections.sort(matchIDList);
    	System.out.println(matchIDList);
    	return matchIDList;
    }
    
    public List<Feedback_Abstract_Model> addWordTreeAnnotation() throws Exception {
    	// create conflicts
    	List<Feedback_Abstract_Model> feedbackBatch = new ArrayList<>();
//    	// add 2 document level feedbacks
//    	Feedback_Document_Model documentFeedback;
////    	documentFeedback = new Feedback_Document_Model();
////    	documentFeedback.setDocId("0001");
////    	documentFeedback.setDocValue("True");
////    	documentFeedback.setVariableName("cecum");
////    	feedbackBatch.add(documentFeedback);
//    	documentFeedback = new Feedback_Document_Model();
//    	documentFeedback.setDocId("0002");
//    	documentFeedback.setDocValue("False");
//    	documentFeedback.setVariableName("cecum");
//    	feedbackBatch.add(documentFeedback);
    	
    	// add 5 span level feedbacks
    	FeedbackSpan_WordTree_Model wordTreeFeedback;
    	List<String> reportIDList;
    	wordTreeFeedback = new FeedbackSpan_WordTree_Model();
    	wordTreeFeedback.setDocValue("True");
    	wordTreeFeedback.setMatchedTextSpan("(ISH & FISH) , Molecular Anatomic Pathology , and\nImmunofluorescent Testing");
    	wordTreeFeedback.setVariableName("cecum");
    	reportIDList = new ArrayList<>();
    	reportIDList.add("0002");
    	wordTreeFeedback.setReportIDList(reportIDList);
    	feedbackBatch.add(wordTreeFeedback);    	
    	wordTreeFeedback = new FeedbackSpan_WordTree_Model();
    	wordTreeFeedback.setDocValue("False");
    	wordTreeFeedback.setMatchedTextSpan("were thoroughly explained , informed consent was obtained");
    	wordTreeFeedback.setVariableName("cecum");
    	reportIDList = new ArrayList<>();
    	reportIDList.add("0001");
    	wordTreeFeedback.setReportIDList(reportIDList);
    	feedbackBatch.add(wordTreeFeedback);
    	
    	return feedbackBatch;
    }
    
    public List<Feedback_WordTree_JSON_Model> addWordTreeJSONAnnotation() throws Exception {
    	List<Feedback_WordTree_JSON_Model> feedbackBatch = new ArrayList<>();
    	Feedback_WordTree_JSON_Model feedback;
    	List<String> docIDList;
    	// add doc level feedback
    	feedback = new Feedback_WordTree_JSON_Model();
    	feedback.setKind("TYPE_DOC");
    	feedback.setClassification("negative");
    	docIDList = new ArrayList<>();
    	docIDList.add("0080");
    	feedback.setDocList("0041");
    	feedback.setVariable("prep-adequateYes");
    	feedbackBatch.add(feedback);
    	
    	// add normal span feedback
    	feedback = new Feedback_WordTree_JSON_Model();
    	feedback.setKind("TYPE_TEXT");
    	feedback.setClassification("negative");
    	docIDList = new ArrayList<>();
    	docIDList.add("0192");
    	feedback.setDocList("0059");
    	feedback.setVariable("prep-adequateYes");
    	feedback.setSelected("blindly into the rectum"); // the span as it is
    	feedbackBatch.add(feedback);
    	
    	// add word tree span feedback
    	feedback = new Feedback_WordTree_JSON_Model();
    	feedback.setKind("TYPE_WORDTREE");
    	feedback.setClassification("negative");
    	docIDList = new ArrayList<>();
    	docIDList.add("0080");
    	feedback.setDocList(docIDList);
    	feedback.setVariable("prep-adequateYes");
    	feedback.setSelected("withdrawn terminating"); // skipped span
    	feedback.setSpan("withdrawn terminating"); // matched span
    	feedbackBatch.add(feedback);
    	
    	return feedbackBatch;
    }
    
    public void verifyFullModel() throws Exception {
    	List<String> instanceIDList = XMLUtil.getReportIDFromXMLList(Util.getOSPath(new String[]{
				Storage_Controller.getDocumentListFolder(),"fullIDList.xml"}));
//    	List<String> instanceIDList = XMLUtil.getReportIDFromXMLList(Util.getOSPath(new String[]{
//				Storage_Controller.getDocumentListFolder(),"initialIDList.xml"}));
    	
//    	String[] varIDList = new String[] {"informed-consent"};
    	String[] varIDList = new String[] {"any-adenoma",
    		      "appendiceal-orifice", "asa", "biopsy", "cecum", "ileo-cecal-valve",
    		      "indication-type", "informed-consent", "nursing-report", "prep-adequateNo",
    		      "prep-adequateNot", "prep-adequateYes", "proc-aborted", "withdraw-time"};
    	ColonoscopyDS_SVMLightFormat ds = new ColonoscopyDS_SVMLightFormat();
    	String fn_trainFeature = Storage_Controller.getTempLearningFeatureFn();
    	String fn_trainIndex = Storage_Controller.getTempLearningIndexFn();
    	String fn_modelWeight = Storage_Controller.getTempLearningWeightFn();
    	for(String varID : varIDList) {
    		Map<String,String> classValueMap = getClassValueMap(varID);
    		String fn_model = Util.getOSPath(new String[]{
    				Storage_Controller.getModelFolder(), "full", "full." + varID + ".model"});
    		String fn_featureWeight = Util.getOSPath(new String[]{
    				Storage_Controller.getModelFolder(), "full", "full." + varID + ".weight.csv"});
    		String[] hyperParamList = Util.loadList(
        			Storage_Controller.getHyperParameterFn(varID), ",");
        	double C = Double.parseDouble(hyperParamList[0]);
        	double C_contrast = Double.parseDouble(hyperParamList[1]);
        	double mu = Double.parseDouble(hyperParamList[2]);
    		ds.trainModelnFeatureWeight(instanceIDList, classValueMap,
    				fn_trainFeature, fn_trainIndex, fn_model,
    				fn_modelWeight, fn_featureWeight, C, C_contrast, mu);
    	}
    }
    
    protected Map<String,String> getClassValueMap(String varID) throws Exception {
    	Map<String,String> classValueMap = new HashMap<>();
    	String[][] classValueTable = Util.loadTable(
				Util.getOSPath(new String[] {Storage_Controller.getClassFn(varID)}));
    	
    	for(int i = 0; i < classValueTable.length; i++) {
    		classValueMap.put(classValueTable[i][0], classValueTable[i][1]);
    	}
    	
    	return classValueMap;
    }
    
    public List<Feedback_WordTree_JSON_Model> addFeedback4() {
    	List<Feedback_WordTree_JSON_Model> batch = new ArrayList<>();
    	Feedback_WordTree_JSON_Model feedback;
    	
    	feedback = new Feedback_WordTree_JSON_Model();
    	feedback.setClassification("negative");
    	feedback.setKind("TYPE_DOC");
    	feedback.setVariable("any-adenoma");
    	feedback.setSelected("alternatives of the procedure");
    	feedback.setDocList("0001");
    	batch.add(feedback);
    	
    	feedback = new Feedback_WordTree_JSON_Model();
    	feedback.setClassification("positive");
    	feedback.setKind("TYPE_DOC");
    	feedback.setVariable("any-adenoma");
    	feedback.setSelected("After the risks");
    	feedback.setDocList("0001");
    	batch.add(feedback);
    	
    	return batch;
    }
	
	public void softResetDB(String fn_modelList,
			String fn_instanceIDList) throws Exception {
		String userID = ""; // default user is ""
    	String sessionID = "0"; // default sessionID = "1"
//    	ColonoscopyDS_SVMLightFormat svm = new ColonoscopyDS_SVMLightFormat();
    	List<String> modelFnList = XMLUtil.getModelFnFromXMLList(fn_modelList);
    	List<String> instanceIDList = XMLUtil.getReportIDFromXMLList(fn_instanceIDList);
    	
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

    	// clear word tree annotation feedback file
    	Util.saveTextFile(Storage_Controller.getWordTreeFeedbackFn(), "");

    	// clean modelList folder
    	String[] fnList = Util.loadFileList(Storage_Controller.getModelListFolder());
    	for(int i = 0; i < fnList.length; i++) {
    		String fnModel = fnList[i];
    		if(!fnModel.contains("modelList.0.")) { // the initial session
    			Util.deleteFile(Util.getOSPath(new String[]{
    					Storage_Controller.getModelListFolder(), fnModel}));
    		}
    	}
    	// clear learning folder
    	fnList = Util.loadFileList(Storage_Controller.getTrainingFileFolder());
    	for(int i = 0; i < fnList.length; i++) {
    		String fnModel = fnList[i];
    		if(!fnModel.substring(0, 3).equals("0..") || // the initial session
    				!fnModel.substring(0, 3).equals("dev")) { // development session
    			Util.deleteFile(Util.getOSPath(new String[]{
    					Storage_Controller.getModelListFolder(), fnModel}));
    		}
    	}
    	
    	// clear model folder
    	fnList = Util.loadFileList(Storage_Controller.getModelFolder());
    	for(int i = 0; i < fnList.length; i++) {
    		String fnModel = fnList[i];
    		if(!fnModel.substring(0, 3).equals("0..")){ // initial session
    			Util.deleteFile(Util.getOSPath(new String[]{
    					Storage_Controller.getModelListFolder(), fnModel}));
    		}
    	}
    	
    	// clear weight folder
    	fnList = Util.loadFileList(Storage_Controller.getWeightFolder());
    	for(int i = 0; i < fnList.length; i++) {
    		String fnModel = fnList[i];
    		if(!fnModel.substring(0, 3).equals("0..")){ // initial session
    			Util.deleteFile(Util.getOSPath(new String[]{
    					Storage_Controller.getModelListFolder(), fnModel}));
    		}
    	}
	}
	
	/**
     * create initial session manager and feedback file from training files of a model list
     * 
     * @param modelFnList
     * @throws Exception
     */
    public void initializeFeedbackLibSVM(List<String> modelFnList,
    		List<String> instanceIDList) throws Exception {
    	// reset the feedback file
    	Util.saveTextFile(Storage_Controller.getFeedbackFn(), "");

    	// clear modelList folder
    	Util.clearFolder(Storage_Controller.getModelListFolder());
    	// add a fake model list with non-existed filenames
    	String[] sessionIDList = new String[varIDList.length];
    	Arrays.fill(sessionIDList, "-1");
    	String[] userIDList = new String[varIDList.length];
    	Arrays.fill(userIDList, "");
    	String fn_initialModelList = Util.getOSPath(new String[]{
    			Storage_Controller.getModelListFolder(),
				"modelList.-1..xml"});
    	XMLUtil.createXMLPredictor(sessionIDList, userIDList, varIDList,
    			Storage_Controller.getModelListFolder(), "0", "",
    			fn_initialModelList);
    	
    	// reset session file
    	Util.saveTextFile(Storage_Controller.getSessionManagerFn(), "");
    	
    	// clear learning folder
    	Util.clearFolder(Storage_Controller.getTrainingFileFolder());
    	
    	// clear model folder
    	Util.clearFolder(Storage_Controller.getModelFolder());
    	
    	// clear weight folder
    	Util.clearFolder(Storage_Controller.getWeightFolder());
    	
    	// clear word tree annotation feedback file
    	Util.saveTextFile(Storage_Controller.getWordTreeFeedbackFn(), "");
    	
    	// add feedback objects
    	List<Feedback_WordTree_JSON_Model> batch = new ArrayList<>();
    	Feedback_WordTree_JSON_Model feedback;
    	String instanceID, instanceClass;       
        Map<String,String> classValueMap;
        
        String initialFolder = Storage_Controller.getInitialIDFolder();
        
        if(!Util.fileExists(initialFolder)) {
        	String fn_initialList = Util.getOSPath(new String[]{
        			Storage_Controller.getDocumentListFolder(), "initialIDList.xml"});
        	Util.createFolder(initialFolder);
        	createInitialIDListForSession0(fn_initialList, initialFolder);
        }
        
        for(String varID : varIDList) {
        	classValueMap = ColonoscopyDS_SVMLightFormat.getClassMap(varID);
        	String[] instanceIDArray = Util.loadList(Util.getOSPath(new String[]{
        			initialFolder, varID + "-id.txt"}));
        	for(int i = 0; i < instanceIDArray.length; i++) {
        		instanceID = instanceIDArray[i];        		
        		instanceClass = classValueMap.get(instanceID).equals("1") ?
        				"positive" : "negative";
        		
        		feedback = new Feedback_WordTree_JSON_Model();
            	feedback.setClassification(instanceClass);
            	feedback.setKind("TYPE_DOC");
            	feedback.setVariable(varID);
            	feedback.setDocList(instanceID);
            	batch.add(feedback);
        	}
        }

        // create initial dataset
        String fn_modelList = "modelList.0..xml";
		String fn_reportIDList = "initialIDList.xml";
		String uid = "1";
		Map<String, Object> map = new Feedback_Controller().getFeedback(batch,
				fn_modelList, fn_reportIDList, uid);
//    	// add verify result here
//		Map<String,Map<String,String>> labelMap = new HashMap<>();
//		for(String varID : varIDList) {
//			classValueMap = ColonoscopyDS_SVMLightFormat.getClassMap(varID);
//			labelMap.put(varID, classValueMap);
//		}
//		StringBuilder sb = new StringBuilder();
//		Map<String, Object> gridVarObj = (Map<String, Object>) map.get("gridVarData");
//		List<Map<String, Object>> reportList = (List<Map<String, Object>>) gridVarObj.get("gridData");
//		for(Map<String, Object> report : reportList) {
//			sb.append(report.get("id")).append(",");
//			for(int iModel = 0; iModel < varIDList.length; iModel++) {
//				ReportPrediction_Model reportPrediction =
//						(ReportPrediction_Model) report.get(
//								Storage_Controller.getVarIdFromFn(varIDList[iModel]));
//				
//				sb.append(reportPrediction.getClassification()).append(",");
//			}
//			sb.append("\n");
//		}
//		Util.saveTextFile("perf-empty.csv", sb.toString());
    }
}
