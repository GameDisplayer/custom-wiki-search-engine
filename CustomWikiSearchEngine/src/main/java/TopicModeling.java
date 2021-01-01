import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.tartarus.snowball.ext.PorterStemmer;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class TopicModeling {


    /**
     * Method to extract documents per topic ie History, Science & Religion/Belief
     * @return
     * @throws IOException
     */
    private static List<List<Document>> topicExtraction() throws IOException {

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
        List<List<Document>> docsPerTopic = new ArrayList<>();

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
        return docsPerTopic;
    }

    /**
     * Method to tokenize and stop word removal the abstract for topic modeling
     * @param docsPerTopic
     * @return
     */
    private static HashMap<String, Integer> topicModeling(List<Document> docsPerTopic) throws IOException {

        HashMap<String, Integer> myWordsCount = new HashMap<String, Integer>();
        HashMap<String, Double> myWordsFreqByDoc = new HashMap<String, Double>();


        List<String> stopwords = Files.readAllLines(Paths.get("src/main/resources/english_stopwords.txt"));

        for(Document doc : docsPerTopic) {
            String abst = doc.getField("abstract").toString();

            Properties props = new Properties();
            props.setProperty("annotators", "tokenize");
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
            CoreDocument document = pipeline.processToCoreDocument(abst);

            int totalWords = 0;
            String[] words = new String[0];
            for (CoreLabel tok : document.tokens()) {

                words = tok.word().replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
                for (String word : words) {
                    //si mot non vide ou stopword ou mots < 2 length
                    if (!word.isEmpty() && !stopwords.contains(word) && word.length() > 2) {
                        totalWords++;
                        if (myWordsCount.containsKey(word)) {
                            myWordsCount.replace(word, myWordsCount.get(word) + 1);
                            myWordsFreqByDoc.replace(word, myWordsFreqByDoc.get(word) + 1);
                        } else {
                            myWordsCount.put(word, 1);
                            myWordsFreqByDoc.put(word, 1.0);
                        }
                    }
                }
                //for (String word : words) if (!word.isEmpty() ) System.out.println(String.format("%s", word));
                for (String word : words) {
                    if (!word.isEmpty() && !stopwords.contains(word) && word.length() > 2)  myWordsFreqByDoc.replace(word,  myWordsFreqByDoc.get(word) + (myWordsFreqByDoc.get(word) / totalWords));
                }
            }



        }
        return myWordsCount;
        //return myWordsFreqByDoc;
    }

    /**
     * Function to sort hashmap by values
     * @param hm
     * @return
     */
    private static HashMap<String, Double> sortByValueD(HashMap<String, Double> hm)
    {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Double> > list =
                new LinkedList<Map.Entry<String, Double> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<String, Double> >() {
            public int compare(Map.Entry<String, Double> o1,
                               Map.Entry<String, Double> o2)
            {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        // put data from sorted list to hashmap
        HashMap<String, Double> temp = new LinkedHashMap<String, Double>();
        for (Map.Entry<String, Double> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    /**
     * Function to sort hashmap by values
     * @param hm
     * @return
     */
    private static HashMap<String, Integer> sortByValue(HashMap<String, Integer> hm)
    {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Integer> > list =
                new LinkedList<Map.Entry<String, Integer> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<String, Integer> >() {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2)
            {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        // put data from sorted list to hashmap
        HashMap<String, Integer> temp = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }


    private static void displayTopWords(HashMap<String, Integer> hm, int top) {

        HashMap<String, Integer> hmOrd = sortByValue(hm);
        int count = 0;
        for (Map.Entry<String, Integer> en : hmOrd.entrySet()) {
            int decount = hm.size() - count;
            if(decount <= 10 ) System.out.println(decount + " -> Key = " + en.getKey() + ", Value = " + en.getValue());
            count++;
        }

    }

    private static void displayTopWordsD(HashMap<String, Double> hm, int top) {

        HashMap<String, Double> hmOrd = sortByValueD(hm);
        int count = 0;
        for (Map.Entry<String, Double> en : hmOrd.entrySet()) {
            int decount = hm.size() - count;
            if(decount <= 10 ) System.out.println(decount + " -> Key = " + en.getKey() + ", Value = " + en.getValue());
            count++;
        }

    }

    public static void main(String[] args) throws IOException {

        /* topic Modeling */
        List<List<Document>> docsPerTopic = topicExtraction();
        /* History */
        List<Document> docsHistory = docsPerTopic.get(0);
        //HashMap<String, Double> hmHist = topicModeling(docsHistory);
        HashMap<String, Integer> hmHist = topicModeling(docsHistory);

        /* Sciences */
        List<Document> docsScience = docsPerTopic.get(1);
        //HashMap<String, Double> hmScienc = topicModeling(docsScience);
        HashMap<String, Integer> hmScienc = topicModeling(docsScience);

        /* Religion & belief */
        List<Document> docsReligion = docsPerTopic.get(2);
        //HashMap<String, Double> hmRel = topicModeling(docsReligion);
        HashMap<String, Integer> hmRel = topicModeling(docsReligion);


        System.out.println("\nTop words of History :");
        displayTopWords(hmHist, 10);
        System.out.println("\nTop words of Sciences :");
        displayTopWords(hmScienc, 10);
        System.out.println("\nTop words of Religion&Belief :");
        displayTopWords(hmRel, 10);
    }
}
