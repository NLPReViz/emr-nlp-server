/**
 * 
 */
package frontEnd.serverSide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.map.ObjectMapper;

import weka.core.Instances;
import edu.pitt.cs.nih.backend.featureVector.ColonoscopyDS_SVMLightFormat;
import edu.pitt.cs.nih.backend.featureVector.FeatureSet.MLInstanceType;
import edu.pitt.cs.nih.backend.featureVector.FeatureSetNGram;
import edu.pitt.cs.nih.backend.featureVector.FeatureVector;
import edu.pitt.cs.nih.backend.featureVector.Preprocess;
import edu.pitt.cs.nih.backend.feedback.FileTextCreateInitialDS;
import edu.pitt.cs.nih.backend.feedback.TextFileFeedbackManagerLibSVM;
import edu.pitt.cs.nih.backend.utils.Util;
import edu.pitt.cs.nih.backend.utils.XMLUtil;
import emr_vis_nlp.ml.LibLinearPredictor;
import emr_vis_nlp.ml.LibSVMPredictor;
import emr_vis_nlp.ml.SVMPredictor;
import frontEnd.serverSide.controller.Dataset_MLModel_Controller;
import frontEnd.serverSide.controller.Feedback_Controller;
import frontEnd.serverSide.controller.Feedback_OverrideConflictLabel_Controller;
import frontEnd.serverSide.controller.GridVar_Controller;
import frontEnd.serverSide.controller.Report_Controller;
import frontEnd.serverSide.controller.Storage_Controller;
import frontEnd.serverSide.controller.WordTree_Controller;
import frontEnd.serverSide.model.Feedback_Abstract_Model;
import frontEnd.serverSide.model.Feedback_WordTree_JSON_Model;
import frontEnd.serverSide.model.ReportPrediction_Model;

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
	public Map<String, Object> getDataSet_ModelList(@HeaderParam("uid") String uid) throws Exception {
//		System.out.println("Data folder is at: " + Storage_Controller.getBaseFolder());
//		return Dataset_MLModel_Controller.instance.getAllDataSetModel(); 
		System.out.println("Userid: " + uid);
		return new Dataset_MLModel_Controller().getAllDataSetModel();
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
		boolean biasFeature = true;
		Map<String, Object> gridVarObj = 
//				GridVar_Controller.instance.getPrediction(fn_reportIDList,
//						fn_modelFnList, topKwords, biasFeature);
				new GridVar_Controller().getPrediction(fn_reportIDList,
						fn_modelFnList, topKwords, biasFeature);
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
	
	/**
	 * Fix a bug when the front end call get wordtree with empty rootword
	 * @param fn_reportIDList
	 * @param rootWord
	 * @return
	 * @throws Exception
	 */
	@GET
	@Path("getWordTree/{fn_reportIDList}")
//	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})	
	@Produces(MediaType.APPLICATION_JSON)	
	public Map<String, Object> getWordTreePseudo(
			@PathParam("fn_reportIDList") String fn_reportIDList,
			@PathParam("query") String rootWord)
			throws Exception {
		Map<String, Object> treeMap = new HashMap<>();
		treeMap.put("matches", 0);
		treeMap.put("matchedList", new ArrayList<String>());
		treeMap.put("total", 0);
		treeMap.put("query", "");
		treeMap.put("lefts", new ArrayList<String>());
		treeMap.put("rights", new ArrayList<String>());
		
		return treeMap;
	}
	
	@PUT
	@Path("putFeedback/{fn_modelFnList}/{fn_reportIDList}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Map<String,Object> getFeedback(List<Feedback_WordTree_JSON_Model> feedbackBatch,
			@PathParam("fn_modelFnList") String fn_modelFnList,
			@PathParam("fn_reportIDList") String fn_reportIDList)
			throws Exception {
//		System.out.println("There are " + feedbackBatch.size() + " feedback");		
		return new Feedback_Controller().getFeedback(feedbackBatch, fn_modelFnList, fn_reportIDList);
//		return new Feedback_OverrideConflictLabel_Controller().getFeedback(feedbackBatch, fn_modelFnList, fn_reportIDList);
	}
	
	@PUT
	@Path("putFeedbackOverride/{fn_modelFnList}/{fn_reportIDList}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Map<String,Object> getFeedbackOverride(List<Feedback_WordTree_JSON_Model> feedbackBatch,
			@PathParam("fn_modelFnList") String fn_modelFnList,
			@PathParam("fn_reportIDList") String fn_reportIDList)
			throws Exception {
//		System.out.println("There are " + feedbackBatch.size() + " feedback");		
//		return new Feedback_Controller().getFeedback(feedbackBatch, fn_modelFnList, fn_reportIDList);
		return new Feedback_OverrideConflictLabel_Controller().getFeedback(feedbackBatch, fn_modelFnList, fn_reportIDList);
	}
	
	/**
	 * Create a data set with an initial training set and models built on the training set
	 * @return
	 * @throws Exception
	 */
	@GET
	@Path("resetDB")
	public String resetDB()
			throws Exception {
		FileTextCreateInitialDS dataSet = new FileTextCreateInitialDS();
		// re-create the data set files
    	String fn_modelList = Util.getOSPath(new String[] {
				Storage_Controller.getModelListFolder(), "modelList.0..xml" });
		String fn_reportIDList = Util.getOSPath(new String[]{
				Storage_Controller.getDocumentListFolder(), "initialIDList.xml"});
		// re-create the whole dataset
		dataSet.initializeFeedbackFile(fn_modelList, fn_reportIDList);
		
		return "resetDB: OK";
	}
	
	
	/**
	 * Create an empty data set with no initial models and no initial training instances
	 * @return
	 * @throws Exception
	 */
	@GET
	@Path("resetDBEmpty")
	public String resetDBEmpty()
			throws Exception {
		FileTextCreateInitialDS dataSet = new FileTextCreateInitialDS();
		// re-create the whole dataset
		dataSet.initializeFeedbackFileEmpty();
		
		return "resetDBEmpty: OK";
	}
	
	/**
	 * Checks login
	 * @return
	 * @throws Exception
	 */
	@GET
	@Path("login")
	public String checkLogin()
			throws Exception {
		return "login: OK";
	}
	
	public static void main(String[] args) throws Exception {
//		long startTime = System.currentTimeMillis();
//		System.out.println(Util.getOSName());
		validateWebServiceOffline();
//		validateFeedbackProcess();
//		evaluateInitialSetOnDevSet();
//		createDataSet();
//		verifyFullModel();
//		verifyWordTree();
		
//		long endTime = System.currentTimeMillis();
//		long totalTime = (endTime - startTime) / 1000;
//	    System.out.println(Util.convertTimeTick2String(totalTime));
	}
	
	protected static void verifyWordTree() throws Exception {
		String fn_reportIDList = "initialIDList.xml";
		String rootWord = "";
		Map<String, Object> map = new WordTree_Controller().getWordTree(fn_reportIDList, rootWord);
		int temp = 0;
	}
	
	protected static void verifyFullModel() throws Exception {
		FileTextCreateInitialDS ds = new FileTextCreateInitialDS();
		ds.verifyFullModel();
	}
	
	protected static void validateWebServiceOffline() throws Exception {
//		// get var grid object
		String fn_reportIDList = "devIDList.xml";
		String fn_modelFnList = "modelList.0..xml";
		int topKwords = 5;
		boolean biasFeature = true;
//		// get grid var
//		Map<String, Object> classifierList = 
//		GridVar_Controller.instance.getPrediction(fn_reportIDList, 
//				fn_modelFnList, topKwords, biasFeature);	
		Map<String, Object> gridVarObj = 
				new GridVar_Controller().getPrediction(fn_reportIDList,
						fn_modelFnList, topKwords, biasFeature);
		
//		List<String> reportIDList = Arrays.asList(new String[]{"0002", "0005"});
//		List<String> reportIDList = XMLUtil.getReportIDFromXMLList(Util.getOSPath(new String[]{Storage_Controller.getDocumentListFolder(),fn_reportIDList}));
//		// word tree
//		String rootWord = "biopsy";
//		System.out.println(new WordTree_Controller().getWordTree(reportIDList, rootWord));
//		countDocuments();
		
		// verify word tree annotation handling
//		verifyWordTreeAnnotation();
		
//		FileTextCreateInitialDS dataSet = new FileTextCreateInitialDS();		
//		List<Feedback_WordTree_JSON_Model> feedbackBatch = dataSet.addWordTreeJSONAnnotation();
//		String jsonStr = "{\"kind\":\"TYPE_WORDTREE\",\"selected\":\"biopsy\",\"span\":\"biopsy\",\"classification\":\"positive\",\"variable\":\"biopsy\",\"docList\":[\"0509\",\"0667\",\"0293\",\"0366\",\"0592\",\"0436\",\"0980\",\"0750\",\"0202\",\"0629\",\"0842\",\"0468\",\"0696\",\"0059\",\"0865\",\"0364\",\"0774\",\"0299\",\"0863\",\"0545\",\"0178\",\"0177\",\"0350\",\"0068\",\"0184\",\"0703\",\"0614\",\"0412\",\"0438\",\"0874\",\"0041\",\"0281\",\"1041\",\"0736\",\"0018\",\"0805\",\"0989\",\"0147\",\"0535\",\"1031\",\"0939\",\"0748\",\"0009\",\"0186\",\"0779\",\"0356\",\"0099\"]}";
//		List<Feedback_WordTree_JSON_Model> feedbackBatch = new ArrayList<>();
//		Feedback_WordTree_JSON_Model singleFeedback =  new ObjectMapper().readValue(jsonStr, Feedback_WordTree_JSON_Model.class);
//		feedbackBatch.add(singleFeedback);
		
//		new Feedback_Controller().getFeedback(feedbackBatch, fn_modelFnList, fn_reportIDList);
		
//		int topKwords = 5;
//		boolean biasFeature = true;
//		Map<String, Object> gridVarObj = 
//				new GridVar_Controller().getPrediction(fn_reportIDList,
//						fn_modelFnList, topKwords, biasFeature);
//		List<Map<String, Object>> reportList = (List<Map<String, Object>>) gridVarObj.get("gridData");
//		StringBuilder sb = new StringBuilder();
//		for(Map<String, Object> report : reportList) {
//			sb.append(report.get("id")).append(",");
//			for(int iModel = 0; iModel < FileTextCreateInitialDS.varIDList.length; iModel++) {
//				ReportPrediction_Model reportPrediction =
//						(ReportPrediction_Model) report.get(
//								Storage_Controller.getVarIdFromFn(FileTextCreateInitialDS.varIDList[iModel]));
//				
//				sb.append(reportPrediction.getClassification()).append(",");
//			}
//			sb.append("\n");
//		}
//		Util.saveTextFile("perf-initialSet.csv", sb.toString());
	}
	
	protected static void verifyWordTreeAnnotation() throws Exception {
		FileTextCreateInitialDS ds = new FileTextCreateInitialDS();
		ds.verifyWordTreeAnnotation();
	}
	
	protected static void evaluateInitialSetOnDevSet() throws Exception {
//		String userID = "";
//		String sessionID = "0";
//		String fn_initialModelList = Storage_Controller.getModelListFn(sessionID, userID);
//		
//		FileTextCreateInitialDS io = new FileTextCreateInitialDS();
//		io.evaluateOnDevSet(fn_initialModelList);
//		verifyTrainingFile();
//		buildFullModel();
	}
	
	protected static void buildFullModel() throws Exception {
//		String fn_fullIDList = Util.getOSPath(new String[]{
//				Storage_Controller.getDocumentListFolder(), "fullIDList.xml"});
//		FileTextCreateInitialDS creator = new FileTextCreateInitialDS();
//		// need to recover the dev set after this test
//		creator.createLearningFileFromFn(fn_fullIDList);
		// build full models
		String[] varIDList = new String[] {"any-adenoma",
  		      "appendiceal-orifice", "asa", "biopsy", "cecum", "ileo-cecal-valve",
  		      "indication-type", "informed-consent", "nursing-report", "prep-adequateNo",
  		      "prep-adequateNot", "prep-adequateYes", "proc-aborted", "withdraw-time"};
    	String feedbackFileName = Storage_Controller.getFeedbackFn();
    	String fn_sessionManager = Storage_Controller.getSessionManagerFn();
    	String learningFolder = Storage_Controller.getTrainingFileFolder();
    	String docsFolder = Storage_Controller.getDocsFolder();
    	String modelFolder = Storage_Controller.getModelFolder();
    	String featureWeightFolder = Storage_Controller.getWeightFolder();
    	String globalFeatureName = Storage_Controller.getGlobalFeatureVectorFn();
    	String xmlPredictorFolder = Storage_Controller.getModelListFolder();
    	String fn_globalFeatureName = Storage_Controller.getGlobalFeatureVectorFn();
    	
    	Util.clearFolder(modelFolder);
    	String sessionID = "0";
    	String userID = "";
    	TextFileFeedbackManagerLibSVM feedbackManager = new TextFileFeedbackManagerLibSVM(feedbackFileName,
			fn_sessionManager, learningFolder, docsFolder, modelFolder, featureWeightFolder,
			globalFeatureName, xmlPredictorFolder);
//    	LibSVMPredictor svm = new LibSVMPredictor();
    	LibLinearPredictor svm = new LibLinearPredictor();
    	ColonoscopyDS_SVMLightFormat fileFormat = new ColonoscopyDS_SVMLightFormat();
    	for(int i = 0; i < varIDList.length; i++) {
    		String varID = varIDList[i];
    		String fn_model = feedbackManager.getModelFileName(sessionID, userID, varID);
    		String fn_featureVectorOut = Storage_Controller.getLearningFeatureFn(
        			sessionID, userID, varID);
        	String fn_instanceWeight = Storage_Controller.getLearningWeightFn(sessionID,
        			userID, varID);
        	String[] svmTrainParams = new String[] {Storage_Controller.getLibSVMPath(),
                    fn_featureVectorOut, fn_model, fn_instanceWeight};
        
        	fileFormat.mergeCostList(Storage_Controller.getLearningIndexFn(sessionID, userID, varID),
        			fn_featureVectorOut, fn_instanceWeight, 1, 1, 1);
        	// train the model
            svm.train((Object)svmTrainParams);
            
            // save model
            svm.saveModel(fn_model);
            
            // save feature weight + keyword weight
            String fn_featureWeight = feedbackManager.getFeatureWeightFileName(sessionID, userID, varID);
            svm.saveFeatureWeight(fn_model, fn_globalFeatureName, fn_featureWeight, true);
    	}
	}
	
	protected static void verifyTrainingFile() throws Exception {
//		String fn_weka = "C:\\Users\\Phuong Pham\\Git\\EclipseGitWorkspace\\testBackEndConnection\\data\\learnings\\0..asa.arff";
//		String fn_libSVM_feature = "C:\\Users\\Phuong Pham\\Git\\EclipseGitWorkspace\\emr-nlp-server\\data\\learnings\\0..asa-feature.txt";
//		String fn_libSVM_index = "C:\\Users\\Phuong Pham\\Git\\EclipseGitWorkspace\\emr-nlp-server\\data\\learnings\\0..asa-index.txt";
//		
//		Instances instances = WekaDataSet.loadInstancesObjectFromFile(fn_weka);
//		StringBuilder sbWeka = new StringBuilder();
//		for(int i = 0; i < instances.numInstances(); i++) {
//			Instance inst = instances.instance(i);
//			sbWeka.append("0:").append(inst.stringValue(0)).append(" ").append("1:1.0 "); // bias feature
//			for(int j = 1; j < inst.numAttributes() - 1; j++) {
//				if(!inst.stringValue(j).equals("0")) {
//					sbWeka.append(j + 1).append(":").append(inst.value(j)).append(" ");
//				}
//			}
//			sbWeka.append("\n");
//		}
//		
//		StringBuilder sbSVM = new StringBuilder();
//		String[][] indexList = Util.loadTable(fn_libSVM_index, ",");
//		String[][] featureList = Util.loadTable(fn_libSVM_feature, " ");
//		for(int i = 0; i < indexList.length; i++) {			
//			sbSVM.append("0:").append(indexList[i][0]).append(" ");
//			for(int j = 1; j < featureList[i].length; j++) {
//				sbSVM.append(featureList[i][j]).append(" ");
//			}
//			sbSVM.append("\n");
//		}
//		System.out.println(sbSVM.toString());
//		System.out.println(sbWeka.toString());
//		
//		System.out.println(sbSVM.toString().equals(sbWeka.toString()));
		
		
		String[] reportIDList = Util.loadList("C:\\Users\\Phuong Pham\\Git\\repos\\emr-vis-nlp\\data\\initialIDList.csv");
		
		FeatureSetNGram featureSet = FeatureSetNGram.createFeatureSetNGram();
		MLInstanceType docType = MLInstanceType.COLONREPORTANDPATHOLOGYREPORT;
		
		String[] instanceTextList = new String[2];
		for(String reportID : reportIDList) {
			instanceTextList[0] = Preprocess.separateReportHeaderFooter(
					Util.loadTextFile(Util.getOSPath(new String[]{
							Storage_Controller.getDocsFolder(), reportID,
							Storage_Controller.getColonoscopyReportFn()})))[1];
			if(Util.fileExists(Util.getOSPath(new String[]{
					Storage_Controller.getDocsFolder(), reportID,
					Storage_Controller.getPathologyReportFn()}))) {
				
				instanceTextList[1] = Preprocess.separatePathologyHeaderFooter(Util.loadTextFile(
						Util.getOSPath(new String[]{
								Storage_Controller.getDocsFolder(), reportID,
								Storage_Controller.getPathologyReportFn()})))[1];
			}
			else{
				instanceTextList[1] = "";
			}
			
			featureSet.addInstance(reportID, instanceTextList, docType);
		}
		
		FeatureVector fv = featureSet.getFeatureVectorFromGlobalFeatureVector(
				Util.loadList(Storage_Controller.getGlobalFeatureVectorFn()));
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < fv.m_InstanceID.length; i++) {
			sb.append(fv.m_InstanceID[i]).append(": ");
			Set<Integer> indexSet = fv.m_FeatureVector[i].keySet();
			List<Integer> sortList = new ArrayList<Integer>(indexSet);
			Collections.sort(sortList);
			sb.append(sortList.toString()).append("\n");
		}
		System.out.println(sb.toString());
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

//		String fn_modelList = Util.getOSPath(new String[] {
//				Storage_Controller.getModelListFolder(), "modelList.0..xml" });
//		String fn_reportIDList = Util.getOSPath(new String[]{
//				Storage_Controller.getDocumentListFolder(), "feedbackIDList.xml"});
//		// initial the whole back end dataset based on modelList.0..xml
//		initialFeedbackSession.initializeFeedbackFile(fn_modelList,
//				fn_reportIDList);
//		// initialFeedbackSession.validateFeedbackInstanceClass();
		
//		String userID = "1";
//		String feedbackFileName = Storage_Controller.getFeedbackFn();
//		String fn_sessionManager = Storage_Controller.getSessionManagerFn();
//		String _learningFolder = Storage_Controller.getTrainingFileFolder();
//		String _docsFolder = Storage_Controller.getDocsFolder();
//		String _modelFolder = Storage_Controller.getModelFolder();
//		String _featureWeightFolder = Storage_Controller.getWeightFolder();
//		String _globalFeatureName = Storage_Controller
//				.getGlobalFeatureVectorFn();		
//		String _xmlPredictorFolder = Storage_Controller.getModelListFolder();
//		
//		TextFileFeedbackManagerLibSVM feedbackManager = new TextFileFeedbackManagerLibSVM(
//				feedbackFileName, fn_sessionManager, _learningFolder,
//				_docsFolder, _modelFolder, _featureWeightFolder,
//				_globalFeatureName, _xmlPredictorFolder);
//		feedbackManager.setUserID(userID);
//		TextFileSessionManager sessionManager = new TextFileSessionManager(
//				Storage_Controller.getSessionManagerFn());

//		List<Feedback_Model> batch;
//		batch = initialFeedbackSession.addFeedBack1(userID);
//		feedbackManager.processFeedback(batch);
//		// feedback and session
//		feedbackManager.saveFeedbackBatch(batch);
//		// create learning file
//		feedbackManager.createLearningFiles();
//		// retrain model
//		feedbackManager.updateModels();
//		// create new model list
//		feedbackManager.createXMLPredictorFile();
//		
//		batch = initialFeedbackSession.addFeedBack2(userID);
//		feedbackManager.processFeedback(batch);
//		
//		sessionManager.deleteCurrentSessionID(userID);
////		String sessionID = "2";
////		sessionManager.deleteSession(sessionID, feedbackManager.getUserID()); // how about delete the whole session?
		
//		batch = initialFeedbackSession.addFeedBack3(userID);
//		feedbackManager.processFeedback(batch);
		
		String fn_modelList = Util.getOSPath(new String[] {
				Storage_Controller.getModelListFolder(), "modelList.0..xml"});
		String fn_reportIDList = Util.getOSPath(new String[]{
				Storage_Controller.getDocumentListFolder(), "initialIDList.xml"});
		new FileTextCreateInitialDS().initializeFeedbackFile(fn_modelList, fn_reportIDList);
//		new FileTextCreateInitialDS().initializeFeedbackFileEmpty();
		
//		List<Feedback_WordTree_JSON_Model> batch = initialFeedbackSession.addFeedback4();
//		fn_modelList = "modelList.0..xml";
//		fn_reportIDList = "devIDList.xml";
//		Map<String, Object> map = new Feedback_Controller().getFeedback(batch,
//				fn_modelList, fn_reportIDList);
//		System.out.println(map.get("msg"));	
		
//		createInitialIDListForSession0();
	}
	
	protected static void createInitialIDListForSession0() throws Exception {
		String initialFolder = Storage_Controller.getInitialIDFolder();
		String fn_reportIDList = Util.getOSPath(new String[]{
				Storage_Controller.getDocumentListFolder(), "initialIDList.xml"});		
		new FileTextCreateInitialDS().createInitialIDListForSession0(fn_reportIDList, initialFolder);
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
	
	protected static void countDocuments() throws Exception {
		List<String> reportIDList = XMLUtil.getReportIDFromXMLList(Util
				.getOSPath(new String[] {
						Storage_Controller.getDocumentListFolder(),
						"fullIDList.xml" }));
		String reportText = "";
		String[] termList = new String[] {"note", "small", "orifice", "ileum", "year", "appendiceal",
				"give", "description", "pediatric", "redundant"};
		int[] countPos = new int[termList.length];
		Arrays.fill(countPos, 0);
		int[] countNeg = new int[termList.length];
		Arrays.fill(countNeg, 0);
		Map<String, String> classMap = ColonoscopyDS_SVMLightFormat
				.getClassMap("appendiceal-orifice");
		for (int i = 0; i < reportIDList.size(); i++) {
			reportText = Preprocess.separateReportHeaderFooter(Util
					.loadTextFile(Util.getOSPath(new String[] {
							Storage_Controller.getDocsFolder(),
							reportIDList.get(i),
							Storage_Controller.getColonoscopyReportFn() })))[1];
			if (Util.fileExists(Util.getOSPath(new String[] {
					Storage_Controller.getDocsFolder(), reportIDList.get(i),
					Storage_Controller.getPathologyReportFn() }))) {
				reportText += "\n"
						+ Preprocess.separatePathologyHeaderFooter(Util
								.loadTextFile(Util.getOSPath(new String[] {
										Storage_Controller.getDocsFolder(),
										reportIDList.get(i),
										Storage_Controller
												.getPathologyReportFn() })))[1];
			}

			reportText = reportText.toLowerCase();
			for(int j = 0; j < termList.length; j++) {
				String term = termList[j];
			if (reportText.contains(term)) {
				if (classMap.get(reportIDList.get(i)).equals("0")) {
					countNeg[j]++;
				}
				else {
					countPos[j]++;
				}
			}
			}

		}
		System.out.println("Term,#Pos,#Neg");
		for(int j = 0; j < termList.length; j++) {
			System.out.println(termList[j] + "," + countPos[j] + "," + countNeg[j]);
		}
	}
}
