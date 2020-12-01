/*
 * AUTHOR: Ruben Tequida
 * FILE: Main.java
 * ASSIGNMENT: Final Project
 * COURSE: CSC 583; Fall 2020
 * Purpose: This program utilizes Lucene in order to create an
 * index and uses Lucene queries to get the documents
 * that match the query as well as the documents similarity score
 * to that query.
 */

package main;

import java.io.IOException;
import java.util.Scanner;

// Purpose: Calls methods to create/open the index as well as calls
// to read in the questions and print out the results.
public class Main {
    public static void main(String[] args ) {
        Scanner input = new Scanner(System.in);
        int again = 1;
        while (again == 1) {
            // Get processing style from user
            System.out.println("Do you want to use Lemmatization, Stemming, or niether?");
            System.out.println("1. Lemmatization\n2. Stemming\n3. None");
            System.out.print("Type option 1, 2, or 3: ");
            int option = Integer.valueOf(input.nextLine());
            // Make/open index
            IndexGenerator index = new IndexGenerator(option);
            // Get scoring method from user
            System.out.println("Which type of scoring method do you want to use?");
            System.out.println("1. Default (BM25)\n2. Boolean\n3. Jelinek-Mercer\n4. TF-IDF");
            System.out.print("Type option 1, 2, 3, or 4: ");
            int method = Integer.valueOf(input.nextLine());
            // Process questions
            QueryDissector scalpel = new QueryDissector(index, option, method);
            // Print results
            System.out.println("Percentage of correct answers using precision at 1: " + scalpel.getPercent());
            System.out.println("Mean Reciprocal Rank result: " + scalpel.getMRR());
            System.out.println("Do you want to run again?");
            System.out.println("1. Yes\n2. No");
            System.out.print("Type option 1 or 2: ");
            again = Integer.valueOf(input.nextLine());
            try {
                index.getIndex().close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
