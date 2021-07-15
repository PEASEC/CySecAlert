package de.tudarmstadt.peasec.program;

import de.tudarmstadt.peasec.service.TweetLabelService;

import java.util.Properties;

public class Labeling {

    //TODO: Documentation
    public static void labelTweets(Properties tweetProperties, int printStatisticsAfterNTweets) {
        TweetLabelService labelService = new TweetLabelService(tweetProperties);

        do {
            labelService.printStatistics();
        } while(labelService.labelUnlabeledSample(printStatisticsAfterNTweets));
    }

    //TODO: Documentation
    public static void labelTweets(Properties tweetProperties) {
        labelTweets(tweetProperties, 10);
    }
}
