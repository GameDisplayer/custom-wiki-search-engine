# Project B: Customized Wikipedia search engine

## Goal : Create a search engine for Wiki content
- search on documents based on its content
- the rank must take into account document text and topic content (e.g. common words on document of the same topic)

## Given steps :
- [X] Create a text database
- [X] Tokenize and index the documents
- [X] Create the topic profile
- [X] Process basic searches
- [X] Process advanced searches

## Database :
Our text database is composed of **1 518 Wikipedia's pages** extracted from 3 mains category :
1. History and events
2. Natural and physical science
3. Religions and belief systems

## The project structure :

- src/main/java 
    1. Application.java : GUI of the project
    2. BarChart.java : a simple BarChart from TopicProfile (not useful)
    3. Main.java : the indexing and search part of the search engine
    4. Statistics.java : a class with functions that give you more information on the corpus and terms
    5. TopicModeling.java : topic extraction and topic profile are done here (the most relevant words are calculated)
 - src/main/resources
    1. Icon folder : icons for the GUI
    2. topics_folder : folder with ranked words per topic based on different metrics (TF, IDF & TF-IDF) *topic*_occurences.txt, *topic*_idfs.txt and *topic*_tfidfs.txt (with *topic* = history or religion or sciences
    3. wordnet_prolog : folder for advanced synonym searches
    4. english_stopwords.txt : txt file with common stop words (and useless words) used for topic profile
    5. WikiData.XML & WikiData.CSV : documents under different file format (for ease of use)
    6. WikiDumpXMLtoCSV.py : python file that is automatically compiled when maven project is launched -> transform WikiData XML into CSV

## How to launch the project ?

As simple as that !

```sh
$ maven clean
$ maven compile
$ maven exec:java
```

## What other things I have to know ?
It is possible to run the TopicModeling and the Statistics files as standalone directly with the main function.

## APIs used and usage :
- [Lucene](https://lucene.apache.org/core/) - for the search engine part (indexing, tokenization, search...)
- [Stanford CoreNLP](https://stanfordnlp.github.io/CoreNLP/) - for the topic profile part/analysis of the documents 
- [OpenCSV](http://opencsv.sourceforge.net/) - for the parsing part of the WikiData CSV file in order to index the documents
- [WikiDumpReader](https://pypi.org/project/wiki-dump-reader/) - the parser for transforming WikiData XML into WikiData CSV
