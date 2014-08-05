/**
 * 
 */
package edu.pitt.cs.nih.backend.utils;

import java.util.ArrayList;
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
}
