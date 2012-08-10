#!/bin/sh

# Zipping is convenient when using data files with git
bzip2 -dk fp7_ict_project_partners_database_2007_2011.xls.bz2

mv fp7_ict_project_partners_database_2007_2011.xls fp7_ict_project_partners_database_2007_2011.xls.tmp

xslFile='fp7_ict_project_partners_database_2007_2011.xls.tmp'
#xslFile='$1'

mappingFile='fp7-projects.sparqlify'
csvFile='fp7.csv.tmp'
csvFileCoordinators='fp7-coordinators.csv.tmp'


# Convert excel to csv
xls2csv "$xslFile" | tail -n +2 > "$csvFile"

# Create a separate csv file for the coordinations
cat "$csvFile" | grep '"Coordinator"' > "$csvFileCoordinators"


# Convert the overall table
sparqlify-csv -c "$mappingFile" -f "$csvFile" -v "fp7main"

# Conver the coordinators
sparqlify-csv -c "$mappingFile" -f "$csvFile" -v "fp7coordinators"

cat geocoded-tagged.csv | grep ',x\s*$' > geocoded-tagged.csv.tmp

sparqlify-csv -c fp7-projects.sparqlify -f geocoded-tagged.csv.tmp -v fp7geocode

sparqlify-csv -c fp7-projects.sparqlify -f dummy.csv -v fp7ontology


#cat test.csv | while read a b c ; do
#    echo "$a --- $b --- $c"
#done



