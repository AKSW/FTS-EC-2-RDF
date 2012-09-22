package org.aksw.fts;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.aksw.commons.collections.SinglePrefetchIterator;
import org.aksw.commons.jena.ModelUtils;
import org.aksw.commons.sparql.api.compare.QuerySolutionWithEquals;
import org.aksw.commons.sparql.api.core.QueryExecutionFactory;
import org.aksw.commons.sparql.api.core.QueryExecutionFactoryBackQuery;
import org.aksw.commons.sparql.api.http.QueryExecutionFactoryHttp;
import org.aksw.commons.sparql.api.model.QueryExecutionFactoryModel;
import org.aksw.commons.sparql.api.pagination.core.QueryExecutionFactoryIterated;
import org.aksw.commons.sparql.api.pagination.core.QueryExecutionFactoryPaginated;
import org.aksw.commons.sparql.api.pagination.core.QueryTransformer;
import org.aksw.sparqlify.expr.util.ExprUtils;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.expr.E_Equals;
import com.hp.hpl.jena.sparql.expr.E_OneOf;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprList;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementFilter;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.PatternVars;
import com.hp.hpl.jena.vocabulary.OWL;

class ResultTable {
	private List<Var> schema = null;
	private Set<List<Node>> data = null;

	public ResultTable() {
		this(new ArrayList<Var>(), new HashSet<List<Node>>());
	}

	public ResultTable(List<Var> schema, Set<List<Node>> data) {
		this.schema = schema;
		this.data = data;
	}

	
	
	
	public List<Var> getSchema() {
		return schema;
	}

	public void setSchema(List<Var> schema) {
		this.schema = schema;
	}

	public Set<List<Node>> getData() {
		return data;
	}

	public void setData(Set<List<Node>> data) {
		this.data = data;
	}	
	
	
	
	public ResultTable createProjected(Collection<Var> retainVars) {
		List<Integer> retainIndexes = new ArrayList<Integer>();

		ResultTable result = new ResultTable();
		
		for(int i = 0; i < schema.size(); ++i) {
			Var var = schema.get(i);
			if(retainVars.contains(var)) {
				retainIndexes.add(i);
			}
		}

		Collections.sort(retainIndexes);
		
		for(Integer i : retainIndexes) {
			result.schema.add(schema.get(i));
		}
		
		if(result.schema.isEmpty()) {
			return result;
		}
		
		
		for(List<Node> row : data) {
			List<Node> entry = new ArrayList<Node>();
			for(Integer i : retainIndexes) {
				entry.add(row.get(i));
			}	
			result.getData().add(entry);
		}
		
		return result;
	}
	
	public static ResultTable create(ResultSet rs) {
		Set<Var> retainVars = new HashSet<Var>();
		for(String varName : rs.getResultVars()) {
			retainVars.add(Var.alloc(varName));
		}
		
		ResultTable result = create(rs, retainVars);
		
		return result;
	}
	
	public static ResultTable create(ResultSet rs, Collection<Var> retainVars) {
		List<Var> schema = new ArrayList<Var>();

		for (String varName : rs.getResultVars()) {
			Var var = Var.alloc(varName);

			if (retainVars.contains(var)) {
				schema.add(var);
			}
		}

		Set<List<Node>> mappings = new HashSet<List<Node>>();

		while (rs.hasNext()) {
			Binding binding = rs.nextBinding();

			List<Node> item = new ArrayList<Node>();

			for (Var var : schema) {
				item.add(binding.get(var));
			}
			
			mappings.add(item);
		}

		return new ResultTable(schema, mappings);
	}
}

/*
 * class BindingBatchIterator extends SinglePrefetchIterator<Binding> { private
 * int batchSize = 100; private List<Binding> bindings = new
 * ArrayList<Binding>();
 * 
 * private int offset = 0;
 * 
 * public BindingBatchIterator(List<Binding> bindings, Set<Var> vars) {
 * for(Binding binding : bindings) {
 * 
 * 
 * } }
 * 
 * @Override protected List<Binding> prefetch() throws Exception { List<Binding>
 * result = new ArrayList<Binding>();
 * 
 * 
 * 
 * 
 * if(result.isEmpty()) { return null; } }
 * 
 * }
 */

class QueryTransformerBind
	implements QueryTransformer
{
	private ResultTable baseTable;
	private int batchSize = 100;
	
	public QueryTransformerBind(ResultSet rs, int batchSize) {
		this(ResultTable.create(rs), batchSize);
	}
	
	public QueryTransformerBind(ResultTable baseTable, int batchSize) {
		this.baseTable = baseTable;
		this.batchSize = batchSize;
	}
	
	public Iterator<Query> transform(Query query) {
		Set<Var> retainVars = PatternVars.vars(query.getQueryPattern());
		
		ResultTable table = baseTable.createProjected(retainVars);
		
		Iterator<Query> result = new QueryIteratorBind(query, table, batchSize);
		return result;
	}
}


class QueryIteratorBind
	extends SinglePrefetchIterator<Query>
{
	private Query baseQuery;
	private ResultTable table;
	private int batchSize = 100;

	private Iterator<List<Node>> current;
	
	
	public QueryIteratorBind(Query baseQuery, ResultTable table, int batchSize) {
		this.baseQuery = baseQuery;
		this.table = table;
		this.batchSize = batchSize;
		
		this.current = table.getData().iterator();
	}

	@Override
	protected Query prefetch() throws Exception {
		List<List<Node>> batch = new ArrayList<List<Node>>();
		
		while(current.hasNext()) {
			
			List<Node> item = current.next();
			
			batch.add(item);
					
			if(batch.size() == batchSize) {
				Query result = QueryBindUtils.expandQuery(baseQuery, table.getSchema(), batch, true);
				return result;
			}
		}

		if(!batch.isEmpty()) {
			Query result = QueryBindUtils.expandQuery(baseQuery, table.getSchema(), batch, true);
			return result;
		}
		

		return finish();
	}
	
}




class QueryBindUtils extends QueryExecutionFactoryBackQuery {
	private Set<Var> rsVars = new HashSet<Var>();
	private Set<QuerySolutionWithEquals> querySolutions;

	@Override
	public String getId() {
		return "id not set";
	}

	@Override
	public String getState() {
		return "state not set";
	}

	public static E_OneOf toIn(List<Var> schema, Collection<List<Node>> mappings) {
		// Filter out all Uris
		// List<RDFNode> uris
		// List<RDFNode> nonUris
		Var var = schema.get(0);

		ExprList exprList = new ExprList();
		for (List<Node> node : mappings) {
			exprList.add(NodeValue.makeNode(node.get(0)));
		}

		E_OneOf expr = new E_OneOf(new ExprVar(var), exprList);

		return expr;
	}

	public static Expr toDisjunction(List<Var> schema, Collection<List<Node>> mappings) {
		Expr result = null;

		List<ExprVar> exprVars = new ArrayList<ExprVar>();
		for (Var var : schema) {
			exprVars.add(new ExprVar(var));
		}

		ExprList ors = new ExprList();

		for (List<Node> mapping : mappings) {
			ExprList ands = new ExprList();

			for (int i = 0; i < schema.size(); ++i) {
				ExprVar exprVar = exprVars.get(i);
				Node node = mapping.get(i);

				Expr exprNode = NodeValue.makeNode(node);

				Expr item = new E_Equals(exprVar, exprNode);
				ands.add(item);
			}

			Expr and = ExprUtils.andifyBalanced(ands);

			ors.add(and);
		}

		result = ExprUtils.orifyBalanced(ors);

		return result;
	}

	public static Query expandQuery(Query query, List<Var> schema,
			Collection<List<Node>> mappings, boolean useInPredicate) {
		Expr expr = null;

		if (schema.size() == 1 && useInPredicate) {
			expr = toIn(schema, mappings);
		} else {
			expr = toDisjunction(schema, mappings);
		}

		ElementFilter filter = new ElementFilter(expr);

		Query result = expandQuery(query, filter);

		return result;
	}

	public static Query expandQuery(Query query, Element newElement) {
		Query result = (Query) query.clone();
		expandQueryInPlace(result, newElement);

		return result;
	}

	public static Query expandQueryInPlace(Query query, Element newElement) {

		Element element = query.getQueryPattern();

		ElementGroup group = null;
		if (element instanceof ElementGroup) {
			group = (ElementGroup) element;
		} else {
			group = new ElementGroup();
			group.addElement(element);
			query.setQueryPattern(group);
		}

		group.addElement(newElement);

		return query;
	}

	@Override
	public QueryExecution createQueryExecution(Query query) {

		// TODO Auto-generated method stub
		return null;
	}

}

public class DownloadDBpediaData {

	public static QueryExecutionFactory createQefBind(String service, ResultSet rs, String baseQuery) {
		QueryExecutionFactory qef = createQef(service);
		
		QueryTransformerBind transformer = new QueryTransformerBind(rs, 100);		
		
		qef = new QueryExecutionFactoryIterated(qef, transformer, false);
		
		return qef;
	}
	
	
	public static Model execConstructService(String service, String queryString, ResultSet rs) {
		QueryExecutionFactory qef = createQefBind(service, rs, queryString);
		Model result = qef.createQueryExecution(queryString).execConstruct();
		
		return result;
	}


	public static Model execConstructService(String service, String queryString) {
		QueryExecutionFactory qef = createQef(service);
		Model result = qef.createQueryExecution(queryString).execConstruct();

		System.out.println("Got from service: " + result.listStatements().toSet().size());
		
		return result;
	}
	
	public static Model execConstruct(Model model, String queryString) {
		QueryExecutionFactory qef = new QueryExecutionFactoryModel(model);
		Model result = qef.createQueryExecution(queryString).execConstruct();
		return result;
	}
	
	
	
	public static ResultSet execSelect(Model model, String queryString) {
		QueryExecutionFactory qef = new QueryExecutionFactoryModel(model);
		ResultSet rs = qef.createQueryExecution(queryString).execSelect();
		return rs;
	}

	
	public static QueryExecutionFactory createQef(String service) {
		QueryExecutionFactory qef = new QueryExecutionFactoryHttp(service);
		qef = new QueryExecutionFactoryPaginated(qef, 500);
		
		return qef;
	}


	public static void main(String[] args) throws IOException {
		//mainCountries(args);
		mainCities(args);
	}

	public static void mainCountries(String[] args) throws IOException {
		
		String prefixes = "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n";
		
		Model ftsModel = execConstructService("http://fts.publicdata.eu/sparql", prefixes + "Construct { ?s a <http://ns.aksw.org/spatialHierarchy/Country> } { ?s a <http://ns.aksw.org/spatialHierarchy/Country> } Order By ?s");

		System.out.println("GOT " + ftsModel.size() + " resources from FTS");
		
		Model sameAsModel = ModelFactory.createDefaultModel();
		
		for(Statement stmt : ftsModel.listStatements().toSet()) {
			Resource fts = stmt.getSubject();
			
			String str = fts.getURI();
			str = str.replace("http://fts.publicdata.eu/resource/co/", "http://dbpedia.org/resource/");
			str = str.replace("+", "_");
			
			Resource dbp = sameAsModel.createResource(str);
			
			sameAsModel.add(fts, OWL.sameAs, dbp);			
		}
		
		ResultSet lgdRs = execSelect(sameAsModel, "Select Distinct ?x { ?o ?p ?x }");		

		Model dbpModel = execConstructService("http://dbpedia.org/sparql", prefixes + "Construct { ?x ?y ?z } { ?x ?y ?z } Order By ?x", lgdRs);
		System.out.println("GOT " + dbpModel.size() + " resources from DBpedia");

		
		ModelUtils.write(sameAsModel, new File("fts-dbp-country-sameas.nt"), "N-TRIPLES");
		ModelUtils.write(dbpModel, new File("fts-dbp-country-data.nt"), "N-TRIPLES");
	}

	
	public static void mainCities(String[] args) throws IOException {

		String prefixes = "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n";

		Model ftsModel = execConstructService("http://fts.publicdata.eu/sparql", prefixes + "Construct { ?s owl:sameAs ?o } { ?s a <http://ns.aksw.org/spatialHierarchy/City> . ?s owl:sameAs ?o . Filter(regex(?o, 'linkedgeodata')) } Order By ?s");
		ResultSet ftsRs = execSelect(ftsModel, "Select Distinct ?o { ?s ?p ?o }");		

		
		Model lgdModel = execConstructService("http://linkedgeodata.org/sparql", prefixes + "Construct { ?o owl:sameAs ?x } {?o owl:sameAs ?x } Order By ?o", ftsRs);
		ResultSet lgdRs = execSelect(lgdModel, "Select Distinct ?x { ?o ?p ?x }");		
		

		Model dbpModel = execConstructService("http://dbpedia.org/sparql", prefixes + "Construct { ?x ?y ?z } { ?x ?y ?z } Order By ?x", lgdRs);

		
		Model ftsToDbp = ModelFactory.createDefaultModel();
		ftsToDbp.add(ftsModel);
		ftsToDbp.add(lgdModel);
		
		Model directLink = execConstruct(ftsToDbp, prefixes + "Construct { ?s owl:sameAs ?x } { ?s owl:sameAs ?o . ?o owl:sameAs ?x }");
		
		ModelUtils.write(directLink, new File("fts-dbp-city-sameas.nt"), "N-TRIPLES");
		ModelUtils.write(dbpModel, new File("fts-dbp-city-data.nt"), "N-TRIPLES");
	}
	
}
