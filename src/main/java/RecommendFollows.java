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
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

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
    private Map<String,Integer> degreeMap; // cache the degree calculations
    private Map<String,String> patternMap; // cache the triad pattern type
    private Map<String,Double> jaccardMap; // cache jaccard


    /** Initialize Neo4j graph database connection
     *
     * @param DB_PATH   relative path of Neo4j data store (graph.db)
     */
    public RecommendFollows(String DB_PATH) {

        this.degreeMap = new HashMap<String,Integer>();
        this.patternMap = new HashMap<>();
        this.jaccardMap = new HashMap<>();

        this.graphDB = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(DB_PATH).
               setConfig(GraphDatabaseSettings.nodestore_mapped_memory_size.name(), "100M").
               setConfig(GraphDatabaseSettings.relationshipstore_mapped_memory_size.name(), "1000M" ).
               setConfig(GraphDatabaseSettings.nodestore_propertystore_mapped_memory_size.name(), "400M").
               setConfig(GraphDatabaseSettings.strings_mapped_memory_size.name(), "200M" ).
               setConfig(GraphDatabaseSettings.arrays_mapped_memory_size.name(), "10M").
                newGraphDatabase();

//        neostore.nodestore.db.mapped_memory=100M
//        neostore.relationshipstore.db.mapped_memory=1000M
//        neostore.propertystore.db.mapped_memory=200M
//        neostore.propertystore.db.strings.mapped_memory=200M
//        neostore.propertystore.db.arrays.mapped_memory=0M

//        static {
//            LARGE_CONFIG.put( GraphDatabaseSettings.nodestore_mapped_memory_size.name(), "100M" );
//            LARGE_CONFIG.put( GraphDatabaseSettings.relationshipstore_mapped_memory_size.name(), "300M" );
//            LARGE_CONFIG.put( GraphDatabaseSettings.nodestore_propertystore_mapped_memory_size.name(), "400M" );
//            LARGE_CONFIG.put( GraphDatabaseSettings.strings_mapped_memory_size.name(), "800M" );
//            LARGE_CONFIG.put( GraphDatabaseSettings.arrays_mapped_memory_size.name(), "10M" );
//            LARGE_CONFIG.put( GraphDatabaseSettings.dump_configuration.name(), "true" );
//        }

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

        //precision = (relevant documents intersect retrieved documents) / (retrieved documents)

        //recall = (relevant documents intersect retrieved documents) / (relevant documents)

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

        if (degreeMap.containsKey(user_id)) {
            return degreeMap.get(user_id);
        } else {

            String query = "MATCH (u:User {name: {user_id}})-[r:FOLLOWS]-(x) RETURN count(DISTINCT x) as k";
            Map<String,Object> params= new HashMap<>();
            params.put("user_id", user_id);

            Integer degree = 0;


            Iterator<Map<String, Object>> result = engine.execute(query, params).iterator();
            while (result.hasNext()) {
                Map<String, Object> row = result.next();
                degree = Integer.parseInt(row.get("k").toString());

            }

            //System.out.println("Degree:" + degree.toString());
            degreeMap.put(user_id, degree);
            return degree;
        }
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

        //System.out.println(freqs);
        return freqs;
    }


    public String getTriadPattern(String u, String z, String v) throws IOException {

        if(patternMap.containsKey(u+z+v)) {
            return patternMap.get(u+z+v);
        } else {

            File file = new File("id_pattern.cql");
            String query = FileUtils.readFileToString(file);

            Map<String, Object> params = new HashMap<>();
            params.put("u", u);
            params.put("v", v);
            params.put("z", z);

            String result = "";

            Iterator<Map<String, Object>> res = engine.execute(query, params).iterator();
            while (res.hasNext()) {
                Map<String, Object> row = res.next();
                //System.out.println("ID pattern: ");
                //System.out.println(row.toString());
                if (!row.containsKey("type")) {
                    System.out.println("UUUUUGGGGHHH");
                }
                if(row.get("type") == null) {
                    result = "t04";
                } else {
                    result = (String) row.get("type");
                }

            }

            if (result == null) {
                System.out.println("UUUUGGGGGHHHHHH");
            }

            //System.out.println("IDPattern: " + result.toString());
            patternMap.put(u + z + v, result);
            return result;
        }

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

        //System.out.println(resultMap.toString());

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

        //System.out.println(resultMap.toString());

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
    public void runWithCrossValidation(Integer folds, Integer k, Integer predicted_links, Integer leaveout_links, Integer user_count) throws IOException {
        this.folds = folds;
        this.k = k;
        this.predicted_links = predicted_links;
        this.leaveout_links = leaveout_links;
        this.user_count = user_count;

        this.rec_count = 0;
        this.valid_count = 0;
        this.pred_link_count = 0;

        // get list of users ordered by number of FOLLOWS

//        // FIXME: recommend using Jaccard similarity
//        // FIXME: UNCOMMENT FOR JACCARD SIMILARITY
//        for (int v=0; v < folds; v++) {
//            this.users = getRandomUsers(user_count);
//            Map<String,Object> linkMap = new HashMap<String,Object>();
//
//            for (String id : this.users) {
//                try {
//                    linkMap = recommendJaccard(id, predicted_links);
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

        // FIXME: recommend using Willimetric
        for (int v=0; v < folds; v++) {
            this.users = getRandomUsers(user_count);
            Map<String,Object> linkMap = new HashMap<>();

            for (String id : this.users) {
                try {

                    linkMap = recommendCombined(id, predicted_links);
                    if ((Boolean) linkMap.get("test_in_pred")) {
                        this.valid_count += 1;
                    }

                    if(((List)linkMap.get("pred")).size() > 0) {
                        this.rec_count += 1;
                        this.pred_link_count += ((List) linkMap.get("pred")).size();
                    }
                } catch (NullPointerException e) {
                    System.out.println("Null Pointer exception");
                    System.out.println(e.getMessage());
                } catch (Exception e) {
                    System.out.println("Exception during TC recommendation: ");
                    System.out.println(e.getMessage());
                }
            }
        }

        // FIXME: recommend using TC similarity
//        for (int v=0; v < folds; v++) {
//            this.users = getRandomUsers(user_count);
//            Map<String,Object> linkMap = new HashMap<>();
//
//            for (String id : this.users) {
//                try {
//                    // FIXME: recommendTC(...)
//                    linkMap = recommendTC(id, predicted_links);
//                    if ((Boolean) linkMap.get("test_in_pred")) {
//                        this.valid_count += 1;
//                    }
//                    this.rec_count += 1;
//                    this.pred_link_count += ((List) linkMap.get("pred")).size();
//                } catch (NullPointerException e) {
//                    System.out.println("Null Pointer exception");
//                    System.out.println(e.getMessage());
//                } catch (Exception e) {
//                    System.out.println("Exception during TC recommendation: ");
//                    System.out.println(e.getMessage());
//                }
//            }
//        }


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

//        String query =
//            "MATCH (u1:User)-[:FOLLOWS]->(x) WITH u1, count(x) as num WHERE num > 0 \n" +
//           // "ORDER BY num DESC\n" +
//          //  "WITH u1 LIMIT 100000\n" +
//            "MATCH (u1)-[:STARS]->(z) WITH u1, num, count(z) as numRepos WHERE numRepos > 0 \n" +
//            "WITH u1, rand() as random, num, numRepos ORDER BY random \n" +
//            "WHERE num < 50 AND numRepos < 50 \n" +
//            //"WITH u1 LIMIT 1\n" +
//            "RETURN u1.name AS id LIMIT {num_users}";

        String query =
                "MATCH (u1:User) WHERE (u1)-->(:User) AND (u1)-->(:Repo) \n" +
                "WITH u1, rand() as random ORDER BY random LIMIT {num_users}\n" +
                "RETURN u1.name AS id";

        //String query =
        //        "MATCH (u1:User) RETURN u1.name AS id LIMIT {num_users}";

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
    public ArrayList<Map<String,Object>> getTriads(String u, Boolean openOnly) {

        // FIXME: does this need to be closed triads only???
        ArrayList<Map<String,Object>> resultsArray = new ArrayList<>();

        String query = "";

        if (openOnly) {
            query = "MATCH (u:User {name: {user_id}})--(z:User)--(v:User) WHERE u<>v AND v<>z AND z<>u AND NOT (u)-->(v) // find triads only \n" +
                    "RETURN u.name AS u, z.name AS z, v.name AS v LIMIT 1000";
        } else {

            query = "MATCH (u:User {name: {user_id}})--(z:User)--(v:User) WHERE u<>v AND v<>z AND z<>u AND (u)-->(v) // find triads only \n" +
                    "RETURN u.name AS u, z.name AS z, v.name AS v LIMIT 1000";
        }

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
        //System.out.println("All triads: " + resultsArray.toString());
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

        ArrayList<Map<String,Object>> triads = getTriads(user_id, Boolean.TRUE);
        //String rm_id = (String) triads.get(0).get("v");  //FIXME: randomly select



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

                //System.out.println("+++++++++++*********** UPDATED TC **********++++++++++ " + tc.toString());
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

        if (this.jaccardMap.containsKey(u1+u2)) {
            return jaccardMap.get(u1+u2);
        } else if (this.jaccardMap.containsKey(u2+u1)) {
            return jaccardMap.get(u2+u1);
        } else {

            File file = new File("jaccard.cql");
            String query = FileUtils.readFileToString(file);

            Map<String, Object> params = new HashMap<>();
            params.put("u1", u1);
            params.put("u2", u2);

            Double result = 0.0;

            Iterator<Map<String, Object>> res = engine.execute(query, params).iterator();
            while (res.hasNext()) {
                Map<String, Object> row = res.next();
                result = (Double) row.get("jaccard");

            }

            this.jaccardMap.put(u1+u2, result);
            this.jaccardMap.put(u2+u1, result);

            return result;

        }
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

        //System.out.println("tc calc called with u: " + u + " v: " + v + " z: " + z);
        //System.out.println("TC_CALC: " + tc_calc.toString());
        return tc_calc;
        //return freqs.get(closed_pattern_id) / freqs.get(pattern_id) * 1/k;


    }

    public Map<String,Object> recommendJaccard(String user_id, Integer k) throws IOException {
        //ArrayList<Map<String,Object>> triads = getTriads(user_id);

        //String rm_id = (String) triads.get(0).get("v"); // FIXME: randomly select

        String rmQuery =
                "MATCH (u1:User {name: {user_id}})-[:FOLLOWS]->(o) WITH o, rand() as r, u1 \n" +
                "ORDER BY r \n" +
                "WITH u1, o LIMIT {leaveout_links} \n" +
                "MATCH (u1)-[r]-(o) DELETE r WITH o, u1 \n" +
                "RETURN o.name as rm_id";

        Map<String, Object> params = new HashMap<>();
        params.put("leaveout_links", 1);
        params.put("user_id", user_id);

        String rm_id = "";
        Iterator<Map<String, Object>> result = engine.execute(rmQuery, params).iterator();
        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            rm_id = (String)row.get("rm_id");
        }

        String knnQuery =
                "MATCH (u1:User {name: {user_id}})-[:STARS]->(r:Repo)<-[:STARS]-(o:User) WITH o \n" +
                "RETURN o.name as k_id";

        ArrayList <String> kList = new ArrayList<>();
        Iterator<Map<String, Object>> resultkList = engine.execute(knnQuery, params).iterator();
        while (resultkList.hasNext()) {
            Map<String, Object> row = resultkList.next();
            kList.add((String)row.get("k_id"));
        }



        Map<String,Double> knn = new HashMap<>();
        ArrayList<String> pred = new ArrayList<>();

        for (String obs : kList) {
            Double jaccard = jaccard(user_id, obs);
            knn.put(obs, jaccard);
        }

        //for (Map<String,Object> obs : triads) {
        //    Double jaccard = jaccard(obs.get("u").toString(), obs.get("v").toString());
        //    knn.put((String)obs.get("v"), jaccard);
        //}

        String addQuery =
                "MATCH (u1:User {name: {user_id}}), (o:User { name: {o}}) \n" +
                        "CREATE UNIQUE (u1)-[:FOLLOWS]->(o)";

        params.put("o", rm_id);

        Iterator<Map<String, Object>> resultAdd = engine.execute(rmQuery, params).iterator();

        Map.Entry<String,Double> maxEntry = null;



        // FIXME: return sorted k recommendations

        for (int p=0; p<predicted_links;p++) {
            if (p>kList.size() - 1){ break; };
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

    /**
     *
     * @param user_id
     * @param k
     * @return
     * @throws IOException
     */
    public Map<String,Object> recommendCombined(String user_id, Integer k) throws IOException {



        String rmQuery =
                "MATCH (u1:User {name: {user_id}})-[:FOLLOWS]->(o) with o, rand() as r, u1 \n" +
                "ORDER BY r \n" +
                "WITH u1, o LIMIT {leaveout_links} \n" +
                "MATCH (u1)-[r]-(o) DELETE r WITH o,u1 \n" +
                "RETURN o.name AS rm_id";

        String rmClosedTriadQuery =
                "MATCH (u1:User {name: {user_id}})-[r:FOLLOWS]-(u2:User {name: {rm_id}}) \n" +
                "DELETE r";

        String weightQuery =
                "MATCH (u1:User {name: {user_id}})-[f:FOLLOWS]->(o:User) WITH u1, count(f) AS fCount \n" +
                "MATCH (u1)-[s:STARS]->(r:Repo) WITH u1, fCount, count(r) as sCount \n" +
                "RETURN fCount, sCount";

        Map<String,Object> params = new HashMap<>();
        params.put("leaveout_links", 1);
        params.put("user_id", user_id);

        String rm_id = "";
        Iterator<Map<String, Object>> result = engine.execute(rmQuery, params).iterator();
        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            rm_id = (String)row.get("rm_id");
        }



       // ArrayList<Map<String,Object>> closedTriads = getTriads(user_id, Boolean.FALSE);

        //if (closedTriads.size() == 0){
        //    Map<String,Object> badRes = new HashMap<>();
        //    badRes.put("u", user_id);
        //    badRes.put("test", "");
        //    badRes.put("test_in_pred", Boolean.FALSE);
        //    badRes.put("pred", new ArrayList<>());
        //    return badRes;
        //}

        //Map<String,Object> randomItem = closedTriads.get(new Random().nextInt(closedTriads.size()));

        //rm_id = randomItem.get("v").toString();
        params.put("rm_id", rm_id);
        //engine.execute(rmClosedTriadQuery, params);

        ArrayList<Map<String,Object>> triads = getTriads(user_id, Boolean.TRUE);

        Map<String,Double> knn = new HashMap<>();
        ArrayList<String> pred = new ArrayList<>();

        for (Map<String,Object> obs : triads) {
            Double tc = tc(obs.get("u").toString(), obs.get("v").toString(), obs.get("z").toString());

            if (knn.containsKey(obs.get("v"))) {
                Double val = knn.get(obs.get("v"));
                val = val + tc;

                knn.put((String)obs.get("v"), val);

            } else {
                knn.put((String)obs.get("v"), tc);
            }
        }


        // for each k in knn:
        //    calculate jaccard(user_id, k)
        //    willimetric = a * tc + b * jaccard
        //    update knn.put((String)obs.get("v"), willimetric)

        for (Map.Entry<String, Double> entry : knn.entrySet()) {

            Long sCount = (long)0;
            Long fCount = (long)0;
            Iterator<Map<String, Object>> abresult = engine.execute(weightQuery, params).iterator();
            while (abresult.hasNext()) {
                Map<String, Object> row = abresult.next();
                sCount = (Long)row.get("sCount");
                fCount = (Long)row.get("fCount");
            }

            // weight as proportion
            Double a = (double)fCount / (sCount + fCount);
            Double b = (double)sCount / (sCount + fCount);

            //Double a = 0.0;
            //Double b = 1.0;

            Double tc = entry.getValue();
            Double jaccard = jaccard(user_id, entry.getKey());

            //System.out.println("Jaccard: " + jaccard.toString());
            //System.out.println("TC: " + tc.toString());

            Double willimetric = a * tc + b * jaccard;
            knn.put(entry.getKey(), willimetric);
        }


        String addQuery =
                "MATCH (u1:User {name: {user_id}}), (o:User {name: {o}}) \n" +
                "CREATE UNIQUE (u1)-[:FOLLOWS]->(o)";

        params.put("o", rm_id);

        Iterator<Map<String, Object>> resultAdd = engine.execute(rmQuery, params).iterator();

        Map.Entry<String,Double> maxEntry= null;

        for (int p=0; p<predicted_links; p++) {
            if (p>triads.size() - 1){ break; };



            for (Map.Entry<String, Double> entry : knn.entrySet()) {
                if (maxEntry == null || entry.getValue() > maxEntry.getValue()) {
                    maxEntry = entry;
                }
            }

            pred.add(maxEntry.getKey());
            //System.out.println("WILLIMETIC: " + maxEntry.getValue().toString());
            knn.remove(maxEntry.getKey());
            maxEntry = null;
        }

        Boolean test_in_pred = Boolean.FALSE;
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
                        "MATCH (u1)-[:STARS]->(x:Repo)<-[:STARS]-(u2:User) WHERE u1 <> u2 WITH x, u1, u2,o\n" +
                        "WITH count(x) AS intersect, u1, u2, o \n" +
                        //"MATCH (u1)-[r:STARS]->(intersection:Repo)<-[:STARS]-(u2) WITH u1, u2, count(intersection) as intersect, o\n" +
                        "MATCH (u1)-[r:STARS]->(rest1:Repo) WITH u1, u2, intersect, collect(DISTINCT rest1) AS coll1, o\n" +
                        "MATCH (u2)-[r:STARS]->(rest2:Repo) WITH u1, u2, collect(DISTINCT rest2) AS coll2, coll1, intersect, o\n" +
                        "WITH u1, u2, intersect, coll1, coll2, length(coll1 + filter(x IN coll2 WHERE NOT x IN coll1)) as union, o\n" +
                        "WITH o,u1, u2, (1.0*intersect/union) as jaccard ORDER BY jaccard DESC LIMIT {k} \n" +
                        "//CREATE (u1)<-[:Jaccard{coef: (1.0*intersect/union)}]-(u2)\n" +
                        "CREATE UNIQUE (u1)-[:FOLLOWS]->(o) WITH o, u2, u1\n" +
                        "MATCH (u2)-[f:FOLLOWS]->(u3:User) WITH u1, count(f) as count_f, o.name as test, u3  ORDER BY count_f DESC LIMIT {predicted_links} \n" +
                        "WITH collect(u3.name) as pred, test, u1 \n" +
                        "RETURN (test IN pred) AS test_in_pred, test, pred, u1.name AS id";

//        String query =
//                "MATCH (u1:User {name: {user_id}})-[:STARS]->(o) WITH o, rand() as r, u1"

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

        RecommendFollows rec_sys = new RecommendFollows("/home/lyonwj/multi_modal_network/data/graph.db");
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

        //System.out.println(rec_sys.tc("siemonday", "lucasdaddiego", "luis-almeida"));

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

        rec_sys.runWithCrossValidation(2, 5, 1, 1, 500);
        rec_sys.reportResults();

        rec_sys.runWithCrossValidation(2, 5, 5, 1, 500);
        rec_sys.reportResults();
        rec_sys.runWithCrossValidation(2, 5, 10, 1, 500);
        rec_sys.reportResults();
        rec_sys.runWithCrossValidation(2, 5, 20, 1, 500);
        rec_sys.reportResults();

        rec_sys.runWithCrossValidation(2, 5, 40, 1, 500);
        rec_sys.reportResults();

        rec_sys.runWithCrossValidation(2, 5, 50, 1, 500);
        rec_sys.reportResults();

        rec_sys.runWithCrossValidation(2, 5, 75, 1, 500);
        rec_sys.reportResults();

        rec_sys.runWithCrossValidation(2, 5, 100, 1, 500);
        rec_sys.reportResults();

        rec_sys.runWithCrossValidation(2, 5, 250, 1, 500);
        rec_sys.reportResults();

        rec_sys.runWithCrossValidation(2, 5, 500, 1, 500);
        rec_sys.reportResults();

        rec_sys.runWithCrossValidation(2, 5, 1000, 1, 500);
        rec_sys.reportResults();

        rec_sys.runWithCrossValidation(2, 5, 5000, 1, 500);
        rec_sys.reportResults();




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
