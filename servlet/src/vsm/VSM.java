package vsm;

import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

public class VSM {

	public static double vsm(Map<String, Double> v1, Map<String, Double> v2) {
        Set<String> both = Sets.newHashSet(v1.keySet());
        both.retainAll(v2.keySet());
        double sclar = 0, norm1 = 0, norm2 = 0;
        for (String k : both) sclar += v1.get(k) * v2.get(k);
        for (String k : v1.keySet()) norm1 += v1.get(k) * v1.get(k);
        for (String k : v2.keySet()) norm2 += v2.get(k) * v2.get(k);
        return sclar / Math.sqrt(norm1 * norm2);
	}
	
	
	
}
