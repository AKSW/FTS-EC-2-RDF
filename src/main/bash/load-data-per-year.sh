#/bin/bash
srcPath="../../../data/en"

virtload.sh "$srcPath/export_2007_en.nt" http://fts.publicdata.eu/en-2007 1100 dba dba
virtload.sh "$srcPath/export_2008_en.nt" http://fts.publicdata.eu/en-2008 1100 dba dba
virtload.sh "$srcPath/export_2009_en.nt" http://fts.publicdata.eu/en-2009 1100 dba dba
virtload.sh "$srcPath/export_2010_en.nt" http://fts.publicdata.eu/en-2010 1100 dba dba
virtload.sh "$srcPath/export_2010_en_fedf.nt" http://fts.publicdata.eu/en-2010-fedf 1100 dba dba


isql-vt 1100 dba dba "EXEC=DB.DBA.RDF_GRAPH_GROUP_CREATE ('http://fts.publicdata.eu', 0);"
isql-vt 1100 dba dba "EXEC=DB.DBA.RDF_GRAPH_GROUP_INS('http://fts.publicdata.eu', 'http://fts.publicdata.eu/en-2008');"
isql-vt 1100 dba dba "EXEC=DB.DBA.RDF_GRAPH_GROUP_INS('http://fts.publicdata.eu', 'http://fts.publicdata.eu/en-2009');"
isql-vt 1100 dba dba "EXEC=DB.DBA.RDF_GRAPH_GROUP_INS('http://fts.publicdata.eu', 'http://fts.publicdata.eu/en-2010');"
isql-vt 1100 dba dba "EXEC=DB.DBA.RDF_GRAPH_GROUP_INS('http://fts.publicdata.eu', 'http://fts.publicdata.eu/en-2010-fedf');"


