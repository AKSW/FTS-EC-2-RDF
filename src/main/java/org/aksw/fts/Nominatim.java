package org.aksw.fts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.aksw.commons.sparql.api.cache.extra.CacheCoreEx;
import org.aksw.commons.sparql.api.cache.extra.CacheCoreH2;
import org.aksw.commons.sparql.api.cache.extra.CacheEx;
import org.aksw.commons.sparql.api.cache.extra.CacheExImpl;
import org.aksw.commons.sparql.api.core.QueryExecutionFactory;
import org.aksw.commons.sparql.api.http.QueryExecutionFactoryHttp;
import org.aksw.commons.sparql.api.pagination.core.QueryExecutionFactoryPaginated;
import org.aksw.commons.util.StreamUtils;
import org.aksw.commons.util.strings.StringUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class Nominatim {

	public static void main(String[] args) throws Exception {
		System.out.println(geocode("AVENUE ANDRE ROUSSIN 7, MARSEILLE, France"));
		//System.out.println(geocode("Leipzig"));
		// System.out.println(candidates);
		//process();
	}

	public static void process() throws ClassNotFoundException, SQLException {
		QueryExecutionFactory qef = new QueryExecutionFactoryHttp(
				"http://localhost/fts/sparql",
				Collections.singleton("http://fts.publicdata.eu"));

		CacheCoreEx cacheBackend = CacheCoreH2.create("fts-cache");
		CacheEx cacheFrontend = new CacheExImpl(cacheBackend);

		//qef = new QueryExecutionFactoryCacheEx(qef, cacheFrontend);
		qef = new QueryExecutionFactoryPaginated(qef);

		String queryString = "Prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#> "
				+ "Prefix fts-o:<http://fts.publicdata.eu/ontology/> "
				+ "Select ?country ?city ?street ?postCode {"
				+ "?s a fts-o:Beneficiary . "
				+ "Optional { ?s fts-o:country [rdfs:label ?country] }"
				+ "Optional { ?s fts-o:city [rdfs:label ?city] }"
				+ "Optional { ?s fts-o:street ?street }"
				+ "Optional { ?s fts-o:postCode ?postCode }" + "}";

		System.out.println(queryString);

		
		Set<String> addressStrings = new HashSet<String>();
		
		QueryExecution qe = qef.createQueryExecution(queryString);
		ResultSet rs = qe.execSelect();
		while (rs.hasNext()) {
			QuerySolution qs = rs.next();
			/*
			String str = "" + qs.get("country")
					//+ " " + StringUtils.coalesce(qs.get("postCode"), "")
					+ " " + StringUtils.coalesce(qs.get("city"), "")
					//+ " " + StringUtils.coalesce(qs.get("street"), "")
					;
					*/
			String street = "" + StringUtils.coalesce(qs.get("street"), "");
			String city = "" + StringUtils.coalesce(qs.get("city"), "");
			String country = "" + StringUtils.coalesce(qs.get("country"), "");

			
			String str = "";
			if(!street.isEmpty()) {
				str += street + ", ";
			}
			
			if(!city.isEmpty()) {
				str += city + ", ";
			}
			
			str += country;
			
			str = str.replaceAll("\\s+", " ");
			
			
			if(addressStrings.add(str)) {
				System.out.println(str);
			}
		}

		System.out.println(addressStrings.size());
	}

	class JsonResponseItem {
		String osm_type;
		long osm_id;

		public JsonResponseItem() {
		}

		public String getOsm_type() {
			return osm_type;
		}

		public void setOsm_type(String osm_type) {
			this.osm_type = osm_type;
		}

		public long getOsm_id() {
			return osm_id;
		}

		public void setOsm_id(long osm_id) {
			this.osm_id = osm_id;
		}
	}

	public static Set<Resource> geocode(String queryString) throws IOException {

		String service = "http://open.mapquestapi.com/nominatim/v1/search";
		// http://nominatim.openstreetmap.org/search


		String uri = service + "?format=json&q=" + StringUtils.urlEncode(queryString);

		URL url = new URL(uri);
		URLConnection c = url.openConnection();
		c.setRequestProperty("User-Agent",
				"http://linkedgeodata.org, mailto:cstadler@informatik.uni-leipzig.de");

		InputStream ins = c.getInputStream();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		StreamUtils.copy(ins, out, 1024);

		String json = out.toString();

		Gson gson = new Gson();

		Type collectionType = new TypeToken<Collection<JsonResponseItem>>() {
		}.getType();
		Collection<JsonResponseItem> items = gson
				.fromJson(json, collectionType);

		// gson.fromJson(json, JsonResponseItem.class);

		Set<Resource> resources = new HashSet<Resource>();
		for (JsonResponseItem item : items) {
			Resource resource = null;

			String baseUri = "http://linkedgeodata.org/triplify/";

			resource = ResourceFactory.createResource(baseUri
					+ item.getOsm_type() + item.getOsm_id());
			resources.add(resource);
		}

		return resources;
	}

	// Model result = createModel();
	//
	//
	//
	// QueryExecutionFactoryHttp qef = new
	// QueryExecutionFactoryHttp("http://live.linkedgeodata.org/sparql",
	// Collections.singleton("http://linkedgeodata.org"));
	//
	// for(Resource resource : resources) {
	// String serviceUri =
	// "http://test.linkedgeodata.org/sparql?format=text%2Fplain&default-graph-uri=http%3A%2F%2Flinkedgeodata.org&query=DESCRIBE+<"
	// + StringUtils.urlEncode(resource.toString()) + ">";
	// URL serviceUrl = new URL(serviceUri);
	// URLConnection conn = serviceUrl.openConnection();
	// conn.addRequestProperty("Accept", "text/plain");
	//
	// /*
	// ByteArrayOutputStream out1 = new ByteArrayOutputStream();
	// StreamUtils.copy(serviceUrl.openStream(), out1);
	// String nt = out1.toString();
	// */
	//
	// InputStream in = null;
	// try {
	// in = conn.getInputStream();
	// result.read(in, null, "N-TRIPLE");
	// } finally {
	// if(in != null) {
	// in.close();
	// }
	// }
	//
	//
	// /*
	// String tmp = "DESCRIBE <" + resource + ">";
	// System.out.println(tmp);
	// QueryExecution qe = qef.createQueryExecution(tmp);
	// qe.execDescribe(result);
	// */
	//
	// }

}
