// Identify triad pattern
// u<->z<->v
// to calc TC for u<->v
MATCH (u:User {id: {u} })
MATCH (z:User {id: {z} })
MATCH (v:User {id: {v} })

OPTIONAL MATCH t01=(z)-->(u)-->(z)-->(v)-->(z)
OPTIONAL MATCH t02=(z)-->(u)-->(z)-->(v)
OPTIONAL MATCH t03=(u)-->(z)-->(v)-->(z)
OPTIONAL MATCH t04=(u)-->(z)-->(v)
OPTIONAL MATCH t05=(v)-->(z)-->(u)-->(z)
OPTIONAL MATCH t06=(u)-->(z)<--(v)
OPTIONAL MATCH t07=(u)<--(z)-->(v)-->(z)
OPTIONAL MATCH t08=(v)-->(z)-->(u)
OPTIONAL MATCH t09=(u)<--(z)-->(v)

WITH
CASE
 WHEN t01 IS NOT NULL THEN 't01'
 WHEN t02 IS NOT NULL THEN 't02'
 WHEN t03 IS NOT NULL THEN 't03'
 WHEN t04 IS NOT NULL THEN 't04'
 WHEN t05 IS NOT NULL THEN 't05'
 WHEN t06 IS NOT NULL THEN 't06'
 WHEN t07 IS NOT NULL THEN 't07'
 WHEN t08 IS NOT NULL THEN 't08'
 WHEN t09 IS NOT NULL THEN 't09'
END
AS type
RETURN type