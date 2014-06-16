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
		sb.append(".").append(userID).append(".").append(varID).append(".arff");
		return sb.toString();
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
		return fn_modelInList + ".model";
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
}
