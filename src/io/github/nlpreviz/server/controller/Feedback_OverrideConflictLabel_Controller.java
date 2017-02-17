/**
 * 
 */
package io.github.nlpreviz.server.controller;

import io.github.nlpreviz.nlp.feedback.TextFileFeedbackManager_LibSVM_WordTree;
import io.github.nlpreviz.nlp.feedback.TextFileFeedbackManager_LibSVM_WordTree_OverrideConflictLabel;

/**
 * @author Phuong Pham
 *
 */
public class Feedback_OverrideConflictLabel_Controller extends  Feedback_Controller {
	
	// call override manager
	protected TextFileFeedbackManager_LibSVM_WordTree getFeedbackManager(
			String feedbackFileName, String fn_sessionManager, 
			String _learningFolder, String _docsFolder, String _modelFolder,
			String _featureWeightFolder, String	_globalFeatureName,
			String _xmlPredictorFolder, String _fn_wordTreeFeedback) {
		
		TextFileFeedbackManager_LibSVM_WordTree manager = new TextFileFeedbackManager_LibSVM_WordTree_OverrideConflictLabel(
				feedbackFileName, fn_sessionManager, _learningFolder,
				_docsFolder, _modelFolder, _featureWeightFolder,
				_globalFeatureName, _xmlPredictorFolder, _fn_wordTreeFeedback);
		
		return manager;
	}
}
