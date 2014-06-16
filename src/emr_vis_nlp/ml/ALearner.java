/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package emr_vis_nlp.ml;

/**
 * @author Phuong Pham An abstract learner provides generic methods: build a
 * model, test a model
 */
public abstract class ALearner {

    /**
     * Use the train set to build a model according to a specific learning
     * algorithm
     *
     * @param trainSet
     * @throws Exception
     */
    public abstract void train(Object trainSet)
            throws Exception;

    /**
     * Run the trained model on the test set
     *
     * @param testSet
     * @return An nInstance x nClass matrix contains probabilities of
     * memberships.
     * @throws Exception
     */
    public abstract double[][] predict(Object testSet)
            throws Exception;

    /**
     * Save the built model. This method varies among different kinds of model
     *
     * @param fn_Model The saved model file name
     */
    public void saveModel(String fn_Model) {};
	
    /**
     * Load a saved model into this object
     * @param fn_Model The model file name
     */
    public void loadModel(String fn_Model) {}; 
	
    /**
     * Return a vector of class indices. Each row is an instance and the value 
     * of each element is the class index having maximum probability. We can trace 
     * class label using this index and the class label list of the LearningInstances object.
     * 
     * @param probPrediction An nInstance x nClass matrix
     * @return
     * @throws Exception
     */
    public int[] getFinalPredictFromProb(double[][] probPrediction)
        throws Exception {
        int[] finalPrediction = new int[probPrediction.length];

        int maxIndex;
        for (int i = 0; i < finalPrediction.length; i++) {
            maxIndex = 0;
            for (int j = 1; j < probPrediction[i].length; j++) {
                if (probPrediction[i][maxIndex] < probPrediction[i][j]) {
                    maxIndex = j;
                }
            }
            finalPrediction[i] = maxIndex;
        }

        return finalPrediction;
    }

    /**
     * Create a confusion matrix for the SVMLight's prediction.
     * <p>
     * predicted as ->    -1          1
     * <p>
     *              -1
     * <p>
     *               1
     * 
     * @param targetIndexList
     * @param predictIndexList 
     * @return
     * @throws Exception 
     */
    public int[][] getConfusionMatrix(int[] targetIndexList, int[] predictIndexList)
            throws Exception {
        if(targetIndexList.length != predictIndexList.length) {
            throw new UnsupportedOperationException("Different number of instances in test file and prediction file");
        }
        
        int[][] confusionMatrix = new int[2][2];
        confusionMatrix[0][0] = 0;
        confusionMatrix[0][1] = 0;
        confusionMatrix[1][0] = 0;
        confusionMatrix[1][1] = 0;
        
        for(int i = 0; i < predictIndexList.length; i++) {
//            ++confusionMatrix[predictIndexList[i]][targetIndexList[i]];
            // Weka format
            ++confusionMatrix[targetIndexList[i]][predictIndexList[i]];
        }
        
        return confusionMatrix;
    }
    
    /**
     * Get performance measures from the confusion matrix according to interested class index (e.g. class True or False).
     * <p>
     * We work on binary classification problem here.
     * 
     * @param confusionMatrix
     * @param interestedClassIndex
     * @return A double array [precision, recall, fScore, accuracy]
     * @throws Exception 
     */
    public double[] getPerformanceMeasure(int[][] confusionMatrix, 
            int interestedClassIndex) throws Exception {        
        
        int theOtherClassIndex = interestedClassIndex == 0 ? 1 : 0;
        
//        double truePositive = confusionMatrix[interestedClassIndex][interestedClassIndex];
//        double trueNegative = confusionMatrix[theOtherClassIndex][theOtherClassIndex];
//        double falsePositive = confusionMatrix[interestedClassIndex][theOtherClassIndex];
//        double falseNegative = confusionMatrix[theOtherClassIndex][interestedClassIndex];
        // Weka format
        double truePositive = confusionMatrix[interestedClassIndex][interestedClassIndex];
        double trueNegative = confusionMatrix[theOtherClassIndex][theOtherClassIndex];
        double falsePositive = confusionMatrix[theOtherClassIndex][interestedClassIndex];
        double falseNegative = confusionMatrix[interestedClassIndex][theOtherClassIndex];
        
        double precision = truePositive / (truePositive + falsePositive);
        // deal with NaN := 0
        precision = handlingNaN(precision);
        double recall = truePositive / (truePositive + falseNegative);
        // deal with NaN := 0
        recall = handlingNaN(recall);
        double fScore = 2 * precision * recall / (precision + recall);
        // deal with NaN := 0
        fScore = handlingNaN(fScore);
        double accuracy = (trueNegative + truePositive) / 
                (trueNegative + truePositive + falseNegative + falsePositive);
        
        return new double[] {precision, recall, fScore, accuracy};
    }
    
    protected double handlingNaN(double value) {
        double convertedValue;
        if(Double.isNaN(value) || Double.isInfinite(value)) {
            convertedValue = 0.0;
        }
        else {
            convertedValue = value;
        }
        
        return convertedValue;
    }
    
    public static String getGlobalFeatureVectorFn() throws Exception {
        return frontEnd.serverSide.controller.Storage_Controller.getGlobalFeatureVectorFn();
    }
}
