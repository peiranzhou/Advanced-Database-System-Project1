import org.json.JSONException;

import java.io.IOException;
import java.util.*;

/**
 * Created by yuntongwang on 10/1/16.
 */
// todo - 1:
/*
    data structure:
    term t,
        DocId id,
            zone 1 ("title" eg)
                term frequency: # times t occurs in doc DocId in zone 1.
            zone 2 ("body" eg)
        Other docs
        .
        .
    other terms
    .
    .
 */
// todo - 2:
/*
    given term & DocId : calculate tf (may giving different weight to different zones)
    given term : calculate inverse document frequence (idf)
 */

public class PostingList {

	public class DocStatForATerm {

		// data structure to store a page on a postingList
		// specifically, for term t, a page is one of the document who contains the term
		public String docId;
		public long tfTitle;
		public long TFDescription;
		public long tfBody;
		public DocStatForATerm (String docId) {
			this.docId = docId;
			TFDescription = 0;
			tfTitle = 0;
			tfBody = 0;
		}

	}

	HashMap<String, HashMap<String, DocStatForATerm>> postingListMap;
	HashSet<String> processedDocIds;

	public PostingList () {
		postingListMap = new HashMap<>();
		processedDocIds = new HashSet<>();
	}

	// Traversing all the words in ten articles, separate them into related and non-related parts
	public void precessNewDocuments(HashMap<String, DocumentList> data) throws JSONException, IOException {

		for (String docId : data.keySet()) {

			if (processedDocIds.contains(docId))
				continue;
			DocumentList doc = data.get(docId);

			// get description zone term frequency for doc with docId
			String description = doc.getDescription();

			// replace anything that is not a character, and split by any length of spaces
			String[] termsInDescription = description.replaceAll("[^A-Za-z]", " ").split("\\s+");

			for (String term : termsInDescription) {

				term = term.toLowerCase();
				HashMap<String, DocStatForATerm> docListForATerm;

				if (postingListMap.containsKey(term)) {
					docListForATerm = postingListMap.get(term);
				} else {
					docListForATerm = new HashMap<String, DocStatForATerm>();
				}
				DocStatForATerm docForATerm;
				if (docListForATerm.containsKey(docId)) {
					docForATerm = docListForATerm.get(docId);
				} else {
					docForATerm = new DocStatForATerm(docId);
				}

				docForATerm.tfTitle ++;
				docListForATerm.put(docId, docForATerm);
				postingListMap.put(term, docListForATerm);
			}

			//  using data in title and do the statistics
			String[] termsInTitle = doc.getTitle().replaceAll("[^A-Za-z]", " ").split("\\s+");
			for (String term : termsInTitle) {

				term = term.toLowerCase();

				// get the document list for the term
				HashMap<String, DocStatForATerm> docListForATerm;

				if (postingListMap.containsKey(term)) {
					docListForATerm = postingListMap.get(term);
				} else {
					docListForATerm = new HashMap<String, DocStatForATerm>();
				}

				// get the document statistics for term and docId
				DocStatForATerm docForATerm;

				if (docListForATerm.containsKey(docId)) {
					docForATerm = docListForATerm.get(docId);
				} else {
					docForATerm = new DocStatForATerm(docId);
				}

				docForATerm.TFDescription ++;
				docListForATerm.put(docId, docForATerm);
				postingListMap.put(term, docListForATerm);

			}

//			//  using data in url and do the statistics
//			String[] termsInBody = doc.getDocument().replaceAll("[^A-Za-z]", " ").split("\\s+");
//			for (String term : termsInBody) {
//
//				term = term.toLowerCase();
//
//				// get the document list for the term
//				HashMap<String, DocStatForATerm> docListForATerm;
//
//				if (postingListMap.containsKey(term)) {
//					docListForATerm = postingListMap.get(term);
//				} else {
//					docListForATerm = new HashMap<String, DocStatForATerm>();
//				}
//
//				// get the document statistics for term and docId
//				DocStatForATerm docForATerm;
//
//				if (docListForATerm.containsKey(docId)) {
//					docForATerm = docListForATerm.get(docId);
//				} else {
//					docForATerm = new DocStatForATerm(docId);
//				}
//
//				docForATerm.tfBody ++;
//				docListForATerm.put(docId, docForATerm);
//				postingListMap.put(term, docListForATerm);
//
//			}

			processedDocIds.add(docId);
		}
	}

	public int sizeOfOneDocument(String term, DocumentList doc) throws JSONException {
		String[] strArr = doc.getDescription().split(" ");
		int count = 0;
		for (String str: strArr) {
			if (str.equals(term)) {
				count++;
			}
		}
		return count;
	}

	public long getTFTitle(String term, String docId){
		// given term & DocId : calculate tf of title
		if (!postingListMap.containsKey(term) || !(postingListMap.get(term).containsKey(docId)))
			return 0;
		return postingListMap.get(term).get(docId).tfTitle;
	}

	public long getTFDescription(String term, String docId){
		// given term & DocId : calculate tf of description
		if (!postingListMap.containsKey(term) || !(postingListMap.get(term).containsKey(docId)))
			return 0;
		return postingListMap.get(term).get(docId).TFDescription;
	}

	public long getTFTitleMax(String docId){
		long max = 0;
		for (String term : postingListMap.keySet()) {
			max = Math.max( max, getTFTitle(term, docId));
		}
		return max;
	}

	public long getTFDescriptionMax(String docId){
		long max = 0;
		for (String term : postingListMap.keySet()) {
			max = Math.max( max, getTFDescription(term, docId));
		}
		return max;
	}

	public long getTFBody(String term, String docId){
		// given term & DocId : calculate tf of description
		if (!postingListMap.containsKey(term) || !(postingListMap.get(term).containsKey(docId)))
			return 0;
		return postingListMap.get(term).get(docId).tfBody;
	}


	public double[] getIDF(String term, HashMap<String, DocumentList> docMap) {
		// idf[0] : related idf
		// idf[1] : non-related idf

		double[] idf = new double[2];
		if (!postingListMap.containsKey(term)) {
			return idf;
		}
		HashMap<String, DocStatForATerm> docListForATerm = postingListMap.get(term);
		double relatedDocuments = 0;
		double nonRelatedDocuments = 0;
		double termOccursInRelatedDocs = 0;
		double termOccursInNonRelatedDocs = 0;
		for (String docId: docMap.keySet()) {
			if (docMap.get(docId).getIfRelevant()) {
				relatedDocuments += 1.;
				if (docListForATerm.containsKey(docId)) {
					termOccursInRelatedDocs += 1.;
				}
			} else {
				nonRelatedDocuments += 1.;
				if (docListForATerm.containsKey(docId)) {
					termOccursInNonRelatedDocs += 1.;
				}
			}

		}
		if (termOccursInRelatedDocs == 0) {
			termOccursInRelatedDocs = 1.;
		}
		if (termOccursInNonRelatedDocs == 0) {
			termOccursInNonRelatedDocs = 1.;
		}
		idf[0] = Math.log10(relatedDocuments / termOccursInRelatedDocs);
		idf[1] = Math.log10(nonRelatedDocuments / termOccursInNonRelatedDocs);
		return idf;
	}

	public String removeLeadingTailingDuplicateSpace(String str) {

		int slow = 0;
		int fast = 0;
		int wordcount = 0;
		char[] array = str.toCharArray();
		while (true) {

			while (fast != str.length() && array[fast] == ' ') {
				fast++;
			}
			if (fast == str.length()) {
				break;
			}
			if (wordcount > 0) {
				array[slow] = ' ';
				slow++;
			}
			while (fast != str.length() && array[fast] != ' ') {
				array[slow] = array[fast];
				slow++;
				fast++;
			}
			wordcount++;

		}

		char[] result = new char[slow];
		for (int i = 0; i < slow; i++) {
			result[i] = array[i];
		}

		return String.valueOf(result);
	}

}