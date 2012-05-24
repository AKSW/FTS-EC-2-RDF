#/bin/bash
srcPath="../../../src/main/resources"

ontFile="$srcPath/germany.dbpedia.n3"
memberGraph="http://fts.publicdata.eu/dbpedia"

virtload.sh "$ontFile" "$memberGraph" 1100 dba dba

groupGraph='http://fts.publicdata.eu/'
isql-vt 1100 dba dba "EXEC=DB.DBA.RDF_GRAPH_GROUP_INS('$groupGraph', '$memberGraph')";

