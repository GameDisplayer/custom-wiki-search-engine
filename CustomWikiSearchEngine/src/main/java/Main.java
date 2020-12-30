import com.opencsv.CSVReader;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.simple.Sentence;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static java.nio.file.Files.notExists;


public class Main {

    /**
     * Parse CSV file in order to get row by row values
     * @param fileName : csv file name
     * @return List<List<String>> : list of list of data
     * @throws Exception csvReader
     */
    private List<List<String>> parseCsv(String fileName) throws Exception {

        List<List<String>> records = new ArrayList<>();
        try (CSVReader csvReader = new CSVReader(new FileReader("src/main/resources/"+fileName))) {
            String[] values;
            while ((values = csvReader.readNext()) != null ) {
                //Be aware of blank lines
                if (values.length > 1) {
                    records.add(Arrays.asList(values));
                }
            }
        }
        //Remove the header
        records.remove(records.get(0));
        return records;
    }

    /**
     * Create Lucene Index of the wiki documents extracted
     * @param data all the data extracted from the csv file
     * @throws Exception indexWriter
     */
    private void createIndex(List<List<String>> data) throws Exception {

        Directory dir = FSDirectory.open(Paths.get("index_folder"));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

        iwc.setOpenMode(OpenMode.CREATE); 			// Create a NEW index in the directory

        // for better indexing performance, increase the RAM buffer
        iwc.setRAMBufferSizeMB(256.0);

        IndexWriter writer = new IndexWriter(dir, iwc);
        addDocuments(data,writer);

        writer.close();
    }


    /**
     * Add documents based on the parsed CSV
     * @param data
     * @param writer
     * @throws Exception
     */
    private void addDocuments(List<List<String>> data, IndexWriter writer) throws Exception {
        int documentsAdded = 0;

        for (List<String> datum : data) {
            Document doc = getDocument(datum);
            writer.addDocument(doc);
            documentsAdded++;
        }

        System.out.println("documents added: " + documentsAdded);
    }


    /**
     * Method to index a single document
     * @param value
     * @return
     */
    private Document getDocument(List<String> value) {

        Document lucene_doc = new Document();
        lucene_doc.add(new StringField("title", value.get(0), Field.Store.YES));
        lucene_doc.add(new StringField("abstract", value.get(1), Field.Store.YES));
        lucene_doc.add(new TextField("content", value.get(2), Field.Store.YES));
        List<String> listTopics = getTopics(value.get(3));
        for (String topic : listTopics) {
            //Lucene documents support the addition of multiple fields of the same name
            if(topic != null && topic.length() > 0) lucene_doc.add(new StringField("topics", topic, Field.Store.YES));
        }
        //maybe the link of the article ?

        return lucene_doc;

    }

    /**
     * Basic search method
     * @param field
     * @param searchFor
     * @return
     * @throws IOException
     * @throws ParseException
     */
    public List<String> search(String field, String searchFor) throws IOException, ParseException {
        int max_results = 100;
        System.out.println("Searching for " + searchFor + " at " + field);
        Directory dir = FSDirectory.open(Paths.get("index_folder"));
        IndexReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = new StandardAnalyzer();

        QueryParser parser = new QueryParser(field, analyzer);
        Query query = parser.parse(searchFor);


        TopDocs results = searcher.search(query, max_results);
        ScoreDoc[] hits = results.scoreDocs;

        List<String> l = showResults(hits, searcher);
        return l;

    }

    /**
     * Get topics from the csv value
     * @param topics
     * @return
     */
    private static List<String> getTopics(String topics){
        String[] d = topics.split("'");//"'|,|\\[|\\]");
        List<String> listTopics = new ArrayList<>();
        for (String s : d) {
            if (s.length() > 2) listTopics.add(s);
        }
        return listTopics;
    }

    /**
     * Show the best results of a query
     * @param hits
     * @param searcher
     * @throws IOException
     */
    private List<String> showResults(ScoreDoc[] hits, IndexSearcher searcher) throws IOException {
        List<String> list = new ArrayList<>();
        for (ScoreDoc scoreDoc : hits) {
            System.out.println("doc="+scoreDoc.doc+" score="+scoreDoc.score);
            Document doc = searcher.doc(scoreDoc.doc);
            System.out.println("\t" + doc.get("title"));

            list.add(doc.get("title"));
        }

        return list;
    }


    private static void topicModeling() throws IOException {

        /* Counters to test */
        int history = 0;
        int science = 0;
        int religion = 0;
        int rest = 0;
        ArrayList<String> historyWordList = new ArrayList<>();

        /* Stanford NLP core to lemmatize the topics */
        Properties props = new Properties();
        // set the list of annotators to run
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        // build pipeline
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        /* Read the document indexed */
        Directory dir = FSDirectory.open(Paths.get("index_folder"));
        IndexReader reader = DirectoryReader.open(dir);
        //for all the document indexed
        for (int i = 0; i < reader.maxDoc(); i++) {
            Document doc = reader.document(i);
            String[] topics = doc.getValues("topics");
            String abstr = doc.getField("abstract").getCharSequenceValue().toString();

            int index = 0;
            //for all the topics of a document
            //System.out.println("TOPS OF DOC");
            for (String topic : topics) {
                //System.out.println(topic);

                // create a document object for nlp
                CoreDocument document = pipeline.processToCoreDocument(topic);
                //System.out.println("TOPIC" + i);
                for (CoreLabel tok : document.tokens()) {
                    PorterStemmer stem = new PorterStemmer();
                    stem.setCurrent(tok.lemma().toLowerCase());
                    stem.stem();
                    String result = stem.getCurrent();
                    switch (result) {
                        case "histor":
                        case "histori":
//                            CoreDocument a = pipeline.processToCoreDocument(abstr);
//                            for (CoreLabel tik : a.tokens()) {
//                                historyWordList.add(tik.lemma());
//                            }
                            history++;
                            index++;
                        case "scienc":
                        case "biotechnologi":
                        case "biologi":
                        case "biolog":
                        case "astronomi":
                        case "chemistri":
                            science++;
                            index++;
                        case "religion":
                        case "religi":
                        case "belief":
                            religion++;
                            index++;
                        default:
                            break;
                    }
                }
            }
            //if no stems have been detected -> rest
            if (index == 0) {
                rest++;
                //for(String topic : topics){ System.out.println(topic);}
            }

        }
        System.out.println("History : " + history + "; Science : " + science + "; Religion : " + religion + "; Rest : " + rest);

        //System.out.println(historyWordList.get(0));
    }

    public static void main(String[] args) throws Exception {
        Main main = new Main();


        /* NOTE : HEGEL ~300 in CSV text -> topics !! */
        /* We parse the cvs file and we create indexes */
        if(notExists(Paths.get("index_folder"))) {
            List<List<String>> documents = main.parseCsv("WikiData.csv");
            main.createIndex(documents);
        }
        /* topic Modeling */
        topicModeling();

        /* Test stemmming
        PorterStemmer stem = new PorterStemmer();
        stem.setCurrent("historically");
        stem.stem();
        String result = stem.getCurrent();
        System.out.println(result);
         */

        /* Test search
        String field="content";
        String searchFor="history";
        main.search(field, searchFor);
         */

    }

}
