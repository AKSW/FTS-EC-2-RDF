iconv -f iso-8859-1 -t utf-8 export_2010_en.xml | iconv -f iso-8859-1 -t utf-8 > tmp.xml
tidy -i -xml tmp.xml > export_2010_en.fixed.xml
mv export_2010_en.xml export_2010_en.xml.orig
rm tmp.xml

iconv -f iso-8859-1 -t utf-8 export_2010_FEDF_en.xml | iconv -f iso-8859-1 -t utf-8 > tmp.xml
tidy -i -xml tmp.xml > export_2010_FEDF_en.fixed.xml
mv export_2010_FEDF_en.xml export_2010_FEDF_en.xml.orig
rm tmp.xml

iconv -f iso-8859-1 -t utf-8 export_2011_en.xml | iconv -f iso-8859-1 -t utf-8 > tmp.xml
tidy -i -xml tmp.xml > export_2011_en.fixed.xml
mv export_2011_en.xml export_2011_en.xml.orig
rm tmp.xml


iconv -f iso-8859-1 -t utf-8 export_2011_FEDF_en.xml | iconv -f iso-8859-1 -t utf-8 > tmp.xml
tidy -i -xml tmp.xml > export_2011_FEDF_en.fixed.xml
mv export_2011_FEDF_en.xml export_2011_FEDF_en.xml.orig
rm tmp.xml

