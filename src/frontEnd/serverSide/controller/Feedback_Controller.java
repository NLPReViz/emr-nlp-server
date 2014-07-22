/**
 * 
 */
package frontEnd.serverSide.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.pitt.cs.nih.backend.feedback.TextFileFeedbackManager_LibSVM_WordTree;
import frontEnd.serverSide.model.Feedback_Abstract_Model;
import frontEnd.serverSide.model.Feedback_WordTree_JSON_Model;

/**
 * @author Phuong Pham
 * 
 */
public class Feedback_Controller {
	public String getFeedback(List<Feedback_WordTree_JSON_Model> feedbackBatch,
			String fn_modelFnList) throws Exception {
		// parse the modelFnList to get userID, (previous) sessionID
		// only use userID at this moment, NO, what if we start from initial
		// set?
		// what we need now is userID
//		Map<String, String> modelListArgMap = parseModelListFn(fn_modelFnList);

		String userID;
//		userID = modelListArgMap.get("userID");
		userID = "1"; // current setting
		// how to convert json upload to a List object?
		String returnMsg = processFeedback(feedbackBatch,
				userID);

		return returnMsg;
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

		TextFileFeedbackManager_LibSVM_WordTree manager = new TextFileFeedbackManager_LibSVM_WordTree(
				feedbackFileName, fn_sessionManager, _learningFolder,
				_docsFolder, _modelFolder, _featureWeightFolder,
				_globalFeatureName, _xmlPredictorFolder, _fn_wordTreeFeedback);
		manager.setUserID(userID);
		// got from the front-end
		// intermediate step, convert Feedback_WordTree_JSON_Model into
		// Feedback_Abstract_Model
		List<Feedback_Abstract_Model> feedbackBatchBackEnd = Feedback_WordTree_JSON_Model
				.toFeedbackModelList(feedbackBatch);
		String feedbackMsg = manager.processFeedback(feedbackBatchBackEnd);

		return feedbackMsg;
	}
}
