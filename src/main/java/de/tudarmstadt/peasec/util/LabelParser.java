package de.tudarmstadt.peasec.util;

import de.tudarmstadt.peasec.entity.ILabel;

import java.util.List;

public class LabelParser {

    public static void toRelevantIrrelevant(ILabel l) {
        String label = "irrelevant";
        if(l.getLabel().equals("3") || l.getLabel().equals("4"))
            label = "relevant";
        l.setLabel(label);
    }

    public static void toRelaventIrrelevant(List<ILabel> labelList) {
        labelList.forEach(l -> {
            String label = "irrelevant";
            if(l.getLabel().equals("3") || l.getLabel().equals("4"))
                label = "relevant";
            l.setLabel(label);
        });
    }
}
