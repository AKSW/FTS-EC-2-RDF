package org.aksw.fts.vocab;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class Vocab {
	public static final String ontologyNs = "http://fts.publicdata.eu/ontology/";
	public static final String resourceNs = "http://fts.publicdata.eu/resource/";
	
	public static final Resource Year = ResourceFactory.createResource("http://dbpedia.org/ontology/Year");
	
	public static final Resource ActionType = createClass("ActionType");
	public static final Resource AmountOfDistribution = createClass("AmountOfDistribution");
	public static final Resource Beneficiary = createClass("Beneficiary");
	//public static final Resource CofinancingRate = createClass("CofinancingRate");
	public static final Resource CofinancingRateType = createClass("CofinancingRateType");
	public static final Resource Commitment = createClass("Commitment");
	public static final Resource Department = createClass("Department");
	public static final Resource BudgetLine = createClass("BudgetLine");
	public static final Resource ExpenseType = createClass("ExpenseType");
	public static final Resource Programme = createClass("Programme");

	public static final Resource COFINANCING_RATE_FIXED = ResourceFactory.createResource(resourceNs + "cfr/fixed");

	
	public static final Property actionType = createProperty("actionType");
	public static final Property address = createProperty("address");
	public static final Property amount = createProperty("amount");
	public static final Property beneficiary = createProperty("beneficiary");
	public static final Property budgetLine = createProperty("budgetLine");
	public static final Property city = createProperty("city");
	public static final Property cofinancingRate = createProperty("cofinancingRate");
	public static final Property cofinancingRateType = createProperty("cofinancingRateType");
	public static final Property commitment = createProperty("commitment");
	public static final Property coordinator = createProperty("coordinator");
	public static final Property country = createProperty("country");
	public static final Property detailAmount = createProperty("detailAmount");
	public static final Property expenseType = createProperty("expenseType");
	public static final Property geoZone = createProperty("geoZone");
	//public static final Property grantSubject = createProperty("grantSubject"); //-> rdfs:comment ?
	public static final Property positionKey = createProperty("positionKey");
	public static final Property postCode = createProperty("postCode");
	public static final Property programme = createProperty("programme");
	public static final Property responsibleDepartment = createProperty("responsibleDepartment");
	public static final Property year = createProperty("year");
	
	public static final Property skosPrefLabel = createProperty("http://www.w3.org/2004/02/skos/core#prefLabel");

	
	
	public static Property createClass(String name) {
		return ResourceFactory.createProperty(ontologyNs + name);
	}

	public static Property createProperty(String name) {
		return ResourceFactory.createProperty(ontologyNs + name);
	}
	
	
}