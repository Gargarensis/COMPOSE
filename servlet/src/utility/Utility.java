package utility;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Utility {

	
	public static Map<String, Double> convertStringToMap(String toConvert){
		Map<String, Double> map = new HashMap<String, Double>();
		
		Pattern space = Pattern.compile(" ");
		String[] tokens = space.split(toConvert);
		
		for(String word:tokens){
			if(!map.containsKey(word)){
				map.put(word, 1.0);
			} else {
				map.put(word, map.get(word)+1);
			}
		}
		
		return map;
	}
	
}
