/**     William Lyon
 *      CSCI 557 - Machine Learning
 *      Spring 2014
 *      Final Project
 *      Graph Based Link Prediction in a Collaboration Network
 *
 */

import org.neo4j.cypher.internal.compiler.v1_9.commands.expressions.Null;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** RecommendFollows
 *
 */
public class RecommendFollows {
    private GraphDatabaseService graphDB;
    private ExecutionEngine engine;
    private Integer folds;
    private Integer k;
    private Integer predicted_links;
    private Integer leaveout_links;
    private Integer user_count;
    private ArrayList<String> users;   // user ids of users to generate recommendations
    private Integer valid_count;        // increment each time generateRecs returns true
    private Integer rec_count;          // total number of users for which recommendations were generated
    private Integer pred_link_count;    // total number of predicated links


    /** Initialize Neo4j graph database connection
     *
     * @param DB_PATH   relative path of Neo4j data store (graph.db)
     */
    public RecommendFollows(String DB_PATH) {
        this.graphDB = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
        registerShutdownHook(graphDB);
        this.engine = new ExecutionEngine(graphDB);
        if (graphDB.isAvailable(10)) {
            System.out.println("Database connection established");
        } else {
            System.out.println("ERROR: Unable to establish database connection");
            System.exit(1);
        }
    }

    /** Log result statistics
     *
     */
    public void reportResults() {
        // FIXME: implement AUC(?) and Precision

        // Simple logging for first pass implementation
        System.out.println("*************************************************");
        System.out.println("k: " + this.k);
        System.out.println("Predicted links: " + this.predicted_links);
        System.out.println("Total pred_link_count: " + this.pred_link_count);
        System.out.println("Total rec_count: " + this.rec_count);
        System.out.println("Total valid_count: " + this.valid_count);
        System.out.println("Accuracy: " + (1.0*this.valid_count/this.rec_count));
        System.out.println("Precision: " + (1.0*this.valid_count/this.pred_link_count));
        System.out.println("*************************************************");

    }


    public Integer getNDegree(String user_id) {
        String query = "MATCH (u:User {name: {user_id}})-[r:FOLLOWS]-(x) RETURN count(DISTINCT x) as k";
        Map<String,Object> params= new HashMap<>();
        params.put("user_id", user_id);

        Integer degree = 0;


        Iterator<Map<String, Object>> result = engine.execute(query, params).iterator();
        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            degree = Integer.parseInt(row.get("k").toString());

        }

        System.out.println("Degree:" + degree.toString());
        return degree;
    }

    /** Return triad pattern frequency per Schall 2014
     *
     * @return
     */
    public Map getScahllTriadFreqs() {
        Map<String,Double> freqs = new HashMap<>();

        freqs.put("t01", 0.23);
        freqs.put("t02", 1.14);
        freqs.put("t03", 0.63);
        freqs.put("t04", 1.37);
        freqs.put("t05", 0.63);
        freqs.put("t06", 39.09);
        freqs.put("t07", 1.22);
        freqs.put("t08", 1.50);
        freqs.put("t09", 53.23);

        freqs.put("t01", 0.23);

        freqs.put("t11", 0.05);
        freqs.put("t12", 0.05);
        freqs.put("t13", 0.05);
        freqs.put("t14", 0.09);
        freqs.put("t15", 0.06);
        freqs.put("t16", 0.23);
        freqs.put("t17", 0.05);
        freqs.put("t18", 0.10);
        freqs.put("t19", 0.25);

        System.out.println(freqs);
        return freqs;
    }


    public String getTriadPattern(String u, String z, String v) throws IOException {
        File file = new File("id_pattern.cql");
        String query = FileUtils.readFileToString(file);

        Map<String,Object> params = new HashMap<>();
        params.put("u", u);
        params.put("v", v);
        params.put("z", z);

        String result = "";

        Iterator<Map<String,Object>> res = engine.execute(query, params).iterator();
        while (res.hasNext()) {
            Map<String,Object> row = res.next();
            System.out.println("ID pattern: ");
            System.out.println(row.toString());
            result = row.get("type").toString();

        }

        System.out.println("IDPattern: " + result.toString());
        return result;

    }

    /** Calculate closed triad frequencies
     *
     * @throws IOException
     */
    public void getClosedTriadFreqs() throws IOException {
        File file = new File("closed_triad_freq.cql");
        String query = FileUtils.readFileToString(file);

        Map<String, Object> resultMap = new HashMap<String, Object>();
        Iterator<Map<String, Object>> result = engine.execute(query).iterator();
        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            resultMap.put("t01", row.get("t01"));
            resultMap.put("t02", row.get("t02"));
            resultMap.put("t03", row.get("t03"));
            resultMap.put("t04", row.get("t04"));
            resultMap.put("t05", row.get("t05"));
            resultMap.put("t06", row.get("t06"));
            resultMap.put("t07", row.get("t07"));
            resultMap.put("t08", row.get("t08"));
            resultMap.put("t09", row.get("t09"));

        }

        System.out.println(resultMap.toString());

        //graphDB.shutdown();
    }


    /** Calculate open triad frequencies
     *
     * @throws IOException
     */
    public void getTriadFreqs() throws IOException{
        File file = new File("id_pattern_freq.cql");
        String query = FileUtils.readFileToString(file);
        //System.out.println(cql);

        Map<String, Object> resultMap = new HashMap<String, Object>();
        Iterator<Map<String, Object>> result = engine.execute(query).iterator();
        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            resultMap.put("t01", row.get("t01"));
            resultMap.put("t02", row.get("t02"));
            resultMap.put("t03", row.get("t03"));
            resultMap.put("t04", row.get("t04"));
            resultMap.put("t05", row.get("t05"));
            resultMap.put("t06", row.get("t06"));
            resultMap.put("t07", row.get("t07"));
            resultMap.put("t08", row.get("t08"));
            resultMap.put("t09", row.get("t09"));

        }

        System.out.println(resultMap.toString());

        //graphDB.shutdown();

    }


    /** Run recommender system with cross fold validation, TODO: define how results are reported
     *
     * @param folds             Number of cross validation folds to run
     * @param k                 Number of neighbors to use for voting
     * @param predicted_links   Number of predicted links to generate
     * @param leaveout_links    Number of test links to leave out for validation
     * @param user_count        Number of users to include in
     */
    public void runWithCrossValidation(Integer folds, Integer k, Integer predicted_links, Integer leaveout_links, Integer user_count) {
        this.folds = folds;
        this.k = k;
        this.predicted_links = predicted_links;
        this.leaveout_links = leaveout_links;
        this.user_count = user_count;

        this.rec_count = 0;
        this.valid_count = 0;
        this.pred_link_count = 0;

        // get list of users ordered by number of FOLLOWS

        // FIXME: recommend using Jaccard similarity
        // FIXME: UNCOMMENT FOR JACCARD SIMILARITY
//        for (int v=0; v < folds; v++) {
//            this.users = getRandomUsers(user_count);
//            Map<String,Object> linkMap = new HashMap<String,Object>();
//
//            for (Integer id : this.users) {
//                try {
//                    linkMap = recommend(id);
//                    if ((Boolean) linkMap.get("test_in_pred")) {
//                        this.valid_count += 1;
//                    }
//                    this.rec_count += 1;
//                    this.pred_link_count += ((List) linkMap.get("pred")).size();
//                } catch (NullPointerException e) {
//                    System.out.println("Null Pointer exception");
//                    System.out.println(e.getMessage());
//                } catch (Exception e) {
//                    System.out.println("Exception during recommendation: ");
//                    System.out.println(e.getMessage());
//                }
//            }
//        }

        // FIXME: recommend using TC similarity
        for (int v=0; v < folds; v++) {
            this.users = getRandomUsers(user_count);
            Map<String,Object> linkMap = new HashMap<>();

            for (String id : this.users) {
                try {
                    // FIXME: recommendTC(...)
                    linkMap = recommendTC(id, predicted_links);
                    if ((Boolean) linkMap.get("test_in_pred")) {
                        this.valid_count += 1;
                    }
                    this.rec_count += 1;
                    this.pred_link_count += ((List) linkMap.get("pred")).size();
                } catch (NullPointerException e) {
                    System.out.println("Null Pointer exception");
                    System.out.println(e.getMessage());
                } catch (Exception e) {
                    System.out.println("Exception during TC recommendation: ");
                    System.out.println(e.getMessage());
                }
            }
        }
        //this.users = getTopUsers(user_count);

        //Map<String,Object> linkMap = new HashMap<String, Object>();

        //for (Integer id : this.users) {
        //    linkMap = recommend(id);
        //    if ((Boolean)linkMap.get("test_in_pred")) {
        //        this.valid_count += 1;
        //    }
        //    this.rec_count += 1;
        //    this.pred_link_count += ((List)linkMap.get("pred")).size();
        //}

    }

    /** Query graph database for user_ids of users with most outgoing :FOLLOWS edges
     *
     * @param num_users             Number of users to return
     * @return ArrayList<Integer>   User ids of top users
     */
    public ArrayList<String> getTopUsers(Integer num_users) {

        ArrayList<String> resultsArray = new ArrayList<>();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("num_users", num_users);

        String query =
                "MATCH (u:User)-[:FOLLOWS]->(o) WITH u, count(o) as c ORDER BY c DESC RETURN u.name as id, c SKIP 50 LIMIT {num_users}";

        Iterator<Map<String, Object>> result = engine.execute(query, params).iterator();
        while(result.hasNext()) {
            Map<String, Object> row = result.next();
            resultsArray.add((String)row.get("id"));
        }

        return resultsArray;
    }

    /** Query graph database for user_ids at random
     *
     * @param num_users             Number of users to return
     * @return ArrayList<String>   User ids
     */
    public ArrayList<String> getRandomUsers(Integer num_users) {
        ArrayList<String> resultsArray = new ArrayList<>();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("num_users", num_users);

        String query =
            "MATCH (u1:User)-[:FOLLOWS]->(x) WITH u1, count(x) as num\n" +
            "ORDER BY num DESC\n" +
            "WITH u1 LIMIT 10000\n" +
            "WITH u1, rand() as random\n" +
            "ORDER BY random\n" +
            //"WITH u1 LIMIT 1\n" +
            "RETURN u1.name AS id LIMIT {num_users}";

        Iterator<Map<String, Object>> result = engine.execute(query, params).iterator();
        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            resultsArray.add((String)row.get("id"));
        }

        return resultsArray;
    }

    /** Find all
     *
     * @param u
     * @return
     */
    public ArrayList<Map<String,Object>> getTriads(String u) {

        // FIXME: does this need to be closed triads only???
        ArrayList<Map<String,Object>> resultsArray = new ArrayList<>();
        String query = "MATCH (u:User {name: {user_id}})--(z:User)--(v:User) WHERE u<>v AND v<>z AND z<>u // find triads only \n" +
                "RETURN u.name AS u, z.name AS z, v.name AS v LIMIT 100";

        Map<String,Object> params = new HashMap<>();
        params.put("user_id", u);

        Iterator<Map<String, Object>> result = engine.execute(query, params).iterator();
        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            Map<String, Object> obs = new HashMap<>();

            obs.put("u", row.get("u"));
            obs.put("v", row.get("v"));
            obs.put("z", row.get("z"));
            resultsArray.add(obs);
        }
        System.out.println("All triads: " + resultsArray.toString());
        return resultsArray;
    }

    /** Generate recommendations for a specific user using TC
     *
     * @param user_id
     * @return
     */
    public Map<String,Object> recommendTC(String user_id, Integer k) throws IOException {

        // TODO: remove link
        // TODO: get this id

        ArrayList<Map<String,Object>> triads = getTriads(user_id);
        //String rm_id = (String) triads.get(0).get("v");

        String rmQuery =
                "MATCH (u1:User {name: {user_id}})-[:FOLLOWS]->(o) WITH o, rand() as r, u1 \n" +
                "ORDER BY r \n" +
                "WITH u1, o LIMIT {leaveout_links} \n" +
                "MATCH (u1)-[r]-(o) DELETE  r WITH o,u1 \n" +
                "RETURN o.name AS rm_id";

        Map<String,Object> params = new HashMap<String,Object>();
        params.put("leaveout_links", 1);
        params.put("user_id", user_id);


        String rm_id = "";
        Iterator<Map<String, Object>> result = engine.execute(rmQuery, params).iterator();
        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            rm_id = (String)row.get("rm_id");
        }

        // get array of list Integer IDs


        //Integer k = getNDegree(user_id.toString()); // FIXME: use consistent type for user_id
        //ArrayList<Map<String,Object>> triads = getTriads(user_id);
        //ArrayList<Map<String,Object>> knn = new ArrayList();
        Map<String, Double> knn = new HashMap<>();
        ArrayList<String> pred = new ArrayList<>();

        for (Map<String,Object> obs : triads) {
            Double tc = tc(obs.get("u").toString(), obs.get("v").toString(), obs.get("z").toString());
            //pred.add((Integer)obs.get("v"));
            //Map<String,Object> n = new HashMap<>();

            if (knn.containsKey(obs.get("v"))) {
                Double val = knn.get(obs.get("v"));
                val = val + tc;

                knn.put((String)obs.get("v"), val);

                System.out.println("+++++++++++*********** UPDATED TC **********++++++++++ " + tc.toString());
            } else {
                knn.put((String)obs.get("v"), tc);
            }
            //n.put("tc", tc);
            //n.put("u", obs.get("u"));
            //n.put("v", obs.get("v"));
            //knn.add(n);
        }
        // find all triads (u,v,z) where user_id = u
        // calc tc for all (u,v,z) triads
        // return top k v ids

        //System.out.println(knn);

        String addQuery =
                "MATCH (u1:User {name: {user_id}}), (o:User { name: {o}}) \n" +
                        "CREATE UNIQUE (u1)-[:FOLLOWS]->(o)";

        params.put("o", rm_id);

        Iterator<Map<String, Object>> resultAdd = engine.execute(rmQuery, params).iterator();
// Don't need the results of this query
//        while (result.hasNext()) {
//            Map<String, Object> row = result.next();
//            rm_id = (Integer)row.get("rm_id");
//        }



        Map.Entry<String,Double> maxEntry = null;



        // FIXME: return sorted k recommendations

        for (int p=0; p<predicted_links;p++) {
            maxEntry = null;
            for (Map.Entry<String, Double> entry : knn.entrySet()) {
                if (maxEntry == null || entry.getValue() > maxEntry.getValue()) {
                    maxEntry = entry;
                    //pred.add(entry.getKey());
                }
            }

            pred.add(maxEntry.getKey());
            knn.remove(maxEntry.getKey());

        }


        // FIXME: handle possible NullPointerException if no recommendations(?)
        //pred.add(maxEntry.getKey());
        //return knn;

        //return pred;
        Boolean test_in_pred = Boolean.FALSE;
        //for (Integer id : pred) {

        for (String id : pred) {
            if (rm_id.equals(id)) {
                test_in_pred = Boolean.TRUE;
            }
        }

        Map<String,Object> recs = new HashMap<>();

        recs.put("test_in_pred", test_in_pred);
        recs.put("test", rm_id);
        recs.put("pred", pred);
        recs.put("u", user_id);

        System.out.println(recs);
        return recs;

    }


    /** Calculate J
     *
     * @param u1
     * @param u2
     * @return
     */
    public Double jaccard(String u1, String u2) throws IOException {

        File file = new File("jaccard.cql");
        String query = FileUtils.readFileToString(file);

        Map<String,Object> params = new HashMap<>();
        params.put("u1", u1);
        params.put("u2", u2);

        Double result = 0.0;

        Iterator<Map<String,Object>> res = engine.execute(query, params).iterator();
        while (res.hasNext()) {
            Map<String,Object> row = res.next();
            result = (Double)row.get("jaccard");

        }

        return result;
    }


    /** Calculate Triadic closeness u<->v
     *
     * @param u
     * @param v
     * @param z
     * @return
     */
    public Double tc(String u, String v, String z) throws IOException {
        // find id triad_id
        // calc k (degree)
        // return (closed_triad_freqs[triad_id] / freqs[triad_id]) * 1/k

        Map<String,String> openToClosedPatterns = new HashMap<>();
        openToClosedPatterns.put("t01", "t11");
        openToClosedPatterns.put("t02", "t12");
        openToClosedPatterns.put("t03", "t13");
        openToClosedPatterns.put("t04", "t14");
        openToClosedPatterns.put("t05", "t15");
        openToClosedPatterns.put("t06", "t16");
        openToClosedPatterns.put("t07", "t17");
        openToClosedPatterns.put("t08", "t18");
        openToClosedPatterns.put("t09", "t19");

        String pattern_id = getTriadPattern(u, z, v);
        Integer k = getNDegree(z);  // degree of z!
        Map<String,Double> freqs = getScahllTriadFreqs();

        String closed_pattern_id = openToClosedPatterns.get(pattern_id);

        Double tc_calc = freqs.get(closed_pattern_id) / freqs.get(pattern_id) * 1/k;

        System.out.println("tc calc called with u: " + u + " v: " + v + " z: " + z);
        System.out.println("TC_CALC: " + tc_calc.toString());

        return freqs.get(closed_pattern_id) / freqs.get(pattern_id) * 1/k;


    }

    /** Generate recommendations for a specific user
     *
     * @param user_id
     * @return          Map {test_in_pred, test, pred, u1}
     *                      test_in_pred -> was the test id (the link held-out) in the predicted link set?
     *                      test         -> the link held-out
     *                      pred         -> the set of predicted links
     *                      u1           -> the user_id for which we are generating recommendations
     */
    public Map<String,Object> recommend(String user_id) {

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("user_count", user_count);
        params.put("user_id", user_id);
        params.put("leaveout_links", leaveout_links);
        params.put("k", k);
        params.put("predicted_links", predicted_links);


        String query =
                        "// SELECT user at random from top 1000 follows initiated\n" +
                        "// remove one FOLLOWS edge at random\n" +
                        "// finds jaccard for all users with follow intersect\n" +
                        "// select kNN based on jaccard, allow each neighbor to vote (TODO: weight the vote!)\n" +
                        "// take x top recommendations\n" +
                        "// replace remove edge\n" +
                        "// Is test edge in x recommendations?\n" +
                        "MATCH (u1:User {name: {user_id}})-[:FOLLOWS]->(o) WITH o, rand() as r, u1 \n" +
                        "ORDER BY r \n" +
                        "WITH u1, o LIMIT {leaveout_links} \n" +
                        "MATCH (u1)-[r]-(o) DELETE  r WITH o,u1 \n" +
                        "MATCH (u1)-[:FOLLOWS]->(x)<-[:FOLLOWS]-(u2:User) WHERE u1 <> u2 WITH u1, u2,o\n" +
                        "MATCH (u1)-[r:FOLLOWS]->(intersection)<-[:FOLLOWS]-(u2) WITH u1, u2, count(intersection) as intersect, o\n" +
                        "MATCH (u1)-[r:FOLLOWS]->(rest1) WITH u1, u2, intersect, collect(DISTINCT rest1) AS coll1, o\n" +
                        "MATCH (u2)-[r:FOLLOWS]->(rest2) WITH u1, u2, collect(DISTINCT rest2) AS coll2, coll1, intersect, o\n" +
                        "WITH u1, u2, intersect, coll1, coll2, length(coll1 + filter(x IN coll2 WHERE NOT x IN coll1)) as union, o\n" +
                        "WITH o,u1, u2, (1.0*intersect/union) as jaccard ORDER BY jaccard DESC LIMIT {k} \n" +
                        "//CREATE (u1)<-[:Jaccard{coef: (1.0*intersect/union)}]-(u2)\n" +
                        "CREATE UNIQUE (u1)-[:FOLLOWS]->(o) WITH o, u2, u1\n" +
                        "MATCH (u2)-[f:FOLLOWS]->(u3) WITH u1, count(f) as count_f, o.name as test, u3  ORDER BY count_f DESC LIMIT {predicted_links} \n" +
                        "WITH collect(u3.name) as pred, test, u1 \n" +
                        "RETURN (test IN pred) AS test_in_pred, test, pred, u1.name AS id";

        Map<String, Object> resultMap = new HashMap<String, Object>();
        Iterator<Map<String, Object>> result = engine.execute(query, params).iterator();
        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            resultMap.put("test_in_pred", row.get("test_in_pred"));
            resultMap.put("test", row.get("test"));
            resultMap.put("pred", row.get("pred"));
            resultMap.put("id", row.get("id"));
        }

        System.out.println(resultMap.toString());
        return resultMap;
    }



    /** Main method
     *
     *
     * @param args  Command line arguments - not used
     */
    public static void main(String[] args) throws IOException {
        //RecommendFollows rec_sys = new RecommendFollows("data/graph.db");   // Initialize graph DB

        RecommendFollows rec_sys = new RecommendFollows("/Users/lyonwj/Copy/willithesis/neo4j_instances/multi_modal_network/data/graph.db");
        //RecommendFollows rec_sys = new RecommendFollows("data/test_db/data/graph.db");

        // Run test with cross validation
        // folds=10, k=50, predicted_links=50, leaveout_links=1, user_count=1000
        //rec_sys.runWithCrossValidation(10, 50, 50, 1, 100);
        //rec_sys.runWithCrossValidation(5, , 25, 1, 100);
        //rec_sys.runWithCrossValidation(folds, k, predicted_links, leaveout_links, user_count);


        // warm up the cache
        rec_sys.engine.execute("MATCH (n:User) RETURN count(n)");

        //rec_sys.getTriadFreqs();
        //rec_sys.getClosedTriadFreqs();

        System.out.println(rec_sys.tc("siemonday", "lucasdaddiego", "luis-almeida"));

        //System.out.println("**********************************" + rec_sys.recommendTC(3915, 10));
        //System.out.println("**********************************" + rec_sys.recommendTC(1, 10));
        //System.out.println("**********************************" + rec_sys.recommendTC(78933, 10));
        //System.out.println("**********************************" + rec_sys.recommendTC(837, 10));


        //rec_sys.graphDB.shutdown();
        /** Run recommender system with cross fold validation, TODO: define how results are reported
         *
         * @param folds             Number of cross validation folds to run
         * @param k                 Number of neighbors to use for voting
         * @param predicted_links   Number of predicted links to generate
         * @param leaveout_links    Number of test links to leave out for validation
         * @param user_count        Number of users to include in
         */
    //public void runWithCrossValidation(Integer folds, Integer k, Integer predicted_links, Integer leaveout_links, Integer user_count) {

        rec_sys.runWithCrossValidation(10, 5, 5, 1, 25);
        rec_sys.reportResults();
//        rec_sys.runWithCrossValidation(10, 5, 50, 1, 10);
//        rec_sys.reportResults();
//        rec_sys.runWithCrossValidation(10, 5, 100, 1, 10);
//        rec_sys.reportResults();
//
//        rec_sys.runWithCrossValidation(10, 10, 25, 1, 10);
//        rec_sys.reportResults();
//        rec_sys.runWithCrossValidation(10, 10, 50, 1, 10);
//        rec_sys.reportResults();
//        rec_sys.runWithCrossValidation(10, 10, 100, 1, 10);
//        rec_sys.reportResults();
//
//        rec_sys.runWithCrossValidation(10, 25, 25, 1, 10);
//        rec_sys.reportResults();
//        rec_sys.runWithCrossValidation(10, 25, 50, 1, 10);
//        rec_sys.reportResults();
//        rec_sys.runWithCrossValidation(10, 25, 100, 1, 10);
//        rec_sys.reportResults();
//
//        rec_sys.runWithCrossValidation(10, 50, 25, 1, 10);
//        rec_sys.reportResults();
//        rec_sys.runWithCrossValidation(10, 50, 50, 1, 10);
//        rec_sys.reportResults();
//        rec_sys.runWithCrossValidation(10, 50, 100, 1, 10);
//        rec_sys.reportResults();
//
//        rec_sys.runWithCrossValidation(10, 100, 25, 1, 10);
//        rec_sys.reportResults();
//        rec_sys.runWithCrossValidation(10, 100, 50, 1, 10);
//        rec_sys.reportResults();
//        rec_sys.runWithCrossValidation(10, 100, 100, 1, 10);
//        rec_sys.reportResults();
//
//        rec_sys.runWithCrossValidation(10, 1000, 25, 1, 10);
//        rec_sys.reportResults();
//        rec_sys.runWithCrossValidation(10, 1000, 50, 1, 10);
//        rec_sys.reportResults();
//        rec_sys.runWithCrossValidation(10, 1000, 100, 1, 10);
//        rec_sys.reportResults();
//
//        rec_sys.runWithCrossValidation(10, 2000, 25, 1, 10);
//        rec_sys.reportResults();
//        rec_sys.runWithCrossValidation(10, 2000, 50, 1, 10);
//        rec_sys.reportResults();
//        rec_sys.runWithCrossValidation(10, 2000, 100, 1, 10);
//        rec_sys.reportResults();



        // TODO: report results
        //rec_sys.reportResults();

    }


    /** Register shutdown hook for the Neo4j instance so that it shuts down when the VM exits
     *
     * @param graphDB   The Neo4j instance to shutdown
     */
    private static void registerShutdownHook(final GraphDatabaseService graphDB) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
        public void run(){
                graphDB.shutdown();
            }
        });
    }


}
