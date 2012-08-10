#/bin/bash

fileName=$1

#fileName="fp7.nt.tmp"
graphName="http://fp7.org/"


##./convert-csv-2-rdf-fp7-project-partners.sh > "$fileName"

virt-clear.sh 1111 dba dba "$graphName"
virtload.sh "$fileName" "$graphName" 1111 dba dba

