package de.tudarmstadt.peasec.util;

import java.util.HashMap;
import java.util.Map;

public class Statistics {

    private static final Statistics singleton = new Statistics();

    private Map<String, Integer> map;

    public Statistics() {
        map = new HashMap<>();
    }

    public void addTo(String s) {
        this.addTo(s, 1);
    }

    public void addTo(String s, int i) {
        if(i < 0)
            return;

        if(!map.containsKey(s))
            map.put(s, 0);

        map.put(s, map.get(s) + i);
    }

    public void print() {
        System.out.println();
        System.out.println("-------------Statistics----------------");
        map.entrySet().forEach(e -> {
            System.out.println(e.getKey() + " : " + e.getValue());
        });
        System.out.println("---------------------------------------");
    }

    public static Statistics getInstance() {
        return singleton;
    }
}
