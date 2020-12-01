package main;

import org.apache.lucene.document.Document;

public class ResultClass {

    // Global variables
    Document name;
    double score;

    /*
     * Purpose: Creates a ResultClass object to keep track of result names
     * and scores.
     */
    public ResultClass(Document name, double score) {
        this.name = name;
        this.score = score;
    }

    // Getter method for document name
    public Document getName() {
        return name;
    }

    // Getter method for document score
    public double getScore() {
        return score;
    }

}
