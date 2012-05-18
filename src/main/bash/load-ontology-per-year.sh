#/bin/bash
srcPath="../../../src/main/resources"

ontFile="$srcPath/fts-ontology.rdf.xml"

virtload.sh "$ontFile" http://fts.publicdata.eu/en-2007 1100 dba dba
virtload.sh "$ontFile" http://fts.publicdata.eu/en-2008 1100 dba dba
virtload.sh "$ontFile" http://fts.publicdata.eu/en-2009 1100 dba dba
virtload.sh "$ontFile" http://fts.publicdata.eu/en-2010 1100 dba dba
virtload.sh "$ontFile" http://fts.publicdata.eu/en-2010-fedf 1100 dba dba



