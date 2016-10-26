import org.json.*;
import java.util.HashMap;
// todo - 1:
    /*
    data structure:
    Title
    ID
    __metadata
    Url
    DisplayUrl
    Summary / Description
    Relevance (from user)
    vector-space model (from PostingList Class) :
        using tf-idf weighting scheme (1 + log(f(t, d))) * log( N / n(t))
     */

// todo - 2:
    /*
    functions:
    calculate vector (with Euc normalization)
    calculate cosine sim (optional)
     */

public class DocumentList {

	enum TfIdfScheme {
		LogTfIdf,
		rawTfIdf,
		dividedByMaxTfIdf,
		noIdf
	}

	private boolean ifRelevant;
	private String title;
	private String URL;
	private String ID;
	private String DisplayUrl;
	private String Description;
	private String metadata;
	private HashMap<String, Double> documentVector;
	
	public DocumentList(JSONObject aResult) throws JSONException {			// Constructor
		this.title = aResult.get("Title").toString();
		this.ID = aResult.get("ID").toString();
		this.DisplayUrl = aResult.get("DisplayUrl").toString();
		this.URL = aResult.get("Url").toString();
		this.Description = aResult.get("Description").toString();
		this.metadata = aResult.get("__metadata").toString();
	}

	public static void calculateDocumentVectors (PostingList postingList, HashMap<String, DocumentList> docMap, TfIdfScheme tfIdfScheme) {
		HashMap<String, HashMap<String, PostingList.DocStatForATerm>> postingListMap = postingList.postingListMap;
		for (DocumentList doc : docMap.values()) {
			doc.documentVector = new HashMap<>();
		}
		for (String term : postingListMap.keySet()) {
			double[] idf = postingList.getIDF(term, docMap);
			for (DocumentList doc : docMap.values()) {
				double idfToUse = doc.ifRelevant ? idf[0] : idf[1];
				long tfTitle = postingList.getTFTitle(term, doc.ID);
				long tfDescription = postingList.getTFDescription(term, doc.ID);
				long tfTitleMax = postingList.getTFTitleMax(doc.ID);
				long tfDescriptionMax = postingList.getTFDescriptionMax(doc.ID);
				long tf = Math.max(tfDescription, tfTitle);
				// long tf = tfTitle + tfDescription;
				long tfMax = tfDescriptionMax;

				switch (tfIdfScheme){
					case LogTfIdf:
						doc.documentVector.put(term, tf == 0 ? 0 : ((1 + Math.log10((double) tf)) * idfToUse));
					case rawTfIdf:
						doc.documentVector.put(term, (double) tf * idfToUse);
					case dividedByMaxTfIdf:
						doc.documentVector.put(term, (0.3 + (1 - 0.3) * (double) tf / (double) tfMax) * idfToUse);
					case noIdf:
						doc.documentVector.put(term, tf == 0 ? 0 : (1 + Math.log10((double) tf)));
				}
			}
		}
		for (DocumentList doc : docMap.values()) {
			doc.EuclidNormalizationOnDocumentVector();
		}
	}

	public void EuclidNormalizationOnDocumentVector () {
		double distance = 0.;
		for (Double d : documentVector.values()) {
			distance += d * d;
		}
		distance = Math.sqrt(distance);
		for (String term : documentVector.keySet()) {
			Double tfIdf = documentVector.get(term);
			documentVector.put(term, tfIdf / distance);
		}
	}

	public String getTitle() throws JSONException {		// getTitle
		return title;
	}
	
	public String getID() throws JSONException {		// getID
		return ID;
	}
	
	public String getURL() throws JSONException {		// getURL
		return URL;
	}
	
	public String getDisplayURL() throws JSONException {	// getDisplayURL
		return DisplayUrl;
	}
	
	public String getDescription() throws JSONException {	// getDescription
		return Description;
	}
	
	public String getMetadata() throws JSONException {		// getMetadata
		return metadata;
	}

	public HashMap<String, Double> getDocumentVector() throws JSONException {		// getURL
		return documentVector;
	}
	
	public void setIfRelevant(boolean ifRelevant) {
		this.ifRelevant = ifRelevant;
	}
	public boolean getIfRelevant(){			// getRelevance
		return ifRelevant;
	}	
}