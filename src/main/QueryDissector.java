package main;

import edu.stanford.nlp.simple.Sentence;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class QueryDissector {

    // Global variables
    IndexGenerator index;
    int option;
    int method;
    int hitsPerPage;
    double percentCorrect;
    double mrrResult;
    String questions;
    IndexReader reader;
    IndexSearcher searcher;
    StandardAnalyzer analyzer;

    /*
     * Purpose: Processes each question in the desired text document
     * and keeps track of which answers are correct and the placement of the
     * correct answers if the first guess is wrong in order to calculate
     * the mean reciprocal rank.
     *
     * param: index, the index of all the wiki pages
     *
     * param: option, processing style for the index
     *
     * param: method, scoring method for queries
     */
    public QueryDissector(IndexGenerator index, int option , int method) {
        // Initializing global variables
        this.index = index;
        this.option = option;
        this.method = method;
        hitsPerPage = 10;
        int total = 0;
        int rightAns = 0;
        double mrr = 0.0;
        String category = "";
        String clue = "";
        String answer = "";
        ArrayList<ResultClass> results;
        // Location of questions text file
        questions = "src/resources/questions.txt";
        try (Scanner scanner = new Scanner(new File(questions))) {
            // Read through each question
            while (scanner.hasNextLine()) {
                category = scanner.nextLine();
                clue = scanner.nextLine();
                answer = scanner.nextLine();
                scanner.nextLine();
                // Query the index
                results = INeedAnswers(category + " " + clue);
//                Option to print out guesses in results list
//                for (int i = 0; i < results.size(); i++) {
//                    System.out.println("Name: " + results.get(i).getName().get("title"));
//                    System.out.println("Score: " + results.get(i).getScore());
//                }
                // Keep track of number of questions asked
                total++;
                // See if the top ranked guess matches the answer
                if (results.size() > 0 && results.get(0).getName().get("title").equals(answer)) {
                    rightAns++;
                    mrr++;
                    // See if answer is in top 10
                } else {
                    for (int i = 0;i < results.size(); i++) {
                        if (results.get(i).getName().get("title").equals(answer)) {
                            mrr += (double) 1 / (i + 1);
                            break;
                        }
                    }
                }
            }
            percentCorrect = (double) rightAns / total;
            mrrResult = (double) mrr / total;
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

    }

    /*
     * Purpose: Parses the query and queries the index getting the programs
     * guesses for the question.
     *
     * param: clue, the query to be passed to the index
     */
    private ArrayList<ResultClass> INeedAnswers(String clue) {
        ArrayList<ResultClass> results = new ArrayList<>();
        analyzer = index.getAnalyzer();
        // Parse clue
        clue = dissector(clue);
        try {
            reader = DirectoryReader.open(index.getIndex());
            searcher = new IndexSearcher(reader);
            // Get query for index
            Query q = new QueryParser("text", analyzer).parse(clue);
            // Determine the type of scoring method to use
            if (method == 2) {
                searcher.setSimilarity(new BooleanSimilarity());
            } else if (method == 3) {
                searcher.setSimilarity(new LMJelinekMercerSimilarity((float) 0.5));
            } else if (method == 4) {
                searcher.setSimilarity(new ClassicSimilarity());
            }
            // Get top hits and put them in results list as ResultClass objects
            TopDocs docs = searcher.search(q, hitsPerPage);
            ScoreDoc[] hits = docs.scoreDocs;
            for (int i = 0; i < hits.length; i++) {
                int docId = hits[i].doc;
                Document d = searcher.doc(docId);
                ResultClass rc = new ResultClass(d, hits[i].score);
                results.add(rc);
            }
            reader.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        return results;
    }

    /*
     * Purpose: Processes the clue according to the users choice.
     *
     * param: clue, the clue to the question to answer
     */
    private String dissector(String clue) {
        StringBuilder c = new StringBuilder();
        // Lemmatize the clue
        if (option == 1) {
            for (String lemma: new Sentence(clue.toLowerCase()).lemmas()) {
                c.append(lemma + " ");
            }
        // Stem the clue
        } else if (option == 2) {
            PorterStemmer stemmer = new PorterStemmer();
            for (String stem: new Sentence(clue.toLowerCase()).words()) {
                stemmer.setCurrent(stem);
                stemmer.stem();
                String stemWord = stemmer.getCurrent();
                c.append(stemWord + " ");
            }
        // Do nothing to the clue
        } else {
            for (String word : new Sentence(clue.toLowerCase()).words()) {
                c.append(word + " ");
            }
        }
        return c.toString();
    }

    // Getter method for the percent correct
    public Double getPercent() {
        return percentCorrect * 100;
    }

    // Getter method for the mrr result
    public Double getMRR() {
        return mrrResult * 100;
    }
}
