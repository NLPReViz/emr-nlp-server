/**
 * 
 */
package frontEnd.serverSide.controller;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import weka.core.Instance;
import weka.core.Instances;
import edu.pitt.cs.nih.backend.featureVector.ColonoscopyDS_SVMLightFormat;
import edu.pitt.cs.nih.backend.featureVector.FeatureVector;
import edu.pitt.cs.nih.backend.featureVector.Preprocess;
import edu.pitt.cs.nih.backend.featureVector.WekaDataSet;
import edu.pitt.cs.nih.backend.utils.Util;
import emr_vis_nlp.ml.LibLinearPredictor;
import emr_vis_nlp.ml.LibSVMPredictor;
import emr_vis_nlp.ml.SVMPredictor;
import frontEnd.serverSide.model.FeatureWeight;
import frontEnd.serverSide.model.ReportPrediction_Model;

/**
 * @author Phuong Pham
 *
 */
public class Report_Controller {
	
	private String m_fn_colonoscopyReport;
	private String m_fn_pathologyReport;
	private String m_weightFolder;
	private String m_docsFolder;
	
	public Report_Controller() {
		// initialize global feature vector
		try {			
			m_fn_colonoscopyReport = Storage_Controller
					.getColonoscopyReportFn();
			m_fn_pathologyReport = Storage_Controller.getPathologyReportFn();
			m_weightFolder = Storage_Controller.getWeightFolder();
			m_docsFolder = Storage_Controller.getDocsFolder();

			if (SVMPredictor.globalFeatureVector == null
					|| SVMPredictor.globalFeatureVector.length == 0) {
				SVMPredictor.globalFeatureVector = Util.loadList(SVMPredictor
						.getGlobalFeatureVectorFn());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public List<Map<String, Object>> getReport_Model(List<String> reportIDList,
			List<String> modelFnList, int topKwords, 
			List<FeatureWeight>[] globalTopPositive,
			List<FeatureWeight>[] globalTopNegative, boolean biasFeature) throws Exception {
		System.out.println("In getReport_Model, reportIDList.length=" + reportIDList.size());
		String positiveClassification = "positive";
		String negativeClassification = "negative";
		String unclassified = "unclassified";
		int numDecConfidence = 2;
		
		// create globalTopPositive and globalTopNegative
		for (int iModel = 0; iModel < modelFnList.size(); iModel++) {
			globalTopPositive[iModel] = new ArrayList<>();
			globalTopNegative[iModel] = new ArrayList<>();
		}
		
		// create test set for all classifiers
//		// weka
//		Instances testSet = getWekaTestSet(reportIDList);
		// libSVM
		String fn_featureVector = Storage_Controller.getTempLearningFeatureFn();
		String fn_index = Storage_Controller.getTempLearningIndexFn();
		createLibSVMLearningFile(reportIDList, fn_featureVector, fn_index);
		// get predictions of each classifier
		double[][][] predictionList = new double[modelFnList.size()][][];
		for (int i = 0; i < modelFnList.size(); i++) {
//			// weka
//			predictionList[i] = getWekaTestSetPrediction(testSet,
//					Storage_Controller.getModelFn(modelFnList.get(i)));
			// libSVM
			predictionList[i] = getLibSVMTestSetPrediction(fn_featureVector,
					Storage_Controller.getModelFn(modelFnList.get(i)));
		}
//		// access to each prediction of each report for each classifier
//		int[] numPositiveDocument = new int[modelFnList.size()];
//		int[] numNegativeDocument = new int[modelFnList.size()];
//		Arrays.fill(numPositiveDocument, 0);
//		Arrays.fill(numNegativeDocument, 0);
		
		List<Map<String, Object>> reportList = new ArrayList<>();
		Map<String, Object> report;
		ReportPrediction_Model reportPrediction;		
		String[] globalFeatureVector = Util.loadList(
				Storage_Controller.getGlobalFeatureVectorFn());
		HashMap<String, Integer> featureIndexMap = new HashMap<>();
		for(int i = 0; i < globalFeatureVector.length; i++) {
			featureIndexMap.put(globalFeatureVector[i], i);
		}
		
		String[][] featureWeightTable;
		List<FeatureWeight> topFeatureList;
		List<List<String[]>> allTokenList;
		TopFeature_Controller topFeatureController = new TopFeature_Controller();
//		StringBuilder sb = new StringBuilder();
//		// weka
//		for (int iInstance = 0; iInstance < testSet.numInstances(); iInstance++) {
//			Instance reportInstance = testSet.instance(iInstance);
//			report = new HashMap<>();
//			report.put("id", reportInstance.stringValue(0)); // att starts from 0
		// libSVM
		String[][] testIndexTable = Util.loadTable(fn_index);
		List<String>[] sparsedIndexData = 
				ColonoscopyDS_SVMLightFormat.fromStringTable2SparseIndexArray(
						Util.loadTable(fn_featureVector, ColonoscopyDS_SVMLightFormat.svmLightDelimiter));
		for(int iInstance = 0; iInstance < testIndexTable.length; iInstance++) {
			report = new HashMap<>();
			report.put("id", testIndexTable[iInstance][0]); // att starts from 0
			for (int iModel = 0; iModel < predictionList.length; iModel++)
			{
				reportPrediction = new ReportPrediction_Model();
				System.out.print("Debug: predictionList length=" + predictionList[iModel].length + "\t");
				System.out.println("iInstance=" + iInstance + ", iModel=" + iModel);
				if(predictionList[iModel][iInstance][0] == 0.0 ||
						predictionList[iModel][iInstance][0] == 1.0 ||
						predictionList[iModel][iInstance][0] == 0.5 ||
						predictionList[iModel][iInstance][0] == predictionList[iModel][iInstance][1]) { // unclassified cases
					reportPrediction.setClassification(unclassified);
					reportPrediction.setConfidence(Util.round(predictionList[iModel][iInstance][1], numDecConfidence));
				}
				else if(predictionList[iModel][iInstance][0] > predictionList[iModel][iInstance][1]) {
					reportPrediction.setClassification(negativeClassification);
					reportPrediction.setConfidence(Util.round(predictionList[iModel][iInstance][0], numDecConfidence));
//					numNegativeDocument[iModel]++;
				}
				else if (predictionList[iModel][iInstance][0] < predictionList[iModel][iInstance][1]) { // positive
					reportPrediction.setClassification(positiveClassification);
					reportPrediction.setConfidence(Util.round(predictionList[iModel][iInstance][1], numDecConfidence));
//					numPositiveDocument[iModel]++;
				}
//				if(Storage_Controller.getVarIdFromFn(modelFnList.get(iModel)).equals("asa")){
//					System.out.println(predictionList[iModel][iInstance][0] + " assign as " + reportPrediction.getClassification());
//				}
				
//				sb.append(report.get("id") + "," + Storage_Controller.getVarIdFromFn(modelFnList.get(iModel)) + "," + reportPrediction.getClassification().toUpperCase() + "," + predictionList[iModel][iInstance][1] + "\n");
				
				if(Util.fileExists(Util.getOSPath(new String[]{m_weightFolder,
						Storage_Controller.convertModelFn2FeatureWeightFn(
								modelFnList.get(iModel))}))) {
					
					featureWeightTable = Util
							.loadTable(Util.getOSPath(new String[] {
									m_weightFolder,
									Storage_Controller
											.convertModelFn2FeatureWeightFn(modelFnList
													.get(iModel)) }));
					allTokenList = topFeatureController
							.getStemmedTokenList(getReportText((String) report
									.get("id")));
					
					// get top negative features
					topFeatureList = getTopNegativeLibSVMFeaturesInReport(
							sparsedIndexData[iInstance], featureIndexMap,
							featureWeightTable, topKwords, biasFeature);

					topFeatureController.extractMatchedUnigram(topFeatureList,
							allTokenList);
					reportPrediction.setTopNegative(topFeatureList);

					// get top positive features
					topFeatureList = getTopPositiveLibSVMFeaturesInReport(
							sparsedIndexData[iInstance], featureIndexMap,
							featureWeightTable, topKwords, biasFeature);

					topFeatureController.extractMatchedUnigram(topFeatureList,
							allTokenList);
					reportPrediction.setTopPositive(topFeatureList);
				}
				else {
					reportPrediction.setTopNegative(new ArrayList<FeatureWeight>());
					reportPrediction.setTopPositive(new ArrayList<FeatureWeight>());
				}

				// update global top negative
				mergeTopFeature2Global(globalTopNegative, iModel,
						reportPrediction.getTopNegative(), topKwords);
				// update global top positive
				mergeTopFeature2Global(globalTopPositive, iModel,
						reportPrediction.getTopPositive(), topKwords);				
				
				// normalize
				List<List<FeatureWeight>> topFeature = new ArrayList<>();
				topFeature.add(reportPrediction.getTopNegative());
				topFeature.add(reportPrediction.getTopPositive());
				double totalWeight = FeatureWeight.getTotalWeight(topFeature);
				FeatureWeight.normalizeFeatureWeights(reportPrediction.getTopNegative(), totalWeight);
				FeatureWeight.normalizeFeatureWeights(reportPrediction.getTopPositive(), totalWeight);
				
				report.put(Storage_Controller.getVarIdFromFn(modelFnList.get(iModel)),
						reportPrediction);
				
				// debug
//				System.out.println("Stemmed tokens");
//				List<String[]> sentences = allTokenList.get(1);
//				for(String[] tokenList : sentences) {
//					for(String token : tokenList) {
//						System.out.println(token);
//					}
//				}
//				System.out.println("----------------------");
//				System.out.println("***" + Storage_Controller.getVarIdFromFn(modelFnList.get(iModel)));
//				System.out.println(reportPrediction.toString());
			}
			reportList.add(report);
		}
		return reportList;
	}
	
	/**
	 * Merge local and global top features, sort them descending 
	 * then return top k words
	 * 
	 * @param globalList
	 * @param localList
	 * @param topKwords
	 * @throws Exception
	 */
	protected void mergeTopFeature2Global(
			List<FeatureWeight>[] globalList, int iModel,
			List<FeatureWeight> localList, int topKwords)
					throws Exception {
		for(FeatureWeight fw : localList) {
			globalList[iModel].add((FeatureWeight)fw.clone());
		}
		Set<FeatureWeight> topSet = new TreeSet<>(globalList[iModel]);
		globalList[iModel] = new ArrayList<>(topSet);
		Collections.sort(globalList[iModel]);
		while(globalList[iModel].size() > topKwords) {
			globalList[iModel].remove(
					globalList[iModel].size() - 1);
		}
	}
	
	protected List<FeatureWeight> getTopNegativeWekaFeaturesInReport(
			Instance reportInstance, HashMap<String, Integer> featureIndexMap,
			String[][] featureWeightTable, int topKwords) throws Exception {
		List<FeatureWeight> topNegativeFeatureList = new ArrayList<>();
		int iFeature = 0;
		double weight;
		FeatureWeight featureWeight;
		while(topNegativeFeatureList.size() < topKwords &&
				iFeature < featureIndexMap.size()) {
			weight = Double.parseDouble(featureWeightTable[iFeature][1]); 
			if(weight < 0 &&
					reportInstance.value(featureIndexMap.get(featureWeightTable[iFeature][0]) + 1) == 1) { // reportID is the first att in reportInstance
				featureWeight = new FeatureWeight();
				featureWeight.setTerm(featureWeightTable[iFeature][0]);
				featureWeight.setWeight(weight);
				topNegativeFeatureList.add(featureWeight);
			}
			iFeature++;
		}
		return topNegativeFeatureList;
	}
	
	protected List<FeatureWeight> getTopPositiveWekaFeaturesInReport(Instance reportInstance,
			HashMap<String, Integer> featureIndexMap, String[][] featureWeightTable, int topKwords) throws Exception {
		List<FeatureWeight> topPositiveFeatureList = new ArrayList<>();
		int iFeature = 0;
		double weight;
		FeatureWeight featureWeight;
		while(topPositiveFeatureList.size() < topKwords &&
				iFeature < featureIndexMap.size()) {
			weight = Double.parseDouble(featureWeightTable[iFeature][1]); 
			if(weight > 0 &&
					reportInstance.value(featureIndexMap.get(featureWeightTable[iFeature][0]) + 1) == 1) { // reportID is the first att in reportInstance
				featureWeight = new FeatureWeight();
				featureWeight.setTerm(featureWeightTable[iFeature][0]);
				featureWeight.setWeight(weight);
				topPositiveFeatureList.add(featureWeight);
			}
			iFeature++;
		}
		return topPositiveFeatureList;
	}
	
	protected List<FeatureWeight> getTopNegativeLibSVMFeaturesInReport(List<String> sparsedIndexInstance,
			HashMap<String, Integer> featureIndexMap, String[][] featureWeightTable, int topKwords,
			boolean biasFeature) throws Exception {
		List<FeatureWeight> topNegativeFeatureList = new ArrayList<>();
		int iFeature = 0;
		double weight;
		FeatureWeight featureWeight;
		int offset = biasFeature ? 2 : 1; // libsvm start from 1 while globalfeature start from 0
		while(topNegativeFeatureList.size() < topKwords &&
				iFeature < featureIndexMap.size()) {
			weight = Double.parseDouble(featureWeightTable[iFeature][1]); 
			if(weight < 0 && !featureWeightTable[iFeature][0].equals("[biasFeature]") &&
					sparsedIndexInstance.contains(
							Integer.toString(
									featureIndexMap.get(featureWeightTable[iFeature][0]) + offset))) {
				featureWeight = new FeatureWeight();
				featureWeight.setTerm(featureWeightTable[iFeature][0]);
				featureWeight.setWeight(weight);
				topNegativeFeatureList.add(featureWeight);
			}
			iFeature++;
		}
		
//		// debug
//		Set<String> unigramList = new TreeSet<>();
//		for(String indexInstance : sparsedIndexInstance) {
//			if(!indexInstance.equals("1")) {
//				unigramList.add(indexInstance);
//			}
//		}
//		String[] globalFeatureVector = Util.loadList(
//				Storage_Controller.getGlobalFeatureVectorFn());
//		for(String unigram : unigramList) {
//			System.out.println(unigram + "," + globalFeatureVector[Integer.parseInt(unigram) - 2]);
//		}
		
		return topNegativeFeatureList;
	}
	
	protected List<FeatureWeight> getTopPositiveLibSVMFeaturesInReport(List<String> sparsedIndexInstance,
			HashMap<String, Integer> featureIndexMap, String[][] featureWeightTable, int topKwords,
			boolean biasFeature) throws Exception {
		List<FeatureWeight> topPositiveFeatureList = new ArrayList<>();
		int iFeature = 0;
		int offset = biasFeature ? 2 : 1; // libsvm start from 1 while globalfeature start from 0
		double weight;
		FeatureWeight featureWeight;
		while(topPositiveFeatureList.size() < topKwords &&
				iFeature < featureIndexMap.size()) {
			weight = Double.parseDouble(featureWeightTable[iFeature][1]); 
			if(weight > 0 && !featureWeightTable[iFeature][0].equals("[biasFeature]") &&
					sparsedIndexInstance.contains(
							Integer.toString(
									featureIndexMap.get(featureWeightTable[iFeature][0]) + offset))) {
				featureWeight = new FeatureWeight();
				featureWeight.setTerm(featureWeightTable[iFeature][0]);
				featureWeight.setWeight(weight);
				topPositiveFeatureList.add(featureWeight);
			}
			iFeature++;
		}
		return topPositiveFeatureList;
	}
	
	protected double[][] getWekaTestSetPrediction(Instances testSet,
			String fn_model) throws Exception {
		SVMPredictor svm = new SVMPredictor();
		svm.loadModel(fn_model);
		return svm.predict(testSet);
	}
	
	protected double[][] getLibSVMTestSetPrediction(String fn_featureTestSet,
			String fn_model) throws Exception {
		double[][] predictionProbabilityList = null;
		
		if(Util.fileExists(fn_model)) {
	//		LibSVMPredictor svm = new LibSVMPredictor();
			LibLinearPredictor svm = new LibLinearPredictor();
			String[] libSVMParamList = new String[4];
			libSVMParamList[0] = Storage_Controller.getLibSVMPath();
			libSVMParamList[1] = fn_featureTestSet;
			libSVMParamList[2] = fn_model;
			libSVMParamList[3] = Storage_Controller.getPredictionFn();
			
			predictionProbabilityList = svm.predict(libSVMParamList);
		}
		else { // if the model does not exist, predict 0 for all classes
			int numInstance = Util.loadList(fn_featureTestSet).length;
			predictionProbabilityList = new double[numInstance][2];
			for(double[] predictInstance : predictionProbabilityList) {
				Arrays.fill(predictInstance, 0.0);
			}
		}
		
		return predictionProbabilityList;
	}
	
	public Instances getWekaTestSet(List<String> reportIDList) throws Exception {
		boolean removeStopWord = true;
		boolean removePunctuation = true;
		boolean stemWord = true;
		boolean caseSensitive = true;
		boolean removeHeaderFooter = true;
		
		FeatureVector testSetFV = WekaDataSet.getTestSetFeatureVector(reportIDList,
				m_docsFolder, m_fn_colonoscopyReport, m_fn_pathologyReport,
				removeStopWord, removePunctuation, stemWord, caseSensitive, removeHeaderFooter);
		
		StringBuilder sb = new StringBuilder();
		// header
		sb.append("@relation Weka_test_set\n");
		sb.append("@attribute \"[ReportID]\" string\n");
		for (int i = 0; i < testSetFV.m_Feature.length; i++) {
			String feature = testSetFV.m_Feature[i];
			sb.append("@attribute \"f_").append(feature.replaceAll("\\s", "_")).append("_f\" {0, 1}\n");
		}
		// class label is also an atrribute
        sb.append("@attribute \"[classLabel]\" {0, 1}\n");
        // adding data
        sb.append("@data\n");
        for (int i = 0; i < testSetFV.m_InstanceID.length; i++) {
        	sb.append("{0 ").append(testSetFV.m_InstanceID[i]).append(",");
        	List<Integer> nonZeroIndices = new ArrayList<>(testSetFV.m_FeatureVector[i].keySet());
        	Collections.sort(nonZeroIndices);
        	for(int iFeature = 0; iFeature < nonZeroIndices.size(); iFeature++) {
        		// 2 assumptions here: 
        		// 1/ the first feature index is instanceID, and Weka sparse format count feature index from 0 -> + 1
        		// 2/ use binary feature, no need to access the "count" value in m_FeatureVector, add 1 by default
        		sb.append(Integer.toString(nonZeroIndices.get(iFeature) + 1)).append(" 1, "); 
        	}
        	// add a dummy class value for each test instance
        	sb.append(testSetFV.m_Feature.length + 1).append(" 0}\n");
        }
        
        StringReader strReader = new StringReader(sb.toString());
        return new Instances(strReader);
	}
	
	protected String getReportText(String reportID) throws Exception {
		String reportText = Preprocess.separateReportHeaderFooter(Util.loadTextFile(Util.getOSPath(new String[]{
				m_docsFolder, reportID, m_fn_colonoscopyReport})))[1];
		if(Util.fileExists(Util.getOSPath(new String[]{m_docsFolder,
				reportID, m_fn_pathologyReport}))) {
			reportText += "\n" + Preprocess.separatePathologyHeaderFooter(Util.loadTextFile(Util.getOSPath(
					new String[]{m_docsFolder, reportID, m_fn_pathologyReport})))[1];
		}
		return reportText;
	}
	
	public Map<String, String> getReport(String reportID) throws Exception {
		Map<String, String> reportText = new HashMap<>();
		reportText.put("reportText", Util.loadTextFile(Util.getOSPath(new String[]{
				m_docsFolder, reportID, m_fn_colonoscopyReport})));
		
		if(Util.fileExists(Util.getOSPath(new String[]{m_docsFolder,
				reportID, m_fn_pathologyReport}))) {
			reportText.put("pathologyText", Util.loadTextFile(Util.getOSPath(
					new String[]{m_docsFolder, reportID, m_fn_pathologyReport})));
		}

		return reportText;
	}

	/**
	 * Create learning file in LibSVM format. This file has dummy labels.
	 * Because test instances are unseen. The same learning file is used for
	 * multi models
	 * 
	 * @throws Exception
	 */
	public void createLibSVMLearningFile(List<String> docIDList,
			String fn_featureVector, String fn_index) throws Exception {
		ColonoscopyDS_SVMLightFormat libSVM = new ColonoscopyDS_SVMLightFormat();		
		FeatureVector fv;
		String docsFolder = Storage_Controller.getDocsFolder();
		String fn_globalFeatureVector = Storage_Controller
				.getGlobalFeatureVectorFn();
		boolean includeBiasFeature = true;	

		libSVM.setClassValueMap(createDummyLabelMap(docIDList));
		fv = libSVM.getFeatureVectorFromReportList(fn_globalFeatureVector,
				docsFolder, docIDList);
//		//debug
//		HashMap<Integer, Double> fvPrint = fv.m_FeatureVector[0];
//		for(Integer index : fvPrint.keySet()) {
//			System.out.println(index);
//		}
		
		libSVM.createLearningFileFromFeatureVector(fv, fn_featureVector,
				fn_index, includeBiasFeature, fn_globalFeatureVector);
	}
	
	public Map<String,String> createDummyLabelMap(List<String> docIDList)
			throws Exception {
		Map<String,String> labelMap = new HashMap<>();
		for(String docID : docIDList) {
			labelMap.put(docID, "1");
		}
		
		return labelMap;
	}
	
//	public static String toJSONStr(List<Report_Model> reportList)
//			throws Exception {
//		StringBuilder sb = new StringBuilder();
//
//		sb.append("{\n");
//		Report_Model report;
//		ReportPrediction_Model reportPrediction;
//		for(int i = 0; i < reportList.size(); i++) {
//			report = reportList.get(i);
//			sb.append("\t\"").append(report.getId()).append("\": {\n");
//			sb.append("\t\t\"id\": \"").append(report.getId()).append("\",\n");
//			for(int j = 0; j < report.getClassifierPredictionList().size(); j++) {
//				reportPrediction = report.getClassifierPredictionList().get(j);
//				sb.append("\t\t\t\"classification\": \"").append(reportPrediction.getClassification()).append("\",\n");
//				sb.append("\t\t\t\"confidence\": ").append(reportPrediction.getConfidence()).append(",\n");
//				
//				sb.append("\t\t\t\"topNegative\": [\n");
//				for(int k = 0; k < reportPrediction.getTopNegative().size(); k++) {
//					FeatureWeight fw = reportPrediction.getTopNegative().get(k);
//					sb.append("\t\t\t\t{\n");
//					sb.append("\t\t\t\t\t\"term\": \"").append(fw.getTerm()).append("\",\n");
//					sb.append("\t\t\t\t\t\"weight\": ").append(fw.getWeight()).append("\n");
//					sb.append("\t\t\t\t},\n");
//				}
//				sb.setLength(sb.length() - 2);
//				sb.append("\n");
//				sb.append("\t\t\t],\n");
//				
//				sb.append("\t\t\t\"topPositive\": [\n");
//				for(int k = 0; k < reportPrediction.getTopPositive().size(); k++) {
//					FeatureWeight fw = reportPrediction.getTopPositive().get(k);
//					sb.append("\t\t\t\t{\n");
//					sb.append("\t\t\t\t\t\"term\": \"").append(fw.getTerm()).append("\",\n");
//					sb.append("\t\t\t\t\t\"weight\": ").append(fw.getWeight()).append("\n");
//					sb.append("\t\t\t\t},\n");
//				}
//				sb.setLength(sb.length() - 2);
//				sb.append("\n");
//				sb.append("\t\t\t]\n");
//				
//				sb.append("\t\t},\n");
//			}
//			
//			sb.append("\t\t]\n");
//			sb.append("\t},\n");
//		}
//		sb.setLength(sb.length() - 2);
//		sb.append("\n");
//		sb.append("}");
//		
//		return sb.toString();
//	}
}
