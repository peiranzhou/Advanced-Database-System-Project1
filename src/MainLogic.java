/**
 * Created by yuntongwang on 9/29/16.
 */
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainLogic {

//    public static String ACCOUNT_KEY = "53OwvtK4r/KYOc3jTU9KarhZhTIREr0CxhSEYw3EIKY";
    public static void main(String[] args) throws IOException, JSONException, ClassNotFoundException {
    	String ACCOUNT_KEY = args[0];
        // read cached data
        File file = new File("../transcript.txt");
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));

        // list of document list
        HashMap<String, DocumentList> docMap = new HashMap<>();
        // posting list
        PostingList postingList = new PostingList();

//        System.out.println("Input keywords:");
//        Scanner scan = new Scanner(System.in);
//        String keywords = scan.nextLine();
//        System.out.println("Input precision: ");
        String keywords = args[2];
//        double targetPrecision = Double.valueOf(scan.nextLine());
        double targetPrecision = Double.parseDouble(args[1]);
        
        // form keywords list
        ArrayList<String> keywordsList = new ArrayList<>();
        String[] keywordsArray = keywords.replaceAll("[!?,.:''/]", "").split("\\s+");
        for (String key : keywordsArray) {
            keywordsList.add(key.toLowerCase());
        }
        // rocchio algorithm
        Rocchio rocchio = new Rocchio(docMap, postingList, keywordsList);

        int round = 1;
        while (true) {
            System.out.println("Parameters:");
            System.out.println("Client key  = " + ACCOUNT_KEY);
            System.out.print("Query       = ");
            for (String key : keywordsList) {
                System.out.print(key + " ");
            }
            System.out.println();
            System.out.println("Precision   = " + targetPrecision);

            bw.newLine();
            bw.write("================================");
            bw.newLine();
            bw.write("ROUND " + round++ );

            String res = getJsonRes(keywordsList, bw, ACCOUNT_KEY);
            // get user feedback and put new document to documentlist map
            double precision = getUserRelevanceFeedBack(res, docMap, bw);
            System.out.print("Query ");
            for (String key : keywordsList) {
                System.out.print(key + " ");
            }
            System.out.println();
            System.out.println("Precision " + precision);
            JSONObject d = new JSONObject(res).getJSONObject("d");
            JSONArray results = d.getJSONArray("results");
            if (results.length() < 10) {
                System.out.println("No enough results returned by Bing API. Program terminates!");
                bw.newLine();
                bw.write("No enough results returned by Bing API. Program terminates!");
                break;
            }
            // update posting list
            postingList.precessNewDocuments(docMap);

            if (precision >= targetPrecision) {
                System.out.println("Required precision@10 " + targetPrecision + " is reached with " + precision);
                bw.newLine();
                bw.write("Required precision@10 " + targetPrecision + " is reached with " + precision);
                break;
            }

            if (precision == 0.0) {
                System.out.println("Precision@10 is 0. Program will terminate!");
                bw.newLine();
                bw.write("Precision@10 is 0. Program will terminate!");
                break;
            }
            System.out.println("Still below the desired precision of " + targetPrecision);
            System.out.println("Indexing results.....");
            System.out.println("Please wait......");

            // update documentVector
            DocumentList.calculateDocumentVectors(postingList, docMap, DocumentList.TfIdfScheme.noIdf);

            // get optimized query from rocchio algorithm and change keywords
            ArrayList<String> augmentedKeywordsList = rocchio.getKeywordsExpansion(keywordsList);
            System.out.println();
            System.out.print("Augmented by   ");
            for (String s : augmentedKeywordsList) {
                boolean isPrev = false;
                for (String prevK : keywordsList) {
                    if (s.equals(prevK))
                        isPrev = true;
                }
                if (!isPrev) {
                    System.out.print(s + " ");
                }
            }
            System.out.println();
            keywordsList = augmentedKeywordsList;
        }

        // write new cache to files;
        bw.close();
    }

    private static double getUserRelevanceFeedBack(String res, HashMap<String, DocumentList> docMap, BufferedWriter bw) throws JSONException, IOException {
        int numOfRelevantDocs = 0;
        JSONObject d = new JSONObject(res).getJSONObject("d");
        JSONArray results = d.getJSONArray("results");
        int resultsLength = results.length();
        System.out.println("Total no of results : " + results.length());
        System.out.println();
        System.out.println("Bing Search Results:");
        System.out.println("======================");
        Scanner scan = new Scanner(System.in);
        for (int i = 0; i < resultsLength; i++) {
            System.out.println("Result " + (i + 1));
            bw.newLine();
            bw.write("Result " + (i + 1));
            JSONObject aResult = results.getJSONObject(i);
            System.out.println("[");
            System.out.println("Title : " + aResult.get("Title"));
            System.out.println("Url : " + aResult.get("Url"));
            System.out.println("Summary : " + aResult.get("Description"));
            System.out.println("]");

            DocumentList list = new DocumentList(aResult);
            while (true) {
                System.out.print("Relevant (Y/N)?");
                String feedback = scan.nextLine();
                if (feedback.equals("y") || feedback.equals("Y")) {
                    numOfRelevantDocs ++;
                    list.setIfRelevant(true);
                    bw.newLine();
                    bw.write("Relevant: YES");
                    break;
                } else if (feedback.equals("n") || feedback.equals("N")) {
                    list.setIfRelevant(false);
                    bw.newLine();
                    bw.write("Relevant: NO");
                    break;
                } else {
                    continue;
                }
            }

            bw.newLine();
            bw.write("[");
            bw.newLine();
            bw.write("Title : " + aResult.get("Title"));
            bw.newLine();
            bw.write("Url : " + aResult.get("Url"));
            bw.newLine();
            bw.write("Summary : " + aResult.get("Description"));
            bw.newLine();
            bw.write("]");
            bw.newLine();

            docMap.put(list.getID(), list);
        }
        System.out.println("============================");
        System.out.println("FEEDBACK SUMMARY");
        bw.newLine();
        bw.write("PRECISION :" + (double) numOfRelevantDocs / 10.0);
        return (double) numOfRelevantDocs / 10.0;
    }

    private static String getJsonRes (ArrayList<String> keywordsList, BufferedWriter bw, String ACCOUNT_KEY) throws IOException, JSONException {
        bw.newLine();
        bw.write("QUERY ");
        bw.newLine();
        StringBuilder keywordsBuilder = new StringBuilder();
        for (int i = 0; i < keywordsList.size(); i++) {
            bw.write(keywordsList.get(i) + " ");
            keywordsBuilder.append(keywordsList.get(i));
            if (i != keywordsList.size() - 1) {
                keywordsBuilder.append("%20");
            }
        }
        String keyWords = keywordsBuilder.toString();

        String accountKey = ACCOUNT_KEY;

        String bingUrl = "https://api.datamarket.azure.com/Bing/Search/Web?Query=%27" + keyWords + "%27&$top=10&$format=JSON";

        System.out.println("URL: " + bingUrl);

        byte[] accountKeyBytes = Base64.encodeBase64((accountKey + ":" + accountKey).getBytes());
        String accountKeyEnc = new String(accountKeyBytes);

        URL url = new URL(bingUrl);
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Authorization", "Basic " + accountKeyEnc);

        try ( BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            String res = response.toString();
            return res;
        }
    }
}