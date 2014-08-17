/**
 * 
 */
package edu.pitt.cs.nih.backend.feedback;

import java.util.List;
import java.util.Map;

import edu.pitt.cs.nih.backend.utils.Util;

/**
 * override conflict, the same as its parent but we skip the verifying conflicting
 * go directly to create final feedback annotation and re-train models
 * @author Phuong Pham
 *
 */
public class TextFileFeedbackManager_LibSVM_WordTree_OverrideConflictLabel extends TextFileFeedbackManager_LibSVM_WordTree {

	/**
	 * @param feedbackFileName
	 * @param fn_sessionManager
	 * @param _learningFolder
	 * @param _docsFolder
	 * @param _modelFolder
	 * @param _featureWeightFolder
	 * @param _globalFeatureName
	 * @param _xmlPredictorFolder
	 * @param _fn_wordTreeFeedback
	 */
	public TextFileFeedbackManager_LibSVM_WordTree_OverrideConflictLabel(
			String feedbackFileName, String fn_sessionManager,
			String _learningFolder, String _docsFolder, String _modelFolder,
			String _featureWeightFolder, String _globalFeatureName,
			String _xmlPredictorFolder, String _fn_wordTreeFeedback) {
		super(feedbackFileName, fn_sessionManager, _learningFolder, _docsFolder,
				_modelFolder, _featureWeightFolder, _globalFeatureName,
				_xmlPredictorFolder, _fn_wordTreeFeedback);		
	}
	
	/**
	 * Convert word tree annotation into final annotation.
	 * 
	 * @param sessionID
	 * @throws Exception
	 */
	protected void convertWordTreeAnnotation2FinalAnnotation(String sessionID) throws Exception {		
		try {
			String[][] feedbackTable = Util.loadTable(fn_wordTreeFeedback);
			// extract all feedback in this session
			// Map<varID, Map<reportID, Map<value, List<String> text spans>>
			Map<String, Map<String, Map<String, List<Map<String,String>>>>> feedbackMap = 
					extractWordTreeAnnotation2Map(sessionID, feedbackTable);
			
			// no need to verify conflict
//			// verify conflicting label values between
//			// the feedback session and existing data before create
//			// final annotation form
//			verifyConflictingLabel(feedbackMap, sessionID);
			// from the structure, convert into final annotation
			convert2FinalAnnotationFormat(feedbackMap, sessionID);
		}
		catch(Exception e) {
			// can't convert into final feedback, roll back saved wordtree feedback
			rollBackWordTreeFeedback(sessionID);
			throw e;
		}
	}

}
