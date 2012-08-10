#!/usr/bin/python

# A nice and simple solution from http://stackoverflow.com/questions/2535255/fastest-way-convert-tab-delimited-file-to-csv-in-linux

import sys
import csv

tabin = csv.reader(sys.stdin, dialect=csv.excel_tab)
commaout = csv.writer(sys.stdout, dialect=csv.excel)

for row in tabin:
    commaout.writerow(row)

