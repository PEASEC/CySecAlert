package de.tudarmstadt.peasec.util;

import de.tudarmstadt.peasec.entity.help.TokenVector;

import java.util.*;

public class CosineDistance {
    /**
     * Only works on sorted lists
     *
     * @param t1
     * @param t2
     * @return
     */
    public static double calculateDistance(String t1, String t2) {
        Map<String, Integer> m1, m2;
        m1 = getMap(t1);
        m2 = getMap(t2);

        double numerator = 0.0;
        for(String s : m1.keySet()) {
            if (m2.containsKey(s)) {
                numerator += m1.get(s) * m2.get(s);
            }
        }

        double denominator = Math.sqrt(sumOfSquares(m1.values())) * Math.sqrt(sumOfSquares(m2.values()));
        return 1 - (numerator / denominator);
    }

    public static double calculateDistance(TokenVector v1, TokenVector v2) {
        return TokenVector.cosineDistance(v1, v2);
    }

    private static Map<String, Integer> getMap(String message) {
        Map<String, Integer> map = new HashMap<>();
        for(String s: message.split(" ")) {
            if(!map.containsKey(s))
                map.put(s, 0);
            map.put(s, map.get(s) + 1);
        }
        return map;
    }

    private static double sumOfSquares(Collection<Integer> list) {
        double out = (double) list.stream()
                .map(n -> n * n)
                .reduce(0, (n1, n2) -> n1 + n2);
        return out;
    }
}
