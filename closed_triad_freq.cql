// Identify triad pattern
// u<->z<->v
// to calc TC for u<->v
//MATCH (u:User {name: 'a'})
//MATCH (z:User {name: 'c'})
//MATCH (v:User {name: 'd'})
//MATCH (u:User), (z:User), (v:User) WHERE u<>v AND v<>z AND z<>u // for all 3 node combos
MATCH (u:User)--(z:User)--(v:User) WHERE u<>v AND v<>z AND z<>u// find triads only
MATCH (u)-->(v) // closed triads only
WITH DISTINCT u, z, v
OPTIONAL MATCH t01=(z)-->(u)-->(z)-->(v)-->(z)
OPTIONAL MATCH t02=(z)-->(u)-->(z)-->(v)
OPTIONAL MATCH t03=(u)-->(z)-->(v)-->(z)
OPTIONAL MATCH t04=(u)-->(z)-->(v)
OPTIONAL MATCH t05=(v)-->(z)-->(u)-->(z)
OPTIONAL MATCH t06=(u)-->(z)<--(v)
OPTIONAL MATCH t07=(u)<--(z)-->(v)-->(z)
OPTIONAL MATCH t08=(v)-->(z)-->(u)
OPTIONAL MATCH t09=(u)<--(z)-->(v)


//RETURN t01, t02, t03, t04, t05, t06, t07, t08, t09

//WITH type =
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
WITH collect(type) AS types
WITH types//, length(types) as triadcount
WITH 
	[x IN types WHERE x = 't01' | x] AS t01_c,
	[x IN types WHERE x = 't02' | x] AS t02_c,
	[x IN types WHERE x = 't03' | x] AS t03_c,
	[x IN types WHERE x = 't04' | x] AS t04_c,
	[x IN types WHERE x = 't05' | x] AS t05_c,
	[x IN types WHERE x = 't06' | x] AS t06_c,
	[x IN types WHERE x = 't07' | x] AS t07_c,
	[x IN types WHERE x = 't08' | x] AS t08_c,
	[x IN types WHERE x = 't09' | x] AS t09_c
RETURN 
	// divide by triadcount for frequency?
	1.0*length(t01_c) AS t01,
	1.0*length(t02_c) AS t02,
	1.0*length(t03_c) AS t03,
	1.0*length(t04_c) AS t04,
	1.0*length(t05_c) AS t05,
	1.0*length(t06_c) AS t06,
	1.0*length(t07_c) AS t07,
	1.0*length(t08_c) AS t08,
	1.0*length(t09_c) AS t09