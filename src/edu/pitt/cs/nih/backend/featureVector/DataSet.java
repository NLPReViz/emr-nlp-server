/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.pitt.cs.nih.backend.featureVector;

/**
 * An abstract class that can handle different type of feature vectors depending 
 * on the learner tool we use
 * 
 * @author phuongpham <a href="mailto:phuongpham@cs.pitt.edu">phuongpham@cs.pitt.edu</a>
 */
public abstract class DataSet {
    public abstract Object createDataSet(Object rawData);
    public abstract void toFile(Object rawData, String fn_ds);
    public abstract Object fromFile(String fn_ds);
}
