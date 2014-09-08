/**
 * 
 */
package edu.pitt.cs.nih.backend.feedback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import frontEnd.serverSide.model.Feedback_WordTree_JSON_Model;

/**
 * @author Phuong Pham
 *
 */
public class FeedbackWarningException extends Exception {
	private String message = null;
	// <fbId,reportID>
	private List<Entry<String,String>> fbIdList;
	private List<Map<String,String>> errorMsgComponentList;
	
	public FeedbackWarningException() {
		super();
	}
	
	public FeedbackWarningException(String message, List<Entry<String,String>> feedbackIDList) {
        super(message);
        this.message = message;
        fbIdList = feedbackIDList;
        errorMsgComponentList = new ArrayList<>();
    }
	
	@Override
    public String toString() {
        return message;
    }
 
    @Override
    public String getMessage() {
        return message;
    }
    
    public List<Map<String,String>> getErrorMsgComponentList() {
    	return errorMsgComponentList;
    }
    
    /**
     * Inject status and conflict reportIDs to the original feedback batch
     * 
     * @param feedbackBatch
     */
    public void injectFeedbackError(List<Feedback_WordTree_JSON_Model> feedbackBatch) {
    	for(Entry<String,String> warningID : fbIdList) {
    		String feedbackID = warningID.getKey();
    		String reportID = warningID.getValue();
    		Feedback_WordTree_JSON_Model feedback = feedbackBatch.get(Integer.parseInt(feedbackID));
    		feedback.setStatus("WARNING");
    		Map<String,String> errorMsg = new HashMap<>();
    		errorMsg.put("docId", reportID);
    		errorMsg.put("variable", feedback.getVariable());
    		errorMsgComponentList.add(errorMsg);
    	}
    }
}
