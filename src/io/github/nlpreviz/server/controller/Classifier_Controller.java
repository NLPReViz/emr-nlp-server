/**
 * 
 */
package io.github.nlpreviz.server.controller;

import io.github.nlpreviz.nlp.utils.Util;
import io.github.nlpreviz.server.model.Classifier_Model;
import io.github.nlpreviz.server.model.FeatureWeight;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Phuong Pham
 * 
 */
public class Classifier_Controller {
	private String m_weightFolder;
	
	public Classifier_Controller() {
		try {
			m_weightFolder = Storage_Controller.getWeightFolder();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Get a map of Classifier_Model objects from classifier names.
	 * Classifier_Model object has not hold the values of 
	 * numNegative document and numPositive document.
	 * 
	 * @param modelFnList
	 * @param topKwords
	 * @return
	 * @throws Exception
	 */
	public Map<String, Classifier_Model> loadClassifierMapFromList(
			List<String> modelFnList, int topKwords) throws Exception {
		// get classifier list
		HashMap<String, Classifier_Model> classifierMap = new HashMap<>();
		Classifier_Model classifier;
		for (int i = 0; i < modelFnList.size(); i++) {
			classifier = intializeClassifier(modelFnList.get(i), topKwords);
			classifierMap.put(
					Storage_Controller.getVarIdFromFn(modelFnList.get(i)),
					classifier);
		}
		
		return classifierMap;
	}

	protected Classifier_Model intializeClassifier(String fn_modelInList,
			int topKwords) throws Exception {
		Classifier_Model classifier = new Classifier_Model();
//		// classifier.setClassifierName(getVarIdFromFn(fn_modelInList));
//		// get top k features
//		String[][] featureWeightTable = Util
//				.loadTable(Util.getOSPath(new String[] {
//						m_weightFolder,
//						Storage_Controller
//								.convertModelFn2FeatureWeightFn(fn_modelInList) }));
//		classifier.setTopNegative(getTopKNegativeFeatures(featureWeightTable,
//				topKwords));
//		classifier.setTopPositive(getTopKPositiveFeatures(featureWeightTable,
//				topKwords));
//		classifier.normalizeTopFeatures();

		return classifier;
	}

	protected List<FeatureWeight> getTopKNegativeFeatures(
			String[][] featureWeightTable, int topKwords) throws Exception {
		List<FeatureWeight> topNegative = new ArrayList<>();
		int i = 0; // skip header row
		double weight;
		FeatureWeight featureWeight;
		while (topNegative.size() < topKwords && i < featureWeightTable.length) {
			weight = Double.parseDouble(featureWeightTable[i][1]);
			if (weight < 0) {
				featureWeight = new FeatureWeight();
				featureWeight.setTerm(featureWeightTable[i][0]);
				featureWeight.setWeight(weight);

				topNegative.add(featureWeight);
			}
			i++;
		}

		return topNegative;
	}

	protected List<FeatureWeight> getTopKPositiveFeatures(
			String[][] featureWeightTable, int topKwords) throws Exception {
		List<FeatureWeight> topPositive = new ArrayList<>();
		int i = 0; // skip header row
		double weight;
		FeatureWeight featureWeight;
		while (topPositive.size() < topKwords && i < featureWeightTable.length) {
			weight = Double.parseDouble(featureWeightTable[i][1]);
			if (weight > 0) {
				featureWeight = new FeatureWeight();
				featureWeight.setTerm(featureWeightTable[i][0]);
				featureWeight.setWeight(weight);

				topPositive.add(featureWeight);
			}
			i++;
		}

		return topPositive;
	}	
	
	public static String toJSONStr(List<Classifier_Model> classifierList)
			throws Exception {
		StringBuilder sb = new StringBuilder();
		Classifier_Model classifier;
		sb.append("{\n");
		for (int i = 0; i < classifierList.size(); i++) {
			classifier = classifierList.get(i);
			sb.append("\t\t\"numNegative\": ")
					.append(classifier.getNumNegative()).append(",\n");
			sb.append("\t\t\"numPositive\": ")
					.append(classifier.getNumPositive()).append(",\n");

			sb.append("\t\t\"topNegative\": [\n");
			for (int j = 0; j < classifier.getTopNegative().size(); j++) {
				sb.append("\t\t\t{\n");
				sb.append("\t\t\t\t\"term\": \"")
						.append(classifier.getTopNegative().get(j).getTerm())
						.append("\",\n");
				sb.append("\t\t\t\t\"weight\": ")
						.append(classifier.getTopNegative().get(j).getWeight())
						.append("\n");
				sb.append("\t\t\t},\n");
			}
			sb.setLength(sb.length() - 2);
			sb.append("\n");
			sb.append("\t\t],\n");

			sb.append("\t\t\"topPositive\": [\n");
			for (int j = 0; j < classifier.getTopPositive().size(); j++) {
				sb.append("\t\t\t{\n");
				sb.append("\t\t\t\t\"term\": \"")
						.append(classifier.getTopPositive().get(j).getTerm())
						.append("\",\n");
				sb.append("\t\t\t\t\"weight\": ")
						.append(classifier.getTopPositive().get(j).getWeight())
						.append("\n");
				sb.append("\t\t\t},\n");
			}
			sb.setLength(sb.length() - 2);
			sb.append("\n");
			sb.append("\t\t]\n");
			sb.append("\t},\n");
		}
		sb.setLength(sb.length() - 2);
		sb.append("\n");

		sb.append("}\n");

		return sb.toString();
	}
}
