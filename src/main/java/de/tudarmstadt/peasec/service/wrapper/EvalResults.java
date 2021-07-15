package de.tudarmstadt.peasec.service.wrapper;

import static java.lang.Double.isNaN;

public class EvalResults {
    int tp = 0;
    int tn = 0;
    int fp = 0;
    int fn = 0;

    public int getTp() {
        return tp;
    }

    public void setTp(int tp) {
        this.tp = tp;
    }

    public int getTn() {
        return tn;
    }

    public void setTn(int tn) {
        this.tn = tn;
    }

    public int getFp() {
        return fp;
    }

    public void setFp(int fp) {
        this.fp = fp;
    }

    public int getFn() {
        return fn;
    }

    public void setFn(int fn) {
        this.fn = fn;
    }

    public double getAccuracy() {
        double out = (double) (tp + tn) / ((double) (tp + tn + fp + fn));
        if(isNaN(out))
            out = 0.0d;
        return out;
    }

    public double getPrecision() {
        double out = (double) tp / ((double) (tp + fp));
        if(isNaN(out))
            out = 0.0d;
        return out;
    }

    public double getRecall() {
        double out = (double) tp / ((double) tp + fn);
        if(isNaN(out))
            out = 0.0d;
        return out;
    }

    public double getF1() {
        double precision = this.getPrecision();
        double recall = this.getRecall();
        double out = 2 * (precision * recall) / (precision + recall);
        if(isNaN(out))
            out = 0.0d;
        return out;
    }

    public int getPositiveCount() {
        return this.tp + this.fn;
    }

    public int getNegativeCount() {
        return this.tn + this.fp;
    }

    public void print() {
        System.out.println("=== Test Results ===");
        System.out.println("Confusion Matrix:");
        System.out.println(tp + "\t" + fn + "\t|\t" + this.getPositiveCount());
        System.out.println(fp + "\t" + tn + "\t|\t" + this.getNegativeCount());
        System.out.println("Accuracy: " + this.getAccuracy());
        System.out.println("Precision: " + this.getPrecision());
        System.out.println("Recall: " + this.getRecall());
        System.out.println("F1-Score: " + this.getF1());
        System.out.println();
    }
}
