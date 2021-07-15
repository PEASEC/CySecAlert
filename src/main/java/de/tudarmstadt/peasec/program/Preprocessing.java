package de.tudarmstadt.peasec.program;

import de.tudarmstadt.peasec.service.TweetTextPreprocessingService;

import java.util.Properties;

public class Preprocessing {
    //TODO: Documentation
    public static void preprocessTweets(Properties properties, boolean timed) {
        TweetTextPreprocessingService service = new TweetTextPreprocessingService(properties);
        service.processAllTweetsAndSave(timed);
    }

    //TODO: Documentation
    public static void preprocessTweets(Properties properties) {
        preprocessTweets(properties, false);
    }
}
