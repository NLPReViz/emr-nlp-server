/**
 * 
 */
package edu.pitt.cs.nih.backend.feedback;

import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.pitt.cs.nih.backend.featureVector.ColonoscopyDS_SVMLightFormat;
import edu.pitt.cs.nih.backend.featureVector.Preprocess;
import edu.pitt.cs.nih.backend.utils.TextUtil;
import edu.pitt.cs.nih.backend.utils.Util;
import frontEnd.serverSide.controller.Storage_Controller;
import frontEnd.serverSide.model.FeedbackSpan_Model;
import frontEnd.serverSide.model.FeedbackSpan_WordTree_Model;
import frontEnd.serverSide.model.Feedback_Abstract_Model;
import frontEnd.serverSide.model.Feedback_Document_Model;

/**
 * Handle WordTree feedback. Note that WordTree feedback includes highlight spans and spans created from Word Tree.
 * This is the feedback version used in Summer 2014
 * 
 * @author Phuong Pham
 *
 */
public class TextFileFeedbackManager_LibSVM_WordTree extends TextFileFeedbackManagerLibSVM {

	protected String fn_wordTreeFeedback;
	protected String inferredKeyword = "inferred";
	
	/**
	 * @param feedbackFileName
	 * @param fn_sessionManager
	 * @param _learningFolder
	 * @param _docsFolder
	 * @param _modelFolder
	 * @param _featureWeightFolder
	 * @param _globalFeatureName
	 * @param _xmlPredictorFolder
	 */
	public TextFileFeedbackManager_LibSVM_WordTree(String feedbackFileName,
			String fn_sessionManager, String _learningFolder,
			String _docsFolder, String _modelFolder,
			String _featureWeightFolder, String _globalFeatureName,
			String _xmlPredictorFolder, String _fn_wordTreeFeedback) {
		super(feedbackFileName, fn_sessionManager, _learningFolder, _docsFolder,
				_modelFolder, _featureWeightFolder, _globalFeatureName,
				_xmlPredictorFolder);
		fn_wordTreeFeedback = _fn_wordTreeFeedback;
	}

	/* Modify the saveFeedbackBatch function to create stand off wordTree annotation
	 * @see edu.pitt.cs.nih.backend.feedback.TextFileFeedbackManagerLibSVM#processFeedback(java.util.List)
	 */
	@Override
	public String processFeedback(List<Feedback_Abstract_Model> batch) throws Exception {
		String feedbackMsg = "";
		try {
			// save Feedback_Abstract_Model objects in wordtree-feedback.txt
			// then convert feedbacks in wordtree-feedback.txt into 
			// final feedback format in feedback.txt for the new sessionID
			saveFeedbackBatch(batch);
			// from feedback.txt, extract all instance / span feedback 
			// of previous to current feedback of the userID
			// independent of previous session
			// note document level feedback will take the latest label value 
			// if there are conflictions
			// only span feedback having the same latest document label value
			// will be used in this learning
			createLearningFiles();
			// generate models and weights directly from learning files
			updateModels();
			// get the current sessionIDs of all varID
			// the modelList ID is the max sessionID
			feedbackMsg = createXMLPredictorFile();
			feedbackMsg = feedbackMsg.substring(feedbackMsg.lastIndexOf("modelList."),
					feedbackMsg.lastIndexOf(".")); 
		}
		catch(Exception e) {
			feedbackMsg = e.getMessage();
		}
		
		System.out.println(feedbackMsg);
		
		return feedbackMsg;
	}
	
	@Override
	public void saveFeedbackBatch(List<Feedback_Abstract_Model> feedbackBatch)
			throws Exception {
		// validate feedback list
		// structure Map<reportID, Map<value, List<Map<String,String>> text spans (selected, matched)>>
		// where selected is the skipped n-gram selected from the word tree
		// matched is the real span that matched in the text
		// from selected and matched, we can get the skipped n-gram patterns (skipped position, n words to be skipped)
		validateFeedbackBatch(feedbackBatch);		
		// save wordTree stand off annotation
		String sessionID = saveWordTreeAnnotationFile(feedbackBatch);

		// convert wordTree stand off annotation to final annotation and update the session manager file
		// it seems to be redundant when converting word tree annotation from file to 
		// however, this make sure we have a stand off annotation and 
		// later annotation types can be turned into the final annotation
		convertWordTreeAnnotation2FinalAnnotation(sessionID);
	}

	/**
	 * Group all feedback into a data structure and verify if feedback 
	 * violate rationale approach.
	 * Highlight text spans explaining why a document was classified as it is
	 * 
	 * @param feedbackBatch
	 * @return
	 * @throws Exception
	 */
	protected Map<String, Map<String, Map<String, List<Map<String,String>>>>> validateFeedbackBatch(
			List<Feedback_Abstract_Model> feedbackBatch) throws Exception {
		// structure Map<varID, Map<reportID, Map<value, List<Map<String,String>> selected, matched spans>>
		Map<String, Map<String, Map<String, List<Map<String,String>>>>> feedbackMap = new HashMap<>();
		for (Feedback_Abstract_Model abstractFeedback : feedbackBatch) {
			if (abstractFeedback instanceof FeedbackSpan_WordTree_Model) { // span level feedback
				FeedbackSpan_WordTree_Model feedback = 
						(FeedbackSpan_WordTree_Model) abstractFeedback;
				// scan through all docIDs
				for(String docID : feedback.getReportIDList()) {
					if(!feedbackMap.containsKey(feedback.getVariableName())) {
						Map<String, Map<String, List<Map<String,String>>>> varMap = new HashMap<>();
						feedbackMap.put(feedback.getVariableName(), varMap);
					}
					
					if (feedbackMap.get(feedback.getVariableName()).containsKey(docID)) {
						// verify document value with span value
						if (!feedbackMap.get(feedback.getVariableName())
								.get(docID).containsKey(
								feedback.getDocValue())) { // conflict
							String span1 = "span \""
									+ feedback.getMatchedTextSpan() + "\" (" + 
									feedback.getDocValue() + ")";

							String documentValue = feedback.getDocValue().equals(
									"True") ? "False" : "True";
							String span2 = feedbackMap.get(feedback.getVariableName())
									.get(docID).get(documentValue).size() > 0 ? 
											"span \"" + feedbackMap.get(feedback.getVariableName())
											.get(docID).get(documentValue).get(0).get("selected") + "\" (" +
									 documentValue + ")"
									: "the document (" + documentValue + ")";

							throw new Exception("Error: Cannot set '" + feedback.getVariableName() + 
									"' to be both True and False (using '" 
									+ span1 + "' and '" + span2 + "') in Doc #" + docID + "!");
						} else { // append the text span
							// create selected, matched spans for this feedback
							Map<String,String> spanMap = new HashMap<>();
							spanMap.put("selected", feedback.getSelectedTextSpan());
							spanMap.put("matched", feedback.getMatchedTextSpan());
							feedbackMap.get(feedback.getVariableName())
									.get(docID)
									.get(feedback.getDocValue())
									.add(spanMap);
						}
					} else { // create a new document level feedback agrees with
								// this span level feedback
						Map<String, List<Map<String,String>>> textSpanMap = new HashMap<>();
						List<Map<String,String>> textSpanList = new ArrayList<>();
						// append the text span
						Map<String,String> spanMap = new HashMap<>();
						spanMap.put("selected", feedback.getSelectedTextSpan());
						spanMap.put("matched", feedback.getMatchedTextSpan());
						textSpanList.add(spanMap);
						textSpanMap.put(feedback.getDocValue(), textSpanList);
						feedbackMap.get(feedback.getVariableName()).put(docID, textSpanMap);
					}
				}
				
			} else { // document level feedback
				Feedback_Document_Model feedback = 
						(Feedback_Document_Model) abstractFeedback;
				if(feedbackMap.containsKey(feedback.getVariableName()) &&
						feedbackMap.get(feedback.getVariableName()).containsKey(feedback.getDocId())) {
					// verify conflict
					if(!feedbackMap.get(feedback.getVariableName()).get(feedback.getDocId())
							.containsKey(feedback.getDocValue())) {
						throw new Exception("Error: Cannot set '"
								+ feedback.getVariableName() + "' to be both True and False in Doc #" 
								+ feedback.getDocId() + "!");
					}
				} else {
					if(!feedbackMap.containsKey(feedback.getVariableName())) {
						Map<String, Map<String, List<Map<String,String>>>> varMap = new HashMap<>();
						feedbackMap.put(feedback.getVariableName(), varMap);
					}

					Map<String, List<Map<String,String>>> textSpanMap = new HashMap<>();
					List<Map<String,String>> textSpanList = new ArrayList<>();
					textSpanMap.put(feedback.getDocValue(), textSpanList);
					
					feedbackMap.get(feedback.getVariableName()).put(feedback.getDocId(), textSpanMap);
				}
			}
		}

		return feedbackMap;
	}
	
	/**
	 * Save all feedback as wordTree annotation format
	 * <lu>
	 * <li> lineID, sessionID, userID, requestID, docID, varID, spanStart, spanEnd, change/create, pointer to old var value (lineID), new value
	 * <li> lineID, sessionID, userID, requestID, varID, value, "selected span" (normalized), "matched span" (normalized), docID list 
	 * </lu>
	 * @param feedbackMap
	 * @throws Exception
	 */
	protected String saveWordTreeAnnotationFile(
			List<Feedback_Abstract_Model> feedbackBatch) throws Exception {
		
		String sessionID = sessionManager.getNewSessionID();
		
		int lineID = getFeedbackLineID(fn_wordTreeFeedback); 
		StringBuilder sb = new StringBuilder(Util.loadTextFile(fn_wordTreeFeedback));
		
		for(Feedback_Abstract_Model abstractFeedback : feedbackBatch) {
			// common info
			sb.append(++lineID).append(",");
			sb.append(sessionID).append(",");
			sb.append(userID).append(",");
			sb.append(Feedback_Abstract_Model.getRequestID()).append(",");
			
			if(abstractFeedback instanceof Feedback_Document_Model) { // document level feedback
				Feedback_Document_Model documentFeedback = (Feedback_Document_Model) abstractFeedback;
				sb.append(documentFeedback.getDocId()).append(",");
				sb.append(documentFeedback.getVariableName()).append(",");
				sb.append("0,0,"); // spanStart, spanEnd; 0,0 means doc level feedback while -1,-1 means inferred feedback
				sb.append("create,-1,"); // change/create, pointer to old var value
				sb.append(documentFeedback.getDocValue()).append("\n");
			}
			else if(abstractFeedback instanceof FeedbackSpan_WordTree_Model) { // span level feedback
				FeedbackSpan_WordTree_Model wordTreeFeedback = (FeedbackSpan_WordTree_Model) abstractFeedback;
				sb.append(wordTreeFeedback.getVariableName()).append(",");
				sb.append(wordTreeFeedback.getDocValue()).append(",");
				sb.append(FeedbackSpan_WordTree_Model.normalizeTextSpan(wordTreeFeedback.getSelectedTextSpan())).append(",");
				sb.append(FeedbackSpan_WordTree_Model.normalizeTextSpan(wordTreeFeedback.getMatchedTextSpan())).append(",");
				sb.append(Util.joinString(wordTreeFeedback.getReportIDList().toArray(
						new String[wordTreeFeedback.getReportIDList().size()]), ",")).append("\n");
			}
		}
		
		Util.saveTextFile(fn_wordTreeFeedback, sb.toString());
		
		return sessionID;
	}
	
	protected int getFeedbackLineID(String fn_feedbackFile) throws Exception {
		String[][] feedbackTable = Util.loadTable(fn_feedbackFile);
		int lineID = 0;
		if(feedbackTable.length > 0) {
			lineID = Integer.parseInt(feedbackTable[feedbackTable.length - 1][0]);
		}
		
		return lineID;
	}
	
	/**
	 * Convert word tree annotation into final annotation.
	 * 
	 * @param sessionID
	 * @throws Exception
	 */
	protected void convertWordTreeAnnotation2FinalAnnotation(String sessionID) throws Exception {		
		try {
			String[][] feedbackTable = Util.loadTable(fn_wordTreeFeedback);
			// extract all feedback in this session
			// Map<varID, Map<reportID, Map<value, List<String> text spans>>
			Map<String, Map<String, Map<String, List<Map<String,String>>>>> feedbackMap = 
					extractWordTreeAnnotation2Map(sessionID, feedbackTable);
			// verify conflicting label values between
			// the feedback session and existing data before create
			// final annotation form
			verifyConflictingLabel(feedbackMap, sessionID);
			// from the structure, convert into final annotation
			convert2FinalAnnotationFormat(feedbackMap, sessionID);
		}
		catch(Exception e) {
			// can't convert into final feedback, roll back saved wordtree feedback
			rollBackWordTreeFeedback(sessionID);
			throw e;
		}
	}

	protected void verifyConflictingLabel(
			Map<String, Map<String, Map<String, List<Map<String, String>>>>> feedbackMap,
			String sessionID) throws Exception {
		Map<String, Map<String, String>> labelMap = new ColonoscopyDS_SVMLightFormat()
				.getAllDocumentLabel(sessionID, userID, fn_feedback);
		Map<String, String> varLabelMap;
		
		// error msg contains all possible warnings
		StringBuilder errorMsg = new StringBuilder();

		// Map<varID, Map<reportID, Map<value, List<String> text spans>>
		for (String varID : feedbackMap.keySet()) {
			if (labelMap.containsKey(varID)) {
				varLabelMap = labelMap.get(varID);

				Map<String, Map<String, List<Map<String, String>>>> reportFeedbackMap = feedbackMap
						.get(varID);
				for (String reportID : reportFeedbackMap.keySet()) {
					// verify conflict
					if (varLabelMap.containsKey(reportID)
							&& !reportFeedbackMap.get(reportID).containsKey(
									varLabelMap.get(reportID))) {
//						// raise warning the first encountered conflict
//						throw new Exception(
//								"Warning: Report "
//										+ reportID
//										+ " in variable "
//										+ varID
//										+ " contains contradictory label value compared to existing training data");
						// accumulate all warnings (conflicts)
						errorMsg.append("Value for '")
								.append(varID)
								.append("' contradicts previous feedback in Doc #")
								.append(reportID)
								.append(".\n");
					}
				}
			}
		}
		
		// if there are warnings, raise an Exception
		if(errorMsg.length() > 0) {
			throw new Exception (errorMsg.insert(0, "Warning: ").toString());
		}
	}
	
	/**
	 * If there is an exception during saving feedback
	 * roll back the wordtree format feedback after it was saved
	 *  
	 * @param sessionID
	 * @throws Exception
	 */
	protected void rollBackWordTreeFeedback(String sessionID) throws Exception {
		String[][] feedbackTable = Util.loadTable(fn_wordTreeFeedback);
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < feedbackTable.length; i++) {
			if(feedbackTable[i][1].equals(sessionID)) {
				break; // roll back the latest session ID, therefore we stop whenever meet it
			}
			sb.append(Util.joinString(feedbackTable[i], ",")).append("\n");
		}
		
		Util.saveTextFile(fn_wordTreeFeedback, sb.toString());
	}
	
	/**
	 * Convert word tree annotation format into data structure Map<varID, Map<reportID, Map<value, List<String> text spans>> 
	 * each report branch (Map<reportID, Map<value, List<String> text spans>>) contains the report value and a pseudo key
	 * "inferred" if the report value is inferred from a text span, not explicitly given by the user
	 * 
	 * <lu>
	 * <li> lineID, sessionID, userID, requestID, docID, varID, spanStart, spanEnd, change/create, pointer to old var value (lineID), new value
	 * <li> lineID, sessionID, userID, requestID, varID, value, "text span" (normalized), docID list 
	 * </lu>
	 * 
	 * @param feedbackMap
	 * @throws Exception
	 */
	protected Map<String, Map<String, Map<String, List<Map<String,String>>>>> extractWordTreeAnnotation2Map(String sessionID,
			String[][] feedbackTable) throws Exception {
		Map<String, Map<String, Map<String, List<Map<String,String>>>>> feedbackMap = new HashMap<>();

		for(int i = feedbackTable.length - 1; i > -1; i--) {
			if(feedbackTable[i][1].equals(sessionID) &&
					feedbackTable[i][2].equals(userID)) {
				if(feedbackTable[i][6].equals("0") &&
						feedbackTable[i][7].equals("0")) { // document level feedback
					if(!feedbackMap.containsKey(feedbackTable[i][5])) { // 5: varID
						Map<String, Map<String, List<Map<String,String>>>> varMap = new HashMap<>();
						feedbackMap.put(feedbackTable[i][5], varMap);
					}
					
					if(feedbackMap.get(feedbackTable[i][5]) // is there a label value for this report?
							.containsKey(feedbackTable[i][4])) { // 4: docID; verify class value
						// verify conflict between the current label value with the label value of this feedback
						if (!feedbackMap.get(feedbackTable[i][5])
								.get(feedbackTable[i][4]).containsKey(
								feedbackTable[i][10])
							) { // 10: class value
							
//							throw new Exception("Error: Report " + feedbackTable[i][4] +
//									" for variable '" + feedbackTable[i][5] +
//									"' contains contradictory feedback! (found in converting process)");
							throw new Exception("Error: Cannot set'" + feedbackTable[i][5] + 
									"' to be both True and False (inferred from text-span) in '" + 
									" in Doc #" + feedbackTable[i][4]);
						}
						// if the current report value is inferred, then make it explicitly
						// because the user said so
						feedbackMap.get(feedbackTable[i][5])
							.get(feedbackTable[i][4]).remove(inferredKeyword);
					}
					else { // add this document level feedback
						Map<String, List<Map<String,String>>> textSpanMap = new HashMap<>();
						List<Map<String,String>> textSpanList = new ArrayList<>();
						textSpanMap.put(feedbackTable[i][10], textSpanList);
						feedbackMap.get(feedbackTable[i][5])
							.put(feedbackTable[i][4], textSpanMap);
						// explicit document label feedback, no need to create the key "inferred"
					}
				}
				else { // span level feedback
					String classValue = feedbackTable[i][5];
					if(!feedbackMap.containsKey(feedbackTable[i][4])) { // 4: varID
						Map<String, Map<String, List<Map<String,String>>>> varMap = new HashMap<>();
						feedbackMap.put(feedbackTable[i][4], varMap);
					}
					
					for(int j = 8; j < feedbackTable[i].length; j++) {
						String docID = feedbackTable[i][j];						
						if (feedbackMap.get(feedbackTable[i][4]).containsKey(docID)) {
							// verify document value with span value
							if (!feedbackMap.get(feedbackTable[i][4])
									.get(docID).containsKey(classValue)) { // conflict
								String span1 = "span \""
										+ feedbackTable[i][6] + "\" (" + // 6: text span 
										classValue + ")";

								String documentValue = classValue.equals(
										"True") ? "False" : "True";
								String span2 = feedbackMap.get(feedbackTable[i][4])
										.get(docID).get(documentValue).size() > 0 ?
												"span \"" + feedbackMap.get(feedbackTable[i][4])
												.get(docID).get(documentValue).get(0).get("selected") + "\" (" +
										 documentValue + ")"
										: "the document (" + documentValue + ")";

//								throw new Exception("Error: In report " + docID
//										+ " in variable " + feedbackTable[i][4] 
//										+ ", \n" + span1 + "\nand " + span2
//										+ "\n have different values! (found in converting process)");
								
								throw new Exception("Error: Cannot set'" + feedbackTable[i][4] + 
										"' to be both True and False (inferred from '" +
										span1 +"' and '"+ span2 +"') in '" + 
										" in Doc #" + docID);
							} else { // append the text span
								Map<String,String> spanMap = new HashMap<>();
								spanMap.put("selected", FeedbackSpan_WordTree_Model
										.deNormalizeTextSpan(feedbackTable[i][6]));
								spanMap.put("matched", FeedbackSpan_WordTree_Model
										.deNormalizeTextSpan(feedbackTable[i][7]));
								feedbackMap.get(feedbackTable[i][4])
										.get(docID)
										.get(classValue)
										.add(spanMap);
							}
						} else { // create a new document level feedback agrees with
									// this span level feedback
							Map<String, List<Map<String,String>>> textSpanMap = new HashMap<>();
							List<Map<String,String>> textSpanList = new ArrayList<>();
							// append the text span
							Map<String,String> spanMap = new HashMap<>();
							spanMap.put("selected", FeedbackSpan_WordTree_Model
									.deNormalizeTextSpan(feedbackTable[i][6]));
							spanMap.put("matched", FeedbackSpan_WordTree_Model
									.deNormalizeTextSpan(feedbackTable[i][7]));
							textSpanList.add(spanMap);
							textSpanMap.put(classValue, textSpanList);
							// add "inferred" key because the report value is inferred from this span feedback
							textSpanMap.put(inferredKeyword, null);
							feedbackMap.get(feedbackTable[i][4]).put(docID, textSpanMap);
						}
					}
				}
			}
			else if(feedbackMap.size() > 0) { // extracted all feedback from the sessionID
				break;
			}
		}
		
		return feedbackMap;
	}
	
	/**
	 * From data structure Map<varID, Map<reportID, Map<value, List<String> text spans>> to final annotation format 
	 * <lu>
	 * <li> lineID, sessionID, userID, requestID, docID, varID, spanStart, spanEnd, change/create, pointer to old var value (lineID), new value 
	 * <li> lineID, sessionID, userID, requestID, docID, varID, spanStart, spanEnd, add/remove, value 
	 * </lu>
	 * Text spans will be converted into start, end position in the document.
	 * <p>
	 * If there are multi-matches, use the first match's positions
	 * <p>
	 * A document may contain both colonoscopy report and pathology report. If the text span comes from pathology report,
	 * the start, end positions will be offset by the length of the colonoscopy report (imagine the pathology report is appended 
	 * after the colonoscopy report)
	 * 
	 * @param feedbackMap
	 * @throws Exception
	 */
	protected void convert2FinalAnnotationFormat(
			Map<String, Map<String, Map<String, List<Map<String,String>>>>> feedbackMap,
			String sessionID) throws Exception {
		StringBuilder feedbackLine = new StringBuilder(Util.loadTextFile(fn_feedback));
		Map<String, Map<String, List<Map<String,String>>>> varMap;
		Map<String, List<Map<String,String>>> docMap;
		List<Map<String,String>> spanList;
		Map<String,String> spanMap;
		String docValue, spanPosition;
		int lineID = getFeedbackLineID(fn_feedback);
		String[][] feedbackTable = Util.loadTable(fn_feedback);

		ArrayList<String[]> sessionAddList = new ArrayList<>();
		
		for(String varID : feedbackMap.keySet()) {
			// update the session manager
			addNewFeedbackSessionItem(userID, varID, sessionAddList);
			// update the feedback manager
			varMap = feedbackMap.get(varID);
			
			for(String docID : varMap.keySet()) {
				docMap = varMap.get(docID);
				docValue = docMap.containsKey("True") ? "True" : "False";
				
				// save the document level feedback
				feedbackLine.append(++lineID).append(",");
				feedbackLine.append(sessionID).append(",");
				feedbackLine.append(userID).append(",");
				feedbackLine.append(Feedback_Abstract_Model.getRequestID()).append(",");
				feedbackLine.append(docID).append(",");
				feedbackLine.append(varID).append(",");
				String position = docMap.containsKey(inferredKeyword) ?
						"-1,-1," : // start, end = -1, -1 for inferred document level label
						"0,0,"; // start, end = 0, 0 for explicit document level label
				
				feedbackLine.append(position);
				// change/create, old variable value line ID(pointer)
		        String varValueType = "create";
		        String oldVarValueLineID = "-1";
		        for(int i = feedbackTable.length - 1; i >= 0; i--) {
		            String[] feedbackRow = feedbackTable[i];
		            // if userID = "" then we use the latest variable value
		            if((TextFileFeedbackManager.getFeedbackType(feedbackRow) == TextFileFeedbackManager.FeedbackType.VariableValue) &&
		                    (feedbackRow[2].equals("") || userID.equals(feedbackRow[2])) && 
		                    feedbackRow[4].equals(docID) && 
		                    feedbackRow[5].equals(varID)) {
		                oldVarValueLineID = feedbackRow[0];
		                varValueType = "change";
		                break;
		            }
		        }
		        feedbackLine.append(varValueType).append(",").append(oldVarValueLineID).append(",");
		        feedbackLine.append(docValue).append("\n");
		        
		        // save all spans
		        spanList = docMap.get(docValue);
		        // modify at this point to specific how many span feedback will be used
//		        for(int i = 0; i < 1; i++) {
		        for(int i = 0; i < spanList.size(); i++) {
		        	spanMap = spanList.get(i);
		        	// search docID for occurrence
		        	// if we can't find the span, an Exception will be
		        	// thrown, no need to check here
		        	spanPosition = getStartEndPosition(docID, spanMap);
		        	
		        	// write the feedback line
		        	feedbackLine.append(++lineID).append(",");
					feedbackLine.append(sessionID).append(",");
					feedbackLine.append(userID).append(",");
					feedbackLine.append(Feedback_Abstract_Model.getRequestID()).append(",");
					feedbackLine.append(docID).append(",");
					feedbackLine.append(varID).append(",");
					feedbackLine.append(spanPosition).append(",");
					feedbackLine.append("add,");
					feedbackLine.append(docValue).append("\n");
		        }
			}
		}
		
		// update the session manager, and de-active previous session of modified variables
        for(String[] sessionAddItem : sessionAddList) {
            sessionManager.addSessionLine(sessionID, sessionAddItem[0], sessionAddItem[1]);
        }
		// save the final annotation file
		Util.saveTextFile(fn_feedback, feedbackLine.toString());
	}
	
	/**
	 * Search docID for start, end positions of the text span.
	 * Search in both colonoscopy and pathology reports, return the first match.
	 * <p>
	 * If the match is in pathology report, offset start, end positions by length of the 
	 * colonoscopy report
	 * 
	 * @param docID
	 * @param spanText
	 * @return
	 * @throws Exception
	 */
	protected String getStartEndPosition(String docID, Map<String,String> spanMap) throws Exception {
		String startEndPos = "0,0";
		String fn_report = Storage_Controller.getColonoscopyReportFn();
		String fn_pathology = Storage_Controller.getPathologyReportFn();
		String docText;
		Pattern pattern = getSearchPatternFromSpanMap(spanMap);
		Matcher m;
		// search text in colonoscopy text, remove header footer
//		docText = Preprocess.separateReportHeaderFooter( 
//				Util.loadTextFile(Util.getOSPath(
//						new String[]{docsFolder, docID, fn_report})))[1];
		// no remove header footer, find a counter code in class ColonoscopyDS_SVMLightFormat
		docText = Util.loadTextFile(Util.getOSPath(
						new String[]{docsFolder, docID, fn_report}));
		m = pattern.matcher(docText);		
		if(m.find()) {
			startEndPos = Integer.toString(m.start()) + "," 
					+ Integer.toString(m.end());
		}
		else { // look into pathology text, remove header footer
			fn_pathology =  Util.getOSPath(
							new String[]{docsFolder, docID, fn_pathology});
			if(Util.fileExists(fn_pathology)) {
				// get the colonoscopy report length before
				// overwrite the docText
				int offset = docText.length();
//				// remove header footer
//				docText = Preprocess.separatePathologyHeaderFooter(Util.loadTextFile(fn_pathology))[1];
				// no remove header footer
				docText = Preprocess.separatePathologyHeaderFooter(Util.loadTextFile(fn_pathology))[1];
				m = pattern.matcher(docText);
				if(m.find()) {					
					startEndPos = Integer.toString(m.start() + offset) + "," 
							+ Integer.toString(m.end() + offset);
				}
				else { // can't find the span in pathology report
					throw new Exception("Error: Cannot find \"" + spanMap.get("selected") +
							"\" in Doc #" + docID);
				}
			}
			else {
				throw new Exception("Error: Cannot find \"" + spanMap.get("selected") +
						"\" in Doc #" + docID);
			}
		}

		return startEndPos;
	}

	/**
	 * This is an important function.
	 * Create the search pattern to get start,end position of a (skipped n-gram) span from text
	 * 
	 * @param spanMap
	 * @return
	 * @throws Exception
	 */
	protected Pattern getSearchPatternFromSpanMap(Map<String, String> spanMap)
			throws Exception {		
//		String patternStr = matchedPatternString(spanMap);
		String patternStr = wordTreeSkippedNGramPatternString(spanMap);		
		// create another function to get skipped n-gram span from feedback
		// wait for Gaurav's diff function
		return Pattern.compile(patternStr, Pattern.DOTALL
				| Pattern.CASE_INSENSITIVE);
	}

	/**
	 * A simple pattern string.
	 * Get the matched span and reverse it according to the 
	 * 
	 * @param spanMap
	 * @return
	 * @throws Exception
	 */
	protected String matchedPatternString(Map<String, String> spanMap)
			throws Exception {
		String spanText = spanMap.get("matched");
		// convert spanText into regular expression
		String whiteSpaceBeforePunc = " (?=[.,!?;])";
		String patternStr;
		// remove white space before punctuation if any
		patternStr = spanText.replaceAll(whiteSpaceBeforePunc, "");
		// quote the string
		patternStr = TextUtil.escapeRegex(patternStr);
		// reverse 's
		patternStr = patternStr.replaceAll("'s", "' {0,1}s");
		// replace whitespace by \s
		patternStr = patternStr.replaceAll("\\s", "\\\\s+");

		return patternStr;
	}
	
	/**
	 * Create skipped n-gram pattern string from selected, matched strings.
	 * These strings come from wordtree control. They are tokenized and 
	 * separated by whitespace. Refer to wordtree tokenizing function 
	 * for more detail.
	 * 
	 * This function does a greedy search
	 * 
	 * @param spanMap
	 * @return
	 * @throws Exception
	 */
	public String wordTreeSkippedNGramPatternString(Map<String, String> spanMap)
			throws Exception {
		String[] selectedTokenList = TextUtil.escapeRegex(spanMap.get("selected")).split(" ");
		String[] matchedTokenList = TextUtil.escapeRegex(spanMap.get("matched")).split(" ");
		
		
		StringBuilder sb = new StringBuilder();
		// matchedTokenList.length >= selectedTokenList.length
		int skippedN = 0;
		int iSelected = 0;
		for(int iMatched = 0; iMatched < matchedTokenList.length; iMatched++) {
			if(iSelected < selectedTokenList.length) {
				if (matchedTokenList[iMatched]
						.equals(selectedTokenList[iSelected])) {
					if (skippedN == 0) {
						sb.append(matchedTokenList[iMatched]).append(" ");
						iSelected++;
					} else { // put skipped ngram pattern skipped pattern \\bword\\b (\\S+ ){0,n}\\bword
						if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
							sb.append(" ");
						}
						sb.append("(\\S+ ){0,").append(skippedN).append("}");
						sb.append(matchedTokenList[iMatched]).append(" "); // append the current position
						skippedN = 0;
						iSelected++;
					}
				} else {
					skippedN++;
				}
			}
			else { // the rest of matched n-gram will be skipped ngram, append skipped ngram pattern
				if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
					sb.append(" ");
				}
				sb.append("(\\S+ ){0,1}");
				break;
			}
		}
		// convert spanText into regular expression
		String whiteSpaceBeforePunc = " (?=[.,!?;])";
		String patternStr;
		// remove white space before punctuation if any
		patternStr = sb.toString().trim().replaceAll(whiteSpaceBeforePunc, "\\\\s{0,1}");
		// in case the first skipped n-gram is a punctuation
		// there would be no white space before the n-gram
		patternStr = patternStr.replaceAll(" (?=(\\(\\\\S\\+))", "\\\\s{0,1}");
//		// quote the string
//		patternStr = TextUtil.escapeRegex(patternStr);
		// reverse 's
		patternStr = patternStr.replaceAll("'s", "' {0,1}s");
		// replace whitespace by \s
		patternStr = patternStr.replaceAll("\\s(?!\\{)", "\\\\s+");

//		System.out.println("Search pattern: " + patternStr);
		
		return patternStr;
	}
}
