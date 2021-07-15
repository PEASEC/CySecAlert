package de.tudarmstadt.peasec.depr;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.tudarmstadt.peasec.entity.ProcessedTextEntity;
import de.tudarmstadt.peasec.depr.util.NaiveBayesCache;
import de.tudarmstadt.peasec.util.MongoHelper;
import de.tudarmstadt.peasec.depr.util.WordEntity;
import de.tudarmstadt.peasec.util.config.CollectionNameProperties;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NaiveBayes {

    public static final String LABELS = "NaiveBayes.Labels";

    private MongoCollection<WordEntity> wordCountCollection;
    private MongoCollection<WordEntity> classProbCollection;

    private List<String> labels;

    private NaiveBayesCache cache = new NaiveBayesCache();

    public NaiveBayes(Properties properties) {
        this.setWordCountCollection(properties.getProperty(CollectionNameProperties.NAIVE_BAYES_WORD_COUNT_COLLECTION_NAME));
        this.setClassProbCollection(properties.getProperty(CollectionNameProperties.NAIVE_BAYES_CLASS_PROBABILITY_COLLECTION_NAME));
        this.setLabels(Arrays.asList(properties.getProperty(LABELS, "").split(",")));
        this.wordCountCollection.drop();
        this.classProbCollection.drop();
        long a = this.wordCountCollection.countDocuments();
        long b = this.wordCountCollection.countDocuments();
        System.out.println("Reset Databases: " + a + " - " + b);
    }


    public NaiveBayes(Properties properties, List<String> labels) {
        this(properties);
        this.labels = labels;
    }

    private void setWordCountCollection(String wordCollectionName) {
        this.wordCountCollection = MongoHelper.getInstance().getCollection(wordCollectionName, WordEntity.class);
        this.wordCountCollection.drop();
    }

    private void setClassProbCollection(String classProbCollectionName) {
        this.classProbCollection = MongoHelper.getInstance().getCollection(classProbCollectionName, WordEntity.class);
        this.classProbCollection.drop();
    }

    public void addLabeledEntry(ProcessedTextEntity entity, String label) {
        this.addLabeledEntry(entity.getText(), label);
    }

    public void addLabeledEntry(String text, String label) {
        this.addLabeledEntry(Arrays.asList(text.split(" ")), label);
    }

    public void addLabeledEntry(List<String> tokenList, String label) {
        List<Bson> filter = Arrays.asList(
                Filters.in("word", tokenList),
                Filters.eq("label", label)
        );

        //Find and Delete already present entries
        List<WordEntity> entityList = this.wordCountCollection.find(Filters.and(filter)).into(new ArrayList<>());
        this.wordCountCollection.deleteMany(Filters.and(filter));

        //build new entries
        List<String> tokenListCopy = new ArrayList<>(tokenList);
        tokenListCopy.removeAll(entityList.stream().map(e -> e.getWord()).collect(Collectors.toList()));
        entityList.addAll(tokenListCopy.stream().map(w -> new WordEntity(w, label)).collect(Collectors.toList()));

        //Increment List Items
        entityList.forEach(e -> e.incrementCount());

        //persist
        this.wordCountCollection.insertMany(entityList);

        //update document prob
        WordEntity docCountEntry = this.classProbCollection.find(Filters.eq("label", label)).first();
        this.classProbCollection.deleteOne(Filters.eq("label", label));
        if(docCountEntry == null)
            docCountEntry = new WordEntity(null, label, 0);
        docCountEntry.incrementCount();
        this.classProbCollection.insertOne(docCountEntry);

        this.cache.clear();
    }

    public String classify(List<String> tokenList) {
        if(labels == null || labels.size() == 0) {
            System.err.println("Naive Bayes needs Label list for classification");
            return null;
        }

        String label = null;
        Double max = -Double.MIN_VALUE;
        for(String l : this.labels) {
               double current = this.calcValueForLabel(tokenList, l);
               if(current > max) {
                   max = current;
                   label = l;
               }
        }
        return label;
    }

    private double calcValueForLabel(List<String> tokenList, String label) {
        double acc = 1.0;
        for(String s : tokenList)
            acc *= this.calculateWordGivenClassProb(s, label);
        acc *= this.calculateClassProb(label);
        return acc;
    }

    private double calculateWordGivenClassProb(String word, String label) {
        WordEntity entity = this.wordCountCollection.find(Filters.and(Filters.eq("word", word), Filters.eq("label", label))).first();
        double count = (entity == null) ? 1 : entity.getCount();
        List<WordEntity> wordEntityList = this.wordCountCollection.find(Filters.eq("label", label)).into(new ArrayList<>());
        Optional<Long> opt = wordEntityList.stream().map(e -> e.getCount()).reduce((c1, c2) -> c1 + c2);
        if(opt.isEmpty()) {
            List<WordEntity> wordEntities = this.wordCountCollection.find().into(new ArrayList<>());
            List<WordEntity> classProbEntries = this.classProbCollection.find().into(new ArrayList<>());
            return 0.0;
        }
        double regularization = (double) opt.get();
        return count / regularization;
    }

    private double calculateClassProb(String label) {
        return this.classProbCollection.find(Filters.eq("label", label)).first().getCount();
    }

    public Map<String, Double> getConfidentialityMap(ProcessedTextEntity entity) {
        return this.getConfidentialityMap(entity.getText());
    }

    public Map<String, Double> getConfidentialityMap(String text) {
        return this.getConfidentialityMap(Arrays.asList(text.split(" ")));
    }

    public Map<String, Double> getConfidentialityMap(List<String> tokenList) {
        if(labels == null || labels.size() == 0) {
            System.err.println("Naive Bayes needs Label list for classification");
            return null;
        }

        Map<String, Double> out = new HashMap<>();
        for(String l : this.labels) {
            out.put(l, this.calcValueForLabel(tokenList, l));
        }
        return out;
    }

    public double getCertainty(ProcessedTextEntity entity) {
        return this.getCertainty(entity.getText());
    }

    public double getCertainty(String text) {
        return this.getCertainty(Arrays.asList(text.split(" ")));
    }

    public double getCertainty(List<String> tokenList) {
        Map<String, Double> map = this.getConfidentialityMap(tokenList);
        return map.entrySet().stream()
                .map(Map.Entry::getValue)
                .reduce(Double::max)
                .get();
    }

    public ProcessedTextEntity getMostUncertain(List<ProcessedTextEntity> aList) {
        this.cache.load(
                this.wordCountCollection.find().into(new ArrayList<>()),
                this.classProbCollection.find().into(new ArrayList<>())
        );
        ConcurrentHashMap<Double, ProcessedTextEntity> probabilityMap = new ConcurrentHashMap<>();
        aList.parallelStream().forEach(e -> {
            double out = 0;
            for(String label : this.labels) {
                out = Math.max(
                        this.cache.calculateProbability(
                                Arrays.asList(e.getText().split(" ")),
                                label
                        ),
                        out
                );
            }
            probabilityMap.put(out, e);
        });

        List<Double> probabilities = new ArrayList<>(probabilityMap.keySet());
        Collections.sort(probabilities);
        return probabilityMap.get(probabilities.get(probabilities.size() - 1));
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public void dropCollections() {
        this.wordCountCollection.drop();
        this.classProbCollection.drop();
    }
}
