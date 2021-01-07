import com.opencsv.CSVReader;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.FlattenGraphFilter;
import org.apache.lucene.analysis.standard.ClassicAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.synonym.SynonymMap.Builder;
import org.apache.lucene.analysis.synonym.WordnetSynonymParser;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.AttributeImpl;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.file.Files.notExists;
import static java.nio.file.Files.setOwner;


public class Main {
    private ScoreDoc[] actualScores;
    private final String indexFolder = "index_folder";
    private final String indexFolderAdvanced = "index_folder_advanced";

    /**
     * Constructor of the main class
     */
    public Main(){
        //Indexation of documents for basic search
        try {
            if (notExists(Paths.get(indexFolder))) {
                List<List<String>> documents = this.parseCsv("WikiData.csv");
                this.createIndex(documents, indexFolder, new WikipediaAnalyzer());
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        //Indexation of documents for advanced search
        try {
            if (notExists(Paths.get(indexFolderAdvanced))){
                List<List<String>> documents = this.parseCsv("WikiData.csv");
                this.createIndex(documents, indexFolderAdvanced, new WikipediaAnalyzerSynonyms(true));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Class in order to create our analyzer
     * Close to StandardAnalyzer, just use WikipediaTokenizer instead of StandardTokenizer
     */
    public class WikipediaAnalyzer extends Analyzer{

        @Override
        protected TokenStreamComponents createComponents(String s) {
            try {
                List<String> stopwords = Files.readAllLines(Paths.get("src/main/resources/english_stopwords.txt"));
                Tokenizer source = new WikipediaTokenizer();
                TokenStream lower = new LowerCaseFilter(source);
                TokenStream stopWords = new StopFilter(lower, StopFilter.makeStopSet(stopwords, true));

                return new TokenStreamComponents(source, stopWords);
            }catch (IOException ioException) {
                ioException.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Class in order to create our analyzer for synonyms
     */
    public class WikipediaAnalyzerSynonyms extends Analyzer{
        boolean indexing;

        /**
         * Constuctor allowing the analyzer to know if is indexing or not
         * @param index true if we are indexing, false otherwise
         */
        public WikipediaAnalyzerSynonyms(boolean index){
            indexing = index;
        }

        @Override
        protected TokenStreamComponents createComponents(String s) {
            try {
                List<String> stopwords = Files.readAllLines(Paths.get("src/main/resources/english_stopwords.txt"));
                Tokenizer source = new WikipediaTokenizer();
                WordnetSynonymParser wordNetparser = new WordnetSynonymParser(true, true, new StandardAnalyzer(CharArraySet.EMPTY_SET));
                wordNetparser.parse(new FileReader("src/main/resources/wordnet_prolog/wn_s.pl"));
                SynonymMap synonymMap = wordNetparser.build();
                TokenStream synonymFilter = new SynonymGraphFilter(source, synonymMap, false);
                TokenStream stopWords;
                if(indexing) {
                    TokenStream flat = new FlattenGraphFilter(synonymFilter);
                    stopWords = new StopFilter(flat, StopFilter.makeStopSet(stopwords,true));
                }else{
                    stopWords = new StopFilter(synonymFilter, StopFilter.makeStopSet(stopwords,true));
                }
                return new TokenStreamComponents(source, stopWords);
            } catch (IOException | java.text.ParseException ioException) {
                ioException.printStackTrace();
            }
            return null;
        }
    }

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
     * @param indexFolderName the name of the index folder
     * @param analyzer the analyzer used
     * @throws Exception indexWriter
     */
    private void createIndex(List<List<String>> data, String indexFolderName, Analyzer analyzer) throws Exception {

        Directory dir = FSDirectory.open(Paths.get(indexFolderName));
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
     * @param data all the documents to add
     * @param writer the IndexWriter
     * @throws Exception of the IndexWriter
     */
    private void addDocuments(List<List<String>> data, IndexWriter writer) throws Exception {
        int documentsAdded = 0;

        for (List<String> datum : data) {
            Document doc = getDocument(datum);
            writer.addDocument(doc);
            documentsAdded++;
        }

        //System.out.println("documents added: " + documentsAdded);
    }


    /**
     * Method to index a single document
     * @param value the value of the document
     * @return Document : the document added
     */
    private Document getDocument(List<String> value) {

        Document lucene_doc = new Document();
        lucene_doc.add(new TextField("title", value.get(0), Field.Store.YES));
        lucene_doc.add(new TextField("abstract", value.get(1), Field.Store.YES));
        lucene_doc.add(new TextField("content", value.get(2), Field.Store.YES));
        List<String> listTopics = getTopics(value.get(3));
        for (String topic : listTopics) {
            //Lucene documents support the addition of multiple fields of the same name
            if(topic != null && topic.length() > 0) lucene_doc.add(new StringField("topics", topic, Field.Store.YES));
        }

        return lucene_doc;
    }

    /**
     * Basic search method
     * @param field the field in which we want to search
     * @param searchFor the query
     * @param synonyms true if we want synonyms, false otherwise
     * @return List<String> : the answer to the querry
     * @throws IOException when opening indexreader
     * @throws ParseException for results
     */
    public List<Document> search(String field, String searchFor, boolean synonyms) throws IOException, ParseException {
        int max_results = 100;
        //System.out.println("Searching for " + searchFor + " at " + field);
        String indexFolder;
        Analyzer analyzer;
        if(synonyms){
            indexFolder = this.indexFolderAdvanced;
            analyzer = new WikipediaAnalyzerSynonyms(false);
        }else{
            indexFolder = this.indexFolder;
            analyzer = new WikipediaAnalyzer();
        }
        Directory dir = FSDirectory.open(Paths.get(indexFolder));
        IndexReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);

        QueryParser parser = new QueryParser(field, analyzer);
        Query query = parser.parse(searchFor);

        TopDocs results = searcher.search(query, max_results);
        ScoreDoc[] hits = results.scoreDocs;
        actualScores = hits;
        return showResults(hits, searcher);

    }

    /**
     * Basic search in multiple fields
     * @param fields the list of field in which we want to search
     * @param searchFor the list of querry (one by fields)
     * @param synonyms true if we want synonyms, false otherwise
     * @return List<String> the answer of the querry
     * @throws IOException when opening IndexReader
     * @throws ParseException when parsing for response
     */
    public List<Document> searchMultipleFields(String[] fields, String[] searchFor, boolean synonyms) throws IOException, ParseException {
        int max_results = 100;
        String indexFolder;
        Analyzer analyzer;
        if(synonyms){
            indexFolder = this.indexFolderAdvanced;
            analyzer = new WikipediaAnalyzerSynonyms(false);
        }else{
            indexFolder = this.indexFolder;
            analyzer = new WikipediaAnalyzer();
        }
        Directory dir = FSDirectory.open(Paths.get(indexFolder));
        IndexReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);

        Query matchQuery = MultiFieldQueryParser.parse(searchFor, fields, analyzer);

        TopDocs results = searcher.search(matchQuery, max_results);
        ScoreDoc[] hits = results.scoreDocs;

        actualScores = hits;

        return showResults(hits, searcher);
    }

    /**
     * Get topics from the csv value
     * @param topics a String containing all topics of a document
     * @return the list of topics extracted
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
     * @param hits ScoreDoc[] variable
     * @param searcher IndexSearcher for result output
     * @throws IOException when displaying score docs
     */
    private List<Document> showResults(ScoreDoc[] hits, IndexSearcher searcher) throws IOException {
        List<Document> list = new ArrayList<>();
        for (ScoreDoc scoreDoc : hits) {
            //System.out.println("doc="+scoreDoc.doc+" score="+scoreDoc.score);
            Document doc = searcher.doc(scoreDoc.doc);
            //System.out.println("\t" + doc.get("title"));
            list.add(doc);
        }
        return list;
    }

    /**
     * Getter for the parameter actualScores
     * @return actualScores
     */
    public ScoreDoc[] getActualScores(){
        return this.actualScores;
    }

}
