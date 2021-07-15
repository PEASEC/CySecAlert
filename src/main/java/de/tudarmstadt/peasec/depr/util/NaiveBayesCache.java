package de.tudarmstadt.peasec.depr.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NaiveBayesCache {

    private class WordLabelWrapper {
        public String word;
        public String label;

        public WordLabelWrapper(String word, String label) {
            this.word = word;
            this.label = label;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WordLabelWrapper that = (WordLabelWrapper) o;
            return word.equals(that.word) &&
                    label.equals(that.label);
        }

        @Override
        public int hashCode() {
            return Objects.hash(word, label);
        }
    }

    ConcurrentHashMap<WordLabelWrapper, Long> wordCountMap = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, Long> tokenPerClassMap = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, Long> classProbMap = new ConcurrentHashMap<>();

    // --- Load/Clear -----------------------------------------------------//

    public void load(List<WordEntity> wordCountEntries, List<WordEntity> classProbEntries) {
        wordCountEntries.parallelStream().forEach(e -> {
            this.wordCountMap.put(new WordLabelWrapper(e.getWord(), e.getLabel()), e.getCount());
        });
        classProbEntries.parallelStream().forEach(e -> {
            this.classProbMap.put(e.getLabel(), e.getCount());
        });

        //calculate tokenPerClassCount
        for(String label : classProbMap.keySet()) {
            Optional<Long> count = wordCountEntries.stream()
                    .filter(e -> e.getLabel().equals(label))
                    .map(WordEntity::getCount)
                    .reduce((e1, e2) -> e1 + e2 );
            this.tokenPerClassMap.put(label, count.orElse(0l));
        }
    }

    public void clear() {
        this.wordCountMap = new ConcurrentHashMap<>();
        this.classProbMap = new ConcurrentHashMap<>();
    }

    public double calculateProbability(List<String> tokenList, String label) {
        WordLabelWrapper wrapper;
        double prop = 1.0d;
        for(String token : tokenList) {
            wrapper = new WordLabelWrapper(token, label);
            double count = this.wordCountMap.contains(wrapper) ? this.wordCountMap.get(wrapper) : 1d;
            prop *= (double) count / (double) this.tokenPerClassMap.get(label);
        }
        prop *= this.classProbMap.get(label);
        return prop;
    }
}
