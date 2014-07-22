/**
 * 
 */
package frontEnd.serverSide.model;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import edu.pitt.cs.nih.backend.feedback.IFeedbackTextFileSerializer;

/**
 * @author Phuong Pham
 *
 */
@XmlRootElement
public class FeedbackSpan_WordTree_Model extends Feedback_Abstract_Model implements IFeedbackTextFileSerializer {
	public FeedbackSpan_WordTree_Model(){};
	
	protected String m_matchedTextSpan; // real matched span
	protected String m_selectedTextSpan; // skipped n-gram span
	protected List<String> m_reportIDList;
	
	public String getMatchedTextSpan() {
		return m_matchedTextSpan;
	}
	
	public void setMatchedTextSpan(String textSpan) {
		m_matchedTextSpan = textSpan;
	}
	
	public String getSelectedTextSpan() {
		return m_selectedTextSpan;
	}
	
	public void setSelectedTextSpan(String selectedTextSpan) {
		m_selectedTextSpan = selectedTextSpan;
	}
	
	public List<String> getReportIDList() {
		return m_reportIDList;
	}
	
	public void setReportIDList(List<String> reportIDList) {
		m_reportIDList = reportIDList;
	}
	
	/**
	 * Generate a feedback line to be written in the feedback.txt. Each line has
	 * the following format: lineID, sessionID, userID, requestID, varID,
	 * new value, "text span", reportID list (length=10).
	 * 
	 * @param sessionID
	 * @return
	 * @throws Exception
	 */
	@Override
	public String getFeedbackLine(String lineID, String sessionID,
			String userID, String[][] feedbackTable) throws Exception {
		// sessionID
		StringBuilder feedbackLine = new StringBuilder(lineID);
		feedbackLine.append(",").append(sessionID).append(",");

		// //in case the INLPChangeRequestEvent supports userID
		// userID = feedback.getUserID();
		feedbackLine.append(userID).append(",");

		// requestID
		feedbackLine.append(m_requestId).append(",");

		// varID, need to check out varID to link between many components
		feedbackLine.append(m_classifierId).append(",");

		// get the class value of this span feedback
		feedbackLine.append(m_docValue).append(",");
		
		// text span - normalize
		m_matchedTextSpan = FeedbackSpan_WordTree_Model.normalizeTextSpan(m_matchedTextSpan);
		feedbackLine.append(m_matchedTextSpan).append(",");
		
		// reportID list
		for(int i = 0; i < m_reportIDList.size() - 1; i++) {
			feedbackLine.append(m_reportIDList.get(i)).append(",");
		}
		feedbackLine.append(m_reportIDList.get(m_reportIDList.size() - 1));

		return feedbackLine.toString();
	}
	
	/**
	 * Normalize text span to save as text file.
	 * Replace:
	 * <lu>
	 * <li>  , by &comma;
	 * <li> new line by \n 
	 * </lu> 
	 * 
	 * @param textSpan
	 * @return
	 * @throws Exception
	 */
	public static String normalizeTextSpan(String textSpan) throws Exception {
		return textSpan.replaceAll(",", "&comma;").replaceAll("\n", "\\\\n");
	}
	
	/**
	 * Denormalize text span from text file to real string for searching
	 * Replace:
	 * <lu>
	 * <li> &comma; by ,
	 * <li> \n by new line 
	 * </lu> 
	 * 
	 * @param textSpan
	 * @return
	 * @throws Exception
	 */
	public static String deNormalizeTextSpan(String textSpan) throws Exception {
		return textSpan.replaceAll("&comma;", ",").replaceAll("\\\\n", "\n");
	}
}
