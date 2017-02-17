/**
 * 
 */
package io.github.nlpreviz.nlp.feedback;

import io.github.nlpreviz.server.model.Feedback_WordTree_JSON_Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
    
    /**
     * Inject status and conflict reportIDs to the original feedback batch
     * 
     * @param feedbackBatch
     */
    public List<Map<String,Object>> injectFeedbackError(List<Feedback_WordTree_JSON_Model> feedbackBatch) {
    	List<Map<String,Object>> feedbackReturnStructure = new ArrayList<>();
    	for(int i = 0; i < feedbackBatch.size(); i++) {
    		Map<String,Object> feedbackReturnEntry = new HashMap<>();
    		feedbackReturnEntry.put("status", "OK");
    		feedbackReturnStructure.add(feedbackReturnEntry);
    	}
    	
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
    		// there is a bug, which I don't know why
    		// but iterate using keySet will not work here
    		for(Entry<Entry<String,String>, List<Map<String,String>>> docValueKeyValue :
    			errorValue.entrySet()) {
    			Entry<String,String> docValueKey = docValueKeyValue.getKey();
    			List<Map<String,String>> docValueValue = docValueKeyValue.getValue();
    			String docValue = docValueKey.getKey();
    			
    			if(docValue.equals("True")) {
//    				trueList = errorValue.get(docValueKey);
    				trueList = docValueValue;
    				if(docValueKey.getValue().length() > 0) {
        				trueDocFbId = docValueKey.getValue();
        			}
    			}
    			else {
//    				falseList = errorValue.get(docValueKey);
    				falseList = docValueValue;
    				if(docValueKey.getValue().length() > 0) {
        				falseDocFbId = docValueKey.getValue();
        			}
    			}
    		}
    		
    		// if there is a doc level feedback, it conflicts with all spans in the other list
    		if(trueDocFbId != null && falseDocFbId != null) { // doc level conflict
//    			Feedback_WordTree_JSON_Model trueDocFeedback = feedbackBatch.get(
//						Integer.parseInt(trueDocFbId));
//    			Feedback_WordTree_JSON_Model falseDocFeedback = feedbackBatch.get(
//						Integer.parseInt(falseDocFbId));
//    			trueDocFeedback.setStatus(this.message);
    			feedbackReturnStructure.get(Integer.parseInt(trueDocFbId))
    				.put("status", message);
//    			falseDocFeedback.setStatus(this.message);
    			feedbackReturnStructure.get(Integer.parseInt(falseDocFbId))
    				.put("status", message);
    			try {
//    				trueDocFeedback.getConflictList().add(falseDocFbId);
    				List<String> conflictList = (List<String>) feedbackReturnStructure.get(
    						Integer.parseInt(trueDocFbId)).get("conflictList");
    				conflictList.add(falseDocFbId);
    			} catch(NullPointerException e) {
//					trueDocFeedback.setConflictList(new ArrayList<String>());
//					trueDocFeedback.getConflictList().add(falseDocFbId);
    				List<String> conflictList = new ArrayList<>();
    				conflictList.add(falseDocFbId);
    				feedbackReturnStructure.get(Integer.parseInt(trueDocFbId))
    					.put("conflictList", conflictList);
				}
    			try {
//    				falseDocFeedback.getConflictList().add(falseDocFbId);
    				List<String> conflictList = (List<String>) feedbackReturnStructure.get(
    						Integer.parseInt(falseDocFbId)).get("conflictList");
        			conflictList.add(trueDocFbId);
    			} catch(NullPointerException e) {
//					falseDocFeedback.setConflictList(new ArrayList<String>());
//					falseDocFeedback.getConflictList().add(trueDocFbId);
    				List<String> conflictList = new ArrayList<>();
    				conflictList.add(trueDocFbId);
    				feedbackReturnStructure.get(Integer.parseInt(falseDocFbId))
    					.put("conflictList", conflictList);
				}
    			
    			// add error msg
    			Map<String,String> errorMsg = new HashMap<>();
    			errorMsg.put("docId", reportID);
    			errorMsg.put("variable", varID);
    			errorMsg.put("type", "errorDoc");
    			errorMsgComponentList.add(errorMsg);
    		}
			if (trueDocFbId != null && falseList != null) {
				// all span in the false list conflict with this doc level feedback
				
			// Feedback_WordTree_JSON_Model docFeedback = feedbackBatch.get(
			// Integer.parseInt(trueDocFbId));
			// docFeedback.setStatus(this.message);
				feedbackReturnStructure.get(Integer.parseInt(trueDocFbId)).put(
						"status", message);
				for (Map<String, String> feedbackError : falseList) {
					String feedbackErrorId = feedbackError.get("fbId");
					Feedback_WordTree_JSON_Model errorFeedback = feedbackBatch
							.get(Integer.parseInt(feedbackErrorId));
//					errorFeedback.setStatus(this.message);
					feedbackReturnStructure.get(Integer.parseInt(feedbackErrorId)).put(
							"status", message);
					// add conflict list
					try {
//						errorFeedback.getConflictList().add(trueDocFbId);
						List<String> conflictList = (List<String>) feedbackReturnStructure
								.get(Integer.parseInt(feedbackErrorId)).get("conflictList");
						conflictList.add(trueDocFbId);
					} catch (NullPointerException e) {
//						errorFeedback.setConflictList(new ArrayList<String>());
//						errorFeedback.getConflictList().add(trueDocFbId);
						List<String> conflictList = new ArrayList<>();
						conflictList.add(trueDocFbId);
						feedbackReturnStructure.get(Integer.parseInt(feedbackErrorId))
							.put("conflictList", conflictList);
					}
					try {
//						docFeedback.getConflictList().add(feedbackErrorId);
						List<String> conflictList = (List<String>) feedbackReturnStructure
								.get(Integer.parseInt(trueDocFbId)).get("conflictList");
						conflictList.add(feedbackErrorId);
					} catch (NullPointerException e) {
//						docFeedback.setConflictList(new ArrayList<String>());
//						docFeedback.getConflictList().add(feedbackErrorId);
						List<String> conflictList = new ArrayList<>();
						conflictList.add(feedbackErrorId);
						feedbackReturnStructure.get(Integer.parseInt(trueDocFbId))
							.put("conflictList", conflictList);
					}
					// add error msg
					Map<String, String> errorMsg = new HashMap<>();
					errorMsg.put("docId", reportID);
					errorMsg.put("variable", varID);
					errorMsg.put("span1", errorFeedback.getSelected());
					errorMsg.put("type", "errorDocSpan");
					errorMsgComponentList.add(errorMsg);
				}
			}
			if (falseDocFbId != null && trueList != null) {
				// doc level feedback conflicts with true list
//				Feedback_WordTree_JSON_Model docFeedback = feedbackBatch
//						.get(Integer.parseInt(falseDocFbId));
//				docFeedback.setStatus(this.message);
				feedbackReturnStructure.get(Integer.parseInt(falseDocFbId))
					.put("status", message);
				for (Map<String, String> feedbackError : trueList) {
					String feedbackErrorId = feedbackError.get("fbId");
					Feedback_WordTree_JSON_Model errorFeedback = feedbackBatch
							.get(Integer.parseInt(feedbackErrorId));
//					errorFeedback.setStatus(this.message);
					feedbackReturnStructure.get(Integer.parseInt(feedbackErrorId))
						.put("status", message);
					// add conflict list
					try {
//						errorFeedback.getConflictList().add(falseDocFbId);
						List<String> conflictList = (List<String>) feedbackReturnStructure
								.get(Integer.parseInt(feedbackErrorId)).get("conflictList");
						conflictList.add(falseDocFbId);
					} catch (NullPointerException e) {
//						errorFeedback.setConflictList(new ArrayList<String>());
//						errorFeedback.getConflictList().add(falseDocFbId);
						List<String> conflictList = new ArrayList<>();
						conflictList.add(falseDocFbId);
						feedbackReturnStructure.get(Integer.parseInt(feedbackErrorId))
							.put("conflictList", conflictList);
					}
					try {
//						docFeedback.getConflictList().add(feedbackErrorId);
						List<String> conflictList = (List<String>) feedbackReturnStructure
								.get(Integer.parseInt(falseDocFbId)).get("conflictList");
						conflictList.add(feedbackErrorId);
					} catch (NullPointerException e) {
//						docFeedback.setConflictList(new ArrayList<String>());
//						docFeedback.getConflictList().add(feedbackErrorId);
						List<String> conflictList = new ArrayList<>();
						conflictList.add(feedbackErrorId);
						feedbackReturnStructure.get(Integer.parseInt(falseDocFbId))
							.put("conflictList", conflictList);
					}
					// add error msg
					Map<String, String> errorMsg = new HashMap<>();
					errorMsg.put("docId", reportID);
					errorMsg.put("variable", varID);
					errorMsg.put("span1", errorFeedback.getSelected());
					errorMsg.put("type", "errorDocSpan");
					errorMsgComponentList.add(errorMsg);
				}
			}

			// each feedback in this list will conflict with all feedback in the
			// other list
			if (trueList != null && falseList != null) {
				for (Map<String, String> feedbackError : trueList) {
					String feedbackErrorId = feedbackError.get("fbId");
					Feedback_WordTree_JSON_Model errorFeedback = feedbackBatch
							.get(Integer.parseInt(feedbackErrorId));
					// set status = ERROR
//					errorFeedback.setStatus(this.message);
					feedbackReturnStructure.get(Integer.parseInt(feedbackErrorId))
						.put("status", message);
					// cross reference with the other list
					for (Map<String, String> conflictError : falseList) {
						String conflictErrorId = conflictError.get("fbId");
						Feedback_WordTree_JSON_Model conflictFeedback = feedbackBatch
								.get(Integer.parseInt(conflictErrorId));
						// set status = ERROR
//						conflictFeedback.setStatus(this.message);
						feedbackReturnStructure.get(Integer.parseInt(conflictErrorId))
							.put("status", message);
						// add conflict list
						try {
//							errorFeedback.getConflictList()
//									.add(conflictErrorId);
							List<String> conflictList = (List<String>) feedbackReturnStructure.get(
									Integer.parseInt(feedbackErrorId)).get("conflictList");
							conflictList.add(conflictErrorId);
						} catch (NullPointerException e) {
//							errorFeedback
//									.setConflictList(new ArrayList<String>());
//							errorFeedback.getConflictList()
//									.add(conflictErrorId);
							List<String> conflictList = new ArrayList<>();
							conflictList.add(conflictErrorId);
							feedbackReturnStructure.get(Integer.parseInt(feedbackErrorId))
								.put("conflictList", conflictList);
						}
						try {
//							conflictFeedback.getConflictList().add(
//									feedbackErrorId);
							List<String> conflictList = (List<String>) feedbackReturnStructure.get(
									Integer.parseInt(conflictErrorId)).get("conflictList");
							conflictList.add(feedbackErrorId);
						} catch (NullPointerException e) {
//							conflictFeedback
//									.setConflictList(new ArrayList<String>());
//							conflictFeedback.getConflictList().add(
//									feedbackErrorId);
							List<String> conflictList = new ArrayList<>();
							conflictList.add(feedbackErrorId);
							feedbackReturnStructure.get(Integer.parseInt(conflictErrorId))
								.put("conflictList", conflictList);
						}
						// add error msg
						Map<String, String> errorMsg = new HashMap<>();
						errorMsg.put("docId", reportID);
						errorMsg.put("variable", varID);
						errorMsg.put("span1", errorFeedback.getSelected());
						errorMsg.put("span2", conflictFeedback.getSelected());
						errorMsg.put("type", "errorSpanSpan");
						errorMsgComponentList.add(errorMsg);
					}
				}
			}
		}

		return feedbackReturnStructure;
    }
}
