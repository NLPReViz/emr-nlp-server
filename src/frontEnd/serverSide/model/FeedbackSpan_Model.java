/**
 * 
 */
package frontEnd.serverSide.model;

import javax.xml.bind.annotation.XmlRootElement;

import edu.pitt.cs.nih.backend.feedback.IFeedbackTextFileSerializer;

/**
 * @author Phuong Pham
 *
 */
@XmlRootElement
public class FeedbackSpan_Model extends Feedback_Document_Model implements IFeedbackTextFileSerializer {
	public FeedbackSpan_Model() {};
	
	protected TextSpan_Model m_textSpan;
	
	public TextSpan_Model getSpan() {
		return m_textSpan;
	}
	
	public void setSpan(TextSpan_Model span) {
		m_textSpan = span;
	}

	/**
     * Generate a feedback line to be written in the feedback.txt.
     * Each line has the following format: lineID, sessionID, userID, requestID, docID, varID, 
     * spanStart, spanEnd, add/remove, new value (length=10).
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

        // spanStart, spanEnd
        feedbackLine.append(m_textSpan.getStart()).append(",");
        feedbackLine.append(m_textSpan.getEnd()).append(",");
        
        // add/remove, if supported
        String _highlightType = "add";
//        String _highlightType = highlightType.toString();
        feedbackLine.append(_highlightType).append(",");
        
//      // old version, span feedback doesn't have its own class value but get from variable value
//      // find the corresponding variable value, i.e. a variable value feedback line 
//      // has the same session ID, varID              
//      String varValueLineID = "-1";
//      for(int i = feedbackTable.length - 1; i >= 0; i--) {
//          String[] feedbackRow = feedbackTable[i];
//          // if userID = "" then we use the latest variable value
//          if(TextFileFeedbackManager.getFeedbackType(feedbackRow) == TextFileFeedbackManager.FeedbackType.VariableValue &&
//                  (feedbackRow[2].equals("") || userID.equals(feedbackRow[2])) && 
//                  feedbackRow[4].equals(m_docId) && 
//                  feedbackRow[5].equals(m_classifierId)) {
//              varValueLineID = feedbackRow[0];
//              break;
//          }
//      }
//      if (varValueLineID.equals("-1")) {
//          throw new UnsupportedOperationException("There is no variable value for this highlight span feedback in session " + sessionID);
//      }
//      // get the class value at varValueLineID (haven't implemented)
      
      // get the class value of this span feedback
      feedbackLine.append(m_docValue);
        
        return feedbackLine.toString();
	}
}
