import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import edu.stanford.nlp.util.logging.RedwoodConfiguration;

public class TopicModeling {

    final static String RESOURCES_PATH = "src/main/resources/";

    List<List<Document>> docsPerTopic;
    List<Document> docsHistory, docsScience, docsReligion;
    HashMap<String, Integer> hmHist, hmScienc, hmRel;

    public TopicModeling(String field) throws IOException {
        docsPerTopic = topicExtraction();

        docsHistory = docsPerTopic.get(0);
        docsScience = docsPerTopic.get(1);
        docsReligion = docsPerTopic.get(2);

        hmHist = sortByValue(topicProfileOccurences(field, docsHistory));
        hmScienc = sortByValue(topicProfileOccurences(field, docsScience));
        hmRel = sortByValue(topicProfileOccurences(field, docsReligion));


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
                    if(result.equals("histor") || result.equals("histori") || result.equals("pseudohistori")) {
                        if (indexh == 0) {
                            historyDocList.add(doc);
                            indexh++;
                            history++;
                        }
                    }
                    if(result.equals("scienc") || result.equals("biotechnologi") || result.equals("biologi")
                            || result.equals("astronomi") || result.equals("chemistri") || result.equals("pseudosci")) {
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
                //for(String topic : topics){ System.out.println(topic);}
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
    private static HashMap<String, Integer> topicProfileOccurences(String field, List<Document> docsPerTopic) throws IOException {

        HashMap<String, Integer> myWordsCount = new HashMap<>();

        List<String> stopwords = Files.readAllLines(Paths.get(RESOURCES_PATH + "english_stopwords.txt"));

        for(Document doc : docsPerTopic) {
            String a = doc.getField(field).toString();

            Properties props = new Properties();
            props.setProperty("annotators", "tokenize");
            RedwoodConfiguration.current().clear().apply();
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
            CoreDocument document = pipeline.processToCoreDocument(a);

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
     * Method to calculate the IDF of the first 1000 words of top occurrences (irrelevant for lower)
     * @param words the raw frequency per term (occurences)
     * @param docs list of the topic documents
     * @return HashMap<String, Double> the first 1000 worlds top occured with their IDF
     */
    private static HashMap<String, Double> topicProfileIDF(String field, HashMap<String, Integer> words, List<Document> docs) {
        HashMap<String, Double> idfs = new HashMap<>();

        int count = 1;
        int stop = 1000;
        for (Map.Entry<String, Integer> en : words.entrySet()) {
            System.out.println(count + " / " + stop);
            double idf = idf(en.getKey(), field, docs);
            idfs.put(en.getKey(), idf);
            count++;

            if (count == stop) break;
        }
        return idfs;
    }

    /**
     * Method to calculate the TFIDF of the words from which IDF has already been calculated
     * @param idfs 1000 words that top occured with their IDF value
     * @param counts words occurences
     * @return HashMap<String, Double> the first 1000 worlds top occured with their TFIDF
     */
    private static HashMap<String, Double> topicProfileTFIDF(HashMap<String, Double> idfs, HashMap<String, Integer> counts) {
        HashMap<String, Double> tfidfs = new HashMap<>();

        int count = 1;
        int stop = 1000;
        for (Map.Entry<String, Double> idf : idfs.entrySet()) {
            System.out.println(count + " / " + stop);
            double tfidf = tfidf(idf.getValue(), counts.get(idf.getKey()));
            tfidfs.put(idf.getKey(), tfidf);
            count++;
        }
        return tfidfs;
    }


    /**
     * Function to sort hashmap by Integer values
     * @param unSortedMap HashMap<String, Integer> not yet sorted
     * @return HashMap<String, Integer> sorted by Integer (descending order)
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
     * Function to sort hashmap by Double values
     * @param unSortedMap HashMap<String, Double> not yet sorted
     * @return HashMap<String, Double> sorted by Double (ascending order if desc = false, descending order if desc = true)
     */
    private static HashMap<String, Double> sortByValueD(HashMap<String, Double> unSortedMap, boolean desc)
    {
        //LinkedHashMap preserve the ordering of elements in which they are inserted
        LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<>();

        if(desc){
            unSortedMap.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
        }
        else {
            unSortedMap.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.naturalOrder()))
                    .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
        }

        return sortedMap;
    }


    /**
     * Method to display in log the top occurences words with their IDF and TFIDF per topic
     * @param hm hashmap of String, Integer -> word, occurences
     * @param top number of printed words
     * @param docs list of docs to calculate idf and tfidf
     */
    public static void displayTopWords(String field, HashMap<String, Integer> hm, int top, List<Document> docs) {

        HashMap<String, Integer> hmOrd = sortByValue(hm);
        int count = 1;
        for (Map.Entry<String, Integer> en : hmOrd.entrySet()) {
            if(count <= top ) {
                double idf = idf(en.getKey(), field, docs);
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
     * @return idf log(N+1/n+1) smoothed -> inverse document frequency smooth
     */
    public static double idf(String term, String field, List<Document> docsTopic) {
        int N = docsTopic.size() + 1;
        int n = 1;

        for(Document doc : docsTopic) {
            String fiel = doc.getField(field).toString();

            /* tokenization with Stanford NLP core */
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize");
            RedwoodConfiguration.current().clear().apply();
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
            CoreDocument document = pipeline.processToCoreDocument(fiel);

            //word present in document ?
            boolean here= false;
            String[] words;
            for (CoreLabel tok : document.tokens()) {

                words = tok.word().replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
                for (String word : words) {
                    if (word.equals(term)) {
                        here = true;
                        break;
                    }
                }
            }
            if(here) n=n+1;
        }
        //System.out.println("Term :" + term + " N : " + N + " n : " + n);
        return Math.log10(N/n);
    }

    /**
     * Method to write the HashMap of words occurences in a txt file named *topic*_occurences.txt
     * @param prefixname prefix name usually the topic
     * @param hm Hashmap of words occurences
     */
    private static void writeInFileOccurrences(String prefixname, HashMap<String, Integer> hm) {

        File file = new File(RESOURCES_PATH + "topics_profile/" + prefixname + "_occurrences.txt");

        try (BufferedWriter bf = new BufferedWriter(new FileWriter(file))) {
            for (Map.Entry<String, Integer> entry : hm.entrySet()) {

                //put key and value separated by a colon
                bf.write(entry.getKey() + ":" + entry.getValue());

                //new line
                bf.newLine();
            }

            bf.flush();

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    /**
     * Method to write the Hashmap of top 1000 words and idf or tfidf (depends on suffix)
     * @param prefixname usually the topic
     * @param suffix idfs or tfidfs depending on the HashMap
     * @param hm HashMap of TFIDF or IDF of top 1000 words
     */
    private static void writeInFileTF_IDFS(String prefixname, String suffix, HashMap<String, Double> hm) {

        File file = new File(RESOURCES_PATH + "topics_profile/" + prefixname + "_"+suffix+".txt");

        try (BufferedWriter bf = new BufferedWriter(new FileWriter(file))) {
            for (Map.Entry<String, Double> entry : hm.entrySet()) {

                //put key and value separated by a colon
                bf.write(entry.getKey() + ":" + entry.getValue());

                //new line
                bf.newLine();
            }

            bf.flush();

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    /**
     * TFIDF function based on the weights calculated
     * @param idf calculated with the idf function
     * @param tf calculated calculated with the raw count of occurrences
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
        HashMap<String, Integer> hmHist = topicProfileOccurences(field, docsHistory);

        /* Sciences */
        List<Document> docsScience = docsPerTopic.get(1);
        HashMap<String, Integer> hmScienc = topicProfileOccurences(field, docsScience);

        /* Religion & belief */
        List<Document> docsReligion = docsPerTopic.get(2);
        HashMap<String, Integer> hmRel = topicProfileOccurences(field, docsReligion);


        /* Display top 15 words based on occurrences and topics */
        System.out.println("\nTop words of History :");
        displayTopWords(field, hmHist, 15, docsHistory);
        System.out.println("\nTop words of Sciences :");
        displayTopWords(field, hmScienc, 15, docsScience);
        System.out.println("\nTop words of Religion&Belief :");
        displayTopWords(field, hmRel, 15, docsReligion);


        /* Create topic_occurrences.txt
        writeInFileOccurrences("history", sortByValue(hmHist));
        writeInFileOccurrences("science", sortByValue(hmScienc));
        writeInFileOccurrences("religion", sortByValue(hmRel));

         */


        HashMap<String, Double> histIDFs= sortByValueD(topicProfileIDF(field, sortByValue(hmHist), docsHistory), false);
        HashMap<String, Double> scienceIDFs= sortByValueD(topicProfileIDF(field, sortByValue(hmScienc), docsScience), false);
        HashMap<String, Double> religionIDFs = sortByValueD(topicProfileIDF(field, sortByValue(hmRel), docsReligion), false);

        /* Create topic_idfs.txt
        writeInFileTF_IDFS("history", "idfs", histIDFs);
        writeInFileTF_IDFS("science", "idfs", scienceIDFs);
        writeInFileTF_IDFS("religion", "idfs",religionIDFs);

         */



        /* topic_tfidfs.txt */
        writeInFileTF_IDFS("history", "tfidfs", sortByValueD(topicProfileTFIDF(histIDFs, hmHist), true));
        writeInFileTF_IDFS("science", "tfidfs", sortByValueD(topicProfileTFIDF(scienceIDFs, hmScienc), true));
        writeInFileTF_IDFS("religion", "tfidfs", sortByValueD(topicProfileTFIDF(religionIDFs, hmRel), true));

    }
}
