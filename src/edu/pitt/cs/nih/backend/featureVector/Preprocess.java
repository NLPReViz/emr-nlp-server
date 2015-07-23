package edu.pitt.cs.nih.backend.featureVector;

import edu.pitt.cs.nih.backend.utils.TextUtil;
import edu.pitt.cs.nih.backend.utils.Util;
import edu.stanford.nlp.process.Morphology;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.HashMap;

//import edu.stanford.nlp.process.*;

public class Preprocess {
	protected boolean m_removeStopWord;
	protected boolean m_removePunctuation;
	protected boolean m_stemWord;
	protected boolean m_removeHeaderFooter;
	protected boolean m_caseSensitive;
        protected List<String> m_excludeStringList;
        protected List<String> m_stopWordList;
        
        public static Character[] exclude = {'!', '#', '$', '%', '&', '(', ')', '*', 
                    '+', ',', ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']',
                    '^', '_', '`', '{', '|', '}', '~', '"'};
        
        public static String[] excludeStringArray = {"!", "#", "$", "%", "&", "(", ")", "*", 
                    "+", ",", ":", ";", "<", "=", ">", "?", "@", "[", "\\", "]", "/", ".",
                    "^", "_", "`", "{", "|", "}", "~", "\"", "'", "-", // end of common punctuation
                    "--", "-LRB-", "-LSB-", "-RRB-", "-RSB-", "''",
                    "**", "``", "<<", ">>"};// based on the current dataset
        public static HashMap<String, String> stanfordParserModifiedCharList = null;
	
	public Preprocess(boolean removeStopWord, boolean removePunctuation,
			boolean stemWord, boolean removeHeaderFooter, boolean caseSensitive) {
		m_removeStopWord = removeStopWord;
		m_removePunctuation = removePunctuation;
		m_stemWord = stemWord;
		m_removeHeaderFooter = removeHeaderFooter;
		m_caseSensitive = caseSensitive;
                m_excludeStringList = new ArrayList<>(Arrays.asList(excludeStringArray));
                if(removeStopWord) {
                    try
                    {
                        String[] paths = {Util.getExecutingPath(), "data", "stop.en"};
                        String stopWordFilename = Util.getOSPath(paths);
                        m_stopWordList = new ArrayList<>(Arrays.asList(Util.loadList(stopWordFilename, " ")));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
	}
        
        public static HashMap<String, String> getStanfordModifiedCharList() {
            if(stanfordParserModifiedCharList == null) {
                stanfordParserModifiedCharList = new HashMap<>();
                stanfordParserModifiedCharList.put("-LRB-", "(");
                stanfordParserModifiedCharList.put("-LSB-", "(");
                stanfordParserModifiedCharList.put("-RRB-", ")");
                stanfordParserModifiedCharList.put("-RSB-", ")");
            }
            return stanfordParserModifiedCharList;
        }
	
//	public String process(String input, String reportType) throws Exception {
//		String output = input.trim();
//		if(m_removeHeaderFooter)
//			output = removeHeaderFooter(output, reportType);
//		if(m_removePunctuation)
//			output = removePunctuation(output);
//		else {
////			output = output.replace("\"", "");
////			output = output.replace(",", "");
//		}
//		if(m_removeStopWord)
//			output = removeStopWord(output);
////		if(m_stemWord)
////			output = stemWord(output);
//		if(!m_caseSensitive)
//			output = output.toLowerCase();
//		output = TextUtil.removeDuplicatedSpace(output);
//		
//		return output;
//	}
//	
//	private String removeStopWord(String input) throws Exception {
//		String[] paths = {Util.getExecutingPath(), "stop.en"};
//		String stopWordFilename = Util.getOSPath(paths);
//		String[] stopWordList = Util.loadList(stopWordFilename, " ");
////		String output = "";
////		String[] lines = input.split("\n");
////		for (int i = 0; i < lines.length; i++) {
////			String[] tokens = lines[i].trim().split("\\s");
////			for (int j = 0; j < tokens.length; j++) {
////				String token = tokens[j].trim();				
////				if(!stopWordList.contains(token)) {
////					output += token + " ";
////				}
////			}
////			output = output.trim() + "\n";
////		}
//		String pattern = "";
//		for(String stopWord : stopWordList) {
//			pattern += "(\\b" + stopWord + "\\b)|";
//		}
//		pattern = pattern.substring(0, pattern.length() - 1);
//		Pattern replacePattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
//		Matcher m = replacePattern.matcher(input);
//		String output = m.replaceAll("");
//		return output.trim();
//	}
//	
//	private String removePunctuation(String input) throws Exception {		
//		List<Character> excludeList = Arrays.asList(exclude);
//		String output = "";
//		for(char token : input.toCharArray()) {			
//			if(!excludeList.contains(token)) {
//				output += token;
//			}
//		}
//                output = output.replaceAll(" \\. ", "");
//                output = output.replaceAll(" \' ", "");
////		System.out.println(output);
////		String[] lines = output.trim().split("\n");
////		output = "";
//		// instead of tokenize words in each sentence and handle ..., we try to do this
//		String[] lines = output.split("\n");
//		output = "";
//		for(String line : lines) {
//			output += line.trim().replaceAll("\\.+$", "") + "\n";
//		}
//		return output;
//	}
//	
//	// we try this basic stemmer, not as goos as Porter
//        // move to TextUtil and word on a single word
//	public String stemWord(String input) throws Exception {
//		String output = "";	
//		String[] lines = input.split("\n");
//		Morphology morph = new Morphology();
//		
//		for(String line : lines){
//			String[] tokens = line.trim().split("\\s");
//			for(String token : tokens) {
//				output += morph.stem(token) + " ";
//			}
//			output = output.trim() + "\n";
//		}
//		return output;
//	}
//	
//	private String removeHeaderFooter(String input, String reportType) throws Exception {
//		String output;
//		if(reportType.equals("report")) {
//			output = removeReportHeaderFooter(input);
//		}
//		else {
//			output = removePathologyHeaderFooter(input);
//		}
//		return output;
//	}
//        
//        protected String removeReportHeaderFooter(String input) throws Exception {
//            // only get the content, skip header and footer
//            return separateReportHeaderFooter(input)[1];
//        }
	
        /**
         * Separate a colonoscopy report into 3 parts: header, content, and footer.
         * 
         * @param input
         * @return text[0]: header; text[1]: content; text[2]: footer
         * @throws Exception 
         */
	public static String[] separateReportHeaderFooter(String input) throws Exception {
		String[] text = new String[3];
                StringBuilder sb;
		String[] lines = input.split("\n");
		int allLines = lines.length;
		int iLine = 0;
		// header starts from the beginning to E_O_H
                sb = new StringBuilder();
		while(iLine < allLines && lines[iLine].indexOf("E_O_H") == -1) {
                        sb.append(lines[iLine].trim());
                        sb.append("\n");
			iLine++;
		}
                // append the E_O_H line
                sb.append(lines[iLine++]);
                sb.append("\n");
                // append the de-identifier signature
                sb.append(lines[iLine++]);
                // make the header
                text[0] = TextUtil.removeDuplicatedSpace(sb.toString().trim());
                
                // the content starts from here until meet E_O_R or the last ____________________
                sb = new StringBuilder();
		if(iLine < allLines - 1) {                        
			iLine++;
//			text += lines[iLine].trim() + "\n";
                        sb.append(lines[iLine].trim());
                        sb.append("\n");
		}
		else {
			iLine = -1;
		}                
		iLine++;
		while(iLine < allLines && lines[iLine].indexOf("E_O_R") == -1 &&
                        lines[iLine].indexOf("____________________") == -1) {
                    // skip any line that has **NAME
			if(!lines[iLine].trim().equals("") &&
                                lines[iLine].trim().indexOf("**NAME") == -1) {
//				text += lines[iLine].trim() + "\n";
                            sb.append(lines[iLine].trim());
                            sb.append("\n");
			}
			iLine++;
		}
                // make the content
                text[1] = TextUtil.removeDuplicatedSpace(sb.toString().trim());
		// remove stop words
		// and **ID-NUM
                // should use StringBuilder instead of String here
		text[1] = text[1].replaceAll("\\*\\*ID\\-NUM", "");
                text[1] = text[1].replaceAll("id\\-num", "");
		text[1] = text[1].replaceAll("\\*\\*\\S+", "");
		text[1] = text[1].replaceAll("={3,}", "");
		text[1] = text[1].replaceAll("_{3,}", "");
                
		// footer starts from here to the end
                sb = new StringBuilder();
                while(iLine < allLines) {
                    sb.append(lines[iLine++].trim());
                    sb.append("\n");
                }
                // make the footer
                text[2] = TextUtil.removeDuplicatedSpace(sb.toString().trim());
                
		return text;
	}
	
//        protected String removePathologyHeaderFooter(String input) throws Exception {
//            // only get the content, skip header and footer
//            return separatePathologyHeaderFooter(input)[1];
//        }
        
        /**
         * Separate a pathology report into 3 parts: header, content, and footer.
         * 
         * @param input
         * @return text[0]: header; text[1]: content; text[2]: footer
         * @throws Exception 
         */
	public static String[] separatePathologyHeaderFooter(String input) throws Exception {
		String[] text = new String[3];
                StringBuilder sb;
		String[] lines = input.split("\n");
		int allLines = lines.length;
		int iLine = 0;
		// header starts from the beginning to PATIENT HISTORY
                sb = new StringBuilder();
		while(iLine < allLines && lines[iLine].indexOf("PATIENT HISTORY") == -1) {
                        sb.append(lines[iLine].trim());
                        sb.append("\n");
			iLine++;
		}
        
        // make header
        text[0] = TextUtil.removeDuplicatedSpace(sb.toString().trim());
                
//                 // content starts from here until meet Pathologist (beginning of a sentence)
//                 sb = new StringBuilder();
// 		if(iLine < allLines - 1) {
// 			iLine++;
// //			text += lines[iLine].trim() + "\n";
//                         sb.append(lines[iLine].trim());
//                         sb.append("\n");
// 		}
// 		else {
// 			iLine = -1;
// 		}
// 		iLine++;
		
// 		Pattern p = Pattern.compile("^\\s*Pathologist");
// 		Matcher m = p.matcher(lines[iLine]);
// 		while(iLine < allLines && !m.find()) {
// 			if(!lines[iLine].trim().equals("")) {
// //				text += lines[iLine].trim() + "\n";
//                                 sb.append(lines[iLine].trim());
//                                 sb.append("\n");
// 			}
// 			iLine++;
// 			m = p.matcher(lines[iLine]);
// 		}
// 		// skip until meet GROSS DESCRIPTION
// 		p = Pattern.compile("^GROSS DESCRIPTION");
// 		m = p.matcher(lines[iLine]);
// 		while(iLine < allLines && !m.find()) {
// 			iLine++;
// 			m = p.matcher(lines[iLine]);
// 		}

		// keep contain until meet E_O_R
		while(iLine < allLines && lines[iLine].indexOf("E_O_R") == -1) {
			if(!lines[iLine].trim().equals("")) {
//				text += lines[iLine].trim() + "\n";
                                sb.append(lines[iLine].trim());
                                sb.append("\n");
			}
			iLine++;
		}
        
        // make content
		// remove stop word
        // and **ID-NUM
        text[1] = TextUtil.removeDuplicatedSpace(sb.toString().trim());
		text[1] = text[1].replaceAll("\\*\\*ID\\-NUM", "");
		text[1] = text[1].replaceAll("\\*\\*INITIALS", "");
		text[1] = text[1].replaceAll("_{3,}", "");
                
        // footer starts from here to the end
        sb = new StringBuilder();
        while(iLine < allLines) {
            sb.append(lines[iLine++].trim());
            sb.append("\n");
        }
        // make the footer
        text[2] = TextUtil.removeDuplicatedSpace(sb.toString().trim());
                
		return text;
	}
        
        /**
         * Separate a report into 3 parts: header, content, and footer.
         * 
         * @param type "report" or "pathology"
         * @param input
         * @return
         * @throws Exception 
         */
        public static String[] separateText(String type, String input) throws Exception {
            String[] text = null;
            if(type.toLowerCase().equals("report")) {
                text = separateReportHeaderFooter(input);
            }
            else {
                text = separatePathologyHeaderFooter(input);
            }
            
            return text;
        }

        /**
         * Preprocess each token in each sentence. Based on control variables
         * 
         * @param tokenPerSentenceList
         * @throws Exception 
         */
        public void process(List<String[]> tokenPerSentenceList) throws Exception {
            String[] tokenPerSentence;
            String token;
            List<String> processedTokenList;
            Morphology morph = new Morphology();
//            System.gc();
            for (int iSentence = 0; iSentence < tokenPerSentenceList.size(); iSentence++) {
                processedTokenList = new ArrayList<>();
                tokenPerSentence = tokenPerSentenceList.get(iSentence);
                for (int iToken = 0; iToken < tokenPerSentence.length; iToken++) {
                    token = tokenPerSentence[iToken];
                    if (!token.equals("") && m_removePunctuation) {
                        token = filterPunctuation(token);
                    }
                    if (!token.equals("") && ! m_caseSensitive) {
                        token = token.toLowerCase();
                    }
                    if (!token.equals("") && m_removeStopWord) {
                        token = filterStopWord(token);
                    }
                    if(!token.equals("") && m_stemWord)
                            token = stemWord(token, morph);

                    // add if it is not an empty token
                    if(!token.equals("")) {
                        processedTokenList.add(token);
                    }
                }
                // update the original token list object
                tokenPerSentenceList.set(iSentence,
                        processedTokenList.toArray(new String[processedTokenList.size()]));
            }
        }
        
        protected String filterPunctuation(String token) throws Exception {
            return m_excludeStringList.contains(token) ? "" : token;
        }
        
        protected String filterStopWord(String token) throws Exception {
            return m_stopWordList.contains(token) ? "" : token;
        }
        
        protected String stemWord(String token, Morphology morph) throws Exception {
            return TextUtil.stemWord(token, morph);
        }
        
        /**
         * Tokenize <code>text</code> into a list of String.
         * <p>
         * Sentence boundaries are removed.
         * 
         * @param text
         * @return
         * @throws Exception 
         */
        public static List<String> tokenize(String text) throws Exception {
            List<String[]> tokenPerSentenceList = tokenizeSentences(text);
            ArrayList<String> tokenList = new ArrayList<>();
            for(String[] tokenPerSentence : tokenPerSentenceList) {
                tokenList.addAll(Arrays.asList(tokenPerSentence));
            }
            
            return tokenList;
        }
        
        /**
         * Tokenize <code>text</code> into list of array of String, where each array contains 
         * tokens of a sentence.
         * 
         * @param text
         * @return
         * @throws Exception 
         */
        public static List<String[]> tokenizeSentences(String text) throws Exception {
            List<String[]> tokenPerSetenceList = TextUtil.extractWordsByStandfordParser(text);
            return tokenPerSetenceList;
        }
}
