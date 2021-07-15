package de.tudarmstadt.peasec.service;

import de.tudarmstadt.peasec.twitter.crawl.CrawlerUtil;
import de.tudarmstadt.peasec.util.Statistics;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

import java.util.ArrayList;
import java.util.List;

public class UserTwitterService {

    private Twitter twitter = CrawlerUtil.getTwitter();

    public UserTwitterService() {

    }

    public User getUserByName(String name) {
        User u = null;
        try {
            List<User> results = twitter.searchUsers(name, 1);
            if(results.size() == 0)
                System.out.println("Username not found:" + name);
            else
                u = results.get(0);

            Statistics.getInstance().addTo("searchUsers");
        } catch (TwitterException e) {
            CrawlerUtil.waitForLimit(e.getRateLimitStatus().getSecondsUntilReset());
            u = getUserByName(name);
        }
        return u;
    }

    public List<User> getUserListByNameList(List<String> nameList) {
        List<User> userList = new ArrayList<>(nameList.size());
        for(String name: nameList) {
            User u = this.getUserByName(name);
            userList.add(u);
        }
        return userList;
    }
}
