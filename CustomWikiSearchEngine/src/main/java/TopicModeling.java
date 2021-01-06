import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import edu.stanford.nlp.util.logging.RedwoodConfiguration;

public class TopicModeling {

    List<List<Document>> docsPerTopic;
    List<Document> docsHistory, docsScience, docsReligion;
    HashMap<String, Integer> hmHist, hmScienc, hmRel;

    public TopicModeling(String field) throws IOException {
        docsPerTopic = topicExtraction();

        docsHistory = docsPerTopic.get(0);
        docsScience = docsPerTopic.get(1);
        docsReligion = docsPerTopic.get(2);

        hmHist = sortByValue(topicModeling(field, docsHistory));
        hmScienc = sortByValue(topicModeling(field, docsScience));
        hmRel = sortByValue(topicModeling(field, docsReligion));


    }


    /**
     * Method to extract documents per topic ie History, Science & Religion/Belief
     * @return List<List<Document>> documents per topics
     * @throws IOException for index directory
     */
    public static List<List<Document>> topicExtraction() throws IOException {

        List<List<Document>> docsPerTopic = new ArrayList<>();

        /* Counters to test */
        int history = 0;
        int science = 0;
        int religion = 0;
        int rest = 0;
        int intersection = 0;

        /* list of documents per topic */
        List<Document> historyDocList = new ArrayList<>();
        List<Document> scienceDocList = new ArrayList<>();
        List<Document> religionDocList = new ArrayList<>();

        /* Stanford NLP core to lemmatize the topics */
        Properties props = new Properties();
        // set the list of annotators to run
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        //no logs
        RedwoodConfiguration.current().clear().apply();
        // build pipeline
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        /* Read the document indexed */
        Directory dir = FSDirectory.open(Paths.get("index_folder"));
        IndexReader reader = DirectoryReader.open(dir);

        //for all the document indexed
        for (int i = 0; i < reader.maxDoc(); i++) {
            Document doc = reader.document(i);
            String[] topics = doc.getValues("topics");

            int indexh = 0;
            int indexs = 0;
            int indexr = 0;

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
                    if(result.equals("histor") || result.equals("histori")) {
                        if (indexh == 0) {
                            historyDocList.add(doc);
                            indexh++;
                            history++;
                        }
                    }
                    if(result.equals("scienc") || result.equals("biotechnologi") || result.equals("biologi")
                            || result.equals("astronomi") || result.equals("chemistri")) {
                        if (indexs == 0) {
                            scienceDocList.add(doc);
                            indexs++;
                            science++;
                        }
                    }
                    if(result.equals("religion") || result.equals("religi") || result.equals("belief")) {
                        if (indexr == 0) {
                            religionDocList.add(doc);
                            indexr++;
                            religion++;
                        }
                    }
                }
            }
            //if no stems have been detected -> rest
            if (indexh + indexs + indexr == 0) {
                rest++;
                for(String topic : topics){ System.out.println(topic);}
            }
            else if (indexh + indexs + indexr > 1) {
                intersection++;
            }

        }
        System.out.println("History : " + history + "; Science : " + science + "; Religion : " + religion + "; Rest : " + rest + "; Intersection : " +intersection);

        docsPerTopic.add(historyDocList);
        docsPerTopic.add(scienceDocList);
        docsPerTopic.add(religionDocList);

        reader.close();
        dir.close();

        return docsPerTopic;
    }

    /**
     * Method to tokenize and stop word removal the field for topic modeling
     * @param docsPerTopic documents per topic
     * @return HashMap<String, Integer> occurences of words in documents per topic
     */
    private static HashMap<String, Integer> topicModeling(String field, List<Document> docsPerTopic) throws IOException {

        HashMap<String, Integer> myWordsCount = new HashMap<>();

        List<String> stopwords = Files.readAllLines(Paths.get("src/main/resources/english_stopwords.txt"));

        for(Document doc : docsPerTopic) {
            String f = doc.getField(field).toString();

            Properties props = new Properties();
            props.setProperty("annotators", "tokenize");
            RedwoodConfiguration.current().clear().apply();
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
            CoreDocument document = pipeline.processToCoreDocument(f);

            String[] words;
            for (CoreLabel tok : document.tokens()) {

                words = tok.word().replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
                for (String word : words) {
                    //si mot non vide ou stopword ou mots < 2 length
                    if (!word.isEmpty() && !stopwords.contains(word) && word.length() > 2) {
                        if (myWordsCount.containsKey(word)) {
                            myWordsCount.replace(word, myWordsCount.get(word) + 1);
                        } else {
                            myWordsCount.put(word, 1);
                        }
                    }
                }
                //for (String word : words) if (!word.isEmpty() ) System.out.println(String.format("%s", word));
            }
        }
        return myWordsCount;
    }

    /**
     * Function to sort hashmap by values
     * @param unSortedMap HashMap<String, Integer> not yet sorted
     * @return HashMap<String, Integer> sorted by Integer (ascending order)
     */
    private static HashMap<String, Integer> sortByValue(HashMap<String, Integer> unSortedMap)
    {
        //LinkedHashMap preserve the ordering of elements in which they are inserted
        LinkedHashMap<String, Integer> reverseSortedMap = new LinkedHashMap<>();

        //Use Comparator.reverseOrder() for reverse ordering
        unSortedMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> reverseSortedMap.put(x.getKey(), x.getValue()));

        return reverseSortedMap;
    }


    /**
     * Method to display in log the top occurences words
     * @param hm hashmap of String, Integer -> word, occurences
     * @param top number of printed words
     * @param docs list of docs to calculate idf and tfidf
     * @throws IOException when opening reader
     */
    public static void displayTopWords(HashMap<String, Integer> hm, int top, List<Document> docs) throws IOException {

        HashMap<String, Integer> hmOrd = sortByValue(hm);
        int count = 1;
        for (Map.Entry<String, Integer> en : hmOrd.entrySet()) {
            if(count <= top ) {
                double idf = idf(en.getKey(), "abstract", docs);
                double tfidf = tfidf(idf, en.getValue());
                System.out.println(count + " -> Key = " + en.getKey() + ", Occurences = " + en.getValue() + ", IDF  = " + idf + ", TFIDF = " + tfidf);
            }
            count++;
        }

    }



    /**
     * Inverse document frequency
     * @param term to calculate
     * @param field to explore
     * @param docsTopic as a subset of the dump
     * @return idf log(N/n) smoothed (+1) inverse document frequency smooth
     * @throws IOException when reading docs or stopwords
     */
    public static double idf(String term, String field, List<Document> docsTopic) throws IOException {
        int N = docsTopic.size() + 1;
        int n = 1;

        for(Document doc : docsTopic) {
            String f = doc.getField(field).toString();

            Properties props = new Properties();
            props.setProperty("annotators", "tokenize");
            RedwoodConfiguration.current().clear().apply();
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
            CoreDocument document = pipeline.processToCoreDocument(f);

            boolean here= false;
            String[] words;
            for (CoreLabel tok : document.tokens()) {

                words = tok.word().replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
                for (String word : words) {
                    if (word.equals(term)) {
                        here = true;
                    }
                }
            }
            if(here) n=n+1;
        }
        //System.out.println("Term :" + term + " N : " + N + " n : " + n);
        return Math.log10(N/n);
    }

    private static void writeInFileOccurences(String prefixname) {
        try {
            File myObj = new File(prefixname + "_occurences.txt");
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            } else {
                System.out.println("File already exists.");
            }


        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    /**
     * tfid function
     * @param idf calculated
     * @param tf calculated
     * @return tfidf value
     */
    public static double tfidf(double idf, Integer tf) {
        return tf * idf;
    }

    public static void main(String[] args) throws IOException {

        /* topic Modeling */
        List<List<Document>> docsPerTopic = topicExtraction();

        String field = "abstract";
        /* History */
        List<Document> docsHistory = docsPerTopic.get(0);
        HashMap<String, Integer> hmHist = topicModeling(field, docsHistory);

        /* Sciences */
        List<Document> docsScience = docsPerTopic.get(1);
        HashMap<String, Integer> hmScienc = topicModeling(field, docsScience);

        /* Religion & belief */
        List<Document> docsReligion = docsPerTopic.get(2);
        HashMap<String, Integer> hmRel = topicModeling(field, docsReligion);


        System.out.println("\nTop words of History :");
        displayTopWords(hmHist, 15, docsHistory);
        System.out.println("\nTop words of Sciences :");
        displayTopWords(hmScienc, 15, docsScience);
        System.out.println("\nTop words of Religion&Belief :");
        displayTopWords(hmRel, 15, docsReligion);

    }
}
