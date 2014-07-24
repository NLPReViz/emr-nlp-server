/**
 * 
 */
package frontEnd.serverSide.model;

import javax.xml.bind.annotation.XmlRootElement;

import edu.pitt.cs.nih.backend.feedback.IFeedbackTextFileSerializer;
import edu.pitt.cs.nih.backend.feedback.TextFileFeedbackManager;

/**
 * @author Phuong Pham
 *
 */
@XmlRootElement
public class Feedback_Document_Model extends Feedback_Abstract_Model 
							implements IFeedbackTextFileSerializer {
	public Feedback_Document_Model() {};
	
	protected String m_docId;
	
	public String getDocId() {
		return m_docId;
	}
	
	public void setDocId(String docId) {
		m_docId = docId;
	}
	
	/**
     * Generate a feedback line to be written in the feedback.txt.
     * Each line has the following format: lineID, sessionID, userID, requestID, docID, varID, 
     * spanStart, spanEnd, change/create, pointer to old var value (lineID), new value (length=11)
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

//            //in case the INLPChangeRequestEvent supports userID            
//            userID = feedback.getUserID();
        feedbackLine.append(userID).append(",");

        // requestID
        feedbackLine.append(m_requestId).append(",");

        // docID            
        feedbackLine.append(m_docId).append(",");

        // varID, need to check out varID to link between many components
        feedbackLine.append(m_classifierId).append(",");

        // spanStart, spanEnd, because instance level feedback affect the whole document
        // we don't use spanStart and spanEnd for this feedback type (skipped when analyzing)
        feedbackLine.append("0,0,");
        
        // change/create, old variable value line ID(pointer)
        String varValueType = "create";
        String oldVarValueLineID = "-1";
        for(int i = feedbackTable.length - 1; i >= 0; i--) {
            String[] feedbackRow = feedbackTable[i];
            // if userID = "" then we use the latest variable value
            if((TextFileFeedbackManager.getFeedbackType(feedbackRow) == TextFileFeedbackManager.FeedbackType.VariableValue) &&
                    (feedbackRow[2].equals("") || userID.equals(feedbackRow[2])) && 
                    feedbackRow[4].equals(m_docId) && 
                    feedbackRow[5].equals(m_classifierId)) {
                oldVarValueLineID = feedbackRow[0];
                varValueType = "change";
                break;
            }
        }
        feedbackLine.append(varValueType).append(",").append(oldVarValueLineID).append(",");
        
        // new value
        feedbackLine.append(m_docValue);
        
        return feedbackLine.toString();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("TYPE_DOC\n");
		sb.append("Classification: ").append(this.m_docValue).append("\n");
		sb.append("DocID: ").append(this.m_docId).append("\n");
		sb.append("Variable: ").append(this.m_classifierId).append("\n");
		return sb.toString();
	}
}
