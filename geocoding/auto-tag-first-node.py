#!/usr/bin/python

# This script assumes (Nominatim geocoder) data with the following schema:
# city | country | display_name | osm_node_type | osm_id | long | lat

# The script appends an "x" to each row with the first occurrence of a city-country pair, whose osm_node_type is node
# In other words: Given multiple possibilities for a city/country pair, the script selects the first one that is a node

import sys
import csv


#ifile = open('geocoded.csv', "rb")
#ofile = open('result.csv', "wb")
reader = csv.reader(sys.stdin, dialect='excel')
writer = csv.writer(sys.stdout, dialect='excel')

rownum = 0
seen=set()
for row in reader:
    key = None
    osmNodeType = None
    l = len(row)

    if l > 4:
        key = row[0] + "\t" + row[1]
        osmNodeType=row[3]

    flag=""

    if (not key in seen) and (osmNodeType == "node"):
        flag = "x"
        seen.add(key)
        
    row.append(flag)

    writer.writerow(row)

#ifile.close()
#ofile.close()

