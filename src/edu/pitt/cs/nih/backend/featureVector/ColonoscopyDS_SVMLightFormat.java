/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.pitt.cs.nih.backend.featureVector;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.pitt.cs.nih.backend.featureVector.FeatureSet.MLInstanceType;
import edu.pitt.cs.nih.backend.feedback.TextFileSessionManager;
import edu.pitt.cs.nih.backend.utils.Util;
import edu.pitt.cs.nih.backend.utils.XMLUtil;
import emr_vis_nlp.ml.LibLinearPredictor;
import emr_vis_nlp.ml.LibSVMPredictor;
import frontEnd.serverSide.controller.Storage_Controller;

/**
 *
 * @author Phuong Pham <a href="mailto:phuongpham@cs.pitt.edu">phuongpham@cs.pitt.edu</a>
 */
public class ColonoscopyDS_SVMLightFormat extends LibSVMFileFormat {
    protected Map<String, String> classValueTable;
    protected String fn_feedback;
    protected String fn_session;
    protected String sessionID;
    protected String userID;
    protected String varID;
//	MLInstanceType reportType = MLInstanceType.COLONREPORTONLY;
	MLInstanceType reportType = MLInstanceType.COLONREPORTANDPATHOLOGYREPORT;
    
    public ColonoscopyDS_SVMLightFormat() {
    	try {
			fn_feedback = Storage_Controller.getFeedbackFn();
			fn_session = Storage_Controller.getSessionManagerFn();
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    }
    /**
     * Set the class value table for this object.
     * This information will be generated on the fly when parsing 
     * the feedback file
     * 
     * @param fn_classValueTable
     * @throws Exception 
     */
    @Deprecated
    public void setClassValueTable(String fn_classValueTable) throws Exception {
//        classValueTable = Util.loadTable(fn_classValueTable);
    }
    
    public void setClassValueMap(String _varID) throws Exception {
    	classValueTable = getClassMap(_varID);
    }
    
    public void setClassValueMap(Map<String,String> map) throws Exception {
    	classValueTable = map;
    }
    
    public static HashMap<String, String> getClassMap(String varID) throws Exception {
    	String[][] classTable = Util.loadTable(
				Storage_Controller.getClassFn(varID));
    	HashMap<String, String> classMap = new HashMap<>();
    	for(int i = 1; i < classTable.length; i++) {
    		classMap.put(classTable[i][0], classTable[i][1]);
    	}
    	
    	return classMap;
    }
    
    /**
     * Create feature vector for the data set. 
     * <p>
     * Class label has not been included in this step.
     * 
     * @param dataFolder
     * @param fn_featureVector
     * @param fn_index
     * @param includeBiasFeature
     * @param fn_globalFeatureVector
     * @throws Exception
     */
    public void createFullDS(String dataFolder, String _fn_feedback, String _sessionID,
            String _userID, String _varID, String fn_featureVector, String fn_index,
            boolean includeBiasFeature, String fn_globalFeatureVector) throws Exception {
        fn_feedback = _fn_feedback;
        sessionID = _sessionID;
        userID = _userID;
        varID = _varID;
        FeatureVector unNormalizedDenseFeatureVector = getFullDSFeatureVector(
                fn_globalFeatureVector, dataFolder);
//        // load class value table before create training file
//        classValueTable = _classValueTable;
        
        createLearningFileFromFeatureVector(unNormalizedDenseFeatureVector,
                fn_featureVector, fn_index, includeBiasFeature, fn_globalFeatureVector);
    }
    
    /**
     * Create feature vectors for the dataset.
     * And create classValueMap
     * <p>
     * Using the MPQA structure.
     * 
     * @param fn_globalFeatureVector
     * @param dataFolder
     * @return
     * @throws Exception
     */
    @Override
    protected FeatureVector getFullDSFeatureVector(String fn_globalFeatureVector,
            String dataFolder) throws Exception {
    	String fileName, instanceID, instanceText;
    	int start, end;
        String[] instanceTextList = new String[2];
        
        int sessionNum = Integer.parseInt(sessionID);
    	String[][] feedbackTable = Util.loadTable(fn_feedback);
    	List<String> deletedSessionIDList = TextFileSessionManager.getDeletedSessionIDList(
    			userID, fn_session);
    	FeatureSetNGram featureSet = FeatureSetNGram.createFeatureSetNGram();
    	// instanceID, <<start, end,> classValue>
    	HashMap<String, Map<Map.Entry<Integer, Integer>, String>> feedbackSpanDocList = 
                new HashMap<>();
        Map<Map.Entry<Integer, Integer>, String> spanLabelMap;
        
        classValueTable = new HashMap<String,String>(); 

        for(int iFeedback = 0; iFeedback < feedbackTable.length; iFeedback++) {
        	if(!deletedSessionIDList.contains(feedbackTable[iFeedback][1]) && // this session has not been deleted
					(feedbackTable[iFeedback][2].equals("") || feedbackTable[iFeedback][2]
							.equals(userID))
					&& // default user or userID
					Integer.parseInt(feedbackTable[iFeedback][1]) <= sessionNum
					&& // <= sessionNum
					feedbackTable[iFeedback][5].equals(varID)) {
				// get reportID
				instanceID = feedbackTable[iFeedback][4];

				if (!isSpanFeedback(feedbackTable[iFeedback])) { // instance feedback
					// add text content if the instance is a new instance
					if (!featureSet.m_Instances.containsKey(instanceID)) {
						fileName = Util.getOSPath(new String[] { dataFolder,
								instanceID,
								Storage_Controller.getColonoscopyReportFn() });

						// the first string is colonocopy report
						instanceTextList[0] = Util.loadTextFile(fileName);
						// remove header and footer
						instanceTextList[0] = Preprocess
								.separateReportHeaderFooter(instanceTextList[0])[1];

						fileName = Util.getOSPath(new String[] { dataFolder,
								instanceID,
								Storage_Controller.getPathologyReportFn() });
						if (Util.fileExists(fileName)) {
							instanceTextList[1] = Util.loadTextFile(fileName);
							// remove header and footer
							instanceTextList[1] = Preprocess
									.separatePathologyHeaderFooter(instanceTextList[1])[1];
						} else {
							instanceTextList[1] = "";
						}

						featureSet.addInstance(instanceID, instanceTextList,
								reportType);
					}
        			// whether it is a new or old instance, update 
        			// class value with the latest value
        			// we move from 0 - latest sessionID so it is
        			// safe to overwrite value here
                    classValueTable.put(instanceID, feedbackTable[iFeedback][10]);
//                    System.out.println(varID + ": " + instanceID + " = " + feedbackTable[iFeedback][10]);
        		}
        		else { // span feedback
        			start = Integer.parseInt(feedbackTable[iFeedback][6]);
                    end = Integer.parseInt(feedbackTable[iFeedback][7]);
                    spanLabelMap = feedbackSpanDocList.get(instanceID);
                    try {
                    	spanLabelMap.put(new AbstractMap.SimpleEntry<> (start, end),
                    			feedbackTable[iFeedback][9]);
                    }
                    catch (NullPointerException e) {
                    	spanLabelMap = new HashMap<>();
                    	spanLabelMap.put(new AbstractMap.SimpleEntry<> (start, end),
                    			feedbackTable[iFeedback][9]);
                    }
                    feedbackSpanDocList.put(instanceID, spanLabelMap);
        		}
        	}
        }
        
        // extract text spans from span feedbacks
        StringBuilder rawTextColon, rawTextPathology;
        int totalFeedback = 0;
        int totalInstance = featureSet.m_Instances.size();
        for(String report_ID : feedbackSpanDocList.keySet()) {
            rawTextColon = new StringBuilder(Util.loadTextFile(Util.getOSPath(new String[] {dataFolder,
                    report_ID, Storage_Controller.getColonoscopyReportFn()})));
//            // remove header footer
//            rawTextColon = new StringBuilder(Preprocess.separateReportHeaderFooter(
//            		rawTextColon.toString())[1]);

			if (Util.fileExists(Util.getOSPath(new String[] {
					dataFolder, report_ID, Storage_Controller.getPathologyReportFn() }))) {
				rawTextPathology = new StringBuilder(Util.loadTextFile(Util
						.getOSPath(new String[] { dataFolder, report_ID,
								Storage_Controller.getPathologyReportFn() })));
//				rawTextPathology = new StringBuilder(Preprocess.separatePathologyHeaderFooter(
//						rawTextPathology.toString())[1]);
			}
            else {
            	rawTextPathology = new StringBuilder();
            }
            
            spanLabelMap = feedbackSpanDocList.get(report_ID);
            
//            // first approach (merge all) & (merge all + flip)
//            // merge all highlight spans as a single feedback
//            // and the contrastive instance is the original instance removed all 
//            // highlight spans
//            int offset = 0;
//            for(Map.Entry<Integer,Integer> span : spanLabelMap.keySet()) {
//                start = span.getKey() - offset;
//                end = span.getValue() - offset;
//                rawText.replace(start, end, "");
//                offset += end - start;                
//            }
//            instanceText = rawText.toString();
//             // remove header and footer
//            instanceTextList[0] = Preprocess.separateReportHeaderFooter(
//                    instanceText)[1];
//            // pseudo instance ID = reportID_000
//            featureSet.addInstance(report_ID + "_000", instanceTextList, reportType);
//            // can't merge like this, take the isntance feedback instead
//            classValueTable.put(report_ID + "_000", classValueTable.get(report_ID));
//            ++totalFeedback;
            
            
            // second approach (single rationale)
            // use each highlight span to create a pseudo span, not use all 
            // as in the first approach above
            String colonText = rawTextColon.toString();
            String pathologyText = rawTextPathology.toString();
//            for(int i = 0; i < spanList.size(); i++) {
//	            start = spanList.get(i).getKey();
//	            end = spanList.get(i).getValue();
            int i = 0;
            for(Map.Entry<Integer,Integer> span : spanLabelMap.keySet()) {
            	start = span.getKey();
                end = span.getValue();
                
                if(start < colonText.length()) { // the span in colonoscopy report
//                	System.out.println("[colon]" + rawTextColon.substring(start, end));
                	// the StringBuilder.replace function will modify rawText
                    // we need to re-initialize the rawText object at each iteration
                    rawTextColon = new StringBuilder(colonText);
                    instanceText = rawTextColon.replace(start, end, "").toString();
                    // remove header and footer of the colonoscopy report
                    instanceTextList[0] = Preprocess.separateReportHeaderFooter(instanceText)[1];
//                    // no remove header footer
//                    instanceTextList[0] = instanceText;
                    // remove header and footer of the pathology report without modifying
                    if(pathologyText.length() > 0) {
                    	instanceTextList[1] = Preprocess.separatePathologyHeaderFooter(pathologyText)[1];
//                    	// no remove header footer
//	                    instanceTextList[1] = pathologyText;
                    }
                    else {
                    	instanceTextList[1] = "";
                    }
                }
                else { // the span in pathology report
                	start -= colonText.length();
                	end -= colonText.length();                	
                    // remove header and footer of the colonoscopy report without modifying
                	instanceTextList[0] = Preprocess.separateReportHeaderFooter(colonText)[1];
//                	// no remove header footer
//                    instanceTextList[0] = colonText;
                    // the StringBuilder.replace function will modify rawText
                    // we need to re-initialize the rawText object at each iteration
//                    System.out.println("[patho]" + rawTextPathology.substring(start, end));
                    rawTextPathology = new StringBuilder(pathologyText);
                    instanceText = rawTextPathology.replace(start, end, "").toString();
                    // remove header and footer of the pathology report
                    if(pathologyText.length() > 0) {
                    	instanceTextList[1] = Preprocess.separatePathologyHeaderFooter(instanceText)[1];
//                    	// no remove header footer
//	                    instanceTextList[1] = instanceText;
                    }
                    else {
                    	instanceTextList[1] = "";
                    }
                }
                
                String spanID = String.format("%s_%03d", report_ID, i++);
                featureSet.addInstance(spanID, instanceTextList, reportType);
                classValueTable.put(spanID, spanLabelMap.get(span));
                ++totalFeedback;
            }
        }
        
//        System.out.println("There are " + totalFeedback + " over " + totalInstance + " reports. On average, there are " + (totalFeedback * 1.0 / totalInstance) + " feedback per report");
        String[] globalFeatureVector = Util.loadList(fn_globalFeatureVector);

        return featureSet.getFeatureVectorFromGlobalFeatureVector(globalFeatureVector);
    }    
    
    public FeatureVector getFeatureVectorFromReportList(String fn_globalFeatureVector,
            String dataFolder, List<String> reportIDList) throws Exception {
    	// dataFolder = docsFolder
    	
    	String fileName, instanceID;
        String[] instanceTextList = new String[2];
        
    	FeatureSetNGram featureSet = FeatureSetNGram.createFeatureSetNGram();
    	
    	for(int iReport = 0; iReport < reportIDList.size(); iReport++) {
    		instanceID = reportIDList.get(iReport);
    		fileName = Util.getOSPath(new String[] {dataFolder, instanceID,
            	"report.txt"});
    		// the first string is colonocopy report
            instanceTextList[0] = Util.loadTextFile(fileName);
            // remove header and footer
            instanceTextList[0] = Preprocess.separateReportHeaderFooter(
                    instanceTextList[0])[1];
            
            fileName = Util.getOSPath(new String[] {dataFolder, instanceID,
                "pathology.txt"});
            if(Util.fileExists(fileName)) {
                instanceTextList[1] = Util.loadTextFile(fileName);
                // remove header and footer
                instanceTextList[1] = Preprocess.separatePathologyHeaderFooter(
                		instanceTextList[1])[1];
            }
            else {
                instanceTextList[1] = "";
            }
            
            featureSet.addInstance(instanceID, instanceTextList,
            		reportType);
    	}
    	String[] globalFeatureVector = Util.loadList(fn_globalFeatureVector);

        return featureSet.getFeatureVectorFromGlobalFeatureVector(globalFeatureVector);
    }
    
    /**
     * If the <code>instanceID</code> has the format of 0000_000, 
     * then it is a rationale instance; otherwise it is a normal instance.
     * 
     * @param instanceID
     * @return 
     */
    @Override
    public boolean isRationaleInstance(String instanceID) {
        return instanceID.length() > 4; // "0000_000".length()
    }
    
//    /**
//     * Create baseline training file (without C_contrast, and rationales).
//     * 
//     * @param foldSize
//     * @param iFold
//     * @param C
//     * @param fn_indexIn
//     * @param fn_featureIn
//     * @param fn_featureOut
//     * @param fn_indexOut
//     * @param dataFolder
//     * @throws Exception 
//     */
//    public void createExperimentTrainSetBaseLine(int foldSize, int iFold, int finalTestFold, double C,
//            String varID, String fn_indexIn, String fn_featureIn, String fn_indexOut,
//            String fn_featureOut, String dataFolder) throws Exception {
//        
////        ColonoscopyGetExpInstanceIDList expInstanceNumListGetter = new ColonoscopyGetExpInstanceIDList(dataFolder);        
////        int[] instanceNumberList = expInstanceNumListGetter.getTrainSetdocIDNum(
////                foldSize, iFold, varID, finalTestFold);
//    	
//        int[] instanceNumberList = null;
//        
////        int[] instanceNumberList = new int[]{0,1,2};
//        List<Integer> instanceIndexList = getInstanceBaseLineIndexFromInstanceNumber(
//                instanceNumberList, fn_indexIn);
//        // we assume all instances in the index file and feature file are located at line # = index #
//        // hence, we access to feature vector directly through these indices
//        StringBuilder sb;
//        int index;
//        String[] itemList;
//        // build the index file
//        sb = new StringBuilder();
//        itemList = Util.loadList(fn_indexIn, newLine);
//        for(int i = 0; i < instanceIndexList.size(); i++) {
//            index = instanceIndexList.get(i);
//            sb.append(itemList[index]);
//            sb.append(newLine);
//        }
//        Util.saveTextFile(fn_indexOut, sb.toString());
//        
//        createDSTrainSet(instanceIndexList, itemList, fn_featureIn, fn_featureOut,
//                C, 0, 0);
//    }
    
//    /**
//     * Create baseline down sampling training file (without C_contrast, and rationales).
//     * 
//     * @param foldSize
//     * @param iFold
//     * @param C
//     * @param fn_indexIn
//     * @param fn_featureIn
//     * @param fn_featureOut
//     * @param fn_indexOut
//     * @param dataFolder
//     * @throws Exception 
//     */
//    public void createExperimentDownSamplingTrainSetBaseLine(int foldSize, int iFold, int finalTestFold, double C,
//            String varID, String fn_indexIn, String fn_featureIn, String fn_indexOut,
//            String fn_featureOut, String dataFolder) throws Exception {
//        
////        ColonoscopyGetExpInstanceIDList expInstanceNumListGetter = new ColonoscopyGetExpInstanceIDList(dataFolder);        
////        int[] instanceNumberList = expInstanceNumListGetter.getDownSamplingTrainSetdocIDNum(
////                foldSize, iFold, varID, finalTestFold);
//        
//        int[] instanceNumberList = null;
//        
//        List<Integer> instanceIndexList = getInstanceBaseLineIndexFromInstanceNumber(
//                instanceNumberList, fn_indexIn);
//        // we assume all instances in the index file and feature file are located at line # = index #
//        // hence, we access to feature vector directly through these indices
//        StringBuilder sb;
//        int index;
//        String[] itemList;
//        // build the index file
//        sb = new StringBuilder();
//        itemList = Util.loadList(fn_indexIn, newLine);
//        for(int i = 0; i < instanceIndexList.size(); i++) {
//            index = instanceIndexList.get(i);
//            sb.append(itemList[index]);
//            sb.append(newLine);
//        }
//        Util.saveTextFile(fn_indexOut, sb.toString());
//        
//        createDSTrainSet(instanceIndexList, itemList, fn_featureIn, fn_featureOut,
//                C, 0, 0);
//    }
    
//    /**
//     * Extract only original instances. Skip rationale instances.
//     * 
//     * @param instanceNumberList
//     * @param fn_index
//     * @return
//     * @throws Exception 
//     */
//    protected List<Integer> getInstanceBaseLineIndexFromInstanceNumber(int[] instanceNumberList,
//            String fn_index) throws Exception {
//        String[][] instanceIndexTable = Util.loadTable(fn_index);
//        ArrayList<Integer> instanceIDList = new ArrayList<>();
//        
//        String[] instanceNumberStrList = new String[instanceNumberList.length];
//        for(int i = 0; i < instanceNumberList.length; i++) {
//            instanceNumberStrList[i] = String.format("%04d", instanceNumberList[i]);
//        }
//        // check whether the instanceID contains the instance number
//        for(int i = 0; i < instanceIndexTable.length; i++) {
//            for(int j = 0; j < instanceNumberList.length; j++) {
//                if(instanceIndexTable[i][0].equals(instanceNumberStrList[j])) {
//                    instanceIDList.add(i);
//                    break;
//                }
//            }
//        }
//        return instanceIDList;
//    }
    
    /**
     * Create baseline training file (with C, and C_contrast).
     * 
     * @param foldSize
     * @param iFold
     * @param C
     * @param C_contrast
     * @param fn_indexIn
     * @param fn_featureIn
     * @param fn_indexOut
     * @param fn_featureOut
     * @throws Exception 
     */
    public void createExperimentTrainSet(int foldSize, int iFold, int finalTestFold, double C,
            double C_contrast, double mu, String varID, String fn_indexIn, String fn_featureIn,
            String fn_indexOut, String fn_featureOut, String dataFolder) throws Exception {
        
//        ColonoscopyGetExpInstanceIDList expInstanceNumListGetter = 
//                new ColonoscopyGetExpInstanceIDList(dataFolder);        
//        int[] instanceNumberList = expInstanceNumListGetter.getTrainSetdocIDNum(
//                foldSize, iFold, varID, finalTestFold);
        
        int[] instanceNumberList = null;
        
        List<Integer> instanceIndexList = getInstanceIndexFromInstanceNumber(instanceNumberList, fn_indexIn);
        // we assume all instances in the index file and feature file are located at line # = index #
        // hence, we access to feature vector directly through these indices
        StringBuilder sb;
        int index;
        String[] itemList;
        // build the index file
        sb = new StringBuilder();
        itemList = Util.loadList(fn_indexIn, newLine);
        for(int i = 0; i < instanceIndexList.size(); i++) {
            index = instanceIndexList.get(i);
            sb.append(itemList[index]);
            sb.append(newLine);
        }
        Util.saveTextFile(fn_indexOut, sb.toString());
        
        createDSTrainSet(instanceIndexList, itemList, fn_featureIn, fn_featureOut,
                C, C_contrast, mu);
    }
    
    /**
     * Extract instances.
     * 
     * @param instanceNumberList
     * @param fn_index
     * @return
     * @throws Exception 
     */
    protected List<Integer> getInstanceIndexFromInstanceNumber(int[] instanceNumberList,
            String fn_index) throws Exception {
        String[][] instanceIndexTable = Util.loadTable(fn_index);
        ArrayList<Integer> instanceIDList = new ArrayList<>();
        
        String[] instanceNumberStrList = new String[instanceNumberList.length];
        for(int i = 0; i < instanceNumberList.length; i++) {
            instanceNumberStrList[i] = String.format("%04d", instanceNumberList[i]);
        }
        // check whether the instanceID contains the instance number
        for(int i = 0; i < instanceIndexTable.length; i++) {
            for(int j = 0; j < instanceNumberList.length; j++) {
                if(instanceIndexTable[i][0].contains(instanceNumberStrList[j])) {
                    instanceIDList.add(i);
                    break;
                }
            }
        }
        
        return instanceIDList;
    }
    
//    /**
//     * Create the normal train set, without hyper-parameter.
//     * <p>
//     * We do not need to use C for test set. Note that test set contains only x_i, 
//     * no C_contrast and \mu.
//     * <p>
//     * We only get baseline instances (normal data instance), skip contrast instances.
//     * 
//     * @param iFold
//     * @param fn_indexIn
//     * @param fn_featureIn
//     * @param fn_indexOut
//     * @param fn_featureOut
//     * @throws Exception 
//     */
//    public void createExperimentNormalCVTrainSet(int iFold, String fn_indexIn, String fn_featureIn,
//            String varID, String fn_indexOut, String fn_featureOut, String dataFolder) throws Exception {
////        ColonoscopyGetExpInstanceIDList expInstanceNumListGetter = new ColonoscopyGetExpInstanceIDList(dataFolder);        
////        int[] instanceNumberList = expInstanceNumListGetter.getNormalCVTrainSetdocIDNum(iFold, varID);
//        
//        int[] instanceNumberList = null;
//        
//        // do not take contrast examples
//        List<Integer> instanceIndexList = getInstanceBaseLineIndexFromInstanceNumber(instanceNumberList, fn_indexIn);
//        // we assume all instances in the index file and feature file are located at line # = index #
//        // hence, we access to feature vector directly through these indices
//        StringBuilder sb;
//        int index;
//        String[] itemList;
//        // build the index file
//        sb = new StringBuilder();
//        itemList = Util.loadList(fn_indexIn, "\n");
//        for(int i = 0; i < instanceIndexList.size(); i++) {
//            index = instanceIndexList.get(i);
//            sb.append(itemList[index]);
//            sb.append("\n");
//        }
//        Util.saveTextFile(fn_indexOut, sb.toString());
//        
//        // build the feature training file
//        sb = new StringBuilder();
//        itemList = Util.loadList(fn_featureIn, "\n");
//        for(int i = 0; i < instanceIndexList.size(); i++) {
//            index = instanceIndexList.get(i);
//            sb.append(itemList[index]);
//            sb.append("\n");
//        }
//        Util.saveTextFile(fn_featureOut, sb.toString());
//    }
    
//    /**
//     * Create the test set.
//     * <p>
//     * We do not need to use C for test set. Note that test set contains only x_i, 
//     * no C_contrast and \mu.
//     * <p>
//     * We only get baseline instances (normal data instance), skip contrast instances.
//     * 
//     * @param iFold
//     * @param fn_indexIn
//     * @param fn_featureIn
//     * @param fn_indexOut
//     * @param fn_featureOut
//     * @throws Exception 
//     */
//    public void createExperimentTestSet(int iFold, String fn_indexIn, String fn_featureIn,
//            String varID, String fn_indexOut, String fn_featureOut, String dataFolder) throws Exception {
////        ColonoscopyGetExpInstanceIDList expInstanceNumListGetter = new ColonoscopyGetExpInstanceIDList(dataFolder);        
////        int[] instanceNumberList = expInstanceNumListGetter.getTestSetIDNumber(
////                iFold, varID);
//        
//        int[] instanceNumberList = null;
//        
//        // do not take contrast examples
//        List<Integer> instanceIndexList = getInstanceBaseLineIndexFromInstanceNumber(instanceNumberList, fn_indexIn);
//        // we assume all instances in the index file and feature file are located at line # = index #
//        // hence, we access to feature vector directly through these indices
//        StringBuilder sb;
//        int index;
//        String[] itemList;
//        // build the index file
//        sb = new StringBuilder();
//        itemList = Util.loadList(fn_indexIn, "\n");
//        for(int i = 0; i < instanceIndexList.size(); i++) {
//            index = instanceIndexList.get(i);
//            sb.append(itemList[index]);
//            sb.append("\n");
//        }
//        Util.saveTextFile(fn_indexOut, sb.toString());
//        
//        // build the feature training file
//        sb = new StringBuilder();
//        itemList = Util.loadList(fn_featureIn, "\n");
//        for(int i = 0; i < instanceIndexList.size(); i++) {
//            index = instanceIndexList.get(i);
//            sb.append(itemList[index]);
//            sb.append("\n");
//        }
//        Util.saveTextFile(fn_featureOut, sb.toString());
//    }
    
//    /**
//     * Create the down sampling test set.
//     * <p>
//     * We do not need to use C for test set. Note that test set contains only x_i, 
//     * no C_contrast and \mu.
//     * <p>
//     * We only get baseline instances (normal data instance), skip contrast instances.
//     * 
//     * @param iFold
//     * @param fn_indexIn
//     * @param fn_featureIn
//     * @param fn_indexOut
//     * @param fn_featureOut
//     * @throws Exception 
//     */
//    public void createExperimentDownSamplingTestSet(int iFold, String fn_indexIn, String fn_featureIn,
//            String varID, String fn_indexOut, String fn_featureOut, String dataFolder) throws Exception {
////        ColonoscopyGetExpInstanceIDList expInstanceNumListGetter = new ColonoscopyGetExpInstanceIDList(dataFolder);        
////        int[] instanceNumberList = expInstanceNumListGetter.getTestSetDownSamplingIDNumber(
////                iFold, varID);
//        
//        int[] instanceNumberList = null;
//        
//        // do not take contrast examples
//        List<Integer> instanceIndexList = getInstanceBaseLineIndexFromInstanceNumber(instanceNumberList, fn_indexIn);
//        // we assume all instances in the index file and feature file are located at line # = index #
//        // hence, we access to feature vector directly through these indices
//        StringBuilder sb;
//        int index;
//        String[] itemList;
//        // build the index file
//        sb = new StringBuilder();
//        itemList = Util.loadList(fn_indexIn, "\n");
//        for(int i = 0; i < instanceIndexList.size(); i++) {
//            index = instanceIndexList.get(i);
//            sb.append(itemList[index]);
//            sb.append("\n");
//        }
//        Util.saveTextFile(fn_indexOut, sb.toString());
//        
//        // build the feature training file
//        sb = new StringBuilder();
//        itemList = Util.loadList(fn_featureIn, "\n");
//        for(int i = 0; i < instanceIndexList.size(); i++) {
//            index = instanceIndexList.get(i);
//            sb.append(itemList[index]);
//            sb.append("\n");
//        }
//        Util.saveTextFile(fn_featureOut, sb.toString());
//    }

    @Override
    protected String getClassValue(String instanceID) throws Exception {
//        String instanceClassValue = "";
//        for(int i = 0; i < classValueTable.length; i++) {
//            // classValueTable only contains colonoscopy report ID (reportID {0000})
//            // pseudo instances have a form of reportID_000
////            if(instanceID.equals(classValueTable[i][0])) {
//            if(instanceID.contains(classValueTable[i][0])) {
//                instanceClassValue = classValueTable[i][1];
//                break;
//            }
//        }
//        
//        return instanceClassValue;
    	
    	String _instanceID = instanceID.substring(0, 4); //"0000"_000
    	String classValue = classValueTable.get(_instanceID).toLowerCase();
    	
    	return classValue.toLowerCase().equals("false") || classValue.equals("0")? "-1" : "+1";
    }
    
    public void printVerbalFeatureAllPseudoInstance(String fn_pseudo,
            String fn_globalFeatureVector, String fn_feature,
            String fn_index) throws Exception {
        String[] globalFeatureVector = Util.loadList(fn_globalFeatureVector);
        String[][] instanceIndexList = Util.loadTable(fn_index);
        String[] instanceFeatureList = Util.loadList(fn_feature);
        boolean biasFeature = true;
        
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < instanceIndexList.length; i++) {
            if(isRationaleInstance(instanceIndexList[i][0])) {
                sb.append(instanceIndexList[i][0]).append(": ");
                sb.append(printBinaryVerbalVector(instanceFeatureList[i], globalFeatureVector, biasFeature));
                sb.append("\n");
            }
        }
        Util.saveTextFile(fn_pseudo, sb.toString());
    }
    
    public boolean isSpanFeedback(String[] feedbackLine) throws Exception {
    	return feedbackLine.length == 10;
    }
    
    public void mergeCostList(String fn_index, String fn_feature, String fn_weight,
    		double C, double C_contrast, double mu) throws Exception {
    	String[][] indexTable = Util.loadTable(fn_index);
    	String[] featureMatrix = Util.loadList(fn_feature);
    	StringBuilder sbWeight = new StringBuilder();
    	StringBuilder sbFeature = new StringBuilder();
    	String instanceID;
    	
    	for(int i = 0; i < indexTable.length; i++) {
    		instanceID = indexTable[i][0];
    		if(isRationaleInstance(instanceID)) {
                sbFeature.append(
                		createContrastFVString(featureMatrix[i], mu)).append("\n");
                sbWeight.append(C_contrast).append("\n");
            }
    		else {
    			sbFeature.append(featureMatrix[i]).append("\n");
    			sbWeight.append(C).append("\n");
    		}
    	}
    		
    	// save weight file
    	Util.saveTextFile(fn_weight, sbWeight.toString());
    	// update feature file
    	Util.saveTextFile(fn_feature, sbFeature.toString());
    }
    
    /**
     * From sessionID and userID, get modelList and create learning files
     * 
     * @param sessionID
     * @param userID
     * @throws Exception
     */
    public void createLearningFileFromSession(String sessionID,
    		String userID) throws Exception {
    	List<String> modelFnList = XMLUtil.getModelFnFromXMLList(
    			Storage_Controller.getModelListFn(sessionID, userID));
    	String varID, localSessionID;
    	
    	for(int iVar = 0; iVar < modelFnList.size(); iVar++) {
    		varID = Storage_Controller.getVarIdFromFn(modelFnList.get(iVar));
    		localSessionID = 
    				Storage_Controller.getLocalSessionIDFromFn(modelFnList.get(iVar));
    		createLearningFileSet(localSessionID, userID, varID);
    	}
    }
    
    /**
     * From feedback file to create learning file set: index file, feature file, and weight file
     * <lu>
     * <li> Index file: contain reportID, lineCount
     * <li> Feature file: feature vector in LibSVM format
     * <li> Weight file: weights of individual instances in feature file
     * </lu>
     * 
     * @param sessionID
     * @param userID
     * @param varID
     * @throws Exception
     */
    public void createLearningFileSet(String sessionID, String userID, String varID)
    		throws Exception {
    	String dataFolder = Storage_Controller.getDocsFolder();
    	String fn_featureVector = Storage_Controller.getLearningFeatureFn(sessionID,
    			userID, varID);
    	String fn_index = Storage_Controller.getLearningIndexFn(sessionID, userID,
    			varID);
    	boolean includeBiasFeature = true;
    	String fn_globalFeatureVector = Storage_Controller.getGlobalFeatureVectorFn();
    	String fn_weight = Storage_Controller.getLearningWeightFn(sessionID, userID,
    			varID);
    	String[] hyperParamList = Util.loadList(
    			Storage_Controller.getHyperParameterFn(varID), ",");
    	double C = Double.parseDouble(hyperParamList[0]);
    	double C_contrast = Double.parseDouble(hyperParamList[1]);
    	double mu = Double.parseDouble(hyperParamList[2]);
    	
    	// create index file and feature file from feedback file
    	// but feature file has not been normalized (divide pseudo instance to \mu)
    	createFullDS(dataFolder, fn_feedback, sessionID, userID, varID,
    			fn_featureVector, fn_index, includeBiasFeature, fn_globalFeatureVector);
    	// create weight file
    	// update feature vector (divide pseudo instance to \mu)
    	mergeCostList(fn_index, fn_featureVector, fn_weight, C, C_contrast, mu);
    }
    
    public static List<String>[] fromStringTable2SparseIndexArray(String[][] dataSet)
    	throws Exception {
    	List<String>[] sparsedIndexTable = new List[dataSet.length];
    	
    	for(int i = 0; i < dataSet.length; i++) {
    		sparsedIndexTable[i] = fromStringArray2SparsedIndexVector(dataSet[i]);
    	}
    	
    	return sparsedIndexTable;
    }
    
    protected static List<String> fromStringArray2SparsedIndexVector(String[] instanceArray)
    		throws Exception {
    	List<String> sparsedIndexList = new ArrayList<>();
    	String[] indexValue;
    	for(int i = 1; i < instanceArray.length; i++) { // the first index is the label
    		indexValue = instanceArray[i].split(svmLightFeatureValueDelimiter);
    		sparsedIndexList.add(indexValue[0]);
    	}
    	return sparsedIndexList;
    }
    
    /**
     * Create model file, weight file train a set of document 
     * (docIDList), and classValueMap.
     * Apply for document level annotation only, not for span 
     * level annotation
     * 
     * @param docIDList
     * @param classValueTable
     * @param fn_trainFeature
     * @param fn_trainIndex
     * @param fn_model
     * @param fn_modelWeight: weight of each instance, not feature weight
     * @throws Exception
     */
    public void trainModelnFeatureWeight(List<String> docIDList,
    		Map<String,String> classValueMap, String fn_trainFeature, String fn_trainIndex,
    		String fn_model, String fn_modelWeight, String fn_featureWeight, double C,
    		double C_contrast, double mu) throws Exception {
    	
    	String fn_globalFeatureVector = Storage_Controller.getGlobalFeatureVectorFn();
    	// create class value map
    	classValueTable = classValueMap;
    	// create training file
    	FeatureVector fv = getFVFromDocIDList(docIDList, fn_globalFeatureVector);
    	boolean includeBiasFeature = true;
    	
    	createLearningFileFromFeatureVector(fv, fn_trainFeature, fn_trainIndex,
    			includeBiasFeature, fn_globalFeatureVector);
    	mergeCostList(fn_trainIndex, fn_trainFeature, fn_modelWeight, C, C_contrast, mu);
    	
    	
    	
    	// train model
    	String[] svmTrainParams = new String[] {Storage_Controller.getLibSVMPath(),
    			fn_trainFeature, fn_model, fn_modelWeight};
//    	LibSVMPredictor libSVM = new LibSVMPredictor();
    	LibLinearPredictor libSVM = new LibLinearPredictor();
    	libSVM.train(svmTrainParams);
    	libSVM.saveFeatureWeight(fn_model, fn_globalFeatureVector, fn_featureWeight,
    			includeBiasFeature);
    }
    
    public FeatureVector getFVFromDocIDList(List<String> docIDList,
    		String fn_globalFeatureVector) throws Exception {
    	FeatureSetNGram featureSet = FeatureSetNGram.createFeatureSetNGram();
    	String dataFolder = Storage_Controller.getDocsFolder();
    	String instanceID;
    	String[] instanceTextList = new String[2];
    	
    	for(int i = 0; i < docIDList.size(); i++) {
    		instanceID = docIDList.get(i);
    		String fileName = Util.getOSPath(new String[] {dataFolder, instanceID,
                Storage_Controller.getColonoscopyReportFn()});
            
            // the first string is colonocopy report
            instanceTextList[0] = Util.loadTextFile(fileName);
            // remove header and footer
            instanceTextList[0] = Preprocess.separateReportHeaderFooter(
                    instanceTextList[0])[1];
            
            fileName = Util.getOSPath(new String[] {dataFolder, instanceID,
                Storage_Controller.getPathologyReportFn()});
            if(Util.fileExists(fileName)) {
                instanceTextList[1] = Util.loadTextFile(fileName);
                // remove header and footer
                instanceTextList[1] = Preprocess.separatePathologyHeaderFooter(
                		instanceTextList[1])[1];
            }
            else {
                instanceTextList[1] = "";
            }
            
            featureSet.addInstance(instanceID, instanceTextList,
            		reportType);
    	}
    	String[] globalFeatureVector = Util.loadList(fn_globalFeatureVector);
    	return featureSet.getFeatureVectorFromGlobalFeatureVector(globalFeatureVector);
    }
}
