/**
 * 
 */
package io.github.nlpreviz.nlp.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.util.CoreMap;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Phuong Pham
 * 
 *         This class provides utility methods for text processing
 */
public class TextUtil {
	private static StanfordCoreNLP pipeline;
	private static Morphology morph;

	/**
	 * Get words (unigrams) of a text (document) by Stanford parser (Tokenizer).
	 * <p>
	 * We use Stanford parser to extract individual words in a document. Each
	 * document has one or many lines. This method processes line by line. Each
	 * line contains one or more sentences.
	 * 
	 * @param text
	 * @return An array of which each element is an array of unigrams
	 * @throws Exception
	 */
	public static ArrayList<String[]> extractWordsByStandfordParser(String text)
			throws Exception {
		ArrayList<String[]> featureList = new ArrayList<>();
		try {
			Annotation document = new Annotation(text);
			pipeline.annotate(document);

			List<CoreMap> sentences = document.get(SentencesAnnotation.class);

			for (CoreMap sentence : sentences) {
				ArrayList<String> unigramPerLine = new ArrayList<>();
				for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
					unigramPerLine.add(token.get(TextAnnotation.class));
				}
				featureList.add(unigramPerLine
						.toArray(new String[unigramPerLine.size()]));
			}
		} catch (NullPointerException e) {
			Properties props = new Properties();
			props.put("annotators", "tokenize, ssplit");
			props.put("ssplit.eolonly", "true");
			pipeline = new StanfordCoreNLP(props);

			Annotation document = new Annotation(text);
			pipeline.annotate(document);

			List<CoreMap> sentences = document.get(SentencesAnnotation.class);

			for (CoreMap sentence : sentences) {
				ArrayList<String> unigramPerLine = new ArrayList<>();
				for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
					unigramPerLine.add(token.get(TextAnnotation.class));
				}
				featureList.add(unigramPerLine
						.toArray(new String[unigramPerLine.size()]));
			}
		}
		return featureList;
	}

	/**
	 * Get words (unigrams) of a text (document) by token
	 * <p>
	 * We use token to extract individual words in a document. The default token
	 * is whitespace "\\s". This approach is more simple than Stanford parser.
	 * Each document has one or many lines. This method processes line by line.
	 * 
	 * @param text
	 * @return An array of which each element is an array of unigrams
	 * @throws Exception
	 */
	public static ArrayList<String[]> extractWordsByToken(String text,
			String token) throws Exception {
		ArrayList<String[]> featureList = new ArrayList<String[]>();

		// simple approach: split by whitespaces
		String[] lines = text.split("\\n");
		for (String line : lines) {
			String[] unigrams = line.split(token);
			featureList.add(unigrams);
		}

		return featureList;
	}

	public static StanfordCoreNLP createStanfordCoreNLPObject()
			throws Exception {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit");
		props.put("ssplit.eolonly", "true");
		return new StanfordCoreNLP(props);
	}

	/**
	 * Split a document into array of sentences. This method is a batch mode. It
	 * requires a StanfordCoreNLP object because the creating time for this
	 * object is quite long. Creating such an object for each document in a
	 * batch mode is a burden
	 * 
	 * @param text
	 *            The document
	 * @param pipeline
	 *            A StanfordCoreNLP object.
	 * @return
	 * @throws Exception
	 */
	public static String[] extractSentences(String text,
			StanfordCoreNLP pipeline) throws Exception {
		Annotation document = new Annotation(text);
		pipeline.annotate(document);

		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		String[] sentenceList = new String[sentences.size()];

		for (int i = 0; i < sentenceList.length; i++) {
			CoreMap sentence = sentences.get(i);
			sentenceList[i] = sentence.toString();
		}

		return sentenceList;
	}

	/**
	 * Split a document into array of sentences
	 * 
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public static String[] extractSentences(String text) throws Exception {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit");
		StanfordCoreNLP pipeline = new StanfordCoreNLP();

		Annotation document = new Annotation(text);
		pipeline.annotate(document);

		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		String[] sentenceList = new String[sentences.size()];

		for (int i = 0; i < sentenceList.length; i++) {
			CoreMap sentence = sentences.get(i);
			sentenceList[i] = sentence.toString();
		}

		return sentenceList;
	}

	public static String removeDuplicatedSpace(String input) throws Exception {
		return input.replace(" +", " ");
	}

	public static String stemWordInSentence(String input) throws Exception {
		String output = "";
		String[] lines = input.split("\n");
		Morphology morph = new Morphology();

		for (String line : lines) {
			String[] tokens = line.trim().split("\\s");
			for (String token : tokens) {
				output += morph.stem(token) + " ";
			}
			output = output.trim() + "\n";
		}
		return output;
	}

	public static String stemWord(String word) throws Exception {
		String stemmedWord = "";
		Morphology local_morph = new Morphology();
		try {
//			stemmedWord = morph.stem(word);
			stemmedWord = local_morph.stem(word);
		} catch (NullPointerException e) {
//			morph = new Morphology();
//			stemmedWord = morph.stem(word);
		}
		return stemmedWord;
	}
	
	/**
	 * Get stemmed word with a given morphology object (reduce instance creation time)
	 * 
	 * @param word
	 * @param local_morph
	 * @return
	 * @throws Exception
	 */
	public static String stemWord(String word, Morphology local_morph) throws Exception {
		String stemmedWord = "";
		stemmedWord = local_morph.stem(word);
		return stemmedWord;
	}
	
	public static String escapeRegex(String input) throws Exception {
		String[] specialCharList = new String[]{"\\[", "\\\\", "\\^",
				"\\$", "\\.", "\\|", "\\?", "\\*", "\\+", "\\(", "\\)", "\\{", "\\}"};
		String output = input;
		for(String specialChar : specialCharList) {
			output = output.replaceAll(specialChar, "\\" + specialChar);
		}
		
		return output;
	}
	
	/**
     * New line characters are manually inserted in medical text. We try to 
     * reconstruct the original sentences of the document. We will replace new 
     * line with a whitespace character to maintain each word's position
     * 
     * @param input
     * @return
     * @throws Exception 
     */
    public static String reconstructSentences(String input) throws Exception {
        StringBuilder output = new StringBuilder();
        String patternStr, currentLine;
        Pattern pattern;
        Matcher matcher;
        int minWordInALine = 7, maxCharPerLine = 60, wordCount, nextLineIndex;
        List<String> endLinePunctuationList = Arrays.asList(new String[] {".", "!", "?"});
        List<String> normalEndLinePunctuationList = Arrays.asList(new String[] {",", ":", ";", "-"});
        
        String[] originalCutList = input.split("\n");
        for(int i = 0; i < originalCutList.length; i++) {
            if(originalCutList[i].equals("")) {
                mergeSentence(output);
                continue;
            }
            
            // \ at the end of the line also signal to merge
            if(originalCutList[i].substring(originalCutList[i].length() - 1).equals("\\")) {
                output.append(originalCutList[i].substring(0, originalCutList[i].length() - 1));
                mergeSentence(output);
                continue;
            }
            output.append(originalCutList[i]);
            
            // all special character means we do not alter the new line
            patternStr = "^[^a-z0-9]+$";
            pattern = Pattern.compile(patternStr);
            matcher = pattern.matcher(originalCutList[i]);
            if(matcher.find()) {
                noMergeSentence(output);
                continue;
            }
            // if this line ends with .
            currentLine = originalCutList[i].trim();
            if(endLinePunctuationList.contains(currentLine.substring(currentLine.length() - 1))) {                
                noMergeSentence(output);
                continue;
            }
            // if the line has less than minWordInALine we do not alter the new line
            patternStr = "\\s+";
            pattern = Pattern.compile(patternStr);
            matcher = pattern.matcher(originalCutList[i].replaceAll("\\s+", " "));
            wordCount = 0;
            while (matcher.find()) {
                wordCount++;
            }
            if(wordCount <= minWordInALine && // small number of words
                    originalCutList[i].replaceAll("\\s+", " ").length() < maxCharPerLine) { // but each word is also not too long
                noMergeSentence(output);
                continue;
            }
            
            // if the next line is divider or head line then not alter
            nextLineIndex = i + 1;
            while(nextLineIndex < originalCutList.length) {
                if(!originalCutList[nextLineIndex].equals("")) {
                    break;
                }
                else {
                    nextLineIndex++;
                }
            }
            if(nextLineIndex > originalCutList.length - 1) continue;
            // all special character means we do not alter the new line
            patternStr = "^[^a-z0-9]+$";
            pattern = Pattern.compile(patternStr);
            matcher = pattern.matcher(originalCutList[nextLineIndex]);
            if(matcher.find()) {
                noMergeSentence(output);
                continue;
            }
            
            
            // next line start with A-Z, no merge or -
            patternStr = "^[A-Z]";
            pattern = Pattern.compile(patternStr);
            matcher = pattern.matcher(originalCutList[nextLineIndex]);
            if(matcher.find()) {
                // if the current line signal a continue, we will merge
                if(!normalEndLinePunctuationList.contains(
                        originalCutList[i].substring(originalCutList[i].length() - 1)) &&
                        originalCutList[i].replaceAll("\\s+", "").length() < maxCharPerLine) { // heuristic if a line contains more than 60 chars (except whitespace) it should be full
                    noMergeSentence(output);
                    continue;
                }
            }
            
            // if next line start with - meaning bullets
            patternStr = "^\\s*\\-";
            pattern = Pattern.compile(patternStr);
            matcher = pattern.matcher(originalCutList[nextLineIndex]);
            if(matcher.find()) {
                noMergeSentence(output);
                continue;
            }
            
            // in VITALS section
            if(originalCutList[i].substring(originalCutList[i].length() - 3).equals("Wt:")) {
                noMergeSentence(output);
                continue;
            }  
            mergeSentence(output);
        }
        return output.toString();
    }
    
    /**
     * Do not merge sentence = insert newline at the end of the line
     * @param sb
     * @throws Exception 
     */
    protected static void noMergeSentence(StringBuilder sb) throws Exception {
        sb.append("\n");
    }
    
    /**
     * Merge sentence = insert a whitespace at the end of the line to replace newline
     * @param sb
     * @throws Exception 
     */
    protected static void mergeSentence(StringBuilder sb) throws Exception {
        sb.append(" ");
    }
}
