import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;

import static org.apache.lucene.index.MultiTerms.getTerms;

public class Statistics {


    /**
     * Method for providing basic information of a specific term in a field on the entire corpus indexed
     * @param termText term/word
     * @param field title, abstract, content or topics
     * @throws IOException indexreader
     */
    public static void basicInfo(String termText, String field) throws IOException {
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("index_folder")));

        Term termInstance = new Term(field, termText);
        long termFreq = reader.totalTermFreq(termInstance);
        long docCount = reader.docFreq(termInstance);

        System.out.println("term: "+termText+", termFreq = "+termFreq+", docCount = "+docCount);

        reader.close();
    }

    /**
     * method to iterate through the vocabulary of a field on the entire corpus (without stop words removal!)
     * @param field of the indexed documents
     * @throws IOException indexreader
     */
    public static void iterateThroughVocab(String field) throws IOException {

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("index_folder")));

        double N = reader.numDocs();
        double corpusLength = reader.getSumTotalTermFreq( field );

        System.out.println(N + " _ " + corpusLength );

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


    /**
     * Method to have some more advanced information on a term in a field on the entire corpus
     * @param term word
     * @param field of documents
     * @throws IOException indexreader
     */
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


    /**
     * Method to get average informations of the specified field on the corpus
     * @param field of documents
     * @return average double value
     * @throws IOException indexreader
     */
    public static double average(String field) throws IOException{
        IndexReader index = DirectoryReader.open(FSDirectory.open(Paths.get("index_folder")));

        int N = index.numDocs(); // the total number of documents in the index

        int n=0;
        //for all the document indexed
        for (int i = 0; i < index.maxDoc(); i++) {
            Document doc = index.document(i);
            if(field.equals("topics"))
            {
                String[] topics = doc.getValues("topics");
                n+=topics.length;
            }
            else {
                String f = "";
                if (doc.getField(field) != null) f = doc.getField(field).toString();
                n += f.length();
            }
        }

        index.close();

        return n/N;
    }

    public static void main(String[] args) throws IOException {

        /* some tests of functions */
        basicInfo("history", "content");

        corpusLevelStatistics("history", "abstract");
        corpusLevelStatistics("religion", "abstract");
        corpusLevelStatistics("science", "abstract");

        iterateThroughVocab("abstract");

        System.out.println("Average length of titles = " + average("title"));
        System.out.println("Average length of abstracts = " + average("abstract"));
        System.out.println("Average length of content = " + average("content"));

        System.out.println("Average number of topics = " + average("topics"));


    }
}
