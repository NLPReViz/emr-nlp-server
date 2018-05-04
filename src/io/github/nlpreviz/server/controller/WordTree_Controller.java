/**
 * 
 */
package io.github.nlpreviz.server.controller;

import io.github.nlpreviz.nlp.utils.TextUtil;
import io.github.nlpreviz.nlp.utils.Util;
import io.github.nlpreviz.nlp.utils.XMLUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Phuong Pham
 *
 */
public class WordTree_Controller {
	private String m_docsFolder;
	private String m_documentListFolder;
	
	public WordTree_Controller() {
		// initialize global feature vector
		try {			
			m_docsFolder = Storage_Controller.getDocsFolder();
			m_documentListFolder = Storage_Controller.getDocumentListFolder();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Map<String, Object> getWordTree(String fn_reportIDList, String rootWord) throws Exception {
		// get reportID list
		List<String> reportIDList = XMLUtil
				.getReportIDFromXMLList(Util.getOSPath(new String[] {
						m_documentListFolder, fn_reportIDList }));
//		System.out.println(String.format("Filename=" + fn_reportIDList + ", %d reports", reportIDList.size()));
		
		return getWordTree(reportIDList, rootWord);
	}
	
	public Map<String, Object> getWordTree(List<String> reportIDList, String rootWord) throws Exception {
		String reportID, reportText;
		List<Map<String, Object>> leftList = new ArrayList<>();
		List<Map<String, Object>> rightList = new ArrayList<>();

		String searchText = rootWord.replaceAll(" ", "\\\\s+");
//		Pattern sentencePattern = Pattern.compile("([^.:]*?" + rootWord + "[^.\n]*\\.)");
		System.out.println("New search is: " + searchText);

		Pattern sentencePattern = Pattern.compile(" ([^.:]*?\\b" + searchText + "\\b[^\n.?!]*)", Pattern.CASE_INSENSITIVE);
//		Pattern sentencePattern = Pattern.compile("([^.:]*?" + rootWord + "[^.\n]*\\.)");
		
		Pattern tokenPattern = Pattern.compile("[\\w']+|[.,!?;]");
		
		
		int matchCount = 0;
		List<String> matchedList = new ArrayList<>();
		
//		int docCount = 0;
		for (int i = 0; i < reportIDList.size(); i++) {
			reportID = reportIDList.get(i);

			// find within the colonoscopy report
			reportText = Util.loadTextFile(Util.getOSPath(new String[] {
					m_docsFolder, reportID,
					Storage_Controller.getColonoscopyReportFn() }));
			// use heuristic merging sentences
			reportText = TextUtil.reconstructSentences(reportText);
			int oldCount = matchCount;
			matchCount = parseWordTree(reportText, sentencePattern,
					tokenPattern, leftList, rightList, reportID, searchText,
					matchCount);
//			docCount++;
			// find within the pathology report
			if (Util.fileExists(Util.getOSPath(new String[] { m_docsFolder,
					reportID, Storage_Controller.getPathologyReportFn() }))) {
				
				reportText = Util.loadTextFile(Util.getOSPath(new String[] {
						m_docsFolder, reportID,
						Storage_Controller.getPathologyReportFn() }));
				// use heuristic merging sentences
				reportText = TextUtil.reconstructSentences(reportText);
				matchCount = parseWordTree(reportText, sentencePattern,
						tokenPattern, leftList, rightList, reportID, searchText,
						matchCount);
//				docCount++;
			}
			
			if(matchCount > oldCount) {
				matchedList.add(reportID);
			}
		}
		
//		Map<String, Object> branchMap = new HashMap<>();
//		branchMap.put("lefts", leftList);
//		branchMap.put("rights", rightList);
		
		Map<String, Object> treeMap = new HashMap<>();
		treeMap.put("matches", matchCount);
		treeMap.put("matchedList", matchedList);
		treeMap.put("total", matchedList.size());
		treeMap.put("query", rootWord);
		treeMap.put("lefts", leftList);
		treeMap.put("rights", rightList);
		
		return treeMap;
	}

	protected int parseWordTree(String reportText, Pattern sentencePattern,
			Pattern tokenPattern,List<Map<String, Object>> leftList,
			List<Map<String, Object>> rightList, String reportID,
			String rootWord, int matchCount) throws Exception {
		String matchedSentence;
		Matcher sentenceMatch, branchMatch;
		Map<String, Object> matchedItem;
		List<String> tokenList;
		
		// preprocess
		reportText = reportText.replaceAll("\r\n", "\n");
		sentenceMatch = sentencePattern.matcher(reportText);
		rootWord = rootWord.toLowerCase();

		while (sentenceMatch.find()) {
			matchCount++;
			matchedSentence = sentenceMatch.group();
			matchedSentence = matchedSentence.replaceAll("\' s", "'s");
			matchedSentence = matchedSentence.replaceAll("\n", " ");
			matchedSentence = matchedSentence.replaceAll(" \t\n", "");

			if (matchedSentence.charAt(matchedSentence.length() - 1) == '.') {
				matchedSentence = matchedSentence.substring(0,
						matchedSentence.length() - 1);
			}

			matchedSentence = matchedSentence.toLowerCase();
			//System.out.println(matchedSentence);

			// left branch
			tokenList = new ArrayList<>();
			
			System.out.println(matchedSentence);
			Pattern pattern = Pattern.compile(rootWord);
			Matcher matcher = pattern.matcher(matchedSentence);

			Integer start = 0;
			Integer end = 0;

			if(matcher.find()){
				start = matcher.start();
				end = matcher.end();
			}

			branchMatch = tokenPattern.matcher(matchedSentence.substring(0,
					start).trim());
			while (branchMatch.find()) {
				tokenList.add(branchMatch.group());
			}

			matchedItem = new HashMap<>();
			matchedItem.put("doc", reportID);
			matchedItem.put("id", Integer.toString(matchCount));
			matchedItem.put("sentence", tokenList);
			leftList.add(matchedItem);

			// right branch
			tokenList = new ArrayList<>();
			branchMatch = tokenPattern.matcher(matchedSentence.substring(
					end).trim());

			while (branchMatch.find()) {
				tokenList.add(branchMatch.group());
			}
			// add . to the right list
			tokenList.add(".");
			matchedItem = new HashMap<>();
			matchedItem.put("doc", reportID);
			matchedItem.put("id", Integer.toString(matchCount));
			matchedItem.put("sentence", tokenList);
			rightList.add(matchedItem);
		}
		
		return matchCount;
	}
}
