import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;

public class Statistics {


    public static void corpusLevelStatistics(String term, String field) throws IOException {
        // Let's just count the IDF and P(w|corpus) for the word "reformulation" in the "text" field

        IndexReader index = DirectoryReader.open(FSDirectory.open(Paths.get("index_folder")));


        int N = index.numDocs(); // the total number of documents in the index
        int n = index.docFreq( new Term( field, term ) ); // get the document frequency of the term in the "text" field
        double idf = Math.log( ( N + 1 ) / ( n + 1 ) ); // well, we normalize N and n by adding 1 to avoid n = 0

        System.out.printf( "%-30sN=%-10dn=%-10dIDF=%-8.2f\n", term, N, n, idf );

        long corpusTF = index.totalTermFreq( new Term( field, term ) ); // get the total frequency of the term in the "text" field
        long corpusLength = index.getSumTotalTermFreq( field ); // get the total length of the "text" field
        double pwc = 1.0 * corpusTF / corpusLength;

        System.out.printf( "%-30slen(corpus)=%-10dfreq(%s)=%-10dP(%s|corpus)=%-10.6f\n", term, corpusLength, term, corpusTF, term, pwc );

        // remember to close the index and the directory
        index.close();
    }

    public static void main(String[] args) throws IOException {
        corpusLevelStatistics("history", "abstract");
        corpusLevelStatistics("religion", "abstract");
        corpusLevelStatistics("science", "abstract");


    }
}
