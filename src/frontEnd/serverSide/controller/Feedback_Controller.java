/**
 * 
 */
package frontEnd.serverSide.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.pitt.cs.nih.backend.feedback.FeedbackErrorException;
import edu.pitt.cs.nih.backend.feedback.FeedbackWarningException;
import edu.pitt.cs.nih.backend.feedback.TextFileFeedbackManager_LibSVM_WordTree;
import frontEnd.serverSide.model.Feedback_Abstract_Model;
import frontEnd.serverSide.model.Feedback_WordTree_JSON_Model;
import frontEnd.serverSide.model.MLModel;

/**
 * @author Phuong Pham
 * 
 */
public class Feedback_Controller {
	public Map<String,Object> getFeedback(
			List<Feedback_WordTree_JSON_Model> feedbackBatch,
			String fn_modelFnList, String fn_reportIDList, String uid) throws Exception {
		// parse the modelFnList to get userID, (previous) sessionID
		// only use userID at this moment, NO, what if we start from initial
		// set?
		// what we need now is userID
//		Map<String, String> modelListArgMap = parseModelListFn(fn_modelFnList);

		String userID;
//		userID = modelListArgMap.get("userID");
		userID = uid; // current setting
		
		// auto add feedbackID
		Feedback_WordTree_JSON_Model.autoSetFeedbackID(feedbackBatch);

		Map<String, Object> feedbackResult = new HashMap<>();
		String returnMsg = "";
		try {
			 returnMsg = processFeedback(feedbackBatch,
					userID);
		} catch(FeedbackErrorException e) {
			e.injectFeedbackError(feedbackBatch);
			feedbackResult.put("errorList", e.getErrorMsgComponentList());
			feedbackResult.put("feedbackList", feedbackBatch);
			// may not need this
			feedbackResult.put("status", "Error");
			
			// debug
			for(Feedback_WordTree_JSON_Model feedback : feedbackBatch) {
				System.out.print(feedback.getFeedbackID() + ":" + feedback.getStatus() + ":");
				if(feedback.getConflictList() != null) {
					for(String conflictID : feedback.getConflictList()) {
						System.out.print(conflictID + ",");
					}
				}
				System.out.println();
				for(Map<String,String> errorMsg : e.getErrorMsgComponentList()) {
					System.out.print("\t");
					for(String key : errorMsg.keySet()) {
						System.out.print(key + ":" + errorMsg.get(key) + ",");
					}
					System.out.println();
				}
			}
		} catch(FeedbackWarningException e) {
			e.injectFeedbackError(feedbackBatch);
			feedbackResult.put("warningList", e.getErrorMsgComponentList());
			feedbackResult.put("feedbackList", feedbackBatch);
			// may not need this
			feedbackResult.put("status", "Error");
			
			// debug
			for(Feedback_WordTree_JSON_Model feedback : feedbackBatch) {
				System.out.print(feedback.getFeedbackID() + ":" + feedback.getStatus() + ":");
				if(feedback.getConflictList() != null) {
					for(String conflictID : feedback.getConflictList()) {
						System.out.print(conflictID + ",");
					}
				}
				System.out.println();
				for(Map<String,String> errorMsg : e.getErrorMsgComponentList()) {
					System.out.print("\t");
					for(String key : errorMsg.keySet()) {
						System.out.print(key + ":" + errorMsg.get(key) + ",");
					}
					System.out.println();
				}
			}
		}
		
		
		// at this point, it must be an OK msg
		// empty errorList and warningList
		if(!feedbackResult.containsKey("errorList") 
				&& !feedbackResult.containsKey("warningList")) {
			feedbackResult.put("latestModel", returnMsg);
			// msg
			feedbackResult.put("msg", "OK");
			// duplicate here
			feedbackResult.put("status", "OK");
			// modelList
//			List<MLModel> modelList = Dataset_MLModel_Controller.instance.getMLModelList();
			List<MLModel> modelList = new Dataset_MLModel_Controller().getMLModelList();
			feedbackResult.put("modelList", modelList);
			// gradVar object
			int topKwords = 5;
			boolean biasFeature = true;
			String[] modelListInfo = Storage_Controller.parseModelListFn(returnMsg); 
			Map<String, Object> gridVarObj = 
//					GridVar_Controller.instance.getPrediction(fn_reportIDList,
//							returnMsg + ".xml", topKwords, biasFeature);
					new GridVar_Controller().getPredictionAfterFeedback(fn_reportIDList,
							returnMsg + ".xml", topKwords, biasFeature, modelListInfo[0], modelListInfo[1]);
			feedbackResult.put("gridVarData", gridVarObj);
			feedbackResult.put("feedbackList", feedbackBatch);
		}
		
		// debug
		for(String key : feedbackResult.keySet()) {
			System.out.println(key);
		}
		
//		if(returnMsg.startsWith("Error:")) {// contradictory error
//			// set status and remove "Error:" from msg
//			feedbackResult.put("msg", returnMsg.replaceAll("Error:", "").trim()); 
//			feedbackResult.put("status", "Error");
//		}
//		else if (returnMsg.startsWith("Warning:")) {// override inferred document label value
//			// create a list of warnings and remove "Error:" from msg
//			String[] warningList = returnMsg.replaceAll("Warning:", "").trim().split("\n");
//			feedbackResult.put("msg", Arrays.asList(warningList)); 
//			feedbackResult.put("status", "Warning");
//		}		
//		else {// if success, load info of the new model
//			feedbackResult.put("latestModel", returnMsg);
//			// msg
//			feedbackResult.put("msg", "OK");
//			// duplicate here
//			feedbackResult.put("status", "OK");
//			// modelList
////			List<MLModel> modelList = Dataset_MLModel_Controller.instance.getMLModelList();
//			List<MLModel> modelList = new Dataset_MLModel_Controller().getMLModelList();
//			feedbackResult.put("modelList", modelList);
//			// gradVar object
//			int topKwords = 5;
//			boolean biasFeature = true;
//			String[] modelListInfo = Storage_Controller.parseModelListFn(returnMsg); 
//			Map<String, Object> gridVarObj = 
////					GridVar_Controller.instance.getPrediction(fn_reportIDList,
////							returnMsg + ".xml", topKwords, biasFeature);
//					new GridVar_Controller().getPredictionAfterFeedback(fn_reportIDList,
//							returnMsg + ".xml", topKwords, biasFeature, modelListInfo[0], modelListInfo[1]);
//			feedbackResult.put("gridVarData", gridVarObj);
//		}

		return feedbackResult;
	}

	/**
	 * Parse the model list filename into <lu> <li>sessionID <li>userID </lu>
	 * 
	 * @param modelListFn
	 * @return
	 * @throws Exception
	 */
	protected Map<String, String> parseModelListFn(String modelListFn)
			throws Exception {
		Map<String, String> argMap = new HashMap<>();
		modelListFn = modelListFn.substring(
				modelListFn.lastIndexOf("modelList."),
				modelListFn.lastIndexOf("."));
		String[] argList = modelListFn.split(".");
		argMap.put("sessionID", argList[0]);
		argMap.put("userID", argList[1]);
		return argMap;
	}

	protected String processFeedback(
			List<Feedback_WordTree_JSON_Model> feedbackBatch, String userID)
					throws Exception {
		String feedbackFileName = Storage_Controller.getFeedbackFn();
		String fn_sessionManager = Storage_Controller.getSessionManagerFn();
		String _learningFolder = Storage_Controller.getTrainingFileFolder();
		String _docsFolder = Storage_Controller.getDocsFolder();
		String _modelFolder = Storage_Controller.getModelFolder();
		String _featureWeightFolder = Storage_Controller.getWeightFolder();
		String _globalFeatureName = Storage_Controller
				.getGlobalFeatureVectorFn();
		String _xmlPredictorFolder = Storage_Controller.getModelListFolder();
		String _fn_wordTreeFeedback = Storage_Controller
				.getWordTreeFeedbackFn();

		TextFileFeedbackManager_LibSVM_WordTree manager = getFeedbackManager(
				feedbackFileName, fn_sessionManager, _learningFolder, _docsFolder,
				_modelFolder, _featureWeightFolder, _globalFeatureName,
				_xmlPredictorFolder, _fn_wordTreeFeedback);
		manager.setUserID(userID);
		// got from the front-end
		// intermediate step, convert Feedback_WordTree_JSON_Model into
		// Feedback_Abstract_Model, delete the "." at the end of text, if any
		// because "." is a dummy character we add to show in the wordtree https://github.com/trivedigaurav/emr-wordtree/issues/1
		List<Feedback_Abstract_Model> feedbackBatchBackEnd = Feedback_WordTree_JSON_Model
				.toFeedbackModelList(feedbackBatch);
		
		String feedbackMsg = manager.processFeedback(feedbackBatchBackEnd);

		return feedbackMsg;
	}
	
	protected TextFileFeedbackManager_LibSVM_WordTree getFeedbackManager(
			String feedbackFileName, String fn_sessionManager, 
			String _learningFolder, String _docsFolder, String _modelFolder,
			String _featureWeightFolder, String	_globalFeatureName,
			String _xmlPredictorFolder, String _fn_wordTreeFeedback) {
		
		TextFileFeedbackManager_LibSVM_WordTree manager = new TextFileFeedbackManager_LibSVM_WordTree(
				feedbackFileName, fn_sessionManager, _learningFolder,
				_docsFolder, _modelFolder, _featureWeightFolder,
				_globalFeatureName, _xmlPredictorFolder, _fn_wordTreeFeedback);
		
		return manager;
	}
}
