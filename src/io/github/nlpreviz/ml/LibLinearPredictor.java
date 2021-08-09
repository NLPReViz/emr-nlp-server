/**
 * 
 */
package io.github.nlpreviz.ml;

import io.github.nlpreviz.nlp.utils.FeatureWeight;
import io.github.nlpreviz.nlp.utils.Mergesort;
import io.github.nlpreviz.nlp.utils.RunCmdLine;
import io.github.nlpreviz.nlp.utils.Util;

import java.util.Arrays;
import java.util.HashMap;

/**
 * @author Phuong Pham
 *
 */
public class LibLinearPredictor extends ALearner {
	String cmdLine;
    String fn_trainSet;
    String fn_testSet;
    String fn_model;
    String fn_weight;
    String fn_prediction;
    int cmdLineVerbolity = 0;
    boolean isWindowsOS;

	public LibLinearPredictor() {
        isWindowsOS = false;
        try {
            if (Util.getOSName().toLowerCase().contains("windows")) {
                isWindowsOS = true;
            }
        } catch (Exception e) {
        }
    }

    /*
     * trainSet is a string array where libSVMPath, fn_trainSet, fn_model,
     * fn_weight
     */
    @Override
    public void train(Object trainSet) throws Exception {
        String[] paramList = (String[]) trainSet;
        String cmdArgs = initialTrainingParameter(paramList[0], paramList[1],
                paramList[2], paramList[3]);
        // System.out.println(cmdArgs);
        RunCmdLine.runCommand(cmdArgs, cmdLineVerbolity);
    }

    /*
     * trainSet is a string array where libSVMPath, fn_testSet, fn_model,
     * fn_prediction
     */
    @Override
    public double[][] predict(Object testSet) throws Exception {
        String[] paramList = (String[]) testSet;
        String cmdArgs = initialTestingParamter(paramList[0], paramList[1],
                paramList[2], paramList[3]);
        RunCmdLine.runCommand(cmdArgs, cmdLineVerbolity);
        // System.out.println(cmdArgs);
        // create the prediciton matrix
        return convertLibSVMPredictionIntoPredictionMatrix(fn_prediction);
    }

    public String initialTrainingParameter(String svmLightPath,
            String _fn_trainSet, String _fn_model, String _fn_weight)
            throws Exception {
        if (Util.fileExists(_fn_trainSet)) {
            fn_trainSet = _fn_trainSet;
        } else {
            throw new UnsupportedOperationException("Training set file "
                    + _fn_trainSet + " doesn't exist.");
        }

        if (Util.fileExists(_fn_weight)) {
            fn_weight = _fn_weight;
        } else {
            throw new UnsupportedOperationException("Weight file " + _fn_weight
                    + " doesn't exist.");
        }

        fn_model = _fn_model;

        if (svmLightPath.equals("")) {
            cmdLine = "train";
        } else {
            // cmdLine = "\"" + Util.getOSPath(new String[] {svmLightPath,
            // "svm-train"}) + "\"";
            cmdLine = Util
                    .getOSPath(new String[]{svmLightPath, "train"});
        }

        // SVMLight control param
//        cmdLine += " -q"; // no output
        cmdLine += " -s 0"; // linear kernel

        if (isWindowsOS) { // windows
            cmdLine += " -W \"" + fn_weight + "\""; // instance weight file
            cmdLine += " \"" + fn_trainSet + "\""; // training file
            cmdLine += " \"" + fn_model + "\""; // output model file
        } else { // linux
            cmdLine += " -W " + fn_weight; // instance weight file
            cmdLine += " " + fn_trainSet; // training file
            cmdLine += " " + fn_model; // output model file
        }

        return cmdLine;
    }

    public String initialTestingParamter(String svmLightPath,
            String _fn_testSet, String _fn_model, String _fn_prediction)
            throws Exception {

        if (Util.fileExists(_fn_testSet)) {
            fn_testSet = _fn_testSet;
        } else {
            throw new UnsupportedOperationException("Testing set file "
                    + _fn_testSet + " doesn't exist.");
        }

        if (Util.fileExists(_fn_model)) {
            fn_model = _fn_model;
        } else {
            throw new UnsupportedOperationException("Model file " + _fn_model
                    + " doesn't exist.");
        }

        fn_prediction = _fn_prediction;

        if (svmLightPath.equals("")) {
            cmdLine = "predict";
        } else {
            // cmdLine = "\"" + Util.getOSPath(new String[] {svmLightPath,
            // "svm-predict"}) + "\"";
            cmdLine = Util
                    .getOSPath(new String[]{svmLightPath, "predict"});
        }

        // SVMLight control param
        cmdLine += " -b 1"; // using probability estimation
        cmdLine += " -q"; // quiet mode

        if (isWindowsOS) { // windows
            cmdLine += " \"" + fn_testSet + "\""; // training file
            cmdLine += " \"" + fn_model + "\""; // model file
            cmdLine += " \"" + fn_prediction + "\""; // output prediction file
        } else { // linux
            cmdLine += " " + fn_testSet; // training file
            cmdLine += " " + fn_model; // model file
            cmdLine += " " + fn_prediction; // output prediction file
        }

        return cmdLine;
    }

    /**
     * Convert the prediction file (in LibSVM prediction file format) into the
     * prediction matrix (nInstance x nClass).
     * <p>
     * We work on binary classification, predictionMatrix[0] <=> "-1",
     * predictionMatrix[1] <=> "1"
     *
     * @param fn_prediction
     * @return
     * @throws Exception
     */
    protected double[][] convertLibSVMPredictionIntoPredictionMatrix(
            String fn_prediction) throws Exception {
        String[][] predictionTable = Util.loadTable(fn_prediction, " ");
        // binary classification
        // predictionMatrix[0] <=> "-1", predictionMatrix[1] <=> "1"
        double[][] predictionMatrix;

        if (predictionTable[0][0].equals("labels")) {// using probability output

            predictionMatrix = new double[predictionTable.length - 1][2]; // skip
            // the
            // header
            // row

            int posInd, negInd;
            if (predictionTable[0][1].contains("-")) { // predictionTable -1 1
                posInd = 2;
                negInd = 1;
            } else { // predictionTable 1 -1
                posInd = 1;
                negInd = 2;
            }

            if (predictionTable[0].length < 3) { // train model with 1 class
                // only
                if (posInd == 1) { // train with positive class only
                    // predict 100% on positive class (which is impossible)
                    for (int i = 0; i < predictionMatrix.length; i++) {
                        predictionMatrix[i][0] = 0;
                        predictionMatrix[i][1] = 1.0;
                    }
                } else { // train with negative class only
                    // predict 100% on negative class (which is
                    // impossible)
                    for (int i = 0; i < predictionMatrix.length; i++) {
                        predictionMatrix[i][0] = 1.0;
                        predictionMatrix[i][1] = 0;
                    }
                }
            } else {
                for (int i = 1; i < predictionTable.length; i++) { // skip the
                    // header
                    // row
                    predictionMatrix[i - 1][0] = Double
                            .parseDouble(predictionTable[i][negInd]); // make
                    // sure
                    // it is
                    // 0 <=>
                    // -1
                    predictionMatrix[i - 1][1] = Double
                            .parseDouble(predictionTable[i][posInd]); // make
                    // sure
                    // it is
                    // 1 <=>
                    // 1
                }
            }
        } else { // using deterministic output
            predictionMatrix = new double[predictionTable.length][2];
            double instancePrediction;
            for (int i = 0; i < predictionTable.length; i++) {
                instancePrediction = Double.parseDouble(predictionTable[i][0]);
                if (instancePrediction < 0) {
                    predictionMatrix[i][0] = 1.0;
                    predictionMatrix[i][1] = 0.0;
                } else {
                    predictionMatrix[i][0] = 0.0;
                    predictionMatrix[i][1] = 1.0;
                }
            }
        }
	    
	int rows = predictionMatrix.length;
    int columns = predictionMatrix[0].length;
    try {
      FileWriter writer = new FileWriter("/ext_data/trial.csv");
      for(int i = 0; i < rows; i++)
       {
          int j;
          for (j=0; j<columns-1; j++)
           {
               writer.append(Double.toString(predictionMatrix[i][j]));
               writer.append(',');
           }
             writer.append(Double.toString(predictionMatrix[i][j]));
             writer.append('\n');
             writer.flush();
       }
       writer.close();

    }
    catch(IOException e) {
      e.printStackTrace();
    }

        return predictionMatrix;
    }

    /**
     * Create a confusion matrix for the SVMLight's prediction.
     * <p>
     * predicted as -> -1 1
     * <p>
     * -1
     * <p> 1
     *
     * @param targetIndexList The test set file (has SVMLight file format)
     * @param predictionMatrix The prediction matrix
     * @return
     * @throws Exception
     */
    public int[][] getConfusionMatrix(String fn_test,
            double[][] predictionMatrix) throws Exception {
        String[] targetStrList = Util.loadList(fn_test);
        int[] targetIndexList = new int[targetStrList.length];
        for (int i = 0; i < targetIndexList.length; i++) {
            if (targetStrList[i].substring(0, 2).equals("-1")) {
                targetIndexList[i] = 0;
            } else {
                targetIndexList[i] = 1;
            }
        }

        int[] predictionIndexList = getFinalPredictFromProb(predictionMatrix);
        return getConfusionMatrix(targetIndexList, predictionIndexList);
    }

    /**
     * Get feature weights from the model saved in
     * <code>fn_model</code>. The feature weight order is the feature order.
     * <p>
     * This function is converted from the Python code of Ori Cohen for
     * SVMLight.
     *
     * @param fn_model
     * @return
     * @throws Exception
     */
    public FeatureWeight[] getFeatureWeights(String fn_model, boolean sorted)
            throws Exception {
        String[] modelLineList = Util.loadList(fn_model);
        int iLine = 0;
        int weightOrientation = 1;
        while(!modelLineList[iLine].trim().matches("\\-{0,1}\\d+(\\.\\d+){0,1}")) {
            if(modelLineList[iLine].contains("-1 1")) { // inverse the weight value
                weightOrientation = -1;
            }
            ++iLine;
        }
        
        // begin input feature weight
        FeatureWeight[] featureWeight = new 
                FeatureWeight[modelLineList.length - iLine];
        for(int i = 0; i < featureWeight.length; i++) {
            featureWeight[i] = new FeatureWeight(i + 1,  // feature index starts from 1, compatible with libsvm
                    Double.parseDouble(modelLineList[i + iLine]) * weightOrientation);
        }

        if (sorted) { // sort descending
            Mergesort.mergesort(featureWeight, 0, featureWeight.length,
                    false);
        }

        return featureWeight;
    }

    /**
     * Save feature weight file in the following format featureName,
     * featureWeight in each row.
     *
     * @param fn_model
     * @param fn_globalFeatureVector
     * @param fn_save
     * @throws Exception
     */
    public void saveFeatureWeight(String fn_model,
            String fn_globalFeatureVector, String fn_save, boolean biasFeature)
            throws Exception {
        FeatureWeight[] featureWeight = getFeatureWeights(fn_model, true);
        String[] globalFeatureVector = Util.loadList(fn_globalFeatureVector);
        saveFeatureWeight(featureWeight, globalFeatureVector, fn_save,
                biasFeature);
    }

    public void saveFeatureWeight(FeatureWeight[] featureWeight,
            String[] globalFeatureVector, String fn_save, boolean biasFeature)
            throws Exception {
        boolean[] zeroWeightIndexList = new boolean[globalFeatureVector.length];
        Arrays.fill(zeroWeightIndexList, true);

        StringBuilder sb = new StringBuilder();
        // save all non-zero feature weight first
        int index;
        // svmLight index starts from 1
        // and if we use biasFeature, we need offset 1 more position
        int offset = biasFeature ? 2 : 1;
        for (int i = 0; i < featureWeight.length; i++) {
            index = featureWeight[i].index - offset;
            // if(index < 0) // skip the biasFeature
            // continue;
            if (index == -1) { // extract bias feature using in -b 0
                sb.append("[biasFeature],"
                        + Double.toString(featureWeight[i].weight) + "\n");
                continue;
            }

            sb.append(globalFeatureVector[index] + ","
                    + Double.toString(featureWeight[i].weight) + "\n");
            // mark as non-zero feature index
            zeroWeightIndexList[index] = false;
        }

        // save all zero feature weight
        for (int i = 0; i < zeroWeightIndexList.length; i++) {
            if (zeroWeightIndexList[i]) {
                sb.append(globalFeatureVector[i]).append(",0\n");
            }
        }

        // save the feature weight file
        Util.saveTextFile(fn_save, sb.toString());
    }

    /**
     * Get misclassified instances. The output has following format
     * <p>
     * recordID; predicted value
     * <p>
     * Merge into a StringBuilder object
     *
     * @param fn_prediction
     * @param fn_testFeatureVector
     * @param fn_testIndex
     * @param errorStr
     * @throws Exception
     */
    public void getMisclassifiedInstanceList(String fn_prediction,
            String fn_testFeatureVector, String fn_testIndex,
            StringBuilder errorStr) throws Exception {
        String[] testInstanceList = Util.loadList(fn_testFeatureVector);
        String[][] testInstanceIDTable = Util.loadTable(fn_testIndex);
        String[][] predictionStrList = Util.loadTable(fn_prediction);
        double predictValue;

        int predictionFileOffset = predictionStrList[0][0].equals("labels") ? 1
                : 0;
        for (int i = 0; i < testInstanceList.length; i++) {
            predictValue = Double.parseDouble(predictionStrList[i
                    + predictionFileOffset][0]);
            if (Double.parseDouble(testInstanceList[i].substring(0, 2))
                    * predictValue < 0) {
                errorStr.append(testInstanceIDTable[i][0]).append(",")
                        .append(predictValue).append("\n");
            }
        }
        // errorStr.append(",-------------\n");
    }
}
