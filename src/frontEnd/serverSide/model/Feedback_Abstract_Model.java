/**
 * 
 */
package frontEnd.serverSide.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author Phuong Pham
 *
 */
public class Feedback_Abstract_Model {
	protected String m_requestId;
	protected String m_classifierId;
	protected String m_docValue;
	protected String m_feedbackID;
	
	public String getRequestId() {
		return m_requestId;
	}
	
	public void setRequestId(String requestId) {
		m_requestId = requestId;
	}
	
	public String getVariableName() {
		return m_classifierId;
	}
	
	public void setVariableName(String classifierId) {
		m_classifierId = classifierId;
	}
	
	public String getDocValue() {
		return m_docValue;
	}
	
	public void setDocValue(String docValue) {
		m_docValue = docValue;
	}
	
	public String getFeedbackID() {
		return m_feedbackID;
	}
	
	public void setFeedbackID(String feedbackID) {
		m_feedbackID = feedbackID;
	}
	
	public static String getRequestID() throws Exception {
		return new SimpleDateFormat("yyyyMMddHHmmss").format(
                Calendar.getInstance().getTime());
	}
}
