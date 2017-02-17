/**
 * 
 */
package io.github.nlpreviz.nlp.simpleWS;

import io.github.nlpreviz.ml.ALearner;
import io.github.nlpreviz.ml.SVMPredictor;
import io.github.nlpreviz.nlp.simpleWS.model.Report;
import io.github.nlpreviz.nlp.utils.Util;
import io.github.nlpreviz.nlp.utils.XMLUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author Phuong Pham
 *
 */
public enum ReportDAO {
	instance;
	
	private String m_baseFolder;
	private String m_docsFolder;
	private String m_documentListFolder;
	private String m_modelListFolder;
	private String m_modelFolder;
	private String m_fn_colonoscopyReport;
	private String m_fn_pathologyReport;
	
	/**
	 * Initialize all path variables
	 */
	private ReportDAO() {
		try {
			m_baseFolder = Util.getOSPath(new String[]{Util.getExecutingPath(), "data"});
			m_documentListFolder = Util.getOSPath(new String[]{m_baseFolder, "documentList"});
			m_modelListFolder = Util.getOSPath(new String[]{m_baseFolder, "modelList"});
			m_docsFolder = Util.getOSPath(new String[]{m_baseFolder, "docs"});
			m_modelFolder = Util.getOSPath(new String[]{m_baseFolder, "models"});
			m_fn_colonoscopyReport = "report.txt";
			m_fn_pathologyReport = "pathology.txt";
			// initialize global feature vector
			SVMPredictor.globalFeatureVector = Util.loadList(
					SVMPredictor.getGlobalFeatureVectorFn());
		}
		catch (Exception e) {			
		}
	}
	
	/**
	 * Get all Report objects from an XML file consisting of report IDs
	 * 
	 * @param fn_reportIDList
	 * @param fn_modelList
	 * @return
	 * @throws Exception
	 */
	public List<Report> getReportFromListFile(String fn_reportIDList,
			String fn_modelList) throws Exception {
		List<String> reportIDList = XMLUtil.getReportIDFromXMLList(
				Util.getOSPath(new String[]{m_documentListFolder, fn_reportIDList}));
		fn_modelList = fn_modelList == null ? null : 
			Util.getOSPath(new String[]{m_modelListFolder, fn_modelList});
		List<String> modelFnList = XMLUtil.getModelFnFromXMLList(fn_modelList);
		return getReportFromIDList(reportIDList, modelFnList);
	}
	
	
	/**
	 * Get all Report objects from an array of report IDs
	 * 
	 * @param reportIDArray
	 * @return
	 * @throws Exception
	 */
	public List<Report> getReportFromIDList(String[] reportIDArray,
			String[] modelFnArray) throws Exception {
		List<String> reportIDList = Arrays.asList(reportIDArray);
		List<String> modelFnList = Arrays.asList(modelFnArray);
		
		return getReportFromIDList(reportIDList, modelFnList);
	}
	
	/**
	 * Get all Report objects from a list of report IDs
	 * 
	 * @param reportIDList
	 * @param modelList
	 * @return
	 * @throws Exception
	 */
	public List<Report> getReportFromIDList(List<String> reportIDList,
			List<String> modelFnList) throws Exception {
		ArrayList<Report> reportList = new ArrayList<>();
		
		HashMap<String, ALearner> classifierMap = new HashMap<>(); 
		// load classifiers
		SVMPredictor classifier;
		for(String fn_model : modelFnList) {
			classifier = new SVMPredictor();
			classifier.loadModel(
					Util.getOSPath(new String[]{m_modelFolder, fn_model + ".model"}));
			classifierMap.put(getVarIdFromFn(fn_model), classifier);
		}
		
		// load reports and predictions if necessary
		for(int i = 0; i < reportIDList.size(); i++) {
			reportList.add(getReportFromID(reportIDList.get(i), classifierMap));
		}
		
		return reportList;
	}
	
	/**
	 * Get a Report object from a record ID
	 * 
	 * @param reportID
	 * @return
	 * @throws Exception
	 */
	public Report getReportFromID(String reportID,
			HashMap<String, ALearner> classifierMap) throws Exception {
		Report report = null;
		
		// create instance if there is a colonoscopy report
		String fn = Util.getOSPath(new String[]{m_docsFolder, reportID, m_fn_colonoscopyReport});
		if(Util.fileExists(fn)) {
			report = new Report(reportID);
			report.setColonoscopyReport(Util.loadTextFile(fn));
			
			fn = Util.getOSPath(new String[]{m_docsFolder, reportID, m_fn_pathologyReport});
			if(Util.fileExists(fn)) {
				report.setPathologyReport(Util.loadTextFile(fn));
			}
			
			// load true class confidence
			if(classifierMap.size() > 0) {
				for(String varID : classifierMap.keySet()) {
					double[][] predictionDistribution = 
							classifierMap.get(varID).predict(report);
					// make sure that 0: false; 1: true
					report.setTrueClassConfidence(predictionDistribution[0][1], varID);
				}
				StringBuilder sb = new StringBuilder();
				for(String varID : classifierMap.keySet()) {
					sb.append(varID).append(":"); 
					sb.append(String.format("%.2f\n", report.getTrueClassConfidence(varID)));						
				}
				report.setPredConfidence(sb.toString());
			}
		}
		
		return report;
	}
	
	protected String getVarIdFromFn(String fn_model) throws Exception {
		return fn_model.substring(fn_model.lastIndexOf(".") + 1,
				fn_model.length());
	}
}
