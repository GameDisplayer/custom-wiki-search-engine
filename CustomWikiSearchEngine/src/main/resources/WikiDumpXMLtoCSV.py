from wiki_dump_reader import Cleaner, iterate
import csv
import os.path
from os import path

def cleanLink(text):
    res = ""
    for char in text :
        if char != "[" and char != "]":
            res+=char
    return res

def cleanCategories(categories):
    res = []
    cat = cleanLink(categories)
    cats = cat.split("\n")
    for elt in cats:
        idx = elt.find(":")
        sep = elt.find("|")
        if sep != -1 :
            res.append(elt[idx+1:sep])
            rest = elt[sep+1:]
            if len(rest) > 1 :
                idx = rest.find(":")
                res.append(rest[idx+1:])
        else:
            res.append(elt[idx+1:])
    return res
    

def saveInCSV(file, titles, abstracts, contents, topics):
    with open(file, mode='w', encoding="utf-8") as csv_file:
        fieldnames = ['title', 'abstract', 'content', 'topics']
        writer = csv.DictWriter(csv_file, fieldnames=fieldnames)
    
        for i in range(len(titles)):
            writer.writerow({'title' : titles[i], 'abstract' : abstracts[i], 'content' : contents[i], 'topics' : topics[i]})
    print("Done")

def getDataFromXML(CSVFile, XMLFile):
    i = 0
    if not path.exists(CSVFile) and path.exists(XMLFile):
        cleaner = Cleaner()
        titles = []
        abstracts = []
        texts = []
        topics = []
        for title, text in iterate(XMLFile):
            titles.append(title)
    
            text = cleaner.clean_text(text)
    
            startText = text.find("==")
            startRef = text.find("==References==")
            if startRef == -1:
                startRef = text.find("== References ==")
            startCat = text.find("[[Category")
            
            abstract = text[:startText]
            if startRef != -1:
                txt = text[startText:startRef]
            else:
                txt = text[startText:]
            categories = text[startCat:]
            topics.append(cleanCategories(categories))

            abstracts.append(cleanLink(abstract))
            texts.append(cleanLink(txt))

        saveInCSV(CSVFile, titles, abstracts, texts, topics)
        
    elif not path.exists(XMLFile):
        raise ValueError("The XML file doesn't exist")
    else:
        print("The data is already extracted")

getDataFromXML("WikiData.csv", "WikiData.xml")
