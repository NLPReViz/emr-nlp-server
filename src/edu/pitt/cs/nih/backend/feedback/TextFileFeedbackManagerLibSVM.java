/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.pitt.cs.nih.backend.feedback;

import edu.pitt.cs.nih.backend.featureVector.ColonoscopyDS_SVMLightFormat;
import edu.pitt.cs.nih.backend.featureVector.Preprocess;
import edu.pitt.cs.nih.backend.featureVector.WekaDataSet;
import edu.pitt.cs.nih.backend.feedback.TextFileFeedbackManager.FeedbackType;
import edu.pitt.cs.nih.backend.utils.Util;
import edu.pitt.cs.nih.backend.utils.XMLUtil;
import emr_vis_nlp.ml.ALearner;
import emr_vis_nlp.ml.LibLinearPredictor;
import emr_vis_nlp.ml.LibSVMPredictor;
import frontEnd.serverSide.controller.Storage_Controller;
import frontEnd.serverSide.model.FeedbackSpan_Model;
import frontEnd.serverSide.model.Feedback_Abstract_Model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import weka.core.Instance;
import weka.core.Instances;

/**
 * This class implement a FeedbackManager using text files. 
 * The feedback manager uses text files depends entirely on the session ID for each batch.
 * This class uses the TextFileSessionManager to manage feedback batch sessions. 
 * All feedbacks will be stored in a single file with session IDs
 * 
 * @author phuongpham <a href="mailto:phuongpham@cs.pitt.edu">phuongpham@cs.pitt.edu</a>
 */
public class TextFileFeedbackManagerLibSVM extends FeedbackManager {

    String fn_feedback;
    TextFileSessionManager sessionManager;
    String userID;
    String learningFolder;
    String docsFolder;
    String fn_globalFeatureName;
    String modelFolder;
    String featureWeightFolder;
    String xmlPredictorFolder;
    boolean includeBiasFeature;
    
    // send response to the front-end
//    List<ALearner> predictors;
//    HashMap<String, Integer> attrNameToIndexMap;
//    List<HashMap<String, Double>> predictorsFeatureWeightMap;
//    List<List<Map.Entry<String, Double>>> predictorsKeywordWeightMap;
//    List<List<Map.Entry<String, Double>>> predictorsSortedTermWeightMap;
    String modelListName;
    
	/**
	 * Initialize with a particular feedback file name (full path)
	 * 
	 * @param feedbackFileName
	 */
	public TextFileFeedbackManagerLibSVM(String feedbackFileName,
			String fn_sessionManager, String _learningFolder,
			String _docsFolder, String _modelFolder,
			String _featureWeightFolder, String _globalFeatureName,
			String _xmlPredictorFolder) {
		try {
			sessionManager = new TextFileSessionManager(fn_sessionManager);
			fn_feedback = feedbackFileName;

			if (!Util.fileExists(fn_feedback)) {
				Util.saveTextFile(fn_feedback, "");
			}

			learningFolder = _learningFolder;

			docsFolder = _docsFolder;

			fn_globalFeatureName = _globalFeatureName;

			modelFolder = _modelFolder;

			featureWeightFolder = _featureWeightFolder;

			xmlPredictorFolder = _xmlPredictorFolder;
			
//			predictors = new ArrayList<>();
//	        attrNameToIndexMap = new HashMap<>();
//	        predictorsFeatureWeightMap = new ArrayList<>();
//	        predictorsKeywordWeightMap = new ArrayList<>();
//	        predictorsSortedTermWeightMap = new ArrayList<>();
	        modelListName = "";
	        includeBiasFeature = true;
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	/**
	 * Do all the steps required to retrain models:
	 * <lu>
	 * <li> 
	 * </lu>
	 * @param batch
	 * @throws Exception
	 */
	public String processFeedback(List<Feedback_Abstract_Model> batch) throws Exception {
		saveFeedbackBatch(batch);
		createLearningFiles();
		updateModels();
		createXMLPredictorFile();
		
		String newModelListName = "";
		return newModelListName;
	}

    /**
     * Set userID used by rollBackward and rollForward functions.
     * 
     * @param _userID 
     */
    public void setUserID(String _userID) {
        userID = _userID;
    }
    
    public String getUserID() {
    	return userID;
    }
    
    /**
     * save all feedbacks into a single file with a sessionID for a batch. This also 
     * updates the session meta file for a new session
     * <p>
     * There are 2 types of feedback, each type has a different format in the file.
     * <ol>
     * <li>Variable value: lineID, sessionID, userID, requestID, docID, varID, 
     * spanStart, spanEnd, change/create, pointer to old var value (lineID), new value (length=11)</li>
     * <li>Highlight span: lineID, sessionID, userID, requestID, docID, varID, 
     * spanStart, spanEnd, add/remove, pointer to var value of this highlight (lineID) (length=10)</li>
     * </ol>
     * 
     * @param feedbackBatch
     * @throws Exception 
     */
    @Override
    public void saveFeedbackBatch(List<Feedback_Abstract_Model> feedbackBatch)
    		throws Exception {
        String batchSessionID = sessionManager.getNewSessionID();
        ArrayList<String[]> sessionAddList = new ArrayList<>();
        String[][] feedbackMetaTable;
        String feedbackLine;
        String lineID;
        String feedbackText;
        String varID = "";
        
        // we would like to add variable value feedbacks first so all 
        // highlight span feedbacks will have a valid referenced value line
        feedbackBatch = sortFeedbackBatch(feedbackBatch);
        
        for(Feedback_Abstract_Model feedback : feedbackBatch) {
            feedbackMetaTable = getFeedbackMetaTable();
            lineID = getLineID(feedbackMetaTable);
            // extract string from the feedback            
            feedbackLine = ((IFeedbackTextFileSerializer)feedback).getFeedbackLine(
                    lineID, batchSessionID, userID, feedbackMetaTable);
            // append this feedback into the feedback file
            feedbackText = Util.loadTextFile(fn_feedback);
            feedbackText = feedbackText.concat(feedbackLine + "\n");
            Util.saveTextFile(fn_feedback, feedbackText);
            
            // add new session item
            varID = feedback.getVariableName();
            addNewFeedbackSessionItem(userID, varID, sessionAddList);
        }
        
        // update the session manager, and de-active previous session of modified variables
        for(String[] sessionAddItem : sessionAddList) {
            sessionManager.addSessionLine(batchSessionID, sessionAddItem[0], sessionAddItem[1]);
        }
    }
    
    /**
     * Move all variable value feedbacks to the head so they will be added earlier 
     * than highlight span feedbacks.
     * 
     * @param originalBatch
     * @return
     * @throws Exception 
     */
    protected List<Feedback_Abstract_Model> sortFeedbackBatch(
            List<Feedback_Abstract_Model> originalBatch) throws Exception {
        ArrayList<Feedback_Abstract_Model> varValueBatch = new ArrayList<>();
        ArrayList<Feedback_Abstract_Model> highlightSpanBatch = new ArrayList<>();
        
        for(Feedback_Abstract_Model feedback : originalBatch) {
            if (feedback instanceof FeedbackSpan_Model) {
            	highlightSpanBatch.add(feedback);
            }
            else {
            	varValueBatch.add(feedback);
            }
        }
        // merge batch so all variable value feedbacks will be added first
        varValueBatch.addAll(highlightSpanBatch);
        return varValueBatch;
    }

    /**
     * Save feedback batch with a specific sessionID, userID. 
     * This function does not update the session management. 
     * The initial purpose is to create initial DS
     * 
     * @param feedbackBatch
     * @param batchSessionID
     * @throws Exception 
     */
    public ArrayList<String[]> saveFeedbackBatch(List<Feedback_Abstract_Model> feedbackBatch,
            String batchSessionID, String userID) throws Exception {
        ArrayList<String[]> sessionAddList = new ArrayList<>();
        String[][] feedbackMetaTable;
        String feedbackLine;
        String lineID;
        String feedbackText;
        String varID = "";
        
        for(Feedback_Abstract_Model feedback : feedbackBatch) {
            feedbackMetaTable = getFeedbackMetaTable();
            lineID = getLineID(feedbackMetaTable);
            // extract string from the feedback            
            feedbackLine = ((IFeedbackTextFileSerializer)feedback).getFeedbackLine(
                    lineID, batchSessionID, userID, feedbackMetaTable);
            // append this feedback into the feedback file
            feedbackText = Util.loadTextFile(fn_feedback);
            feedbackText = feedbackText.concat(feedbackLine + "\n");
            Util.saveTextFile(fn_feedback, feedbackText);
            
            // add new session item
//            userID = feedback.getUserID();
//            varID = feedback.getNLPVariable().toString();
            varID = feedback.getVariableName();
            addNewFeedbackSessionItem(userID, varID, sessionAddList);
        }
        return sessionAddList;
    }

    @Override
    public void rollBackward() throws Exception {
        sessionManager.rollBackward(userID);
    }

    @Override
    public void rollForward() throws Exception {
        sessionManager.rollForward(userID);
    }
    
    /**
     * Check if the userID and varID exist in the list or not? If not, add.
     * 
     * @param userID
     * @param varID
     * @param sessionAddList 
     */
    protected void addNewFeedbackSessionItem(String userID, String varID, List<String[]> sessionAddList) {
        boolean existed = false;
        for(String[] sessionAddItem : sessionAddList) {
            if(userID.equals(sessionAddItem[0]) && varID.equals(sessionAddItem[1])) 
            {
                existed = true;
                break;
            }
        }
        if(! existed) {
            sessionAddList.add(new String[] {userID, varID});
        }
    }
    
    public String[][] getFeedbackMetaTable() {
        String[][] metaTable = null;
        try {
            metaTable = Util.loadTable(fn_feedback);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        
        return metaTable;
    }
    
    /**
     * Get the new LineID for a feedback file.
     * At the moment, we select the smallest integer number that is greater than 
     * the current max ID in the file (feedbackTable). Other choices could be 
     * adding prefix or suffix to distinguish between each kind of feedback.
     * <p>
     * Because of current setup does not remove any middle feedbacks, the latest line 
     * will contain the max ID.
     * <p>
     * The only case when we remove feedback out of the feedback file is when the user 
     * reject the feedback batch. But at that time, the rejected batch is at the end 
     * of the feedback file.
     * 
     * @param feedbackTable
     * @return 
     */
    public static String getLineID(String[][] feedbackTable) {
        int maxID = -1;
        if(feedbackTable.length > 0) {
            maxID = Integer.parseInt(feedbackTable[feedbackTable.length - 1][0]);
        }
        return Integer.toString(maxID + 1);
    }
    
    public static String convertDocID2Str(int docID) {
        return String.format("%04d", docID);
    }

    /**
     * Create learning files for the current active session.
     * The caller function must set the userID value before calling this function.
     * 
     * @throws Exception 
     */
    @Override
    public void createLearningFiles() throws Exception {
        // get the current state of each model
        String[][] currentSessionList = sessionManager.getCurrentState(userID);
        ColonoscopyDS_SVMLightFormat libSVMFile = new ColonoscopyDS_SVMLightFormat();
        
        String sessionID, userID, varID;
        // create learning files for all current states
        for(String[] currentSession : currentSessionList) {
            if(currentSession[4].equals("valid")) {
            	sessionID = currentSession[0];
            	userID = currentSession[1];
            	varID = currentSession[2];
            	libSVMFile.createLearningFileSet(sessionID, userID, varID);
            }
        }
    }
    
    /**
     * Create instance set (var value) learning files for a session of a user on a variable.
     * 
     * @param sessionID
     * @param userID
     * @param varID
     * @return
     * @throws Exception 
     */
    public Instances createInstanceSetLearningFile(String sessionID, String userID, 
            String varID) throws Exception {
        String prevSessionID;
        Instances instanceDS;               
        String fn_instanceSet = getInstanceDSFileName(sessionID, userID, varID);
        
        if(Util.fileExists(fn_instanceSet)) {
            instanceDS = loadInstancesFromFile(fn_instanceSet);
        }
        else { 
            // create the instance set based on the instance set from previous
            // session and instance feedback (variable value) in this session
            prevSessionID = sessionManager.getPreviousSessionID(sessionID,
                    userID, varID);
            List<String[]> varValueFeedbackList;
            if(prevSessionID.equals("")) {
                // there is no instance Set learing files at the initial data set
                // that's why we don't have learning file at this session ID (sessionID = 0)
                // only previous session of sessionID=0 is ""
                // create the learning files for initial DS using
                // var values of sessionID=0 in the feedback file
                // make user that sessionID = "0 at this step
                // create an empty data set
                instanceDS = WekaDataSet.createAnEmptyDataSet(varID, fn_globalFeatureName);
            }
            else {
                // get the previous learning files and the current feedback list
                instanceDS = createInstanceSetLearningFile(prevSessionID, userID, varID);
            }
            // get the list of feedback at the sessionID
            varValueFeedbackList = getFeedbackAtSession(
                        sessionID, userID, varID, FeedbackType.VariableValue);
            // add all feedback to the previous session learning file
            int feedbackIndex;
            Instance instance;
            String reportID;
            String instanceLabel;
            String[] instanceTextList = new String[2];
            String[] globalFeatureName = Util.loadList(fn_globalFeatureName);
            
            for(String[] feedback : varValueFeedbackList) {
                reportID = String.format("%04d", Integer.parseInt(feedback[4]));
                instanceLabel = feedback[10].toUpperCase().equals("TRUE") ? "1" : "0";               
                
                // whether the docID exists in the learning file
                feedbackIndex = -1;
                for(int i = 0; i < instanceDS.numInstances(); i++) {
                   instance = instanceDS.instance(i);
                   if(instance.stringValue(0).equals(reportID)) {
                       feedbackIndex = i;
                       break;
                   }
                }
                if(feedbackIndex == -1) {
                    // load raw text                   
                    instanceTextList[0] = Util.loadTextFile(
                            Util.getOSPath(new String[] {docsFolder, reportID,
                                "report.txt"}));
                     // remove header and footer before create feature vector (learning instance)
                    instanceTextList[0] = Preprocess.separateReportHeaderFooter(instanceTextList[0])[1];
                    
                    if(Util.fileExists(Util.getOSPath(new String[] {docsFolder, reportID,
                                "pathology.txt"}))) {
                        instanceTextList[1] = Util.loadTextFile(Util.getOSPath(
                                new String[] {docsFolder, reportID,
                                "pathology.txt"}));
                         // remove header and footer before create feature vector (learning instance)
                        instanceTextList[1] = Preprocess.separatePathologyHeaderFooter(instanceTextList[1])[1];
                    }
                    else {
                        instanceTextList[1] = "";
                    }
                    
                    // add the new instance to instanceDS
                    instanceDS.add(
                            WekaDataSet.getInstanceObject(instanceTextList,
                            globalFeatureName, reportID, instanceLabel, instanceDS));
                }
                else {
                    // only modify the existed
//                    instanceDS.instance(feedbackIndex).attribute(
//                            instanceDS.numAttributes() - 1).addStringValue(instanceLabel);
                	instanceDS.instance(feedbackIndex).setValue(instanceDS.numAttributes() - 1, instanceLabel);
                }
            }
            
            // save the learning file of sessionID
            WekaDataSet.saveInstancesToFile(fn_instanceSet, instanceDS);
        }
        
        return instanceDS;
    }
    
    /**
     * Create feedback set (var value) learning files for a session of a user on a variable.
     * 
     * @param sessionID
     * @param userID
     * @param varID
     * @return
     * @throws Exception 
     */
    public Instances createFeedbackSetLearningFile(String sessionID, String userID, 
            String varID) throws Exception {
        String prevSessionID;
        Instances highlightSpanDS;
        String fn_feedbackSet = getFeedbackDSFileName(sessionID, userID, varID);
        
        if(Util.fileExists(fn_feedbackSet)) {
            highlightSpanDS = loadInstancesFromFile(fn_feedbackSet);
        }
        else { 
            // create the instance set based on the instance set from previous
            // session and instance feedback (variable value) in this session
            prevSessionID = sessionManager.getPreviousSessionID(sessionID,
                    userID, varID);
            List<String[]> highlightSpanFeedbackList;
            if(prevSessionID.equals("")) {
                // there is no instance Set learing files at the initial data set
                // that's why we don't have learning file at this session ID (sessionID = 0)
                // only previous session of sessionID=0 is ""
                // create the learning files for initial DS using
                // var values of sessionID=0 in the feedback file
                // make user that sessionID = "0 at this step
                // create an empty data set
                highlightSpanDS = WekaDataSet.createAnEmptyDataSet(varID, fn_globalFeatureName);
            }
            else {
                // get the previous learning files and the current feedback list
                highlightSpanDS = createFeedbackSetLearningFile(prevSessionID, userID, varID);
            }
            // for highlight span feedback, first, we must remove all feedback in 
            // the previous session from the learning file
            // IF A USER ONLY FEEDBACK ON NEW DOC, THIS STEP DOES NOTHING
            highlightSpanFeedbackList = getFeedbackAtSession(prevSessionID,
                    userID, varID, FeedbackType.HighlightSpan);
            // because when create learning file for highlight span feedback (for now)
            // we merge all highlight spans of a document into a new instance,
            // here, we only need to search for each docID and remove out from the dataset
            for(String[] feedback : highlightSpanFeedbackList) {
                String reportID = String.format("%04d", Integer.parseInt(feedback[4]));
                for(int i = 0; i < highlightSpanDS.numInstances(); i++) {
                    if(highlightSpanDS.instance(i).attribute(0).toString().equals(reportID)) {
                        highlightSpanDS.delete(i);
                        break;
                    }
                }
            }
                    
            // get the list of highlight span feedback at the sessionID
            highlightSpanFeedbackList = getFeedbackAtSession(
                        sessionID, userID, varID, FeedbackType.HighlightSpan);
            // merge all feedback on a docID into a single text file
            HashMap<String, String> feedbackInstanceList = new HashMap<>();
            HashMap<String, String> feedbackLabelList = new HashMap<>();
            int spanStart;
            int spanEnd;
            String feedbackText;
            String feedbackLabel;
            String reportID;
            
            for(String[] feedback : highlightSpanFeedbackList) {
                reportID = String.format("%04d", Integer.parseInt(feedback[4]));
                
                // load text
                feedbackText = Util.loadTextFile(
                        Util.getOSPath(new String[]{docsFolder, reportID,
                    "report.txt"}));
                if (Util.fileExists(Util.getOSPath(new String[]{docsFolder, reportID,
                    "pathology.txt"}))) {
                    feedbackText += System.lineSeparator() + Util.loadTextFile(Util.getOSPath(
                            new String[]{docsFolder, reportID,
                        "pathology.txt"}));
                }
//                // old approach, get class value of a span using its instance class value
//                feedbackLabel = getVarValueAtLineID(feedback[9]);
                // new approach, each span has its own class label (could be different from 
                // its instance's class label)
                feedbackLabel = feedback[9];
                // extract the highlight span
                spanStart =  Integer.parseInt(feedback[6]);
                spanEnd =  Integer.parseInt(feedback[7]);
                feedbackText = feedbackText.substring(spanStart, spanEnd);
                
                
                try {
                    feedbackInstanceList.put(reportID, feedbackInstanceList.get(reportID).concat( " " + feedbackText));
                    feedbackLabelList.put(reportID, feedbackLabel);
                }
                catch (Exception e) {
                    feedbackInstanceList.put(reportID, feedbackText);
                    feedbackLabelList.put(reportID, feedbackLabel);
                }
            }
            
            // add all feedback to the previous session learning file
            String[] globalFeatureName = Util.loadList(fn_globalFeatureName);
            String[] instanceTextList = new String[2];
            instanceTextList[1] = "";
            Iterator<String> it = feedbackInstanceList.keySet().iterator();
            while (it.hasNext()) {
                reportID = (String) it.next();
                instanceTextList[0] = feedbackInstanceList.get(reportID);
                feedbackLabel = feedbackLabelList.get(reportID).toUpperCase().equals("TRUE") ? "1" : "0";
                // assume that user never give highlight at header and footer section
                highlightSpanDS.add(WekaDataSet.getInstanceObject(instanceTextList,
                            globalFeatureName, reportID, feedbackLabel, highlightSpanDS));
            }
            
            // save the learning file of sessionID
            WekaDataSet.saveInstancesToFile(fn_feedbackSet, highlightSpanDS);
        }
        
        return highlightSpanDS;
    }
    
    /**
     * Load a Weka learning file.
     * 
     * @param fn_instances
     * @return
     * @throws Exception 
     */
    protected Instances loadInstancesFromFile(String fn_instances) throws Exception {
        return new Instances(new BufferedReader(new FileReader(fn_instances)));
    }
    /**
     * Get all feedback of a session from a user at a variable.
     * 
     * @param sessionID
     * @param userID
     * @param varID
     * @param feedbackType
     * @return
     * @throws Exception 
     */
    public List<String[]> getFeedbackAtSession(String sessionID, String userID,
            String varID, FeedbackType feedbackType) throws Exception {
        ArrayList<String[]> feedbackList = new ArrayList<>();
        if (! sessionID.equals("")) {
            // the initial data set does not have feedback
            int sessionNum = Integer.parseInt(sessionID);
            String[][] metaTable = getFeedbackMetaTable();
            for (int i = 0; i < metaTable.length; i++) {
                if (sessionNum < Integer.parseInt(metaTable[i][1])) {
                    // we have passed the session, quit
                    break;
                }
                if (metaTable[i][1].equals(sessionID)
                        && (metaTable[i][2].equals("") || metaTable[i][2].equals(userID))
                        && TextFileFeedbackManager.getFeedbackType(metaTable[i]) == feedbackType
                        && metaTable[i][5].equals(varID)) {
                    // do we need to check valid of the correspond session info????
                    feedbackList.add(metaTable[i]);
                }
            }
        }
        return feedbackList;
    }
    
    public String getVarValueAtLineID(String lineID) throws Exception {
        String[][] metaTable = getFeedbackMetaTable();
        String varValue = "";
        for(int i = 0; i < metaTable.length; i++) {
            if(metaTable[i][0].equals(lineID) &&
                    TextFileFeedbackManager.getFeedbackType(metaTable[i]) == FeedbackType.VariableValue) {
                varValue = metaTable[i][10];
                break;
            } 
        }
                
        return varValue;
    }
    
    /**
     * Training models and creating feature weight files for the current model.
     * The caller function must set the userID value before calling this function.
     * <p>
     * This implementation only uses SVMPredictor, which can be abstracted as 
     * ALearner object for later learners
     * 
     * @throws Exception 
     */
    @Override
    public void updateModels() throws Exception {
        // prepare response to the front-end
//        predictors.clear();
//        attrNameToIndexMap.clear();
//        predictorsFeatureWeightMap.clear();
//        predictorsKeywordWeightMap.clear();
//        predictorsSortedTermWeightMap.clear();
    	
        // get the current state of each model
        String[][] currentSessionList = sessionManager.getCurrentState(userID);
        // create learning files for all current states
        for(String[] currentSession : currentSessionList) {
            if(currentSession[4].equals("valid")) {
//                LibSVMPredictor model = new LibSVMPredictor();
            	LibLinearPredictor model = new LibLinearPredictor();
                updateModels(currentSession[0], currentSession[1], 
                        currentSession[2], model);
            }
        }
    }
    
    /**
     * Train a model and create the feature weight.
     * This implementation will take each highlight span feedback as a "normal" 
     * feedback. Hence, we will merge the instanceDS and the feedbackDS into one 
     * training set for the new model.
     * 
     * @param sessionID
     * @param userID
     * @param varID
     * @throws Exception 
     */
    public void updateModels(String sessionID, String userID, String varID,
    		ALearner model) throws Exception {
        // if the model exists, do nothing
        String fn_model = getModelFileName(sessionID, userID, varID);
//        HashMap<String, Double> predictorFeatureWeightMap;

        if(! Util.fileExists(fn_model)) {
        	// get the training file
        	String fn_featureVectorOut = Storage_Controller.getLearningFeatureFn(
        			sessionID, userID, varID);
        	String fn_instanceWeight = Storage_Controller.getLearningWeightFn(sessionID,
        			userID, varID);
        	String[] svmTrainParams = new String[] {Storage_Controller.getLibSVMPath(),
                    fn_featureVectorOut, fn_model, fn_instanceWeight};
            // train the model
            model.train((Object)svmTrainParams);
            
            // save model
            model.saveModel(fn_model);
            
            // save feature weight + keyword weight
            String fn_featureWeight = getFeatureWeightFileName(sessionID, userID, varID);
            LibLinearPredictor svm = (LibLinearPredictor) model;
            svm.saveFeatureWeight(fn_model, fn_globalFeatureName, fn_featureWeight, includeBiasFeature);
        }
    }
    
    public String getModelFileName(String sessionID, String userID,
            String varID) throws Exception {
        return getModelFileName(modelFolder, varID, sessionID, userID);
    }
    
    public String getFeatureWeightFileName(String sessionID, String userID,
            String varID) throws Exception {
        return getFeatureWeightFileName(featureWeightFolder, varID, sessionID, userID);
    }
    
    protected String getInstanceDSFileName(String sessionID, String userID,
            String varID) throws Exception {
        return getInstanceTrainingFileName(learningFolder, varID, sessionID, userID);
    }
    
    protected String getFeedbackDSFileName(String sessionID, String userID,
            String varID) throws Exception {
        return getFeedbackTrainingFileName(learningFolder, varID, sessionID, userID);
    }
    
//    /**
//     * Get the UpdatedModelList object.
//     * This approach can work for the current userID and sessionID. Hence, no need 
//     * to pass these parameters here
//     * @return 
//     */
//    public UpdatedModelList getUpdatedModelList() {
//        return new UpdatedModelList(attrNameToIndexMap, predictors, 
//                predictorsFeatureWeightMap, predictorsKeywordWeightMap, 
//                predictorsSortedTermWeightMap, modelListName);
//    }
    
    public static String getModelFileName(String modelFolder, String varID,
            String sessionID, String userID) throws Exception {
        if(sessionID.equals("0")) {
            userID = "";
        }
        String fn_model = sessionID + "." + userID + "." + varID + "." + "model";
        return Util.getOSPath(new String[] {modelFolder, fn_model});
    }
    
    public static String getFeatureWeightFileName(String modelFolder, String varID,
            String sessionID, String userID) throws Exception {
        if(sessionID.equals("0")) {
            userID = "";
        }
        String fn_featureWeight = sessionID + "." + userID + "." + varID + "." + 
                "weight" + "." + "csv";
        return Util.getOSPath(new String[] {modelFolder, fn_featureWeight});
    }
    
    public static String getKeywordFeatureWeightFileName(String modelFolder, String varID,
            String sessionID, String userID) throws Exception {
        if(sessionID.equals("0")) {
            userID = "";
        }
        String fn_featureWeight = sessionID + "." + userID + "." + varID + "." + 
                "weight" + "." + "keyword" + "." + "csv";
        return Util.getOSPath(new String[] {modelFolder, fn_featureWeight});
    }
    
    public static String getInstanceTrainingFileName(String trainingFolder,
            String varID, String sessionID, String userID) throws Exception {
        if(sessionID.equals("0")) {
            userID = "";
        }
        String fn_instanceSet = sessionID + "." + userID + "." + varID + "." +
                "instanceSet" + "." + "arff";
        return Util.getOSPath(new String[] {trainingFolder, fn_instanceSet});
    }
    
    public static String getFeedbackTrainingFileName(String trainingFolder,
            String varID, String sessionID, String userID) throws Exception {
        if(sessionID.equals("0")) {
            userID = "";
        }
        String fn_feedbackSet = sessionID + "." + userID + "." + varID + "." +
                "feedbackSet" + "." + "arff";
        return Util.getOSPath(new String[] {trainingFolder, fn_feedbackSet});
    }
    
    public static String getXMLPredictorFileName(String xmlFolder, String sessionID,
            String userID) throws Exception {
        if(sessionID.equals("0")) {
            userID = "";
        }
        String fn_xmlPredictor = "modelList." + sessionID + "." + userID + "." + "xml";
        return Util.getOSPath(new String[] {xmlFolder, fn_xmlPredictor});
    }
    
    public static String getXMLDocListFileName(String xmlFolder, String sessionID,
            String userID) throws Exception {
        if(sessionID.equals("0")) {
            userID = "";
        }
        String fn_xmlPredictor = "docList.emr-vis-nlp." + sessionID + "." + userID + "." + "xml";
        return Util.getOSPath(new String[] {xmlFolder, fn_xmlPredictor});
    }
    
    public String createXMLPredictorFile() throws Exception {
        return createXMLPredictorFile(userID, xmlPredictorFolder);
    }
    
    public String createXMLPredictorFile(String userID,
            String xmlPredictorFolder) throws Exception {
        String[][] currentSessionList = sessionManager.getCurrentState(userID);
        String[] sessionIDList = new String[currentSessionList.length];
        String[] userIDList = new String[currentSessionList.length];
        String[] varIDList = new String[currentSessionList.length];
        int maxSessionID = -1;
        for(int i = 0; i < currentSessionList.length; i++) {
            sessionIDList[i] = currentSessionList[i][0];
            userIDList[i] = currentSessionList[i][1];
            varIDList[i] = currentSessionList[i][2];
            if(Integer.parseInt(currentSessionList[i][0]) > maxSessionID) {
                maxSessionID = Integer.parseInt(currentSessionList[i][0]);
            }
        }
        String sessionID = Integer.toString(maxSessionID);
        
        String fn_xmlPredictor = TextFileFeedbackManager.getXMLPredictorFileName(
                xmlPredictorFolder, sessionID, userID);
        modelListName = XMLUtil.createXMLPredictor(sessionIDList, userIDList, varIDList,
                xmlPredictorFolder, sessionID, userID, fn_xmlPredictor);
        return fn_xmlPredictor;
    }
}
