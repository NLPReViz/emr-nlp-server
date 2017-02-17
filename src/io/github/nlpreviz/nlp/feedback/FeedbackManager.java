/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package io.github.nlpreviz.nlp.feedback;

import io.github.nlpreviz.server.model.Feedback_Abstract_Model;

import java.util.List;

/**
 * This is an abstract class to deal with feedback on the back-end side.
 * For example, store user feedback for later use, convert feedback into learning 
 * file for building updated models, etc
 * <p>
 * The underline technique to use stored feedback is flexible. It could be text files 
 * or DBMS. This class only provides required functions that a FeedbackManager for this 
 * system should have.
 * <p>
 * There are no constraints on how feedbacks are stored and used, each implementation class 
 * must uses its internal structure in specify how to store and use the feedbacks.
 * 
 * @author phuongpham <a href="mailto:phuongpham@cs.pitt.edu">phuongpham@cs.pitt.edu</a>
 */
public abstract class FeedbackManager {
    
    /**
     * Store all feedbacks for the current batch for later use.
     * All feedbacks implement the INLPChangeRequestEvent interface. 
     * 
     * @param feedbackBatch A list of INLPChangeRequestEvent object
     * @throws Exception 
     */
    public abstract void saveFeedbackBatch(List<Feedback_Abstract_Model> feedbackBatch) throws Exception;
    
    /**
     * Create learning files for the current batch. 
     * 
     * @param sessionID
     * @throws Exception 
     */
    public abstract void createLearningFiles() throws Exception;
    
    /**
     * Update (re-train) models with additional feedbacks from the current session.
     * 
     * @throws Exception 
     */
    public abstract void updateModels() throws Exception;
    
    /**
     * When a user rejects new feedback, wants to restore the system to the previous state
     * 
     * @throws Exception 
     */
    public abstract void rollBackward() throws Exception;
    
    /**
     * When a user wants to restore the system to a later state after roll back
     * 
     * @param sessionID
     * @throws Exception 
     */
    public abstract void rollForward() throws Exception;
}
