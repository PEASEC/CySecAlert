package de.tudarmstadt.peasec.entity.help;

import com.google.protobuf.MapEntry;
import edu.stanford.nlp.ling.CoreAnnotations;
import org.j_paine.formatter.Token;

import java.util.*;
import java.util.stream.Collectors;

public class TokenVector {
    Map<String, Double> map;

    public TokenVector(String string) {
        map = new HashMap<>();
        for(String s : string.split(" ")) {
            if(!map.containsKey(s))
                map.put(s, 1.0);
            else
                map.put(s, map.get(s) + 1.0);
        }
    }

    public TokenVector(Map<String, Double> map) {
        this.map = map;
    }

    public TokenVector multiplyIdf(Map<String, Integer> df) {
        Map<String, Double> newMap = new HashMap<>();
        double idf;
        for(String s : map.keySet()) {
            idf = 1d;
            if(df.containsKey(s))
                idf = 1 / (double) df.get(s);
            newMap.put(s, map.get(s) * idf);
        }
        return new TokenVector(newMap);
    }

    protected Map<String, Double> getMap() {
        return this.map;
    }

    private static Map<String, Double> addMaps(List<Map<String, Double>> tokenVectorMapList) {
        HashMap<String, Double> aMap = new HashMap<String, Double>();
        if(tokenVectorMapList != null && tokenVectorMapList.size() > 0) {
            for(Map<String, Double> m : tokenVectorMapList) {
                for(Map.Entry<String, Double> e : m.entrySet()) {
                    String key = e.getKey();
                    Double val = e.getValue();
                    if(!aMap.containsKey(key))
                        aMap.put(key, val);
                    else
                        aMap.put(key, aMap.get(key) + val);
                }
            }
        }
        return aMap;
    }

    public static TokenVector getCenter(List<TokenVector> tokenVectors) {
        int vectorCount = tokenVectors.size();
        Map<String, Double> aMap = addMaps(tokenVectors.stream().map(tv -> tv.getMap()).collect(Collectors.toList()));
        for(String key : aMap.keySet())
            aMap.put(key, aMap.get(key) / (double) vectorCount);
        return new TokenVector(aMap);
    }

    public static double cosineDistance(TokenVector v1, TokenVector v2) {
        Map<String, Double> m1 = v1.getMap();
        Map<String, Double> m2 = v2.getMap();

        double numerator = 0.0;
        Set<String> intersection = new HashSet<>(m1.keySet());
        intersection.retainAll(m2.keySet());
        for(String s : intersection)
            numerator += (double) m1.get(s) * (double) m2.get(s);

        double tmp1 = 0.0;
        double d;
        for(String key : m1.keySet()) {
            d = m1.get(key);
            tmp1 += d * d;
        }

        double tmp2 = 0.0;
        for(String key : m2.keySet()) {
            d = m2.get(key);
            tmp2 += d * d;
        }

        return 1 - (numerator / (Math.sqrt(tmp1) * Math.sqrt(tmp2)));
    }
}
