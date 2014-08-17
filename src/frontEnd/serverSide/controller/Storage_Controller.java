/**
 * 
 */
package frontEnd.serverSide.controller;

import edu.pitt.cs.nih.backend.utils.Util;

/**
 * @author Phuong Pham
 *
 */
public class Storage_Controller {
	public static String getBaseFolder() throws Exception {
		return Util.getOSPath(new String[]{Util.getExecutingPath(), "data"});
	}
	
	public static String getDocumentListFolder() throws Exception {
		return Util.getOSPath(new String[]{getBaseFolder(), "documentList"});
	}
	
	public static String getModelListFolder() throws Exception {
		return Util.getOSPath(new String[]{getBaseFolder(), "modelList"});
	}
	
	public static String getDocsFolder() throws Exception {
		return Util.getOSPath(new String[]{getBaseFolder(), "docs"});
	}
	
	public static String getModelFolder() throws Exception {
		return Util.getOSPath(new String[]{getBaseFolder(), "models"});
	}
	
	public static String getColonoscopyReportFn() throws Exception {
		return "report.txt";
	}
	
	public static String getPathologyReportFn() throws Exception {
		return "pathology.txt";
	}
	
	public static String getWeightFolder() throws Exception {
		return Util.getOSPath(new String[]{getBaseFolder(), "weights"});
	}
	
	public static String getGlobalFeatureVectorFn() throws Exception {
		return Util.getOSPath(new String[]{getBaseFolder(), "globalFeatureVector.txt"});
	}
	
	public static String getFeedbackFn() throws Exception {
		return Util.getOSPath(new String[]{getBaseFolder(), "feedback", "feedback.txt"});
	}
	
	public static String getSessionManagerFn() throws Exception {
		return Util.getOSPath(new String[]{getBaseFolder(), "feedback", "sessionManager.txt"});
	}
	
	public static String getWordTreeFeedbackFn() throws Exception {
		return Util.getOSPath(new String[]{getBaseFolder(), "feedback", "wordTree-feedback.txt"});
	}
	
	public static String getTrainingFileFolder() throws Exception {
		return Util.getOSPath(new String[]{getBaseFolder(), "learnings"});
	}
	
	public static String getLabelsFolder() throws Exception {
		return Util.getOSPath(new String[]{getBaseFolder(), "labels"});
	}
	
	public static String getClassFn(String varID) throws Exception {
		return Util.getOSPath(new String[]{getLabelsFolder(),
				"class-" + varID + ".csv"});
	}
	
	public static String getLearningFn(String varID, String userID, String sessionID)
			throws Exception {
		StringBuilder sb = new StringBuilder(sessionID);
		// Weka format
//		sb.append(".").append(userID).append(".").append(varID).append(".arff");
		// LibSVM format
		sb.append(".").append(userID).append(".").append(varID).append(".txt");
		return sb.toString();
	}
	
	public static String getModelListFn(String sessionID, String userID) 
			throws Exception {
		return Util.getOSPath(new String[]{getModelListFolder(), "modelList." +
			sessionID + "." + userID + ".xml"});
	}
	
	/**
	 * From model name in model list (xml), get the full model name by 
	 * appending it with file extension ".model"
	 * 
	 * @param fn_modelInList
	 * @return
	 * @throws Exception
	 */
	public static String getModelFn(String fn_modelInList) throws Exception {
		return Util.getOSPath(new String[]{getModelFolder(), fn_modelInList + ".model"});
	}
	
	/**
	 * From model name in model list (xml), get the full model name by
	 * appending it with file extension ".weight.csv"
	 * 
	 * @param fn_modelInList
	 * @return
	 * @throws Exception
	 */
	public static String convertModelFn2FeatureWeightFn(String fn_modelInList) throws Exception {
		return fn_modelInList + ".weight.csv";
	}
	
	public static String getVarIdFromFn(String fn_model) throws Exception {
		return fn_model.substring(fn_model.lastIndexOf(".") + 1,
				fn_model.length());
	}
	
	public static String getLocalSessionIDFromFn(String fn_model) throws Exception {
		return fn_model.substring(0, fn_model.indexOf("."));
	}
	
	public static String getLearningFeatureFn(String sessionID, String userID,
			String varID) throws Exception {
		return Util.getOSPath(new String[]{getTrainingFileFolder(),
				sessionID + "." + userID + "." + varID + "-feature.txt"});
	}
	
	public static String getLearningIndexFn(String sessionID, String userID,
			String varID) throws Exception {
		return Util.getOSPath(new String[]{getTrainingFileFolder(),
				sessionID + "." + userID + "." + varID + "-index.txt"});
	}
	
	public static String getLearningWeightFn(String sessionID, String userID,
			String varID) throws Exception {
		return Util.getOSPath(new String[]{getTrainingFileFolder(),
				sessionID + "." + userID + "." + varID + "-weight.txt"});
	}
	
	public static String getHyperParameterFn(String varID) throws Exception {
		return Util.getOSPath(new String[]{getBaseFolder(), "hyperParams",
				varID + "-hyperParams.txt"});
	}
	
	public static String getDevFeatureFn(String varID) throws Exception {
		return Util.getOSPath(new String[]{getTrainingFileFolder(), "dev." + varID +
				"-feature.txt"});
	}
	
	public static String getDevIndexFn(String varID) throws Exception {
		return Util.getOSPath(new String[]{getTrainingFileFolder(), "dev." + varID +
				"-index.txt"});
	}
	
	public static String getLibSVMPath() throws Exception {
//		String osFolder = Util.getOSName().toLowerCase();
//		if(osFolder.contains("windows")) {
//			osFolder = "windows";
//		}
//		else if(osFolder.contains("mac")) {
//			osFolder = "mac";
//		}
//		else {
//			osFolder = "linux";
//		}
//		
//		return Util.getOSPath(new String[]{getBaseFolder(), "libsvm", osFolder});
		return Util.getOSPath(new String[]{getBaseFolder(), "libsvm"});
	}
	
	public static String getPredictionFn() throws Exception {
		return Util.getOSPath(new String[]{getLibSVMPath(), "prediction.txt"});
	}
	
	public static String getTempLearningFeatureFn() throws Exception {
		return Util.getOSPath(new String[]{getLibSVMPath(), "temp-feature.txt"});
	}
	
	public static String getTempLearningIndexFn() throws Exception {
		return Util.getOSPath(new String[]{getLibSVMPath(), "temp-index.txt"});
	}
	
	public static String getTempLearningWeightFn() throws Exception {
		return Util.getOSPath(new String[]{getLibSVMPath(), "temp-weight.txt"});
	}
	
	public static String[] parseModelListFn(String modelListFn) throws Exception {
		String[] parseResult = new String[2];
		String[] tokenList = modelListFn.split("\\.");
		parseResult[0] = tokenList[1]; // sessionID
		parseResult[1] = tokenList[2]; // userID
		return parseResult;
	}
}
