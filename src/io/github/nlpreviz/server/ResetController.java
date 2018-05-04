package io.github.nlpreviz.server;

import io.github.nlpreviz.nlp.featureVector.ColonoscopyDS_SVMLightFormat;
import io.github.nlpreviz.nlp.utils.Util;
import io.github.nlpreviz.nlp.utils.XMLUtil;
import io.github.nlpreviz.nlp.featureVector.FeatureSetNGram;
import io.github.nlpreviz.nlp.simpleWS.model.Report;
import io.github.nlpreviz.nlp.simpleWS.ReportDAO;
import io.github.nlpreviz.nlp.featureVector.FeatureSet;
import io.github.nlpreviz.nlp.featureVector.FeatureSet.MLInstanceType;
import io.github.nlpreviz.nlp.featureVector.Preprocess;

import io.github.nlpreviz.ml.ALearner;

import io.github.nlpreviz.server.controller.Feedback_Controller;
import io.github.nlpreviz.server.controller.Storage_Controller;
import io.github.nlpreviz.server.model.Feedback_WordTree_JSON_Model;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ResetController
{
  public static String[] varIDList = { "any-adenoma", "appendiceal-orifice", "asa", "biopsy", "cecum", "ileo-cecal-valve", "indication-type", "informed-consent", "nursing-report", "prep-adequateNo", "prep-adequateNot", "prep-adequateYes", "proc-aborted", "withdraw-time" };
  
  public void initializeFeedbackFile(String fn_modelList, String fn_instanceIDList, String uid)
    throws Exception
  {
    createGlobalFeatureVector();
    System.out.println("initializeFeedbackFile called!");
    initializeFeedbackLibSVM(XMLUtil.getModelFnFromXMLList(fn_modelList), XMLUtil.getReportIDFromXMLList(fn_instanceIDList), uid);
  }
  
  public void initializeFeedbackLibSVM(List<String> modelFnList, List<String> instanceIDList, String uid)
    throws Exception
  {
    System.out.println("Clearing out files...");

    System.out.println(Storage_Controller.getFeedbackFn());
    Util.saveTextFile(Storage_Controller.getFeedbackFn(), "");
    
    System.out.println(Storage_Controller.getModelListFolder());
    Util.clearFolder(Storage_Controller.getModelListFolder());
    
    String[] sessionIDList = new String[varIDList.length];
    Arrays.fill(sessionIDList, "-1");
    String[] userIDList = new String[varIDList.length];
    Arrays.fill(userIDList, "");
    String fn_initialModelList = Util.getOSPath(new String[] { Storage_Controller.getModelListFolder(), "modelList.-1..xml" });
    
    XMLUtil.createXMLPredictor(sessionIDList, userIDList, varIDList, Storage_Controller.getModelListFolder(), "0", "", fn_initialModelList);
    
    System.out.println(Storage_Controller.getSessionManagerFn());
    Util.saveTextFile(Storage_Controller.getSessionManagerFn(), "");
    
    System.out.println(Storage_Controller.getTrainingFileFolder());
    Util.clearFolder(Storage_Controller.getTrainingFileFolder());
    
    System.out.println(Storage_Controller.getModelFolder());
    Util.clearFolder(Storage_Controller.getModelFolder());
    
    System.out.println(Storage_Controller.getWeightFolder());
    Util.clearFolder(Storage_Controller.getWeightFolder());
    
    System.out.println(Storage_Controller.getWordTreeFeedbackFn());
    Util.saveTextFile(Storage_Controller.getWordTreeFeedbackFn(), "");
    
    List<Feedback_WordTree_JSON_Model> batch = new ArrayList();
    
    String initialFolder = Storage_Controller.getInitialIDFolder();
    System.out.println("Initial folder: " + initialFolder);

    if (!Util.fileExists(initialFolder)) {
      Util.createFolder(initialFolder);
    }

    createInitialIDListForSession0(instanceIDList, initialFolder);
    
    for (String varID : varIDList)
    {
      Map<String, String> classValueMap = ColonoscopyDS_SVMLightFormat.getClassMap(varID);
      String[] instanceIDArray = Util.loadList(Util.getOSPath(new String[] { initialFolder, varID + "-id.txt" }));
      for (int i = 0; i < instanceIDArray.length; i++)
      {
        String instanceID = instanceIDArray[i];
        String instanceClass = ((String)classValueMap.get(instanceID)).equals("1") ? "positive" : "negative";
        
        System.out.println(varID + ", " + instanceID + ", " + instanceClass);

        Feedback_WordTree_JSON_Model feedback = new Feedback_WordTree_JSON_Model();
        feedback.setClassification(instanceClass);
        feedback.setKind("TYPE_DOC");
        feedback.setVariable(varID);
        feedback.setDocList(instanceID);
        batch.add(feedback);
      }
    }
    String fn_modelList = "modelList.0." + uid + ".xml";
    String fn_reportIDList = "initialIDList.xml";
    
    Map<String, Object> map = new Feedback_Controller().getFeedback(batch, fn_modelList, fn_reportIDList, uid);
  }
  
  public void createInitialIDListForSession0(List<String> reportIDList, String initialFolder)
    throws Exception
  {
    Collections.sort(reportIDList);
    String[] initialReportID = (String[])reportIDList.toArray(new String[reportIDList.size()]);
    String idContent = Util.joinString(initialReportID, "\n");
    for (String varID : varIDList) {
      Util.saveTextFile(Util.getOSPath(new String[] { initialFolder, varID + "-id.txt" }), idContent);
    }
  }
  
  public void initializeFeedbackFileEmpty()
    throws Exception
  {
    createGlobalFeatureVector();    

    Util.saveTextFile(Storage_Controller.getFeedbackFn(), "");
    
    Util.clearFolder(Storage_Controller.getModelListFolder());
    
    String[] sessionIDList = new String[varIDList.length];
    Arrays.fill(sessionIDList, "-1");
    String[] userIDList = new String[varIDList.length];
    Arrays.fill(userIDList, "");
    String fn_initialModelList = Util.getOSPath(new String[] { Storage_Controller.getModelListFolder(), "modelList.-1..xml" });
    
    XMLUtil.createXMLPredictor(sessionIDList, userIDList, varIDList, Storage_Controller.getModelListFolder(), "0", "", fn_initialModelList);
    
    String userID = "";
    String sessionID = "0";
    if (Util.fileExists(Storage_Controller.getSessionManagerFn())) {
      Util.deleteFile(Storage_Controller.getSessionManagerFn());
    }
    List<String> modelFnList = XMLUtil.getModelFnFromXMLList(fn_initialModelList);
    createSessionEntries(modelFnList, sessionID, userID, Storage_Controller.getSessionManagerFn());
    
    Util.clearFolder(Storage_Controller.getTrainingFileFolder());
    
    Util.clearFolder(Storage_Controller.getModelFolder());
    
    Util.clearFolder(Storage_Controller.getWeightFolder());
    
    Util.saveTextFile(Storage_Controller.getWordTreeFeedbackFn(), "");
  }
  
  protected void createSessionEntries(List<String> modelFnList, String sessionID, String userID, String fn_sessionMeta)
    throws Exception
  {
    StringBuilder sessionText = new StringBuilder();
    for (String modelInList : modelFnList)
    {
      sessionText.append(sessionID).append(",").append(userID).append(",");
      sessionText.append(Storage_Controller.getVarIdFromFn(modelInList));
      sessionText.append(",active,valid\n");
    }
    Util.saveTextFile(fn_sessionMeta, sessionText.toString());
  }

  protected void createGlobalFeatureVector () throws Exception {
    // create global feature vector
    FeatureSetNGram featureSet = FeatureSetNGram.createFeatureSetNGram();
    // load all documents
    // String fn_fullIDList = Util.getOSPath(new String[]{
    //    Storage_Controller.getDocumentListFolder(), });
    List<Report> documentList = ReportDAO.instance.getReportFromListFile(
        "feedbackIDList.xml", null);
    // System.out.println("Loading all reports for global feature creating");

    FeatureSet.MLInstanceType instanceType = MLInstanceType.COLONREPORTANDPATHOLOGYREPORT;
    for(int i = 0; i < documentList.size(); i++) {
            Report document = documentList.get(i);
            
            String instanceID = document.getId();
            String[] instanceTextList = new String[2];
            // the first string is colonocopy report
            // get content only, skip header and footer
            instanceTextList[0] = Preprocess.separateReportHeaderFooter(
                document.getColonoscopyReport())[1];
            instanceTextList[1] = "";
            // if(document.getPathologyReport().length() > 0) {
            //  instanceTextList[1] = Preprocess.separatePathologyHeaderFooter(
            //      document.getPathologyReport())[1];
            // }
            // else {
            //  instanceTextList[1] = "";
            // }
            
            featureSet.addInstance(instanceID, instanceTextList, instanceType);
        }
    // get the global feature vector only
    // System.out.println("Start extracting");
    String[] globalFeatureVector = featureSet.getGlobalFeatureVector();
    // save the global feature vector
    Util.saveList(ALearner.getGlobalFeatureVectorFn(), globalFeatureVector);
    System.out.println("File is saved at " + ALearner.getGlobalFeatureVectorFn());
  }
}
