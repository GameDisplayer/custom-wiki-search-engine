import com.opencsv.CSVReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


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

        IndexWriter writer = new IndexWriter(dir, iwc);
        addDocuments(data,writer);

        writer.close();
    }


    private void addDocuments(List<List<String>> data, IndexWriter writer) throws Exception {
        int documentsAdded = 0;

        for (List<String> datum : data) {
            Document doc = getDocument(datum);
            writer.addDocument(doc);
            documentsAdded++;
        }

        System.out.println("documents added: " + documentsAdded);
    }


    private Document getDocument(List<String> value) {

        Document lucene_doc = new Document();
        lucene_doc.add(new StringField("title", value.get(0), Field.Store.YES));
        lucene_doc.add(new StringField("abstract", value.get(1), Field.Store.YES));
        lucene_doc.add(new TextField("content", value.get(2), Field.Store.YES));
        List<String> listTopics = getTopics(value.get(3));
        for (String topic : listTopics) {
            //Lucene documents support the addition of multiple fields of the same name
            lucene_doc.add(new StringField("topics", topic, Field.Store.YES));
        }
        //maybe the link of the article ?

        return lucene_doc;

    }

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



    public static void main(String[] args) throws Exception {
        Main main = new Main();
        List<List<String>> documents = main.parseCsv("WikiData.csv");

        //main.createIndex(documents);

        String field="content";
        String searchFor="history";
        main.search(field, searchFor);

    }

}
