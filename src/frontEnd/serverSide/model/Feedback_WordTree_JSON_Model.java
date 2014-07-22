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
	private List<String> m_docIDList; // list of reportID
	
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
		m_value = value;
	}
	
	public String getVariable() {
		return m_varID;
	}
	
	public void setVariable(String varID) {
		m_varID = varID;
	}
	
	public List<String> getDocList() {
		return m_docIDList;
	}
	
	public void setDocList(List<String> docIDList) {
		m_docIDList = docIDList;
	}
	
	public Feedback_Abstract_Model toFeedbackModel() throws Exception {
		Feedback_Abstract_Model feedback = null;
		if(m_type.toUpperCase().equals("DOC")) {
			Feedback_Document_Model docFeedback = new Feedback_Document_Model();
			docFeedback.setDocId(m_docIDList.get(0)); // docID is the first element
			docFeedback.setDocValue(m_value);
			docFeedback.setVariableName(m_varID);
			feedback = docFeedback;
		}
		else if(m_type.toUpperCase().equals("TEXT")) {
			// we treat a normal highlight text span as a special case of 
			// word tree highlight span, where the span is the same as the
			// selected so there would be no diff between these 2 strings
			FeedbackSpan_WordTree_Model spanFeedback = new FeedbackSpan_WordTree_Model();
			spanFeedback.setDocValue(m_value);
			spanFeedback.setVariableName(m_varID);
			spanFeedback.setSelectedTextSpan(m_selectedSpan);
			spanFeedback.setMatchedTextSpan(m_selectedSpan); // the same as selected
			spanFeedback.setReportIDList(m_docIDList);
			feedback = spanFeedback;
		}
		else if(m_type.toUpperCase().equals("WORDTREE")) {
			FeedbackSpan_WordTree_Model spanFeedback = new FeedbackSpan_WordTree_Model();
			spanFeedback.setDocValue(m_value);
			spanFeedback.setVariableName(m_varID);
			spanFeedback.setSelectedTextSpan(m_selectedSpan);
			spanFeedback.setMatchedTextSpan(m_matchedSpan); // the same as selected
			spanFeedback.setReportIDList(m_docIDList);
			feedback = spanFeedback;
		}
		
		return feedback;
	}
	
	public static List<Feedback_Abstract_Model> toFeedbackModelList(
			List<Feedback_WordTree_JSON_Model> feedbackBatch) throws Exception {
		List<Feedback_Abstract_Model> abstractFeedbackBatch = new ArrayList<>();
		
		for(Feedback_WordTree_JSON_Model feedback : feedbackBatch) {
			abstractFeedbackBatch.add(feedback.toFeedbackModel());
		}
		
		return abstractFeedbackBatch;
	}
}
