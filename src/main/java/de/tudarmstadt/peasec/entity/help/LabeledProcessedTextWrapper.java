package de.tudarmstadt.peasec.entity.help;

import de.tudarmstadt.peasec.entity.ILabel;
import de.tudarmstadt.peasec.entity.IProcessedText;
import de.tudarmstadt.peasec.entity.IAdressableById;

public class LabeledProcessedTextWrapper implements IAdressableById, ILabel, IProcessedText {

    long tweetId;

    String text;

    String label;

    public LabeledProcessedTextWrapper() {
    }

    public long getTweetId() {
        return tweetId;
    }

    public void setTweetId(long tweetId) {
        this.tweetId = tweetId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
