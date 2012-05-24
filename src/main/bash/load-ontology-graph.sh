#/bin/bash
srcPath="../../../data/en"

srcPath="../../../src/main/resources"
ontFile="$srcPath/fts-ontology.rdf"

memberGraph="http://fts.publicdata.eu/ontology"
virtload.sh "$ontFile" "$memberGraph" 1100 dba dba


groupGraph='http://fts.publicdata.eu/'
isql-vt 1100 dba dba "EXEC=DB.DBA.RDF_GRAPH_GROUP_INS('$groupGraph', '$memberGraph');"


