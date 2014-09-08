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
public class FeedbackErrorException extends Exception {
	private String message = null;
	// Map<<varID,docID>, Map<<value,fbId>,List<Map<selected,matched,fbId>>>>
	private  Map<Entry<String,String>, Map<Entry<String,String>, List<Map<String,String>>>>
		errorMap;
	private List<Map<String,String>> errorMsgComponentList;
	
	public FeedbackErrorException() {
		super();
		errorMap = null;
	}
	
	public FeedbackErrorException(String message, 
			Map<Entry<String,String>, Map<Entry<String,String>, List<Map<String,String>>>> errorList) {
        super(message);
        this.message = message;
        errorMap = errorList;
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
    
    public void injectFeedbackError(List<Feedback_WordTree_JSON_Model> feedbackBatch) {
    	for(Entry<String,String> errorKey : errorMap.keySet()) {
    		String varID = errorKey.getKey();
    		String reportID = errorKey.getValue();
    		Map<Entry<String,String>, List<Map<String,String>>> errorValue = 
    				errorMap.get(errorKey);
    		
    		// divide into true list and false list and extract fbId at doc level (if any)
    		List<Map<String,String>> trueList = null;
    		List<Map<String,String>> falseList = null;
    		String trueDocFbId = null;
    		String falseDocFbId = null;
    		for(Entry<String,String> docValueKey : errorValue.keySet()) {
    			String docValue = docValueKey.getKey();    			
    			
    			if(docValue.equals("True")) {
    				trueList = errorValue.get(docValueKey);
    				if(docValueKey.getValue().length() > 0) {
        				trueDocFbId = docValueKey.getValue();
        			}
    			}
    			else {
    				falseList = errorValue.get(docValueKey);
    				if(docValueKey.getValue().length() > 0) {
        				falseDocFbId = docValueKey.getValue();
        			}
    			}
    		}
    		
    		// if there is a doc level feedback, it conflicts with all spans in the other list
    		if(trueDocFbId != null && falseDocFbId != null) { // doc level conflict
    			Feedback_WordTree_JSON_Model trueDocFeedback = feedbackBatch.get(
						Integer.parseInt(trueDocFbId));
    			Feedback_WordTree_JSON_Model falseDocFeedback = feedbackBatch.get(
						Integer.parseInt(falseDocFbId));
    			trueDocFeedback.setStatus(this.message);
    			falseDocFeedback.setStatus(this.message);
    			try {
    				trueDocFeedback.getConflictList().add(falseDocFbId);
    			} catch(NullPointerException e) {
					trueDocFeedback.setConflictList(new ArrayList<String>());
					trueDocFeedback.getConflictList().add(falseDocFbId);
				}
    			try {
    				falseDocFeedback.getConflictList().add(falseDocFbId);
    			} catch(NullPointerException e) {
					falseDocFeedback.setConflictList(new ArrayList<String>());
					falseDocFeedback.getConflictList().add(trueDocFbId);
				}
    			
    			// add error msg
    			Map<String,String> errorMsg = new HashMap<>();
    			errorMsg.put("docId", reportID);
    			errorMsg.put("variable", varID);
    			errorMsg.put("type", "errorDoc");
    			errorMsgComponentList.add(errorMsg);
    		}
    		else if(trueDocFbId != null) {
				Feedback_WordTree_JSON_Model docFeedback = feedbackBatch.get(
						Integer.parseInt(trueDocFbId));
				docFeedback.setStatus(this.message);
					for(Map<String,String> feedbackError : falseList) {
						String feedbackErrorId = feedbackError.get("fbId");
						Feedback_WordTree_JSON_Model errorFeedback = feedbackBatch.get(
								Integer.parseInt(feedbackErrorId));
						errorFeedback.setStatus(this.message);
						// add conflict list
						try {
							errorFeedback.getConflictList().add(trueDocFbId);
						} catch(NullPointerException e) {
							errorFeedback.setConflictList(new ArrayList<String>());
							errorFeedback.getConflictList().add(trueDocFbId);
						}
						try {
							docFeedback.getConflictList().add(feedbackErrorId);
						} catch(NullPointerException e) {
							docFeedback.setConflictList(new ArrayList<String>());
							docFeedback.getConflictList().add(feedbackErrorId);
						}
						// add error msg
		    			Map<String,String> errorMsg = new HashMap<>();
		    			errorMsg.put("docId", reportID);
		    			errorMsg.put("variable", varID);
		    			errorMsg.put("span1", errorFeedback.getSelected());
		    			errorMsg.put("type", "errorDocSpan");
					}
    		}
    		else if(falseDocFbId != null) { // doc level feedback conflicts with true list
				Feedback_WordTree_JSON_Model docFeedback = feedbackBatch.get(
						Integer.parseInt(trueDocFbId));
				docFeedback.setStatus(this.message);
				for(Map<String,String> feedbackError : trueList) {
					String feedbackErrorId = feedbackError.get("fbId");
					Feedback_WordTree_JSON_Model errorFeedback = feedbackBatch.get(
							Integer.parseInt(feedbackErrorId));
					errorFeedback.setStatus(this.message);
					// add conflict list
					try {
						errorFeedback.getConflictList().add(falseDocFbId);
					} catch(NullPointerException e) {
						errorFeedback.setConflictList(new ArrayList<String>());
						errorFeedback.getConflictList().add(falseDocFbId);
					}
					try {
						docFeedback.getConflictList().add(feedbackErrorId);
					} catch(NullPointerException e) {
						docFeedback.setConflictList(new ArrayList<String>());
						docFeedback.getConflictList().add(feedbackErrorId);
					}
					// add error msg
	    			Map<String,String> errorMsg = new HashMap<>();
	    			errorMsg.put("docId", reportID);
	    			errorMsg.put("variable", varID);
	    			errorMsg.put("span1", errorFeedback.getSelected());
	    			errorMsg.put("type", "errorDocSpan");
				}
			}
			
			
			// each feedback in this list will conflict with all feedback in the other list
			for(Map<String,String> feedbackError : trueList) {
				String feedbackErrorId = feedbackError.get("fbId");
				Feedback_WordTree_JSON_Model errorFeedback = feedbackBatch.get(
						Integer.parseInt(feedbackErrorId));
				// set status = ERROR
				errorFeedback.setStatus(this.message);
				// cross reference with the other list
				for(Map<String,String> conflictError : falseList) {
					String conflictErrorId = conflictError.get("fbId");
					Feedback_WordTree_JSON_Model conflictFeedback = feedbackBatch.get(
							Integer.parseInt(conflictErrorId));
					// set status = ERROR
					conflictFeedback.setStatus(this.message);
					// add conflict list
					try {
						errorFeedback.getConflictList().add(conflictErrorId);
					} catch(NullPointerException e) {
						errorFeedback.setConflictList(new ArrayList<String>());
						errorFeedback.getConflictList().add(conflictErrorId);
					}
					try {
						conflictFeedback.getConflictList().add(feedbackErrorId);
					} catch(NullPointerException e) {
						conflictFeedback.setConflictList(new ArrayList<String>());
						conflictFeedback.getConflictList().add(feedbackErrorId);
					}
					// add error msg
	    			Map<String,String> errorMsg = new HashMap<>();
	    			errorMsg.put("docId", reportID);
	    			errorMsg.put("variable", varID);
	    			errorMsg.put("span1", errorFeedback.getSelected());
	    			errorMsg.put("span2", conflictFeedback.getSelected());
	    			errorMsg.put("type", "errorSpanSpan");
				}
			}
    	}
    }
}
