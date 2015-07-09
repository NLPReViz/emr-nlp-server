/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.pitt.cs.nih.backend.feedback;

import edu.pitt.cs.nih.backend.featureVector.WekaDataSet;
import edu.pitt.cs.nih.backend.utils.Util;
import emr_vis_nlp.ml.ALearner;
import emr_vis_nlp.ml.SVMPredictor;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Phuong Pham <a href="mailto:phuongpham@cs.pitt.edu">phuongpham@cs.pitt.edu</a>
 */
public class VerifyWekaPrediction {
    public static void runExp() throws Exception {
        String dataFolder = Util.getOSPath(new String[] {Util.getExecutingPath(), "alaska-data"});
        String docsFolder = Util.getOSPath(new String[] {dataFolder,
            "emr-vis-nlp_colonoscopy2", "docs"});
        String labelsFolder = Util.getOSPath(new String[] {dataFolder,
            "emr-vis-nlp_colonoscopy2", "labels"});
        String fn_initialIDXML = Util.getOSPath(new String[] {dataFolder,
            "emr-vis-nlp_colonoscopy2", "initialIDList.xml"});
        String fn_globalFeatureVector = ALearner.getGlobalFeatureVectorFn();
        String trainingFolder = Util.getOSPath(new String[] {dataFolder,
            "backend_vars2", "learnings"});
        String outputFolder = Util.getOSPath(new String[]{dataFolder,
            "emr-vis-nlp_colonoscopy2", "outputs"});
        String tempModelFolder = Util.getOSPath(new String[]{dataFolder,
            "emr-vis-nlp_colonoscopy2", "outputs", "model"});
        String fn_test = Util.getOSPath(new String[]{dataFolder,
            "learningFile", "any-adenoma-test.arff"});
        
        VerifyWekaPrediction verifier = new VerifyWekaPrediction();
//        verifier.verifyTrainingFiles(labelsFolder, docsFolder, outputFolder,
//                fn_globalFeatureVector, fn_initialIDXML);
        verifier.verifyModelFiles(trainingFolder, outputFolder);
//        verifier.verifyOutputs(tempModelFolder, fn_test, outputFolder);
    }

//    public static String[] varNameList = new String[] {"any-adenoma",
//        "appendiceal-orifice", "asa", "biopsy", "cecum", "ileo-cecal-valve",
//        "indication-type", "informed-consent", "nursing-report", "prep-adequateNo",
//        "prep-adequateNot", "prep-adequateYes", "proc-aborted", "withdraw-time"};
    public static String[] varNameList = new String[] {"any-adenoma"};
    
    /**
     * The same training set creates the same training files?
     * 
     * @param labelsFolder
     * @param docsFolder
     * @param outputFolder
     * @param fn_globalFeatureVector
     * @param fn_xml
     * @throws Exception 
     */
    public void verifyTrainingFiles(String labelsFolder, String docsFolder,
            String outputFolder, String fn_globalFeatureVector, String fn_xml) throws Exception {
        String[] initialIDList = extractDocListFromXMLFile(fn_xml);
        int epoch = 1;
        for(int iTry = 0; iTry < epoch; iTry++) {
            createTrainTestFilesFromList(labelsFolder, docsFolder, initialIDList,
                    outputFolder, fn_globalFeatureVector, iTry);
        }
        
        verifyFiles(outputFolder);
    }
    
    public void verifyModelFiles(String trainingFolder, String outputFolder) throws Exception {
        int epoch = 10;
        for(int iVar = 0; iVar < varNameList.length; iVar++) {
            for(int iTry = 0; iTry < epoch; iTry++) {
                trainSVMModel(trainingFolder, varNameList[iVar], outputFolder, iTry);
            }
        }
        
        verifyFiles(outputFolder);
    }
    
    public void verifyOutputs(String modelFolder, String fn_test, String outputFolder) throws Exception {
        int epoch = 1;
        for(int iVar = 0; iVar < varNameList.length; iVar++) {
            for(int iTry = 0; iTry < epoch; iTry++) {
                predictSVMModel(modelFolder, varNameList[iVar], fn_test, outputFolder, iTry);
            }
        }
        
        verifyFiles(outputFolder);
    }
    
    protected void verifyFiles(String outputFolder) throws Exception {
        String[] fnList = Util.loadFileList(outputFolder);
        // verify these files
        String fn_1, fn_2;
        for (int iVar = 0; iVar < varNameList.length; iVar++) {
            for (int i = 0; i < fnList.length - 1; i++) {
                for (int j = i + 1; j < fnList.length; j++) {
                    fn_1 = Util.getOSPath(new String[]{outputFolder, fnList[i]});
                    fn_2 = Util.getOSPath(new String[]{outputFolder, fnList[j]});
                    if(!Util.isFileBinaryEqual(fn_1, fn_2)) {
                        System.out.println(varNameList[iVar] + ":" + fn_1 + " <> " + fn_2);
                    }
                    else {
                        System.out.println(varNameList[iVar] + ":" + fn_1 + " == " + fn_2);
                    }
                }
            }
        }
    }
    
    public void createTrainTestFilesFromList(String labelsFolder, String docsFolder,
            String[] instanceIDList, String outputFolder,
            String fn_globalFeatureVector, int iTry) throws Exception {
        String[] globalFeatureVector = Util.loadList(fn_globalFeatureVector);
        WekaDataSet wekaDS = new WekaDataSet();
        String fn_suffix, fn_prefix, varID;

        // create train file
        fn_prefix = "";
        fn_suffix = "-" + Integer.toString(iTry) + ".arff";
        wekaDS.createDataSetFromIDListWithPrefixSuffixFN(varNameList,
                instanceIDList, docsFolder, labelsFolder, globalFeatureVector,
                outputFolder, fn_prefix, fn_suffix);
    }
    
    protected String[] extractDocListFromXMLFile(String fn_xml) throws Exception {
        String begTag = Pattern.quote("<Document>");
        String endTag = Pattern.quote("</Document>");
        ArrayList<String> docIDList = new ArrayList<>();
        String xmlText = Util.loadTextFile(fn_xml);
        Pattern pattern = Pattern.compile("(?<=" + begTag + ")\\d{4}(?=" + endTag + ")");
        Matcher m = pattern.matcher(xmlText);
        
        while(m.find()) {
            docIDList.add(m.group());
        }
        
        return docIDList.toArray(new String[docIDList.size()]);
    }
    
    public void trainSVMModel(String trainingFolder, String varID,
            String outputFolder, int iTry) throws Exception {
        String fn_arff = Util.getOSPath(new String[]{trainingFolder,
            "0.." + varID + ".arff"});
        SVMPredictor svm = new SVMPredictor();
        // begin train the model
        svm.trainModelFromFile(fn_arff);
        // save model
        String fn_model = Util.getOSPath(new String[]{outputFolder,
            varID + "-" + Integer.toString(iTry) + ".model"});
        svm.saveModel(fn_model);
    }
    
    public void predictSVMModel(String modelFolder, String varID, String fn_test,
            String outputFolder, int iTry) throws Exception {
        String fn_model = Util.getOSPath(new String[]{modelFolder,
            varID + "-" + Integer.toString(iTry) + ".model"});
        SVMPredictor svm = new SVMPredictor();
        svm.loadModel(fn_model);
        double[][] predDist;
        int[][] confusionMatrix;
        int[] predIndex, targetIndex;
        predDist = svm.predictFromFile(fn_test);
        predIndex = svm.getFinalPredictFromProb(predDist);
        targetIndex = svm.getGoldStandardFromFile(fn_test);
//        confusionMatrix = svm.getConfusionMatrix(targetIndex, predIndex);
        StringBuilder sb = new StringBuilder();
//        for(int i = 0; i < confusionMatrix.length; i++) {
//            for(int j = 0; j < confusionMatrix[i].length; j++) {
//                sb.append(confusionMatrix[i][j]).append(",");
//            }
//        }
        
//        Util.saveTextFile(Util.getOSPath(new String[]{outputFolder,
//            varID + "-" + Integer.toString(iTry) + ".txt"}), sb.toString());
//        sb = new StringBuilder();
        for(int instance = 0; instance < predDist.length; instance++) {
            for(int pred = 0; pred < predDist[instance].length; pred++) {
                sb.append(predDist[instance][pred]).append(",");
            }
            sb.append("\n");
        }
        Util.saveTextFile(Util.getOSPath(new String[]{outputFolder,
            varID + "-pred" + Integer.toString(iTry) + ".csv"}), sb.toString());
    }
}
