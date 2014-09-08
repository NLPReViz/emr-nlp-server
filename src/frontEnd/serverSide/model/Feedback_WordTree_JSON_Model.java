/**
 * 
 */
package frontEnd.serverSide.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This is the data structure that the front-end will send to the back-end 
 * in JSON format.
 * This format is used since Summer 2014 with WordTree in the front-end
 * @author Phuong Pham
 *
 */
@XmlRootElement
public class Feedback_WordTree_JSON_Model {
	private String m_type; // DOC | TEXT | WORDTREE
	private String m_selectedSpan; // the skipped n-gram span
	private String m_matchedSpan; // the matched span (realization of the skipped n-gram span)
	private String m_value; // classification value (class label)
	private String m_varID; // varID
//	private List<String> m_docIDList; // list of reportID
	private Object m_docIDList;
	private String m_feedbackID;
	private String m_status; // OK | ERROR | WARNING
	private List<String> m_conflictList;
	
	public Feedback_WordTree_JSON_Model(){};
	
	public String getKind() {
		return m_type;
	}
	
	public void setKind(String type) {
		m_type = type;
	}
	
	public String getSelected() {
		return m_selectedSpan;
	}
	
	public void setSelected(String selectedSpan) {
		m_selectedSpan = selectedSpan;
	}
	
	public String getSpan() {
		return m_matchedSpan;
	}
	
	public void setSpan(String matchedSpan) {
		m_matchedSpan = matchedSpan;
	}
	
	public String getClassification() {
		return m_value;
	}
	
	public void setClassification(String value) {		
//		m_value = value;
		m_value = value.toLowerCase().equals("positive") ? "True" : "False";
	}
	
	public String getVariable() {
		return m_varID;
	}
	
	public void setVariable(String varID) {
		m_varID = varID;
	}
	
//	public List<String> getDocList() {
//		return m_docIDList;
//	}
//	
//	public void setDocList(List<String> docIDList) {
//		m_docIDList = docIDList;
//	}
	
	public Object getDocList() {
		return m_docIDList;
	}
	
	public void setDocList(Object docIDList) {
		m_docIDList = docIDList;
	}
	
	public String getFeedbackID() {
		return m_feedbackID;
	}
	
	public void setFeedbackID(String feedbackID) {
		m_feedbackID = feedbackID;
	}
	
	public String getStatus() {
		return m_status;
	}
	
	public void setStatus(String status) {
		m_status = status;
	}
	
	public List<String> getConflictList() {
		return m_conflictList;
	}
	
	public void setConflictList(List<String> conflictList) {
		m_conflictList = conflictList;
	}
	
	public Feedback_Abstract_Model toFeedbackModel() throws Exception {
		Feedback_Abstract_Model feedback = null;
		if(m_type.toUpperCase().equals("TYPE_DOC")) {
			Feedback_Document_Model docFeedback = new Feedback_Document_Model();
			String docId = (String) m_docIDList;
			docFeedback.setDocId(docId); // docID is the first element
			docFeedback.setDocValue(m_value);
			docFeedback.setVariableName(m_varID);
			docFeedback.setFeedbackID(m_feedbackID);
			feedback = docFeedback;
		}
		else if(m_type.toUpperCase().equals("TYPE_TEXT")) {
			// remove the last "." because it is a dummy character
			// we add to display on wordtree https://github.com/trivedigaurav/emr-wordtree/issues/1
			m_selectedSpan = m_selectedSpan.trim().replaceAll("\\.$", "").trim();
			// we treat a normal highlight text span as a special case of 
			// word tree highlight span, where the span is the same as the
			// selected so there would be no diff between these 2 strings
			FeedbackSpan_WordTree_Model spanFeedback = new FeedbackSpan_WordTree_Model();
			spanFeedback.setDocValue(m_value);
			spanFeedback.setVariableName(m_varID);
			spanFeedback.setSelectedTextSpan(m_selectedSpan);
			spanFeedback.setMatchedTextSpan(m_selectedSpan); // the same as selected
			ArrayList<String> docIdList = new ArrayList<>();
			docIdList.add((String) m_docIDList);
			spanFeedback.setReportIDList(docIdList);
			spanFeedback.setFeedbackID(m_feedbackID);
			feedback = spanFeedback;
		}
		else if(m_type.toUpperCase().equals("TYPE_WORDTREE")) {
			// remove the last "." because it is a dummy character
			// we add to display on wordtree https://github.com/trivedigaurav/emr-wordtree/issues/1
			m_selectedSpan = m_selectedSpan.trim().replaceAll("\\.$", "").trim();
			m_matchedSpan = m_matchedSpan.trim().replaceAll("\\.$", "").trim();
			
			FeedbackSpan_WordTree_Model spanFeedback = new FeedbackSpan_WordTree_Model();
			spanFeedback.setDocValue(m_value);
			spanFeedback.setVariableName(m_varID);
			spanFeedback.setSelectedTextSpan(m_selectedSpan);
			spanFeedback.setMatchedTextSpan(m_matchedSpan);
			List<String> docIdList = (List<String>) m_docIDList;
			spanFeedback.setReportIDList(docIdList);
			spanFeedback.setFeedbackID(m_feedbackID);
			feedback = spanFeedback;
		}
		else {
			throw new Exception(m_type.toUpperCase() + " is not a defined feedback type");
		}
		
		return feedback;
	}
	
	public static List<Feedback_Abstract_Model> toFeedbackModelList(
			List<Feedback_WordTree_JSON_Model> feedbackBatch) throws Exception {
		List<Feedback_Abstract_Model> abstractFeedbackBatch = new ArrayList<>();
		
		for(Feedback_WordTree_JSON_Model feedback : feedbackBatch) {
			// set default status OK
			feedback.setStatus("OK");
			abstractFeedbackBatch.add(feedback.toFeedbackModel());
		}
		
//		// debug
//		for(Feedback_Abstract_Model fb : abstractFeedbackBatch) {
//			System.out.println(fb.toString());
//		}
		
		return abstractFeedbackBatch;
	}
	
	/**
	 * For now, set feedbackID of a feedback = the order of the feedback in the batch
	 * 
	 * @param feedbackBatch
	 * @throws Exception
	 */
	public static void autoSetFeedbackID(List<Feedback_WordTree_JSON_Model> feedbackBatch) throws Exception {
		for(int i = 0; i < feedbackBatch.size(); i++) {
			Feedback_WordTree_JSON_Model feedback = feedbackBatch.get(i);
			feedback.setFeedbackID(Integer.toString(i));
		}
	}
}
