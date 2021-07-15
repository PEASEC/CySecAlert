package de.tudarmstadt.peasec.twitter.crawl;

import de.tudarmstadt.peasec.service.UserMongoService;
import de.tudarmstadt.peasec.service.UserTwitterService;
import de.tudarmstadt.peasec.util.ListExtractor;
import de.tudarmstadt.peasec.util.Statistics;
import twitter4j.User;

import java.util.List;
import java.util.Properties;

public class UserCrawler {

    public static final String ACCOUNT_NAME_FILE = "UserCrawler.AccountNameFile";

    private String fileName;

    UserMongoService mongoService;
    UserTwitterService twitterService;

    public UserCrawler() {}

    public UserCrawler(Properties properties) {
        super();
        this.fileName = properties.getProperty(ACCOUNT_NAME_FILE);
        this.mongoService = new UserMongoService(properties);
        this.twitterService = new UserTwitterService();
    }

    public void downloadUserFromFile() {
        this.downloadUserFromFile(this.fileName);
    }

    public void downloadUserFromFile(String fileName) {
        List<String> userNames = ListExtractor.readSemicolonSeperatedFile(fileName);
        this.downloadUserList(userNames);
    }

    public void downloadUserList(List<String> nameList) {
        nameList.forEach(this::downloadUser);
    }

    public void downloadUser(String name) {
        if(mongoService.getUserByScreenName(name) != null) { //skip if user is already downloaded
            Statistics.getInstance().addTo("UserAlreadyKnown-NoTwitterCall");
            return;
        }
        User user = twitterService.getUserByName(name); // download user from twitter
        mongoService.saveUser(user); // save user to db
    }

}
