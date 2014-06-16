/**
 * 
 */
package frontEnd.serverSide;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import weka.core.Instances;
import edu.pitt.cs.nih.backend.feedback.FileTextCreateInitialDS;
import edu.pitt.cs.nih.backend.feedback.TextFileFeedbackManager;
import edu.pitt.cs.nih.backend.feedback.TextFileSessionManager;
import edu.pitt.cs.nih.backend.utils.Util;
import edu.pitt.cs.nih.backend.utils.XMLUtil;
import emr_vis_nlp.ml.SVMPredictor;
import frontEnd.serverSide.controller.GridVar_Controller;
import frontEnd.serverSide.controller.Dataset_MLModel_Controller;
import frontEnd.serverSide.controller.Report_Controller;
import frontEnd.serverSide.controller.Storage_Controller;
import frontEnd.serverSide.controller.WordTree_Controller;
import frontEnd.serverSide.model.Feedback_Model;

/**
 * @author Phuong Pham
 *
 */
@Path("/server")
public class WSInterface {
	@Context
	UriInfo uriInfo;
	@Context
	Request request;

	@GET
	@Path("getVarDatasetList")
//	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})	
	@Produces(MediaType.APPLICATION_JSON)	
	public Map<String, Object> getDataSet_ModelList() throws Exception {
//		System.out.println("Data folder is at: " + Storage_Controller.getBaseFolder());
		return Dataset_MLModel_Controller.instance.getAllDataSetModel(); 
	}
//	@Produces("application/x-javascript")
//	public JSONWithPadding getDataSet_ModelList(@QueryParam("callback") String callback) throws Exception {
////		System.out.println("Data folder is at: " + Storage_Controller.getBaseFolder());
//		return new JSONWithPadding(Dataset_MLModel_Controller.instance.getAllDataSetModel(), callback); 
//	}
	
	@GET
	@Path("getVarGridObj/{fn_modelFnList}/{fn_reportIDList}")
//	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Produces(MediaType.APPLICATION_JSON)	
	public Map<String, Object> getReportFromList(
			@PathParam("fn_reportIDList") String fn_reportIDList,
			@PathParam("fn_modelFnList") String fn_modelFnList,
			@QueryParam("callback") String callback) throws Exception {
		int topKwords = 5;
		Map<String, Object> gridVarObj = 
				GridVar_Controller.instance.getPrediction(fn_reportIDList,
						fn_modelFnList, topKwords);
		
		return gridVarObj;
	}
//	@Produces("application/x-javascript")
//	public JSONWithPadding getReportFromList(
//			@PathParam("fn_reportIDList") String fn_reportIDList,
//			@PathParam("fn_modelFnList") String fn_modelFnList,
//			@QueryParam("callback") String callback) throws Exception {
//		int topKwords = 5;
//		Map<String, Object> gridVarObj = 
//				GridVar_Controller.instance.getPrediction(fn_reportIDList,
//						fn_modelFnList, topKwords);
//		
//		return new JSONWithPadding(gridVarObj, callback);
//	}
	
	@GET
	@Path("getReport/{reportID}")
//	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})	
	@Produces(MediaType.APPLICATION_JSON)	
	public Map<String, String> getReportText(@PathParam("reportID") String reportID)
			throws Exception {
		return new Report_Controller().getReport(reportID); 
	}
	
	@GET
	@Path("getWordTree/{fn_reportIDList}/{query}")
//	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})	
	@Produces(MediaType.APPLICATION_JSON)	
	public Map<String, Object> getWordTree(
			@PathParam("fn_reportIDList") String fn_reportIDList,
			@PathParam("query") String rootWord)
			throws Exception {
		return new WordTree_Controller().getWordTree(fn_reportIDList, rootWord); 
	}
	
	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();

//		validateWebServiceOffline();
//		validateFeedbackProcess();
		createDataSet();
		
		long endTime = System.currentTimeMillis();
		long totalTime = (endTime - startTime) / 1000;
	    System.out.println(Util.convertTimeTick2String(totalTime));
	}
	
	protected static void validateWebServiceOffline() throws Exception {
//		// get var grid object
//		String fn_reportIDList = "initialIDList.xml";
//		String fn_modelFnList = "modelList.0..xml";
//		int topKwords = 5;
////		@SuppressWarnings("unused")
//		Map<String, Object> classifierList = 
//		GridVar_Controller.instance.getPrediction(fn_reportIDList, 
//				fn_modelFnList, topKwords);	
//		List<String> reportIDList = Arrays.asList(new String[]{"0002", "0005"});
//		String rootWord = "biopsy";
//		System.out.println(new WordTree_Controller().getWordTree(reportIDList, rootWord));
//		verifyCecum();
	}
	
	protected static void createDataSet() throws Exception {
		List<String> reportIDList = XMLUtil
				.getReportIDFromXMLList(Util.getOSPath(new String[] {
						Storage_Controller.getDocumentListFolder(), "devIDList.xml" }));
	    String[] varIDList = new String[] {"any-adenoma",
	      "appendiceal-orifice", "asa", "biopsy", "cecum", "ileo-cecal-valve",
	      "indication-type", "informed-consent", "nursing-report", "prep-adequateNo",
	      "prep-adequateNot", "prep-adequateYes", "proc-aborted", "withdraw-time"};		
		FileTextCreateInitialDS initDS = new FileTextCreateInitialDS();
		String outputFolder = Storage_Controller.getTrainingFileFolder(); 
		initDS.createWekaDataSet(reportIDList, varIDList, outputFolder, Storage_Controller.getLabelsFolder());
	}
	
	protected static void verifyCecum() throws Exception {
		// train model from cecum.arff
		String fn_train = Util.getOSPath(new String[]{Storage_Controller.getTrainingFileFolder(),
				"0..cecum.arff"});
		SVMPredictor svm = new SVMPredictor();
		svm.trainModelFromFile(fn_train);
		List<String> reportIDList = XMLUtil
				.getReportIDFromXMLList(Util.getOSPath(new String[] {
						Storage_Controller.getDocumentListFolder(), "devIDList.xml"}));
		Report_Controller reportController = new Report_Controller();
		Instances testSet = reportController.getWekaTestSet(reportIDList);
		double[][] predTable = svm.predict(testSet);
		for(int i = 0; i < testSet.numInstances(); i++) {
			System.out.print(testSet.instance(i).stringValue(0) + ",");
			System.out.println(predTable[i][0] + "," + predTable[i][1]);
		}
	}
	
	/**
     * Test case: show that add new feedback is good, can discriminate between add and change; it is safe 
     * to delete (reject a feedback session) and keep on new feedback session
     * 
     * Init session and feedback
     * add1: 0003, 0004, 0005 biopsy -> it's good to add new feedback
     * add2: 0003 cecum (add) asa (add); 0004 biopsy (change) -> can differentiate add and change
     * delete + add3: 0003 cecum (add); 0005 biopsy (change) asa (add) -> it's safe to reject a feedback and move on new one
     * 
     */
	protected static void validateFeedbackProcess() throws Exception {
		FileTextCreateInitialDS initialFeedbackSession = new FileTextCreateInitialDS();

		String fn_modelList = Util.getOSPath(new String[] {
				Storage_Controller.getModelListFolder(), "modelList.0..xml" });
		// initial the whole back end dataset based on modelList.0..xml
		initialFeedbackSession.initializeFeedbackFile(fn_modelList);
		// initialFeedbackSession.validateFeedbackInstanceClass();

		
		String userID = "1";
		String feedbackFileName = Storage_Controller.getFeedbackFn();
		String fn_sessionManager = Storage_Controller.getSessionManagerFn();
		String _learningFolder = Storage_Controller.getTrainingFileFolder();
		String _docsFolder = Storage_Controller.getDocsFolder();
		String _modelFolder = Storage_Controller.getModelFolder();
		String _featureWeightFolder = Storage_Controller.getWeightFolder();
		String _globalFeatureName = Storage_Controller
				.getGlobalFeatureVectorFn();		
		String _xmlPredictorFolder = Storage_Controller.getModelListFolder();
		
		TextFileFeedbackManager feedbackManager = new TextFileFeedbackManager(
				feedbackFileName, fn_sessionManager, _learningFolder,
				_docsFolder, _modelFolder, _featureWeightFolder,
				_globalFeatureName, _xmlPredictorFolder);
		feedbackManager.setUserID(userID);

		List<Feedback_Model> batch;
		batch = initialFeedbackSession.addFeedBack1(userID);
		feedbackManager.processFeedback(batch);
//		// feedback and session
//		feedbackManager.saveFeedbackBatch(batch);
//		// create learning file
//		feedbackManager.createLearningFiles();
//		// retrain model
//		feedbackManager.updateModels();
//		// create new model list
//		feedbackManager.createXMLPredictorFile();
		
		batch = initialFeedbackSession.addFeedBack2(userID);
		feedbackManager.processFeedback(batch);
		
		TextFileSessionManager sessionManager = new TextFileSessionManager(
				Storage_Controller.getSessionManagerFn());
		String sessionID = "2";
		sessionManager.deleteSession(sessionID, feedbackManager.getUserID()); // how about delete the whole session?
		
		batch = initialFeedbackSession.addFeedBack3(userID);
		feedbackManager.processFeedback(batch);
	}
	
	protected static void validateOutputWebServiceWinApp() throws Exception {
//		String fn_suffix = "-dev.csv";
//		
//		String[][] jerseyTable = Util.loadTable("app" + fn_suffix);
//		HashMap<String, HashMap<String,String>> jerseyMap = new HashMap<>();
//		for(int i = 0; i < jerseyTable.length; i++) {
//			if(!jerseyMap.containsKey(jerseyTable[i][0])) {
//				HashMap<String, String> map = new HashMap<>();
//				jerseyMap.put(jerseyTable[i][0], map);
//			}
//				jerseyMap.get(jerseyTable[i][0]).put(jerseyTable[i][1].toLowerCase(), jerseyTable[i][2]);
//		}
//		System.out.println(jerseyMap.size());
//		String[][] appTable = Util.loadTable("jersey" + fn_suffix);
//		if(appTable.length != jerseyTable.length) {
//			System.out.println("Different length");
//		}
//		for(int i = 0; i < appTable.length; i++) {
////				System.out.println(appTable[i][0] + "," + appTable[i][1]);
//				if(!jerseyMap.get(appTable[i][0]).get(appTable[i][1].toLowerCase()).equals(appTable[i][2])) {
//					System.out.println("Not match " + appTable[i][0] + "," + appTable[i][1] + ","  + appTable[i][2] + "," + appTable[i][3]);
//				}
//		}
	}
}
