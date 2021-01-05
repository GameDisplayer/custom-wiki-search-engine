import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;

import static org.apache.lucene.index.MultiTerms.getTerms;

public class Statistics {
    

    public static void basicInfo(String termText, String field) throws IOException {
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("index_folder")));

        Term termInstance = new Term(field, termText);
        long termFreq = reader.totalTermFreq(termInstance);
        long docCount = reader.docFreq(termInstance);

        System.out.println("term: "+termText+", termFreq = "+termFreq+", docCount = "+docCount);

        reader.close();
    }

    public static void iterateThroughVocab(String field) throws IOException {

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("index_folder")));

        double N = reader.numDocs();
        double corpusLength = reader.getSumTotalTermFreq( field );

        System.out.printf( "%-30s%-10s%-10s%-10s%-10s\n", "TERM", "DF", "TOTAL_TF", "IDF", "p(w|c)" );

        // Get the vocabulary of the index.
        Terms voc = getTerms( reader, field );
        // You need to use TermsEnum to iterate each entry of the vocabulary.
        TermsEnum termsEnum = voc.iterator();
        BytesRef term;
        int count = 0;
        while ( ( term = termsEnum.next() ) != null ) {
            count++;
            String termstr = term.utf8ToString(); // get the text string of the term
            int df = termsEnum.docFreq(); // get the document frequency (DF) of the term
            long freq = termsEnum.totalTermFreq(); // get the total frequency of the term
            double idf = Math.log( ( N + 1 ) / ( df + 1 ) );
            double pwc = freq / corpusLength;
            System.out.printf( "%-30s%-10d%-10d%-10.2f%-10.8f\n", termstr, df, freq, idf, pwc );
            if ( count >= 100 ) {
                break;
            }
        }

        reader.close();
    }


    public static void corpusLevelStatistics(String term, String field) throws IOException {

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

        basicInfo("history", "content");

        corpusLevelStatistics("history", "abstract");
        corpusLevelStatistics("religion", "abstract");
        corpusLevelStatistics("science", "abstract");

        iterateThroughVocab("content");


    }
}
