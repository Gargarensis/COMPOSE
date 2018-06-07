package utility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

public class TextualNormalization {

	private String basicTextNormalization(String toClean){
		
		//Remove punctuation and numbers
		String cleanedString = toClean.replaceAll("[^a-zA-Z ]", "");
		//Replace all multiple spaces with one space
		cleanedString = cleanedString.replaceAll("\\s+", " ");
		//Replace new lines with one space
		cleanedString = cleanedString.replaceAll("[\\t\\n\\r]+"," ");
		//Put all to lowercase
		cleanedString = cleanedString.toLowerCase();
		//remove single letters
		cleanedString = " " + cleanedString + " ";
		cleanedString = cleanedString.replaceAll("\\s[a-z]\\s", " ");
		
		return cleanedString;
	}
	
	
	private String removeStopwordList(String toClean, String stopWordListPath) throws IOException{
		
		String cleanedString = toClean.trim().toLowerCase();
		
		BufferedReader reader = new BufferedReader(new FileReader(stopWordListPath));
        String line;
        while ((line = reader.readLine()) != null) {
           cleanedString = cleanedString.replaceAll(" " + line + " ", " ");
        }
		reader.close();
        cleanedString = cleanedString.replaceAll("\\s+", " ");
        
		return cleanedString;
	}
	
	private String applyPorterStemmer(String toStem){
		
		String stemmedString = toStem.trim().toLowerCase();
		Pattern space = Pattern.compile(" ");
		
		String[] words = space.split(stemmedString);
		Vector<String> alreadyAnalysedWords = new Vector<String>();
		
		for(String word:words){
			if(!alreadyAnalysedWords.contains(word)){
				alreadyAnalysedWords.add(word);
				PorterStemmer porterStemmer = new PorterStemmer();
				char[] wordArray = word.toCharArray();
				porterStemmer.add(wordArray, wordArray.length);
				porterStemmer.stem();
				String stemmedWord = porterStemmer.toString();
				stemmedString = stemmedString.replaceAll(word, stemmedWord);
			}
		}
		
		return stemmedString;
	}
	
	
	private String splitUnderscore(String s) {
		return s.replaceAll("_", " ");
	}
	
	private String splitCamelCase(String s) {
		return s.replaceAll(String.format("%s|%s|%s",
				"(?<=[A-Z])(?=[A-Z][a-z])", "(?<=[^A-Z])(?=[A-Z])",
				"(?<=[A-Za-z])(?=[^A-Za-z])"), " ");
	}
	
	public Map<String, Double> performTextNormalization(String toNormalize, boolean splitCamelCase, boolean splitUnderscore, 
			boolean removeEnglishStopWords, boolean removeJavaKeywords, boolean removeCkeywords,
			boolean applyPorterStemmer) throws IOException{
		
		TextualNormalization textualNormalization = new TextualNormalization();
		
		String cleanedString = toNormalize.trim();
		
		//1. Split Underscore
		/*if(splitUnderscore)
			cleanedString = textualNormalization.splitUnderscore(cleanedString);
		
		//2. Split Camel Case
		if(splitCamelCase)
			cleanedString = textualNormalization.splitCamelCase(cleanedString);*/
		
		//3. Perform basic text normalization
		cleanedString = textualNormalization.basicTextNormalization(cleanedString);
		
		//4. Remove stopword (english)
		if(removeEnglishStopWords)
			cleanedString = textualNormalization.removeStopwordList(cleanedString, "/Users/admin/IdeaProjects/prova/src/stop-words/stop-words-english.txt");
		
		/*//5. Remove stopword (java)
		if(removeJavaKeywords)
			cleanedString = textualNormalization.removeStopwordList(cleanedString, "/Users/gbavota/workspace/IR-based-textual-similarity/stop-words/stop-words-java.txt");
		
		//6. Remove stopword (java)
		if(removeCkeywords)
			cleanedString = textualNormalization.removeStopwordList(cleanedString, "/Users/gbavota/workspace/IR-based-textual-similarity/stop-words/stop-words-c.txt");
		
		//7. Run porter Stemmer
		if(applyPorterStemmer)
			cleanedString = textualNormalization.applyPorterStemmer(cleanedString);*/
		
		return Utility.convertStringToMap(cleanedString);
		
	}

	public static String normalizeText(String toNormalize, boolean splitCamelCase, boolean splitUnderscore,
														boolean removeEnglishStopWords, boolean removeJavaKeywords, boolean removeCkeywords,
														boolean applyPorterStemmer) throws IOException{

		TextualNormalization textualNormalization = new TextualNormalization();

		String cleanedString = toNormalize.trim();

		//1. Split Underscore
		/*if(splitUnderscore)
			cleanedString = textualNormalization.splitUnderscore(cleanedString);

		//2. Split Camel Case
		if(splitCamelCase)
			cleanedString = textualNormalization.splitCamelCase(cleanedString);*/

		//3. Perform basic text normalization
		cleanedString = textualNormalization.basicTextNormalization(cleanedString);

		//4. Remove stopword (english)
		if(removeEnglishStopWords)
			cleanedString = textualNormalization.removeStopwordList(cleanedString, "/Users/admin/IdeaProjects/prova/src/stop-words/stop-words-english.txt");

		/*//5. Remove stopword (java)
		if(removeJavaKeywords)
			cleanedString = textualNormalization.removeStopwordList(cleanedString, "/Users/gbavota/workspace/IR-based-textual-similarity/stop-words/stop-words-java.txt");

		//6. Remove stopword (java)
		if(removeCkeywords)
			cleanedString = textualNormalization.removeStopwordList(cleanedString, "/Users/gbavota/workspace/IR-based-textual-similarity/stop-words/stop-words-c.txt");

		//7. Run porter Stemmer
		if(applyPorterStemmer)
			cleanedString = textualNormalization.applyPorterStemmer(cleanedString);*/

		return cleanedString;

	}


}
