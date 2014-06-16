/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.pitt.cs.nih.backend.feedback;

/**
 * This interface helps each kind of feedback, e.g. VarValue, HighlighSpan turn 
 * into a string line to be added into the feedback file for storage.
 * <p>
 * Each feedback type which uses the text file storage must implement this interface
 * 
 * @author phuongpham
 */
public interface IFeedbackTextFileSerializer {
    /**
     * The default feedback file name in the man_anns folder
     */
    public String feedbackFileName = "feedback.txt";
    
    /**
     * Generate a feedback line based on the INLPRequestChangeEvent object.
     * 
     * @param lineID
     * @param sessionID
     * @param feedbackTable
     * @return
     * @throws Exception 
     */
    public String getFeedbackLine(String lineID, String sessionID, String userID,
            String[][] feedbackTable) throws Exception;
}
