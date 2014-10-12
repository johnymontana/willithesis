MATCH (u1:User {name: {u1}}), (u2:User { name:{u2}})
MATCH (u1)-[:STARS]->(x:Repo)<-[:STARS]-(u2) WITH x, u1, u2
WITH count(x) as intersect, u1, u2
//MATCH (u1)-[r:STARS]->(intersection:Repo)<-[:STARS]-(u2) WITH u1, u2, intersection LIMIT 10
//WITH count(intersection) as intersect, u1, u2
MATCH (u1)-[r:STARS]->(rest1) WITH u1, u2, intersect, collect(DISTINCT rest1) AS coll1
MATCH (u2)-[r:STARS]->(rest2) WITH u1, u2, collect(DISTINCT rest2) AS coll2, coll1, intersect
WITH u1, u2, intersect, coll1, coll2, length(coll1 + filter(x IN coll2 WHERE NOT x IN coll1)) as union
WITH u1, u2, (1.0*intersect/union) as jaccard
RETURN jaccard