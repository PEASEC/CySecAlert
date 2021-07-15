package de.tudarmstadt.peasec.pipeline;

import de.tudarmstadt.peasec.util.ListExtractor;
import de.tudarmstadt.peasec.util.config.UrlConfigurations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.process.Stemmer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TextPreprocessor {

//    private static final String stopWordListUrl = "D:/Dropbox/MasterThesis/experts/stopwords_stanford.txt";

    private String stopWordListUrl;

    private List<String> stopWordList;

    private StanfordCoreNLP stanfordPipeline;

    public enum Features {
        TO_LOWER_CASE,
        REMOVE_URL,
        REMOVE_USER_LINK,
        LEMMATIZE,
        STEM,
        REMOVE_STOP_WORDS,
        COPY_HYPHEN_CONNECTED_WORDS,
        REPLACE_VERSION_RELEVANT_CHARACTERS,
        REMOVE_NON_ALPHANUMERICAL_CHARACTERS,
        SORT,
        FILTER_EMPTY_TOKEN
    }

    private List<Features> pipeline;

    public TextPreprocessor() {
        this.pipeline = new ArrayList<>();
//      this.pipeline.add(Features.LEMMATIZE);
        this.pipeline.add(Features.TO_LOWER_CASE);
        this.pipeline.add(Features.REMOVE_URL);
        this.pipeline.add(Features.REMOVE_USER_LINK);
        this.pipeline.add(Features.REMOVE_STOP_WORDS);
        this.pipeline.add(Features.REMOVE_NON_ALPHANUMERICAL_CHARACTERS);
//        this.pipeline.add(Features.COPY_HYPHEN_CONNECTED_WORDS);
//        this.pipeline.add(Features.REPLACE_VERSION_RELEVANT_CHARACTERS);
        this.pipeline.add(Features.SORT);
        this.pipeline.add(Features.FILTER_EMPTY_TOKEN);
        this.pipeline.add(Features.STEM);
    }

    public TextPreprocessor(List<Features> list) {
        this.pipeline.addAll(list);
    }

    public TextPreprocessor(Properties properties) {
        this();
        this.processProperties(properties);
    }

    public TextPreprocessor(Properties properties, List<Features> list) {
        this(list);
        this.processProperties(properties);
    }

    public void processProperties(Properties properties) {
        this.stopWordListUrl = properties.getProperty(UrlConfigurations.STOP_WORD_LIST_URL);
    }

    public String process(String text) {
        if(this.pipeline.contains(Features.REMOVE_STOP_WORDS)) {
            this.fetchStopWordList();
        }
        if(this.pipeline.contains(Features.LEMMATIZE)) {
            this.initStanfordPipeline();
        }

        Stream<String> tokenStream = this.pipeline.contains(Features.LEMMATIZE)
                ? this.lemmatize(text)
                : this.splitIntoToken(text);
        for(Features e : this.pipeline) {
            switch (e) {
                case TO_LOWER_CASE:
                    tokenStream = this.toLowerCase(tokenStream);
                    break;
                case REMOVE_URL:
                    tokenStream = this.removeUrl(tokenStream);
                    break;
                case REMOVE_USER_LINK:
                    tokenStream = this.removeUserLink(tokenStream);
                    break;
                case REMOVE_STOP_WORDS:
                    tokenStream = this.removeStopWords(tokenStream);
                    break;
                case COPY_HYPHEN_CONNECTED_WORDS:
                    tokenStream = this.copyHyphenConnectionWords(tokenStream);
                    break;
                case REPLACE_VERSION_RELEVANT_CHARACTERS:
                    tokenStream = this.replaceVersionRelevantCharacters(tokenStream);
                    break;
                case REMOVE_NON_ALPHANUMERICAL_CHARACTERS:
                    tokenStream = this.removeNonAlphaNumericalCharacters(tokenStream);
                    break;
                case SORT:
                    tokenStream = this.sort(tokenStream);
                    break;
                case FILTER_EMPTY_TOKEN:
                    tokenStream = this.filterEmptyToken(tokenStream);
                    break;
                case STEM:
                    tokenStream = this.stem(tokenStream);
                    break;
            }
        }
        return String.join(" ", tokenStream.collect(Collectors.toList()));
    }

    private Stream<String> splitIntoToken(String s) {
        List<String> lines = Arrays.asList(s.split("\\r?\\n"));
        List<String> outList = new ArrayList<>();
        for(List<String> line :  lines.stream().map(line -> Arrays.asList(line.split("\\s+"))).collect(Collectors.toList())) {
            outList.addAll(line);
        }
        return outList.stream();
    }

    private Stream<String> toLowerCase(Stream<String> tokenStream) {
        return tokenStream.map(s -> s.toLowerCase());
    }

    private void fetchStopWordList() {
        this.stopWordList = ListExtractor.readLineSeperatedFile(this.stopWordListUrl);
    }

    private void initStanfordPipeline() {
        Properties props;
        props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma");
        this.stanfordPipeline = new StanfordCoreNLP(props);
    }

    private Stream<String> lemmatize(String text) {
        List tokenList = new ArrayList();

        Annotation document = new Annotation(text);
        this.stanfordPipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for(CoreMap sentence : sentences) {
            for(CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                tokenList.add(token.get(CoreAnnotations.LemmaAnnotation.class));
            }
        }

        return tokenList.stream();
    }

    private Stream<String> removeUrl(Stream<String> tokenStream) {
        return tokenStream.filter(t -> !t.matches("https://t.co/(.*)"));
    }

    private Stream<String> removeUserLink(Stream<String> tokenStream) {
        return tokenStream.filter(t -> t.length() > 0 && !(t.charAt(0) == '@'));
    }

    private Stream<String> removeStopWords(Stream<String> tokenStream) {
        return tokenStream.filter(t -> !this.isStopWord(t));
    }

    private Stream<String> copyHyphenConnectionWords(Stream<String> tokenStream) {
        return tokenStream.flatMap(token -> {
            List<String> outList = new ArrayList<>();
            outList.add(token);
            if(token.contains("-")) {
                for (String subToken : token.split("-")) {
                    if (Pattern.matches("[a-z]+", subToken)) {
                        outList.add(subToken);
                    }
                }
            }
            return outList.stream();
        });
    }

    private Stream<String> replaceVersionRelevantCharacters(Stream<String> tokenStream) {
        return tokenStream.map(s -> {
                    return s.replace(".", "dot")
                            .replace("-", "hyphen")
                            .replace("_", "underscore");
                }
        );
    }

    private boolean isStopWord(String s) {
        boolean acc = false;
        for(String stopword : this.stopWordList) {
            acc = acc || s.equals(stopword);
        }
        return acc;
    }

    private Stream<String> removeNonAlphaNumericalCharacters(Stream<String> tokenStream) {
        return tokenStream.flatMap(s -> {
            return Arrays.asList(
                    s.replaceAll("[^a-z0-9\\.\\-\\_]|\\.$", "")
                    .replaceAll("\\.\\.\\.", "")
                    .split(" ")
            ).stream();
        });
    }

    private Stream<String> sort(Stream<String> tokenStream) {
        return tokenStream.sorted((t1, t2) -> t1.compareTo(t2));
    }

    private Stream<String> filterEmptyToken(Stream<String> tokenStream) {
        return tokenStream.filter(t -> t.length() > 0);
    }

    private Stream<String> stem(Stream<String> tokenStream) {

        Stemmer stemmer = new Stemmer();
        return tokenStream.map(s -> {
            return stemmer.stem(s);
        });
    }
}
