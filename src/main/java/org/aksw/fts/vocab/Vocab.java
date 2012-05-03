package org.aksw.fts.vocab;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class Vocab {
	public static final String ontologyNs = "http://fts.publicdata.eu/ontology/";
	
	public static final Resource Year = createClass("Year");
	public static final Resource Commitment = createClass("Commitment");
	public static final Resource Beneficiary = createClass("Beneficiary");
	public static final Resource AmountOfDistribution = createClass("AmountOfDistribution");


	public static final Property amount = createProperty("amount");
	public static final Property commitment = createProperty("commitment");
	public static final Property cofinancingRate = createProperty("cofinancingRate");
	public static final Property actionType = createProperty("actionType");
	public static final Property expenseType = createProperty("expenseType");
	public static final Property positionKey = createProperty("positionKey");
	public static final Property budgetLine = createProperty("budgetLine");
	public static final Property year = createProperty("year");
	public static final Property geoZone = createProperty("geoZone");
	public static final Property country = createProperty("country");
	public static final Property city = createProperty("city");
	public static final Property address = createProperty("address");
	public static final Property detailAmount = createProperty("detailAmount");
	public static final Property coordinator = createProperty("coordinator");
	public static final Property responsibleDepartment = createProperty("responsibleDepartment");
	public static final Property programme = createProperty("programme");
	public static final Property beneficiary = createProperty("beneficiary");
	public static final Property postCode = createProperty("postCode");
	
	public static final Property skosPrefLabel = createProperty("http://www.w3.org/2004/02/skos/core#prefLabel");

	//public static final Property grantSubject = createProperty("grantSubject"); -> rdfs:comment
	
	
	public static Property createClass(String name) {
		return ResourceFactory.createProperty(ontologyNs + name);
	}

	public static Property createProperty(String name) {
		return ResourceFactory.createProperty(ontologyNs + name);
	}
	
	
}