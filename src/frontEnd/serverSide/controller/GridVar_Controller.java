/**
 * 
 */
package frontEnd.serverSide.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.print.DocPrintJob;

import edu.pitt.cs.nih.backend.utils.Util;
import edu.pitt.cs.nih.backend.utils.XMLUtil;
import emr_vis_nlp.ml.SVMPredictor;
import frontEnd.serverSide.model.Classifier_Model;
import frontEnd.serverSide.model.FeatureWeight;
import frontEnd.serverSide.model.ReportPrediction_Model;

/**
 * @author Phuong Pham
 *
 */
public enum GridVar_Controller {
	instance;
	
	private String m_modelListFolder;
	private String m_documentListFolder;
	private String m_weightFolder;
	
	private GridVar_Controller() {
		// initialize global feature vector
		try {
			m_modelListFolder = Storage_Controller.getModelListFolder();
			m_documentListFolder = Storage_Controller.getDocumentListFolder();
			m_weightFolder = Storage_Controller.getWeightFolder();
			
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
	
	public Map<String, Object> getPrediction(String fn_reportIDList,
			String fn_modelList, int topKwords) throws Exception {
		HashMap<String, Object> gridVarObj = new HashMap<>();
		// get classifier name list
		List<String> modelFnList = XMLUtil.getModelFnFromXMLList(
				Util.getOSPath(new String[]{m_modelListFolder, fn_modelList}));
		
		// get classifier map
		Classifier_Controller classifierController = new Classifier_Controller(); 
		Map<String, Classifier_Model> classifierMap = 
				classifierController.loadClassifierMapFromList(	modelFnList, topKwords);

		// get reportID list
		List<String> reportIDList = XMLUtil
				.getReportIDFromXMLList(Util.getOSPath(new String[] {
						m_documentListFolder, fn_reportIDList }));
		Report_Controller report_Controller = new Report_Controller();
		List<FeatureWeight>[] topGlobalPositive = new ArrayList[modelFnList.size()];
		List<FeatureWeight>[] topGlobalNegative = new ArrayList[modelFnList.size()];
		
		List<Map<String, Object>> reportList = report_Controller.getReport_Model(
				reportIDList, modelFnList, topKwords, topGlobalPositive,
				topGlobalNegative);

		// update meta count
		ArrayList<String>[] docIDPositiveList = new ArrayList[modelFnList.size()];
		ArrayList<String>[] docIDNegativeList = new ArrayList[modelFnList.size()];
		ArrayList<String>[] docIDUnclassifiedList = new ArrayList[modelFnList.size()];
		for(int iModel = 0; iModel < modelFnList.size(); iModel++) {
			docIDPositiveList[iModel] = new ArrayList<>();
			docIDNegativeList[iModel] = new ArrayList<>();
			docIDUnclassifiedList[iModel] = new ArrayList<>();
		}
		for(Map<String, Object> report : reportList) {
			for(int iModel = 0; iModel < modelFnList.size(); iModel++) {
				ReportPrediction_Model reportPrediction =
						(ReportPrediction_Model) report.get(
								Storage_Controller.getVarIdFromFn(modelFnList.get(iModel)));
				if(reportPrediction.getClassification().equals("positive")) {
					docIDPositiveList[iModel].add((String)report.get("id"));
				}
				else if(reportPrediction.getClassification().equals("negative")) {
					docIDNegativeList[iModel].add((String)report.get("id"));
				}
				else {
					docIDUnclassifiedList[iModel].add((String)report.get("id"));
				}
			}	
		}
		
		for(int iModel = 0; iModel < modelFnList.size(); iModel++) {
			classifierMap.get(Storage_Controller.getVarIdFromFn(modelFnList.get(iModel)))
				.setDocPositive(docIDPositiveList[iModel]);
			classifierMap.get(Storage_Controller.getVarIdFromFn(modelFnList.get(iModel)))
				.setDocNegative(docIDNegativeList[iModel]);
			classifierMap.get(Storage_Controller.getVarIdFromFn(modelFnList.get(iModel)))
				.setDocUnclassified(docIDUnclassifiedList[iModel]);
			
			// set global top feature
			classifierMap.get(Storage_Controller.getVarIdFromFn(modelFnList.get(iModel)))
				.setTopNegative(topGlobalNegative[iModel]);
			classifierMap.get(Storage_Controller.getVarIdFromFn(modelFnList.get(iModel)))
				.setTopPositive(topGlobalPositive[iModel]);
			// normalize
			classifierMap.get(Storage_Controller.getVarIdFromFn(modelFnList.get(iModel)))
				.normalizeTopFeatures();
		}
		
		gridVarObj.put("variableData", classifierMap);
		gridVarObj.put("gridData", reportList);
		
		return gridVarObj;
	}
}
