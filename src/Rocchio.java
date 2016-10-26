/**
 * Created by yuntongwang on 10/1/16.
 */

// todo - 1:
    /*
    data structure:
    parameters
    formula
    previous query vector (initialize to 1 for existing word in initial query)
     */
// todo - 2:
    /*
    functions:
    optimize query, output q(optimized)
    may include re-order?? (trivial)
     */

import java.util.*;

import org.json.JSONException;

public class Rocchio {

    public HashSet stopwords;

    private double alpha = 1;
    private double beta = 0.75;
    private double gamma = 0.15;
    long relevantDocumentNumer;
    long nonRelevantDocumentNumber;
    private HashMap<String, DocumentList> documentListHashMap;                          // ten data records
    HashMap<String, Double> scoreMap;   // used to store <term, score> pair
    PostingList postingList;


    public Rocchio(HashMap<String, DocumentList> data, PostingList postingList, ArrayList<String> keywordsList)  {
        this.stopwords = new Stopwords().m_Words;
        this.documentListHashMap = data;
        this.postingList = postingList;
        this.scoreMap = new HashMap<>();
        for (String key : keywordsList) {
            scoreMap.put(key, 1. / Math.sqrt(2.));
        }
    }

    public void getRelevantDocumentNumber () {
        relevantDocumentNumer = 0;
        nonRelevantDocumentNumber = 0;
        for (DocumentList docList : documentListHashMap.values()) {
            if (docList.getIfRelevant()) {
                relevantDocumentNumer ++;
            } else {
                nonRelevantDocumentNumber ++;
            }
        }
    }

    public ArrayList<String> getKeywordsExpansion(ArrayList<String> previousKeyWords) throws JSONException {
        getRelevantDocumentNumber();
        for (String term : postingList.postingListMap.keySet()) {
            scoreMap.put(term, score(term));
        }
        return addAndReorderKeyWords(previousKeyWords);
    }
    
    public double score(String term) throws JSONException {

        double sumRelatedWeight = 0;
        double sumNonRelatedWeight = 0;

        for (DocumentList doc : documentListHashMap.values()) {
            if (doc.getIfRelevant()) {
                HashMap<String, Double> docVector = doc.getDocumentVector();
                sumRelatedWeight += docVector.get(term);
            } else {
                sumNonRelatedWeight += doc.getDocumentVector().get(term);
            }
        }
        double score = alpha * (scoreMap.containsKey(term) ? scoreMap.get(term) : 0)
                        + beta * sumRelatedWeight / (double) relevantDocumentNumer
                        - gamma * sumNonRelatedWeight / (double) nonRelevantDocumentNumber;
//        double score = beta * sumRelatedWeight / (double) relevantDocumentNumer
//                - gamma * sumNonRelatedWeight / (double) nonRelevantDocumentNumber;
        return score < 0 ? 0 : score;
    }
    
    // HashMap --> (string, score)
    
    public ArrayList<String> addAndReorderKeyWords(ArrayList<String> keywordsList) {

        // sort the score map and put into alpha scoreList
        ArrayList<Map.Entry<String, Double>> scoreList = new ArrayList<>(scoreMap.entrySet());
        Comparator<Map.Entry<String, Double>> descendComparator = new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        };

        Collections.sort(scoreList, descendComparator);

        HashSet<String> previousKeyWordsSet = new HashSet<>(keywordsList);

        // build resultList to store keywords along with their score for next round search
        ArrayList<Map.Entry<String, Double>> resultList = new ArrayList<>();

        // put previous keywords first
        for (String previousKeyword : keywordsList) {
            resultList.add(new AbstractMap.SimpleEntry<>(previousKeyword, scoreMap.get(previousKeyword)));
        }

        // add new keywords from sorted scoreList
        int iterScoreList = 0;
        while (resultList.size() < keywordsList.size() + 2 && iterScoreList < scoreList.size()) {
            Map.Entry<String, Double> entry = scoreList.get(iterScoreList ++);
            String termCandidate = entry.getKey();
            // skip previous keywords and stopwords
            if (!previousKeyWordsSet.contains(termCandidate) && !stopwords.contains(termCandidate)) {
                resultList.add(new AbstractMap.SimpleEntry<>(termCandidate, entry.getValue()));
            }
        }

        // sort the result by their score
        Collections.sort(resultList, descendComparator);

        // reorder keywords by their score
        keywordsList = new ArrayList<>(resultList.size());
        for (int i = 0; i < resultList.size(); i++) {
            keywordsList.add(resultList.get(i).getKey());
            // System.out.println("Keyword : " + resultList.get(i).getKey() + " with score " + resultList.get(i).getValue());
        }
        return keywordsList;
    }
}