<?php
require_once( "../cmClasses/import.php5" );						//  load cmClass Importer
import( 'de.ceus-media.file.csv.IteratorReader' );
import( 'de.ceus-media.file.Writer' );
import( 'de.ceus-media.net.cURL' );


class Transformer
{
	var $statements 	= array();
	var $graphUri;
    var $namespaces;
    const error_character = '\\uFFFD';
    var $subventions = array ();
	var $beneficiaries ;
    var $cities ;
    var $filehandle;

	public function __construct()
	{
            #$this->graphUri		= "http://ec.europa.eu/beneficiaries/";
            $this->graphUri		= "http://fintrans.publicdata.eu/ec/";
            $this->namespaces = array (
                  "fts-o" =>  "http://fintrans.publicdata.eu/ec/ontology/",
                  "fts-r" =>  "http://fintrans.publicdata.eu/ec/resource/",
                  "fts-ci" => "http://fintrans.publicdata.eu/ec/resource/ci/",
                  "fts-co" => "http://fintrans.publicdata.eu/ec/resource/co/",
                  "fts-su" => "http://fintrans.publicdata.eu/ec/resource/su/",
                  "fts-sa" => "http://fintrans.publicdata.eu/ec/resource/sa/",
                  "fts-be" => "http://fintrans.publicdata.eu/ec/resource/be/",
                  "fts-cr" => "http://fintrans.publicdata.eu/ec/resource/cr/",
                  "fts-et" => "http://fintrans.publicdata.eu/ec/resource/et/",
                  "fts-dg" => "http://fintrans.publicdata.eu/ec/resource/dg/",
                  "fts-at" => "http://fintrans.publicdata.eu/ec/resource/at/",
                  "fts-bu" => "http://fintrans.publicdata.eu/ec/resource/bu/",
                  "fts-ad" => "http://fintrans.publicdata.eu/ec/resource/ad/",
                  "dbpedia" => "http://dbpedia.org/resource/",

                  "owl"  => "http://www.w3.org/2002/07/owl#",
                  "rdf"  => "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                  "rdfs" => "http://www.w3.org/2000/01/rdf-schema#",
                  "foaf" => "http://xmlns.com/foaf/0.1/",
                  "shv"  => "http://ns.aksw.org/spatialHierarchy/"
            );
            $this->beneficiaries = array();
            $this->cities = array();

            $this->filehandle = fopen('logs/transformation.log',"a");
            $this->resultFilehandle = fopen('govData.nt',"a");

	}

        public function __destruct() {

            fclose($this->filehandle);
            #fclose($this->resultFilehandle);
        }

        function log( $message )
        {
            fwrite($this->filehandle,$message);
        }

	function readCSV($fileName)
	{
            $csvReader = new File_CSV_IteratorReader($fileName, ",") ;
            $assocArray = $csvReader->toArray(FALSE);
            return ($assocArray);
	}

	function createStatement ($subject, $predicate, $object, $lang = null , $datatype = null)
	{
            $subject = "<".$subject.">";
            $predicate = "<".$predicate.">";

            $object = str_replace ('"', "'", $object);
            $object = str_replace ('—', "-", $object);
            //$object = unicode_encode ($object, 'ISO-8859-1');
            
            $etiquette = null;
            if ($lang != null) {
                if ($lang == "unknown") {
                    $etiquette = " ";
                } else {
                    $etiquette = "@".$lang;
                }
            } else if ($datatype != null) {
                $etiquette = "^^<".$datatype.">";
            } 
            $object = $this->escape( utf8_encode($object) );
            if ($etiquette != null) {
                $object = '"'.$object.'"'.$etiquette;
            } else {
                $object = "<".$object.">";
            }

            $statement = array (
                    "subject" => $subject,
                    "predicate" => $predicate,
                    "object" => $object,
            );
            $stId = md5($subject.$predicate.$object);
            #$this->statements[$stId] = $statement;
            $this->writeStatement($statement);
	}

	function writeStatement($statement)
	{
            $line = implode (" ", $statement);
            $content .= $line. ".\n";
            fwrite($this->resultFilehandle, $content);
            #var_dump ($this->statements);
	}


	function createStatements($elements, $namedGraphIdent, $lang = "en" , $limit = 0)
	{
            $elementCount = 0;
            $lineCount = 0;
            foreach ($elements as $id => $line)
            {
                #unset($this->beneficiaries);
                #unset($this->cities);
                $this->beneficiaries = array();
                $this->cities = array();
                $lineCount++;
                if($lineCount == 1) {
                    #var_dump($line);die;
                    continue;
                }
                if (count($line) < 5 ) {
                    $this->log("\ncontinue while line contain less than 5 elements: " . $lineCount);
                    continue;
                }
                #      0 => string 'Year' (length=4)
                #      1 => string 'Name of beneficiary' (length=19)
                #      2 => string 'Coordinator' (length=11)
                #      3 => string 'Address' (length=7)
                #      4 => string 'City' (length=4)
                #      5 => string 'Postal code' (length=11)
                #      6 => string 'Country / Territory' (length=19)
                #      7 => string 'Amount' (length=6)
                #      8 => string 'Geographical Zone' (length=17)
                #      9 => string 'Expense Type' (length=12)
                #      10 => string 'Co-financing rate' (length=17)
                #      11 => string 'Amount' (length=6)
                #      12 => string 'Commitment position key' (length=23)
                #      13 => string 'Subject of grant or contract' (length=28)
                #      14 => string 'Responsible Department' (length=22)
                #      15 => string 'Budget line name and number' (length=27)
                #      16 => string 'Action Type' (length=11)

                #---------------------------------------------------------------
                //CLEANUP of 2009er Dataset

                if (count($line) == 18 ) {
                    $line[10] = $line[10] ."," . $line[11];
                    $line[11] = $line[12];
                    $line[12] = $line[13];
                    $line[13] = $line[14];
                    $line[14] = $line[15];
                    $line[15] = $line[16];
                    $line[16] = $line[17];
                    array_pop($line);
                    $this->log("\nCleanUp Line of ".$line[0]." commitment: " . $line[12] . "of line: " . $lineCount);
                }


                #---------------------------------------------------------------




                #---------------------------------------------------------------
                #At first we create the Subvention Resource

                $subventionUri = null;
                echo "\n Line: " . $lineCount;
                if(!empty($line[12])){
                    $subventionId   = $line[12];
                    
                } else {
                    echo "\n Autogenerate Subvention Uri : " . $line[12];
                    var_dump($line);die;
                    $subventionId   = md5(microtime(true));
                    $this->log("\nAutogenerate Subvention ID: " . $this->namespaces["fts-su"]."".urlencode($subventionId));
                }
                $subventionUri  = $this->namespaces["fts-su"]."".urlencode($subventionId);
                if (!array_key_exists($subventionId, $this->subventions)) {
                    $this->subventions[$subventionId] = 0;
                } else {
                    $addition = $this->subventions[$subventionId] + 1;
                    $this->log("\nSubvention ID exists: " . $subventionUri . " ->" . $this->namespaces["fts-su"]."".urlencode($subventionId."+".$addition));
                    $this->subventions[$subventionId] = $addition ;
                    $subventionUri  = $this->namespaces["fts-su"]."".urlencode($subventionId."+".$addition);
                    

                }

                $this->createStatement ( $subventionUri,
                        $this->namespaces["rdf"]."type",
                        $this->namespaces["fts-o"]."Commitment");

                if(!empty($line[12])) {
                    $this->createStatement ( $subventionUri,
                                $this->namespaces["fts-o"]."commitmentPositionKey",
                                $line[12],
                                null,
                                "http://www.w3.org/2001/XMLSchema#string");
                    $this->createStatement ( $subventionUri,
                            $this->namespaces["rdfs"]."label",
                            "Subvention " . $line[12], 
                            null,
                            "http://www.w3.org/2001/XMLSchema#string");
                }
                #-------------------------------------------------------------
                #case "Year":
                if(!empty($line[0])){
                    $this->createStatement ( $subventionUri,
                                $this->namespaces["fts-o"]."year",
                                $this->namespaces["dbpedia"].$line[0]);
                }
                #-------------------------------------------------------------
                #case "Name of beneficiary" :
                if(!empty($line[1])) {
                    $beneficiaries = explode("+++",$line[1]);
                    
                    foreach ( $beneficiaries as $beneficiary ) {
                        $labels                 = explode("*",$beneficiary);
                        $beHash                 = urlencode($labels[0]); # md5($beneficiary);
                        $beneficiaryUri         = $this->namespaces["fts-be"].$beHash;
                        $this->beneficiaries[]  = $beneficiaryUri;

                        $this->createStatement ( $subventionUri,
                                $this->namespaces["fts-o"]."beneficiary",
                                $beneficiaryUri);

                        $this->createStatement ( $beneficiaryUri,
                                $this->namespaces["rdf"]."type",
                                $this->namespaces["fts-o"]."Beneficiary");

                        foreach ($labels as $label) {
                            if(trim($label) != "") {
                                $this->createStatement ($beneficiaryUri,
                                    $this->namespaces["rdfs"]."label",
                                    $label, 
                                    "unknown");
                            }
                        }
                    }
                }
                
                #-------------------------------------------------------------
                #case "Coordinator":
                if(!empty($line[2])) {
                    $coordinators = explode("+++",$line[2]);
                    $count=0;
                    foreach ($coordinators as $coordinator) {
                        $beneficiaryUri = $this->beneficiaries[$count];
                        if ($coordinator == "1") {
                            $this->createStatement ( $subventionUri,
                                    $this->namespaces["fts-o"]."coordinator",
                                    $beneficiaryUri);
                        }
                        $count++;
                    }
                }
                #-------------------------------------------------------------
		#'Address' => string 'AVENUE DE CORTENBERGH 100' (length=25)
		#case "Address":
                if(!empty($line[3])) {
                    $streets = explode("+++",$line[3]);
                    $count=0;
                    foreach ($streets as $street) {
                        if(trim($street) != "") {
                            $beneficiaryUri = $this->beneficiaries[$count];
                            $this->createStatement ( $beneficiaryUri,
                                    $this->namespaces["fts-o"]."street",
                                    $street,
                                    null,
                                    "http://www.w3.org/2001/XMLSchema#string");
                        }
                        $count++;
                    }
                }
                #-------------------------------------------------------------
		#'City' => string 'BRUXELLES' (length=9)
                #case "City":
                if(!empty($line[4])) {
                    $cities = explode("+++",$line[4]);
                    $count=0;
                    foreach ($cities as $city) {
                        if (trim($city) != "") {
                            $beneficiaryUri = $this->beneficiaries[$count];
                            #$cityUri = $this->namespaces["dbpedia"].urlencode(str_replace(" ", "_", (utf8_encode($city))));
                            $cityUri = $this->namespaces["fts-ci"].(urlencode(strtolower($city)))

                            $this->cities[$count] = $cityUri;

                            $this->createStatement ( $cityUri,
                                    $this->namespaces["rdf"]."type",
                                    $this->namespaces["shv"]. "City");

                            $this->createStatement ( $cityUri,
                                    $this->namespaces["rdfs"]."label",
                                    $city,
                                    '',
                                    "http://www.w3.org/2001/XMLSchema#string");

                            $this->createStatement ( $beneficiaryUri,
                                    $this->namespaces["fts-o"]."city",
                                    $cityUri);
                        }
                        $count++;
                    }
                }
                #-------------------------------------------------------------
                # 'Postal code' => string '1000' (length=4)
		#case "Postal code":
                if(!empty($line[5])) {
                    $pCodes = explode("+++",$line[5]);
                    $count=0;
                    foreach ($pCodes as $pCode) {
                        if (trim($pCode) != "") {
                            $beneficiaryUri = $this->beneficiaries[$count];
                            
                            $this->createStatement ( $beneficiaryUri,
                                    $this->namespaces["fts-o"]."postal",
                                    $pCode,
                                    null,
                                    "http://www.w3.org/2001/XMLSchema#string");
                        }
                        $count++;
                    }
                }
		#-------------------------------------------------------------
                # 'Country / Territory' => string 'Belgium' (length=7)
		# case "Country / Territory":
                if(!empty($line[6])) {
                    $countries = explode("+++",$line[6]);
                    $count=0;
                    foreach ($countries as $country) {
                        if (trim($country) != "") {
                            $beneficiaryUri = $this->beneficiaries[$count];
                            $countryUri = $this->namespaces["fts-co"].(strtolower(urlencode($country)));
                            #$countryUri = $this->namespaces["dbpedia"].urlencode(str_replace(" ", "_", (utf8_encode($country))));
                            $this->createStatement ( $countryUri,
                                    $this->namespaces["rdf"]."type",
                                    $this->namespaces["shv"]."Country");

                            $this->createStatement ( $countryUri,
                                    $this->namespaces["rdfs"]."label",
                                    $country,
                                    null,
                                    "http://www.w3.org/2001/XMLSchema#string");
                            
                            if (!empty($this->cities[$count])) {
                                $cityUri = $this->cities[$count];

                                $this->createStatement ( $countryUri, 
                                        $this->namespaces["shv"]."contains", 
                                        $cityUri);

                                $this->createStatement ( $cityUri, 
                                        $this->namespaces["shv"]."isLocatedIn", 
                                        $countryUri);
                            }

                            $this->createStatement ( $beneficiaryUri,
                                    $this->namespaces["fts-o"]."country",
                                    $countryUri);
                        }

                        $count++;
                    }
                }

                #-------------------------------------------------------------
                #	case "Amount Per Party":
                if(!empty($line[7])) {
                    if (trim($line[7]) != "") {
                        $amounts = "";
                        $amounts = explode("\n",$line[7]);
                        $count=0;
                        foreach ($amounts as $amount) {
                            $amount = str_replace("\n", "", $amount);
                            $amount = str_replace("\r", "", $amount);

                            $beneficiaryUri = $this->beneficiaries[$count];
                            $amountUri = $this->namespaces["fts-ad"]. md5($beneficiaryUri.$subventionUri);

                            $this->createStatement ( $amountUri ,
                                    $this->namespaces["rdf"]."type" ,
                                    $this->namespaces["fts-o"]."AmountOfDistribution");

                            $this->createStatement ( $amountUri ,
                                    $this->namespaces["fts-o"] . "distributedAmountofSubvention" ,
                                    $subventionUri);

                            $this->createStatement ( $amountUri ,
                                    $this->namespaces["fts-o"] . "distributedAmountForBeneficiary" ,
                                    $beneficiaryUri);

                            $this->createStatement ( $amountUri ,
                                    $this->namespaces["fts-o"] . "totalSum" ,
                                    $amount,
                                    null,
                                    "http://www.w3.org/2001/XMLSchema#string");

                            $this->createStatement ( $amountUri ,
                                    $this->namespaces["rdfs"] . "label" ,
                                    "distributed amount of Subvention " . $line[12] . " for defined beneficiary." ,
                                    null,
                                    "http://www.w3.org/2001/XMLSchema#string");
                            $count++;
                        }
                    }
                }

                #-------------------------------------------------------------
                #case "Geographical Zone":
                if(!empty($line[8])) {
                    if(trim($line[8]) != "") {
                        $zoneUri = $this->namespaces["fts-sa"].(strtolower(urlencode($line[8])));

                        $this->createStatement ( $zoneUri,
                               $this->namespaces["rdf"]."type",
                               $this->namespaces["fts-o"]."GeographicalZone");

                        $this->createStatement ( $zoneUri,
                               $this->namespaces["rdfs"]."label",
                               $line[8],
                               "unknown");
                       
                        $this->createStatement ( $subventionUri,
                               $this->namespaces["fts-o"]."geoZone",
                               $zoneUri );
                    }
                }
                #-------------------------------------------------------------
                #case "Expense Type":
                if(!empty($line[9])) {
                    if(trim($line[9]) != "") {
                        $exTypeUri = $this->namespaces["fts-et"].(strtolower(urlencode($line[9])));

                        $this->createStatement ( $exTypeUri,
                                $this->namespaces["rdf"]."type",
                                $this->namespaces["fts-o"]."ExpenseType");

    			$this->createStatement ( $exTypeUri,
                                $this->namespaces["rdfs"]."label",
                                $line[9],
                                'en');
                        $this->createStatement ( $subventionUri,
                                $this->namespaces["fts-o"] . "expenseType",
                                $exTypeUri);
                    }
                }
                #-------------------------------------------------------------
                #case "Co-financing rate":
                if(!empty($line[10])) {
                    if(trim($line[10]) != "") {
                        $objectType = "Datatype";
                        $objectValue = $line[10];
                        switch ($line[10]) {
                            case "Lump sum":
                                $objectValue = $this->namespaces["fts-cr"] . "flat";
                                $objectType = "Object";
                            break;
                            case "Mixed financing":
                                $objectValue = $this->namespaces["fts-cr"] . "mixed";
                                $objectType = "Object";
                            break;
                            case "N.A. %":
                                $objectValue = $this->namespaces["fts-cr"] . "na";
                                $objectType = "Object";                                
                            break;
                        }
                        if ($objectType == "Datatype") {
                            $this->createStatement ( $subventionUri,
                                    $this->namespaces["fts-o"]."cofinancingRate",
                                    $objectValue,
                                    null,
                                    "http://www.w3.org/2001/XMLSchema#string");
                        } else if ($objectType == "Object") {
                            $this->createStatement ( $subventionUri,
                                    $this->namespaces["fts-o"]."cofinancingRate",
                                    $objectValue);
                        }
                    }
                }
					
                #-------------------------------------------------------------
		#'Amount' => string '50.000.000,00' (length=13)
		#case "Amount":
                if(!empty($line[11])) {
                    if(trim($line[11]) != "") {
                        $this->createStatement ( $subventionUri,
                                $this->namespaces["fts-o"] . "totalSum" ,
                                $line[11],
                                null,
                                "http://www.w3.org/2001/XMLSchema#string");
                    }
                }

                #-------------------------------------------------------------
                #'Commitment position key' => string 'SI2.521048.1' (length=12)
                #case "Commitment position key":
                #if(!empty($line[12])) {...
                #->already done on Top
                #-------------------------------------------------------------
		#'Subject of grant or contract' => string '=FV=36=CONTRIBUTION DIR F2 SESAR JOINT UNDERTAKING' (length=50)
		#case "Subject of grant or contract":
                if(!empty($line[13])) {
                    if (trim($line[13]) != "") {
                        $this->createStatement ( $subventionUri,
                                $this->namespaces["fts-o"]."subject",
                                $line[13],
                                "en");
                    }
                }
                #-------------------------------------------------------------
                #'Responsible Department' => string 'Energy and Transport' (length=20)
                #case "Responsible Department":
                if(!empty($line[14])) {
                    if(trim($line[14]) != "") {
                        $departmentUri =  $this->namespaces["fts-dg"].(urlencode($line[14]));

		        $this->createStatement ( $departmentUri,
                                $this->namespaces["rdf"]."type",
                                $this->namespaces["fts-o"]."DirectoratesGeneral");

                        $this->createStatement ( $departmentUri,
                                $this->namespaces["rdfs"]."label",
                                $line[14],
                                'en');

                        $this->createStatement ( $subventionUri,
                                $this->namespaces["fts-o"]."responsibleDepartment",
                                $departmentUri );
                    }
                }
                #-------------------------------------------------------------
        	#'Budget line name and number' => string 'Research related to transport (including aeronautics) (06.06.02)' (length=64)
		#case "Budget line name and number":
                if(!empty($line[15])) {
                    if(trim($line[15]) != "") {
                        $haveBudgetNumber = true;
                        $budgetNumber = null ;
                        $budgetName = null;

                        $budget = trim($line[15]);
                        $budgetName = $budget;
                        if(strpos($budget, "(") !== false) {
                            $budgetNumber = substr($budget, strrpos($budget, "(" ) + 1 , strrpos($budget, ")" ) - strrpos($budget, "(" ) -1 );
                            $budgetName   = substr($budget, 0, strrpos($budget, "(" ) - 1 );
                        } else {
                            $budgetNumber = md5($budget);
                            $haveBudgetNumber = false;
                        }

                        $budgetUri = $this->namespaces["fts-bu"].$budgetNumber;

        	        $this->createStatement ( $budgetUri,
                                $this->namespaces["rdf"]."type",
                                $this->namespaces["fts-o"]."Budget");

                        $this->createStatement ( $budgetUri,
                                $this->namespaces["fts-o"]."budgetLineName",
                                $budgetName ,
                                "en");

                        if ($haveBudgetNumber) {
                            $this->createStatement ( $budgetUri,
                                    $this->namespaces["fts-o"]. "budgetNumber",
                                    $budgetNumber,
                                    null,
                                    "http://www.w3.org/2001/XMLSchema#string");
                        }

    			$this->createStatement ( $subventionUri,
                                $this->namespaces["fts-o"]. "budget",
                                $budgetUri);
                    }
                    #-------------------------------------------------------------
		    #'Programme' => string 'Research Framework (CE)' (length=23)
		    #case "Action Type":
                    if(!empty($line[16])) {
                        if(trim($line[16]) != "") {
                            $programmUri = $this->namespaces["fts-at"].(urlencode($line[16]));
                            $this->createStatement ( $programmUri,
                                    $this->namespaces["rdf"]."type",
                                    $this->namespaces["fts-o"]."ActionType");

                            $this->createStatement ( $programmUri,
                                    $this->namespaces["rdfs"]."label",
                                    $line[16], 'en');

                            $this->createStatement ($subventionUri,
                                    $this->namespaces["fts-o"]."actionType",
                                    $programmUri );
                        }
                    }
                }
                print_r($statements);
                die;
            }
    	}

    // Replaces all byte sequences that need escaping. Characters that can
    // remain unencoded in N-Triples are not touched by the regex. The
    // replaced sequences are:
    //
    // 0x00-0x1F   non-printable characters
    // 0x22        double quote (")
    // 0x5C        backslash (\)
    // 0x7F        non-printable character (Control)
    // 0x80-0xBF   unexpected continuation byte,
    // 0xC0-0xFF   first byte of multi-byte character,
    //             followed by one or more continuation byte (0x80-0xBF)
    //
    // The regex accepts multi-byte sequences that don't have the correct
    // number of continuation bytes (0x80-0xBF). This is handled by the
    // callback.
    private function escape( $str ) {
        return preg_replace_callback(
            "/[\\x00-\\x1F\\x22\\x5C\\x7F]|[\\x80-\\xBF]|[\\xC0-\\xFF][\\x80-\\xBF]*/",
            array('Transformer','escape_callback'),
            $str);
    }
     
    private static function escape_callback($matches) {
        $encoded_character = $matches[0];
        $byte = ord($encoded_character[0]);
        // Single-byte characters (0xxxxxxx, hex 00-7E)
        if ($byte == 0x09) return "\\t";
        if ($byte == 0x0A) return "\\n";
        if ($byte == 0x0D) return "\\r";
        if ($byte == 0x22) return "\\\"";
        if ($byte == 0x5C) return "\\\\";
        if ($byte < 0x20 || $byte == 0x7F) {
            // encode as \u00XX
            return "\\u00" . sprintf("%02X", $byte);
        }

        // Multi-byte characters
        if ($byte < 0xC0) {
            // Continuation bytes (0x80-0xBF) are not allowed to appear as first byte
            return Transformer::error_character;
        }
        if ($byte < 0xE0) { // 110xxxxx, hex C0-DF
            $bytes = 2;
            $codepoint = $byte & 0x1F;
        } else if ($byte < 0xF0) { 
            // 1110xxxx, hex E0-EF
            $bytes = 3;
            $codepoint = $byte & 0x0F;
        } else if ($byte < 0xF8) { 
            // 11110xxx, hex F0-F7
            $bytes = 4;
            $codepoint = $byte & 0x07;
        } else if ($byte < 0xFC) { 
            // 111110xx, hex F8-FB
            $bytes = 5;
            $codepoint = $byte & 0x03;
        } else if ($byte < 0xFE) { 
            // 1111110x, hex FC-FD
            $bytes = 6;
            $codepoint = $byte & 0x01;
        } else { 
            // 11111110 and 11111111, hex FE-FF, are not allowed
            return Transformer::error_character;
        }

        // Verify correct number of continuation bytes (0x80 to 0xBF)
        $length = strlen($encoded_character);
        if ($length < $bytes) { 
            // not enough continuation bytes
            return Transformer::error_character;
        }

        if ($length > $bytes) { 
            // Too many continuation bytes -- show each as one error
            $rest = str_repeat(Transformer::error_character, $length - $bytes);
        } else {
            $rest = '';
        }

        // Calculate Unicode codepoints from the bytes
        for ($i = 1; $i < $bytes; $i++) {
            // Loop over the additional bytes (0x80-0xBF, 10xxxxxx)
            // Add their lowest six bits to the end of the codepoint
            $byte = ord($encoded_character[$i]);
            $codepoint = ($codepoint << 6) | ($byte & 0x3F);
        }

        // Check for overlong encoding (character is encoded as more bytes than
        // necessary, this must be rejected by a safe UTF-8 decoder)
        if (($bytes == 2 && $codepoint <= 0x7F) ||
            ($bytes == 3 && $codepoint <= 0x7FF) ||
            ($bytes == 4 && $codepoint <= 0xFFFF) ||
            ($bytes == 5 && $codepoint <= 0x1FFFFF) ||
            ($bytes == 6 && $codepoint <= 0x3FFFFF)) {
            return Transformer::error_character . $rest;
        }

        // Check for UTF-16 surrogates, which must not be used in UTF-8
        if ($codepoint >= 0xD800 && $codepoint <= 0xDFFF) {
            return Transformer::error_character . $rest;
        }

        // Misc. illegal code positions
        if ($codepoint == 0xFFFE || $codepoint == 0xFFFF) {
            return Transformer::error_character . $rest;
        }

        if ($codepoint <= 0xFFFF) {
            // 0x0100-0xFFFF, encode as \uXXXX
            return "\\u" . sprintf("%04X", $codepoint) . $rest;
        }

        if ($codepoint <= 0x10FFFF) {
            // 0x10000-0x10FFFF, encode as \UXXXXXXXX
            return "\\U" . sprintf("%08X", $codepoint) . $rest;
        }
        // Unicode codepoint above 0x10FFFF, no characters have been assigned
        // to those codepoints
        return Transformer::error_character . $rest;
    }
}

ini_set("max_execution_time","1500");
ini_set("memory_limit","4000M");

$transformer = new Transformer();

echo "\n READING CSV 2007 ...";
$elements_2007_en = $transformer->readCSV("export_2007_en.csv");
echo "\n Creating Statements 2007 ...";
$transformer->createStatements($elements_2007_en, "2007", "en" );

echo "\n READING CSV 2008 ...";
$elements_2008_en = $transformer->readCSV("export_2008_en.csv");
echo "\n Creating Statements 2008 ...";
$transformer->createStatements($elements_2008_en, "2008", "en" );

echo "\n READING CSV 2009 ...";
$elements_2009_en = $transformer->readCSV("export_2009_en.csv");
echo "\n Creating Statements 2009 ...";
$transformer->createStatements($elements_2009_en, "2009", "en" );

echo "\n READING CSV 2010 ...";
$elements_2010_en = $transformer->readCSV("export_2010_en.csv");
echo "\n Creating Statements 2009 ...";
$transformer->createStatements($elements_2010_en, "2010", "en" );


echo "\n Writing Statements To File ...";
$transformer->writeStatements("govData.ntriples.complete");
echo "\n FINISH ...";

#DB.DBA.SPARQL_EVAL('Create Silent GRAPH <http://publicdata.eu/eu-transparency/>','http://publicdata.eu/eu-transparency/',0);


?>
