package de.tudarmstadt.peasec.depr.util;

public class WordEntity {
    private String word;
    private String label;
    private long count;

    public WordEntity() {}

    public WordEntity(String word, String label) {
        this(word, label, 1);
    }

    public WordEntity(String word, String label, long count) {
        this.word = word;
        this.label = label;
        this.count = count;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public void incrementCount() {
        this.setCount(this.getCount() + 1);
    }
}
