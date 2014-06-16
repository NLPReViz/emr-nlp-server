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

import weka.core.Instance;
import weka.core.Instances;
import edu.pitt.cs.nih.backend.featureVector.FeatureVector;
import edu.pitt.cs.nih.backend.featureVector.Preprocess;
import edu.pitt.cs.nih.backend.featureVector.WekaDataSet;
import edu.pitt.cs.nih.backend.utils.Util;
import emr_vis_nlp.ml.SVMPredictor;
import frontEnd.serverSide.model.FeatureWeight;
import frontEnd.serverSide.model.ReportPrediction_Model;

/**
 * @author Phuong Pham
 *
 */
public class Report_Controller {
	
	private String m_modelFolder;
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
			m_modelFolder = Storage_Controller.getModelFolder();
			m_weightFolder = Storage_Controller.getWeightFolder();
			m_docsFolder = Storage_Controller.getDocsFolder();

			if (SVMPredictor.globalFeatureVector == null
					|| SVMPredictor.globalFeatureVector.length == 0) {
				SVMPredictor.globalFeatureVector = Util.loadList(SVMPredictor
						.getGlobalFeatureVectorFn());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public List<Map<String, Object>> getReport_Model(List<String> reportIDList,
			List<String> modelFnList, int topKwords) throws Exception {
		String positiveClassification = "positive";
		String negativeClassification = "negative";
		String unclassified = "unclassified";
		int numDecConfidence = 2;
		
		// create test set for all classifiers
		Instances testSet = getWekaTestSet(reportIDList);
		// get predictions of each classifier
		double[][][] predictionList = new double[modelFnList.size()][][];
		for (int i = 0; i < modelFnList.size(); i++) {
			predictionList[i] = getTestSetPrediction(testSet,
					Util.getOSPath(new String[] { m_modelFolder,
							Storage_Controller.getModelFn(modelFnList.get(i)) }));
		}
		// access to each prediction of each report for each classifier
		int[] numPositiveDocument = new int[modelFnList.size()];
		Arrays.fill(numPositiveDocument, 0);

		List<Map<String, Object>> reportList = new ArrayList<>();
		Map<String, Object> report;
		ReportPrediction_Model reportPrediction;
		Instance reportInstance;
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
		for (int iInstance = 0; iInstance < testSet.numInstances(); iInstance++) {
			reportInstance = testSet.instance(iInstance);
			report = new HashMap<>();
			report.put("id", reportInstance.stringValue(0)); // att starts from 0
			for (int iModel = 0; iModel < predictionList.length; iModel++)
			{
				reportPrediction = new ReportPrediction_Model();	
				if(predictionList[iModel][iInstance][0] == 0.0 ||
						predictionList[iModel][iInstance][0] == 1.0 ||
						predictionList[iModel][iInstance][0] == 0.5 ||
						predictionList[iModel][iInstance][0] == predictionList[iModel][iInstance][1]) { // unclassified cases
					reportPrediction.setClassification(unclassified);
					reportPrediction.setConfidence(Util.round(predictionList[iModel][iInstance][1], numDecConfidence));
				}
				if(predictionList[iModel][iInstance][0] > predictionList[iModel][iInstance][1]) {
					reportPrediction.setClassification(negativeClassification);
					reportPrediction.setConfidence(Util.round(predictionList[iModel][iInstance][0], numDecConfidence));
				}
				else if (predictionList[iModel][iInstance][0] < predictionList[iModel][iInstance][1]) { // positive
					reportPrediction.setClassification(positiveClassification);
					reportPrediction.setConfidence(Util.round(predictionList[iModel][iInstance][1], numDecConfidence));
					numPositiveDocument[iModel]++;
				}
				
//				sb.append(report.get("id") + "," + Storage_Controller.getVarIdFromFn(modelFnList.get(iModel)) + "," + reportPrediction.getClassification().toUpperCase() + "," + predictionList[iModel][iInstance][1] + "\n");
				
				featureWeightTable = Util.loadTable(
						Util.getOSPath(new String[]{m_weightFolder,
								Storage_Controller.convertModelFn2FeatureWeightFn(
										modelFnList.get(iModel))}));
				allTokenList = topFeatureController.getStemmedTokenList(
						getReportText((String)report.get("id")));
				// get top negative features
				topFeatureList = getTopNegativeFeaturesInReport(
						reportInstance, featureIndexMap, featureWeightTable, topKwords);
				topFeatureController.extractMatchedUnigram(topFeatureList, allTokenList);
				reportPrediction.setTopNegative(topFeatureList);
				// get top positive features
				topFeatureList = getTopPositiveFeaturesInReport(
						reportInstance, featureIndexMap, featureWeightTable, topKwords);
				topFeatureController.extractMatchedUnigram(topFeatureList, allTokenList);
				reportPrediction.setTopPositive(topFeatureList);
				
				// normalize
				List<List<FeatureWeight>> topFeature = new ArrayList<>();
				topFeature.add(reportPrediction.getTopNegative());
				topFeature.add(reportPrediction.getTopPositive());
				double totalWeight = FeatureWeight.getTotalWeight(topFeature);
				FeatureWeight.normalizeFeatureWeights(reportPrediction.getTopNegative(), totalWeight);
				FeatureWeight.normalizeFeatureWeights(reportPrediction.getTopPositive(), totalWeight);
				
				report.put(Storage_Controller.getVarIdFromFn(modelFnList.get(iModel)),
						reportPrediction);
			}
			reportList.add(report);
		}
//		Util.saveTextFile("jersey-dev.csv", sb.toString());
		return reportList;
	}
	
	protected List<FeatureWeight> getTopNegativeFeaturesInReport(Instance reportInstance,
			HashMap<String, Integer> featureIndexMap, String[][] featureWeightTable, int topKwords) throws Exception {
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
	
	protected List<FeatureWeight> getTopPositiveFeaturesInReport(Instance reportInstance,
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
	
	protected double[][] getTestSetPrediction(Instances testSet,
			String fn_model) throws Exception {
		SVMPredictor svm = new SVMPredictor();
		svm.loadModel(fn_model);
		return svm.predict(testSet);
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
