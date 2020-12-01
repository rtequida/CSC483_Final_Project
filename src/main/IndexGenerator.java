package main;

import edu.stanford.nlp.simple.Sentence;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.*;
import org.tartarus.snowball.ext.PorterStemmer;

import javax.print.Doc;
import java.io.FileNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.io.File;
import java.io.IOException;

public class IndexGenerator {

    // Global variables
    int option;
    String indexPath;
    File dataSet = new File("src/resources/Complete_Data_Set");
    StringBuilder entry;
    Directory index;
    StandardAnalyzer analyzer;
    IndexWriter w;
    Stack<Boolean> tpl = new Stack<Boolean>();

    /*
     * Purpose: Calls method to build index.
     *
     * param: option, the user's choice of how to add content to the index
     */
    public IndexGenerator(int option) {
        this.option = option;
        buildIndex();
    }

    /*
     * Purpose: Makes or opens the index and then
     * opens the directory containing the wiki pages and goes
     * line by line through the files and passes them to the method that
     * adds them to the index.
     */
    private void buildIndex() {
        // Determine path for index directories
        if (option == 1) {
            indexPath = "src/resources/indexLem/";
        } else if (option == 2) {
            indexPath = "src/resources/indexStem/";
        } else {
            indexPath = "src/resources/indexNone/";
        }
        Path path = FileSystems.getDefault().getPath(indexPath);
        // Checking if directory exists or not and creating it if it doesn't
        File directory = new File(indexPath);
        // Make directory if it's not already created
        if (!directory.exists()) {
            directory.mkdir();
        }
        try {
            index = FSDirectory.open(path);
            // Check if index is already created
            if (!DirectoryReader.indexExists(index)) {
                analyzer = new StandardAnalyzer();
                IndexWriterConfig config = new IndexWriterConfig(analyzer);
                config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                w = new IndexWriter(index, config);
                // Going through each file in the directory
                for (File file : dataSet.listFiles()) {
                    filizer(file);
                }
                w.close();
            } else {
                analyzer = new StandardAnalyzer();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Purpose: Adds the passed in file to the index after properly
     * parsing its contents.
     *
     * param: file, the file to be parsed and added to the index
     */
    private void filizer(File file) {
        String title = "";
        String categories = "";
        entry = new StringBuilder();
        // Try to open file
        try (Scanner scanner = new Scanner(file)) {
            // Go through each line
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                int len = line.length();
                // Check for title
                if (len > 4 && line.substring(0,2).equals("[[") && line.substring(len - 2, len).equals("]]")) {
                    // Add to index if new title is found
                    if (!title.isEmpty() && !title.contains("Image:") && !title.contains("File:")) {
                        toTheIndexWithYou(title, categories.trim());
                    }
                    // Parse title line
                    entry = new StringBuilder();
                    title = line.substring(2, len - 2);
                    if (!tpl.empty()) {
                        tpl.clear();
                    }
                // Check for categories
                } else if (line.startsWith("CATEGORIES:")) {
                    categories = line.substring(12);
                // Check for other subject lines
                } else if (line.startsWith("=") && line.endsWith("=")) {
                    while (line.length() > 2 && line.startsWith("=") && line.endsWith("=")) {
                        line = line.substring(1, len - 1);
                        len = line.length();
                    }
                    // Don't add these to index
                    if (!line.equals("References") && !line.equals("See also") && !line.equals("External links") && !line.equals("Further reading") && !line.equals("Notes")) {
                        entry.append(line + " ");
                    }
                // Removing tpl sections
                } else tplPathToExile(line);
            }
            scanner.close();
            toTheIndexWithYou(title, categories.trim());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /*
     * Purpose: Adds titles and lines with categories to the index.
     *
     * param: title, a string representing the title of a wiki page
     *
     * param: categories, a string of the categories of a wiki page
     */
    private void toTheIndexWithYou(String title, String categories) {
        String goodStuff = entry.toString().trim().toLowerCase();
        categories = categories.toLowerCase();
        StringBuilder c = new StringBuilder();
        StringBuilder gs = new StringBuilder();
        // Check for empty strings before further processing
        if (categories.isEmpty()) {
            categories = ".";
        }
        if (goodStuff.isEmpty()) {
            goodStuff = ".";
        }
        // Option to lemmatize tokens going into the index
        if (option == 1) {
            for (String lemma: new Sentence(categories).lemmas()) {
                c.append(lemma + " ");
            }
            for (String lemma: new Sentence(goodStuff).lemmas()) {
                gs.append(lemma + " ");
            }
        // Option to stem tokens going into the index
        } else if (option == 2) {
            PorterStemmer stemmer = new PorterStemmer();
            for (String stem: new Sentence(categories).words()) {
                stemmer.setCurrent(stem);
                stemmer.stem();
                String stemWord = stemmer.getCurrent();
                c.append(stemWord + " ");
            }
            for (String stem: new Sentence(goodStuff).words()) {
                stemmer.setCurrent(stem);
                stemmer.stem();
                String stemWord = stemmer.getCurrent();
                gs.append(stemWord + " ");
            }
        // Option for leaving tokens as is
        } else {
            c.append(categories);
            gs.append(goodStuff);
        }
        // Adding lines to the index
        Document doc = new Document();
        // Title isn't tokenized
        doc.add(new StringField("title", title, Field.Store.YES));
        doc.add(new TextField("categories", c.toString().trim(), Field.Store.YES));
        doc.add(new TextField("text", title + " " + c.toString().trim() + " " + gs.toString().trim(), Field.Store.YES));
        try {
            w.addDocument(doc);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    /*
     * Purpose: Removes tpl sections from lines in wiki pages.
     *
     * param: line, a section of a wiki page
     */
    private void tplPathToExile(String line) {
        // Get starting and ending postions of tpl sections
        int TPLStart = line.indexOf("[tpl]");
        int TPLFinish = line.indexOf("[/tpl]");
        // TPL found so remove
        if (TPLStart != -1) {
            // Continue to do so until all TPLs are gone
            while (TPLStart != -1) {
                // Get content before tpl start tag
                entry.append(line.substring(0, TPLStart) + " ");
                // Remove tpl section for the rest of the line
                line = line.substring(TPLFinish + 6);
                // Look for remaining tpl tags
                TPLStart = line.indexOf("[tpl]");
                TPLFinish = line.indexOf("[/tpl]");
            }
        // Line has no tpl tags to add entire line to index
        } else {
            entry.append(line + " ");
        }
    }

    // Getter method for index
    public Directory getIndex() {
        return index;
    }

    // Getter method for analyzer
    public StandardAnalyzer getAnalyzer() {
        return analyzer;
    }
}
