package de.tudarmstadt.peasec.pipeline;

import de.tudarmstadt.peasec.entity.TweetEntity;
import de.tudarmstadt.peasec.service.TweetLabelService;

import java.util.Properties;
import java.util.Scanner;

public class TweetLabeler {

    Scanner scanner = new Scanner(System.in);

    public TweetLabeler() {
    }

    public TweetLabeler(Properties properties) {

    }

    private String openLabelDialogue(TweetEntity t) {
        System.out.println("--LABEL-THIS-----------------------------------------------------------");
        System.out.println(t.getText().substring(0, t.getDisplayTextRangeStart()));
        System.out.println(t.getText().substring(t.getDisplayTextRangeStart(), Math.min( t.getText().length(), t.getGetDisplayTextRangeEnd())));
        System.out.println(t.getText().substring(Math.min( t.getText().length(), t.getGetDisplayTextRangeEnd())));
        System.out.println("-----------------------------------------------------------------------");
        System.out.println("https://twitter.com/laparisa/status/" + t.getTweetId());
        System.out.println("-----------------------------------------------------------------------");

        String s = scanner.next();
        System.out.println();

        return s;
    }

    private String openLabelDialogue(TweetEntity t, String additionalInfo) {
        System.out.println("--LABEL-THIS-----------------------------------------------------------");
        System.out.println(t.getText().substring(0, t.getDisplayTextRangeStart()));
        System.out.println(t.getText().substring(t.getDisplayTextRangeStart(), Math.min( t.getText().length(), t.getGetDisplayTextRangeEnd())));
        System.out.println(t.getText().substring(Math.min( t.getText().length(), t.getGetDisplayTextRangeEnd())));
        System.out.println("-----------------------------------------------------------------------");
        System.out.println("https://twitter.com/laparisa/status/" + t.getTweetId());
        System.out.println("-----------------------------------------------------------------------");
        System.out.println(additionalInfo);
        System.out.println("-----------------------------------------------------------------------");

        String s = scanner.next();
        System.out.println();

        return s;
    }

    public String label(TweetEntity t) {
        String label = this.openLabelDialogue(t);
        return label;
    }

    public String label(TweetEntity t, String additionalInfo) {
        String label = this.openLabelDialogue(t, additionalInfo);
        return label;
    }

}
