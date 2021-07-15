package de.tudarmstadt.peasec.service;

import de.tudarmstadt.peasec.entity.TweetEntity;
import de.tudarmstadt.peasec.entity.TweetLabelEntity;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class CsvService {

    public static void generateEmptyLabelCsv(List<TweetEntity> list, String pathName) {
        Path path = Paths.get(pathName);
        List<String> lines = list.stream()
                .map(tweet -> {
                    String s = "";
                    s += tweet.getTweetId();
                    s += ";";
                    s += tweet.getText().replace("\n", "<br>").replace(";","");
                    s += ";";
                    return s;
                }).collect(Collectors.toList());


        try {
            Files.write(path, lines, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void generateMultiLabeledCsv(List<TweetEntity> list, List<TweetLabelEntity> reference, List<TweetLabelEntity> revise, String pathName) {
        HashMap<Long, TweetLabelEntity> referenceHashMap = new HashMap<>();
        reference.forEach(e -> referenceHashMap.put(e.getTweetId(), e));
        HashMap<Long, TweetLabelEntity> reviseHashMap = new HashMap<>();
        revise.forEach(e -> reviseHashMap.put(e.getTweetId(), e));

        Path path = Paths.get(pathName);
        List<String> lines = new ArrayList<>();
        lines.add("TweetId; Text; TristanLabel; JuliaLabel");
        for(TweetEntity entity : list) {
            long id = entity.getTweetId();
            String s = "";
            s += entity.getTweetId();
            s += ";";
            s += entity.getText().replace("\n", "<br>").replace(";","");
            s += ";";
            s += referenceHashMap.get(id).getLabel();
            s += ";";
            s += reviseHashMap.get(id).getLabel();
            lines.add(s);
        }

        try {
            Files.write(path, lines, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<TweetLabelEntity> getTweetLabelEntityFromMultiLabelCSV(String pathName) {
        List<TweetLabelEntity> outList = new ArrayList<>();
        try {
            Path filePath = Paths.get(URI.create("file:/" + pathName));
            Files.lines(filePath)
                    .map(s -> {
                        TweetLabelEntity entity = new TweetLabelEntity();
                        String [] arr = s.split(";");
                        if(arr.length > 0) {
                            String tmp = arr[0];
                            if(tmp.startsWith("\uFEFF"))
                                tmp = tmp.substring(1);
                            entity.setTweetId(Long.parseLong(tmp));
                        }
                        if(arr.length >= 4)
                            entity.setLabel(arr[3].trim());
                        else
                            entity.setLabel("");
                        return entity;
                    })
                    .filter(e -> !e.getLabel().equals(""))
                    .forEach(e -> {
                        outList.add(e);
                    });

        }
        catch(IOException e) {
        }
        return outList;
    }

    public static List<TweetLabelEntity> getTweetLabelEntityFromCSV(String pathName) {
        List<TweetLabelEntity> outList = new ArrayList<>();
        try {
            Path filePath = Paths.get(URI.create("file:/" + pathName));
            Files.lines(filePath)
                    .map(s -> {
                        TweetLabelEntity entity = new TweetLabelEntity();
                        String [] arr = s.split(";");
                        if(arr.length > 0) {
                            String tmp = arr[0];
                            if(tmp.startsWith("\uFEFF"))
                                tmp = tmp.substring(1);
                            entity.setTweetId(Long.parseLong(tmp));
                        }
                        if(arr.length >= 3)
                            entity.setLabel(arr[2]);
                        else
                            entity.setLabel("");
                        return entity;
                    })
                    .filter(e -> !e.getLabel().equals(""))
                    .forEach(e -> {
                        outList.add(e);
                    });

        }
        catch(IOException e) {
        }
        return outList;
    }
}
