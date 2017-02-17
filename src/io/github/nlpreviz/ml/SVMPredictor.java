/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package io.github.nlpreviz.ml;

import io.github.nlpreviz.nlp.featureVector.FeatureSet;
import io.github.nlpreviz.nlp.featureVector.FeatureVector;
import io.github.nlpreviz.nlp.featureVector.Preprocess;
import io.github.nlpreviz.nlp.featureVector.WekaDataSet;
import io.github.nlpreviz.nlp.simpleWS.model.Report;

import io.github.nlpreviz.nlp.utils.ExportFeatureWeight;
import io.github.nlpreviz.nlp.utils.FeatureWeight;
import io.github.nlpreviz.nlp.utils.Util;

//import emr_vis_nlp.model.Document;
//import emr_vis_nlp.model.TextInstance;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LibSVM;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.filters.Filter;

/**
 * This class is a wrapper for SVM in Weka.
 * The SVM is modified to return feature weights (work for binary classification)
 * 
 * @author phuongpham <a href="mailto:phuongpham@cs.pitt.edu">phuongpham@cs.pitt.edu</a>
 */
public class SVMPredictor extends ALearner {
    protected Classifier m_Classifier = null;
    public static String[] globalFeatureVector;
    
//    static final long serialVersionUID = -864263723;
    
    public SVMPredictor () {
        
    }
    
    /**
     * This class builds a LibSVM from an Instances object from Weka.
     * <p> 
     * The function is customized for this project. The first feature, i.e. instanceID, 
     * will be removed
     * 
     * @param trainSet A Weka Instances object
     * @throws Exception 
     */
    @Override
    public void train(Object trainSet) throws Exception {
        Instances data = (Instances) trainSet;
        // set class attribute
        // preprocess instances: remove the first attribute: ReportID
        Instances unlabeled = removeAttribute("1", data);
        // train model
        this.train(unlabeled);
    }
    
    public Instances removeAttribute(String attrIndex, Instances data) throws Exception {
        String[] options = weka.core.Utils.splitOptions("-R " + attrIndex);
        String filterName = "weka.filters.unsupervised.attribute.Remove";
        Filter filter = (Filter) Class.forName(filterName).newInstance();
        if (filter instanceof OptionHandler) {
            ((OptionHandler) filter).setOptions(options);
        }
        filter.setInputFormat(data);
        // make the instances
        return Filter.useFilter(data, filter);
    }
    
    /**
     * This function only train the model with the trainSet as it is.
     * In other words, no feature removal will done here.
     * 
     * @param trainSet
     * @throws Exception 
     */
    public void train(Instances trainSet) throws Exception {
        trainSet.setClassIndex(trainSet.numAttributes() - 1);
        // set classifier: use linear SVM only
        String[] options = weka.core.Utils.splitOptions("-K 0");
        String classifierName = "weka.classifiers.functions.LibSVM";
        this.m_Classifier = Classifier.forName(classifierName, options);
        // get probability instead of explicit prediction
        LibSVM libsvm = (LibSVM) this.m_Classifier;
        libsvm.setProbabilityEstimates(true);
        // build model
        this.m_Classifier.buildClassifier(trainSet);
    }
    
    /**
     * Build an SVM model from a Weka learning file (arff format).
     * 
     * @param fnTrainData The filename of the data (.arff)
     * @throws Exception 
     */
    public void trainModelFromFile(String fnTrainData) throws Exception {
        // load instances
        Instances data = new Instances(new BufferedReader(new FileReader(fnTrainData)));
        this.train((Object) data);
    }

    /**
     * This class predicts an Instances object from Weka
     * 
     * @param testSet Weka Instances object
     * @return
     * @throws Exception 
     */
    @Override
    public double[][] predict(Object testSet) throws Exception {
        Instances unlabeled;
        if(testSet instanceof Reader) {
            unlabeled = getInstancesFromReader((Reader) testSet);
        }
//        else if(testSet instanceof Document) {
//            unlabeled = getInstancesFromDocument((Document) testSet);
//        }
        else if(testSet instanceof Report) {
        	unlabeled = getInstancesFromWebServiceReport((Report) testSet);
        }
        else {
            unlabeled = (Instances) testSet;
        }
        // set class attribute
        unlabeled.setClassIndex(unlabeled.numAttributes() - 1);
        // we must remove the first attribute
        Instances filtered = removeAttribute("1", unlabeled);

        // distribution for instance
        double[][] dist = new double[filtered.numInstances()][filtered.numClasses()];
        LibSVM libsvm = (LibSVM) m_Classifier;
//        libsvm.setProbabilityEstimates(true); // only require in buildClassifier
        // label instances
        Instance classMissing;
        // make sure class 0 is at index 0 and class 1 is at index 1
        int posInd, negInd;
        posInd = filtered.classAttribute().indexOfValue("1");
        negInd = filtered.classAttribute().indexOfValue("0");
        for (int i = 0; i < filtered.numInstances(); i++) {
            classMissing = (Instance)filtered.instance(i).copy();
            classMissing.setDataset(filtered);
            classMissing.setClassMissing();
            double[] instanceDist = libsvm.distributionForInstance(classMissing);
//            dist[i] = instanceDist;
            dist[i] = new double[2];
            dist[i][0] = instanceDist[negInd];
            dist[i][1] = instanceDist[posInd];
//            dist[i][2] = libsvm.classifyInstance(classMissing);
        }

        return dist;
    }
    
    public double[][] predictFromFile(String fn_test) throws Exception {
        // load instances
        Instances data = new Instances(new BufferedReader(new FileReader(fn_test)));
        return this.predict((Object) data);
    }
    
//    protected Instances getInstancesFromDocument(Document document)
//            throws Exception {
//        // eliminate tempFile, build reader directly without writing to disk
//        StringBuilder tempFileBuilder = new StringBuilder();       
//
//        String header = "% This is the Colonoscopy problem\n@relation current_working_report\n@attribute [report_identifier] numeric\n";
//
//        tempFileBuilder.append(header);
//
//        List<Integer> termVals = new ArrayList<>();
//        // preprocess and produce the feature vector
//        String[] instanceTextList = new String[2];
//        List<TextInstance> docTextInstances = document.getTextInstances();
//        // only extract the content, skip header and footer parts
//        instanceTextList[0] = docTextInstances.get(0).getTextContent();
//        if (docTextInstances.size() > 1) {
//            // only extract the content, skip header and footer parts
//            instanceTextList[1] = docTextInstances.get(1).getTextContent();
//        } else {
//            instanceTextList[1] = "";
//        }
//        FeatureVector featureVector = null;
//        try {
//            featureVector = WekaDataSet.getInstanceFeatureVector(instanceTextList,
//                    globalFeatureVector, document.getName());
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//
//        for (int i = 0; i < globalFeatureVector.length; i++) {
//            String line = "@attribute \"f_" + globalFeatureVector[i] + "_f\" {0, 1}\n";
//            tempFileBuilder.append(line);
//            termVals.add(featureVector.m_FeatureVector[0][i]);
//        }
//
//        String attrFooter = "@attribute \"[classLabel]\" {0, 1}\n@data\n0";
//        tempFileBuilder.append(attrFooter);
//        for (Integer termVal : termVals) {
//
//            String termValStr = "," + termVal;
//            tempFileBuilder.append(termValStr);
//
//        }
//
//        // append final value for classlabel; doesn't matter, since we're doing prediction on this document?
//        tempFileBuilder.append(",0\n");
//
//        StringReader strReader = new StringReader(tempFileBuilder.toString());
//
//        return getInstancesFromReader(strReader);
//    }
    
	protected Instances getInstancesFromWebServiceReport(Report document)
			throws Exception {
		// eliminate tempFile, build reader directly without writing to disk
		StringBuilder tempFileBuilder = new StringBuilder();

		String header = "% This is the Colonoscopy problem\n@relation current_working_report\n@attribute [report_identifier] numeric\n";

		tempFileBuilder.append(header);

		// preprocess and produce the feature vector
		String[] instanceTextList = new String[2];		
		// only extract the content, skip header and footer parts
		instanceTextList[0] = Preprocess.separateReportHeaderFooter(
				document.getColonoscopyReport())[1];
		if (document.getPathologyReport() != null &&
				document.getPathologyReport().length() > 0) {
			// only extract the content, skip header and footer parts
			instanceTextList[1] = Preprocess.separatePathologyHeaderFooter(
					document.getPathologyReport())[1];
		} else {
			instanceTextList[1] = "";
		}
		
		FeatureVector featureVector = null;
		try {
			featureVector = WekaDataSet.getInstanceFeatureVector(
					instanceTextList, globalFeatureVector, document.getId());
//			System.out.println("Feature table:");
//			System.out.println(featureVector.m_FeatureVector.length + "," + featureVector.m_FeatureVector[0].length);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		StringBuilder featureValueVector = new StringBuilder();
		for (int i = 0; i < globalFeatureVector.length; i++) {
			String line = "@attribute \"f_" + globalFeatureVector[i]
					+ "_f\" {0, 1}\n";
			tempFileBuilder.append(line);
			if(featureVector.m_FeatureVector[0].containsKey(i)) {
				featureValueVector.append(i + 1).append(" 1, ");
			}
		}
		String attrFooter = "@attribute \"[classLabel]\" {0, 1}\n@data\n{0 0000, ";		
		tempFileBuilder.append(attrFooter);
		tempFileBuilder.append(featureValueVector);

		// append final value for classlabel; doesn't matter, since we're doing
		// prediction on this document?
		tempFileBuilder.append(globalFeatureVector.length + 1).append(" 0}\n");

		StringReader strReader = new StringReader(tempFileBuilder.toString());

		return getInstancesFromReader(strReader);
	}
    
    protected Instances getInstancesFromReader(Reader reader) throws Exception {
        return new Instances((Reader) reader);
    }
    
    @Override
    public void saveModel(String fn_Model) {
        try {
            weka.core.SerializationHelper.write(fn_Model, m_Classifier);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    /**
     *
     * @param fn_Model
     */
    @Override
    public void loadModel(String fn_Model) {
        try {
            m_Classifier = (Classifier) weka.core.SerializationHelper.read(fn_Model);
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
        }
    } 

    /**
     * Get feature weights. Each weight corresponds to a feature name with the 
     * same index
     * 
     * @return a double array 
     */
    public double[] getFeatureWeight(int nFeatures) {
        LibSVM libsvm = (LibSVM) m_Classifier;
        double[] weights = null;
        try {
            weights = libsvm.getFeatureWeights(nFeatures);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return weights;
    }
    
    public double[] saveFeatureWeights(String[] featureNames, String fn_csv) {
        double[] weights = getFeatureWeight(featureNames.length);
        try {
            ExportFeatureWeight.saveFeatureWeightList(fn_csv, weights, featureNames);
//            saveKeywords(featureNames, weights, fn_csv);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        
        return weights;
    }
    
    public static void saveKeywords(String[] featureNames, double[] featureWeights,
            String fn_featureWeight) throws Exception {
        FeatureWeight[] keywordSortedList;
        // make sure that the weight array is sorted
        keywordSortedList = ExportFeatureWeight.sortWeights(featureWeights);
        // separate into pos and neg weight lists
        ArrayList<FeatureWeight> posList = new ArrayList<>();
        ArrayList<FeatureWeight> negList = new ArrayList<>();
        for(int i = 0; i < keywordSortedList.length; i++) {
            if(keywordSortedList[i].weight > 0)  {
                posList.add(new FeatureWeight(keywordSortedList[i].index, keywordSortedList[i].weight));
            }
            else if(keywordSortedList[i].weight < 0) {
                negList.add(new FeatureWeight(keywordSortedList[i].index, keywordSortedList[i].weight));
            }
            else {
                break;
            }
        }
        // calculate ratio arrays
        ArrayList<FeatureWeight> keywordList = new ArrayList<>();
        if(posList.size() > 0) {
            keywordList.addAll(extractKeyword(featureNames, posList));
        }
        if(negList.size() > 0) {
            keywordList.addAll(extractKeyword(featureNames, negList));
        }
        
        // sort the keyword list
        double[] keywordWeigthList = new double[keywordList.size()];
        for(int i = 0; i < keywordList.size(); i++) {
            keywordWeigthList[i] = keywordList.get(i).weight;
        }
        keywordSortedList = ExportFeatureWeight.sortWeights(keywordWeigthList);
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < keywordSortedList.length; i++) {
            sb.append(featureNames[keywordList.get(keywordSortedList[i].index).index]);
            sb.append(",");
            sb.append(keywordSortedList[i].weight);
            sb.append("\n");
        }
        
        String fn_keyword = getKeywordFileName(fn_featureWeight);
        Util.saveTextFile(fn_keyword, sb.toString());
    }
    
    protected static List<FeatureWeight> extractKeyword(String[] featureNames,
            List<FeatureWeight> weightList) throws Exception {
        ArrayList<FeatureWeight> keywordList = new ArrayList<>();
        int startInd;
        // find the cut-off position
        // we find the first decresing ratio position, which means later features 
        // are not that distinguish
        startInd = 0;
        while(FeatureSet.isNumberOrDate(featureNames[weightList.get(startInd).index]) &&
                startInd < weightList.size()) {
            startInd++;
        }
        double weightRatio = weightList.get(startInd).weight / weightList.get(startInd + 1).weight;
        double nextWeightRatio;
        for(int i = startInd; i < weightList.size() - 1; i++) {
            nextWeightRatio = weightList.get(i + 1).weight / weightList.get(i + 2).weight;
            if(! FeatureSet.isNumberOrDate(featureNames[weightList.get(i).index])) {
                keywordList.add(weightList.get(i));
            }
            if(weightRatio > nextWeightRatio) {
                break;
            }
            weightRatio = nextWeightRatio;
        }
        
        return keywordList;
    }    
    
    public static String getKeywordFileName(String fn_featureWeight) 
            throws Exception {
        String fn_keyword;
        fn_keyword = fn_featureWeight.substring(0, fn_featureWeight.lastIndexOf(".")) +
                ".keyword.csv";
        return fn_keyword;
    }
    
    public String[] getLabelList() {
        LibSVM libsvm = (LibSVM) m_Classifier;
        int[] intLabelList = libsvm.getLabelList();
        String[] labelList = new String[intLabelList.length];
        for(int i = 0; i < labelList.length; i++) {
            labelList[i] = Integer.toString(intLabelList[i]);
        }
        
        return labelList;
    }
    
    public Classifier getClassifier() {
        return m_Classifier;
    }
    
    public int[][] getConfusionMatrix(String fn_test) throws Exception {
        // load instances
        Instances data = new Instances(new BufferedReader(new FileReader(fn_test)));
        data.setClassIndex(data.numAttributes() - 1);
        Evaluation eval = new Evaluation(data);
        eval.evaluateModel(m_Classifier, data);
        double[][] cMatrix = eval.confusionMatrix();
        int[][] confusionMatrix = new int[cMatrix.length][];
        for(int i = 0; i < cMatrix.length; i++) {
            confusionMatrix[i] = new int[cMatrix[i].length];
            for(int j = 0; j < cMatrix[i].length; j++) {
                confusionMatrix[i][j] = (int) cMatrix[i][j];
            }
        }
        
        return confusionMatrix;
    }
    
    public int[] getGoldStandardFromFile(String fn_test) throws Exception {
        // load instances
        Instances data = new Instances(new BufferedReader(new FileReader(fn_test)));
        data.setClassIndex(data.numAttributes() - 1);
        int[] goldStandardIndexList = new int[data.numInstances()];
        for(int i = 0; i < data.numInstances(); i++) {
            goldStandardIndexList[i] = (int) data.instance(i).classValue();
        }
        
        return goldStandardIndexList;
    }
}
