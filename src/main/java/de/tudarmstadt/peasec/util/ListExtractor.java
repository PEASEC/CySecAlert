package de.tudarmstadt.peasec.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class ListExtractor {

    public static List<String> readSemicolonSeperatedFile(String fileName) {
        List<String> out = new ArrayList<>();
        try {
            Path filePath = Paths.get(URI.create("file:/" + fileName));
            return Files.lines(filePath)
                    .filter(not(ListExtractor::isLineCommentedOut))
                    .map(ListExtractor::splitLineBySemicolon)
                    .reduce(new ArrayList<String>(), (a, b) -> {
                        a.addAll(b);
                        return a;
                    });
        }
        catch(IOException e) {
        }
        return out;
    }

    public static List<String> readLineSeperatedFile(String fileName) {
        List<String> out = new ArrayList<>();
        try {
            Path filePath = Paths.get(URI.create("file:/" + fileName));
            return Files.lines(filePath)
                    .filter(not(ListExtractor::isLineCommentedOut))
                    .collect(Collectors.toList());
        }
        catch(IOException e) {
        }
        return out;
    }

    private static boolean isLineCommentedOut(String line) {
        return (line.length() < 1 || line.charAt(0) == '#');
    }

    private static List<String> splitLineBySemicolon(String line) {
        return Arrays.asList(line.split(";"));
    }

}
