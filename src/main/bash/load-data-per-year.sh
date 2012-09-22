#/bin/bash
srcPath="../../../data/en"

port=$1
user=$2
pass=$3

isql-vt "$port" "$user" "$pass" "EXEC=SPARQL Clear Graph <http://fts.publicdata.eu/en-2007>;"
isql-vt "$port" "$user" "$pass" "EXEC=SPARQL Clear Graph <http://fts.publicdata.eu/en-2008>;"
isql-vt "$port" "$user" "$pass" "EXEC=SPARQL Clear Graph <http://fts.publicdata.eu/en-2009>;"
isql-vt "$port" "$user" "$pass" "EXEC=SPARQL Clear Graph <http://fts.publicdata.eu/en-2010>;"
isql-vt "$port" "$user" "$pass" "EXEC=SPARQL Clear Graph <http://fts.publicdata.eu/en-2011>;"

isql-vt "$port" "$user" "$pass" "EXEC=SPARQL Clear Graph <http://fts.publicdata.eu/en-2010-fedf>;"
isql-vt "$port" "$user" "$pass" "EXEC=SPARQL Clear Graph <http://fts.publicdata.eu/en-2011-fedf>;"


virtload.sh "$srcPath/export_2007_en.nt" http://fts.publicdata.eu/en-2007 "$port" "$user" "$pass"
virtload.sh "$srcPath/export_2008_en.nt" http://fts.publicdata.eu/en-2008 "$port" "$user" "$pass"
virtload.sh "$srcPath/export_2009_en.nt" http://fts.publicdata.eu/en-2009 "$port" "$user" "$pass"
virtload.sh "$srcPath/export_2010_en.nt" http://fts.publicdata.eu/en-2010 "$port" "$user" "$pass"
virtload.sh "$srcPath/export_2011_en.nt" http://fts.publicdata.eu/en-2011 "$port" "$user" "$pass"

virtload.sh "$srcPath/export_2010_FEDF_en.nt" http://fts.publicdata.eu/en-2010-fedf "$port" "$user" "$pass"
virtload.sh "$srcPath/export_2011_FEDF_en.nt" http://fts.publicdata.eu/en-2011-fedf "$port" "$user" "$pass"


groupGraph='http://fts.publicdata.eu/'
isql-vt "$port" "$user" "$pass" "EXEC=DB.DBA.RDF_GRAPH_GROUP_CREATE ('$groupGraph', 0);"
isql-vt "$port" "$user" "$pass" "EXEC=DB.DBA.RDF_GRAPH_GROUP_INS('$groupGraph', 'http://fts.publicdata.eu/en-2007');"
isql-vt "$port" "$user" "$pass" "EXEC=DB.DBA.RDF_GRAPH_GROUP_INS('$groupGraph', 'http://fts.publicdata.eu/en-2008');"
isql-vt "$port" "$user" "$pass" "EXEC=DB.DBA.RDF_GRAPH_GROUP_INS('$groupGraph', 'http://fts.publicdata.eu/en-2009');"
isql-vt "$port" "$user" "$pass" "EXEC=DB.DBA.RDF_GRAPH_GROUP_INS('$groupGraph', 'http://fts.publicdata.eu/en-2010');"
isql-vt "$port" "$user" "$pass" "EXEC=DB.DBA.RDF_GRAPH_GROUP_INS('$groupGraph', 'http://fts.publicdata.eu/en-2011');"

isql-vt "$port" "$user" "$pass" "EXEC=DB.DBA.RDF_GRAPH_GROUP_INS('$groupGraph', 'http://fts.publicdata.eu/en-2010-fedf');"
isql-vt "$port" "$user" "$pass" "EXEC=DB.DBA.RDF_GRAPH_GROUP_INS('$groupGraph', 'http://fts.publicdata.eu/en-2011-fedf');"


