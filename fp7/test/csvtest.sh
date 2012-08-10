csvFile='/home/raven/Projects/Current/Eclipse/Fts/data/fp7/fp7_ict_project_partners_database_2007_2011.xls'

xls2csv "$csvFile" | tail -n +2 > test.csv
./csv2tsv test.csv > test.tsv


./sparqlify-csv -c src/test/resources/mappings/csv/fp7-projects.sparqlify -f test.tsv

#cat test.csv | while read a b c ; do
#    echo "$a --- $b --- $c"
#done

