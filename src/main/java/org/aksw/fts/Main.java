package org.aksw.fts;

import java.io.File;
import java.math.BigDecimal;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.aksw.commons.util.strings.StringUtils;
import org.aksw.fts.domain.xml.Beneficiary;
import org.aksw.fts.domain.xml.Commitment;
import org.aksw.fts.domain.xml.Fts;
import org.aksw.fts.vocab.SpatialHierarchy;
import org.aksw.fts.vocab.Vocab;
import org.openjena.atlas.lib.Sink;
import org.openjena.riot.lang.SinkTriplesToGraph;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.graph.GraphFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class Main {
	public static void main(String[] args) throws Exception {
		processDir(new File("data/en"));
	}

	public static void processDir(File dir) throws Exception {
		for (File file : dir.listFiles()) {
			if (!(file.isFile() && file.getName().endsWith(".xml"))) {
				continue;
			}

			// InputStream in = new FileInputStream(file);
			System.out.println("Processing " + file.getAbsolutePath());
			process(file);
		}
	}

	public static BigDecimal processAmount(String value) {
		if (value == null || value.isEmpty()) {
			return null;
		}

		try {
			return new BigDecimal(value.replace(".", "").replace(',', '.'));
		} catch (Exception e) {
			System.err.println("Error parsing: " + value);
			return null;
		}
	}

	public static void emit(Sink<Triple> sink, Node s, Node p, Node o) {
		if (o == null) {
			return;
		}

		sink.send(new Triple(s, p, o));
	}

	public static Node emit(Sink<Triple> sink, Node subject, Node property,
			String prefix, String label) {
		Node object = emitLabelledNode(sink, prefix, label);
		if (object == null) {
			return null;
		}

		sink.send(new Triple(subject, property, object));

		return object;
	}

	public static Node createTypedLiteralNode(Object value) {
		return value == null ? null : ResourceFactory.createTypedLiteral(value)
				.asNode();
	}

	/*
	 * public static void emit(Sink<Triple> sink, Node subject, Node property,
	 * String prefix, XMLGregorianCalendar calendar) { Node object =
	 * emitLabelledNode(sink, prefix, label); sink.send(new Triple(subject,
	 * property, object)); }
	 */

	public static Node emitLabelledNode(Sink<Triple> sink, String prefix,
			String label) {
		if (label == null) {
			// System.out.println("null");
			return null;
		}

		Node result = Node.createURI(prefix + StringUtils.urlEncode(label));
		sink.send(new Triple(result, RDFS.label.asNode(), Node
				.createLiteral(label)));

		return result;
	}

	public static void process(File file) throws Exception {
		Graph graph = GraphFactory.createDefaultGraph();
		Sink<Triple> sink = new SinkTriplesToGraph(graph);

		process(file, sink);
		
		Model model = ModelFactory.createModelForGraph(graph);
		model.write(System.out, "TURTLE");
	}
	
	public static void process(File file, Sink<Triple> sink) throws Exception {
	
		JAXBContext context = JAXBContext.newInstance(Fts.class.getPackage()
				.getName());

		Unmarshaller unmarshaller = context.createUnmarshaller();
		Object result = unmarshaller.unmarshal(file);

		Fts fts = (Fts) result;

		for (Commitment commitment : fts.getCommitment()) {

			Node commitmentNode = Node.createURI("http://fts.publicdata.eu/ct/"
					+ commitment.getPositionKey());

			// Node yearNode = Node.createURI("http://dbpedia.org/resource/"
			// + commitment.getYear());

			Node amountNode = createTypedLiteralNode(processAmount(commitment
					.getAmount()));
			emit(sink, commitmentNode, Vocab.amount.asNode(), amountNode);

			emit(sink, commitmentNode, Vocab.budgetLine.asNode(),
					"http://fts.publicdata.eu/bl/", commitment.getBudgetLine());
			emit(sink, commitmentNode, Vocab.cofinancingRate.asNode(),
					"http://fts.publicdata.eu/cfr",
					commitment.getCofinancingRate());
			emit(sink, commitmentNode, RDFS.comment.asNode(),
					"http://fts.publicdata.eu/gs/",
					commitment.getGrantSubject());
			emit(sink, commitmentNode, Vocab.programme.asNode(),
					"http://fts.publicdata.eu/pg/", commitment.getProgramme());
			emit(sink, commitmentNode, Vocab.responsibleDepartment.asNode(),
					"http://fts.publicdata.eu/rd/",
					commitment.getResponsibleDepartment());
			emit(sink, commitmentNode, Vocab.year.asNode(),
					"http://dbpedia.org/resource/", commitment.getYear()
							.toString());

			// http://fts.opendata.org/resource/cm/{position-key}/SI2.566788.1
			for (Beneficiary beneficiary : commitment.getBeneficiaries()
					.getBeneficiary()) {

				/*
				 * Obtain all labels
				 */
				String[] names = beneficiary.getName().split("\\s*\\*\\s*");
				String primaryName = names[0];

				/*
				 * Beneficiary URI generation
				 */
				String addressStr = beneficiary.getCountry() + " "
						+ beneficiary.getCity() + " "
						+ beneficiary.getPostCode() + " "
						+ beneficiary.getAddress();
				String hashPart = StringUtils.md5Hash(addressStr);
				Node beneficiaryNode = Node
						.createURI("http://fts.publicdata.eu/be/"
								+ StringUtils.urlEncode(primaryName + "-"
										+ hashPart));

				/*
				 * Link the beneficiary with the commitment
				 */
				emit(sink, commitmentNode, Vocab.beneficiary.asNode(),
						beneficiaryNode);

				/*
				 * All of a beneficiary's names become rdfs:labels; the primary
				 * name additionally becomes skos:prefLabel
				 */
				for (String name : names) {
					emit(sink, beneficiaryNode, RDFS.label.asNode(),
							ResourceFactory.createPlainLiteral(name).asNode());
				}
				emit(sink, beneficiaryNode, Vocab.skosPrefLabel.asNode(),
						ResourceFactory.createPlainLiteral(names[0]).asNode());

				/*
				 * Coordinator role
				 */
				if (("" + beneficiary.getCoordinator()).trim().equals("1")) {
					emit(sink, commitmentNode, Vocab.coordinator.asNode(),
							beneficiaryNode);
				}

				/*
				 * Address
				 */
				emit(sink, beneficiaryNode, Vocab.address.asNode(),
						Node.createLiteral(beneficiary.getAddress()));
				emit(sink, beneficiaryNode, Vocab.postCode.asNode(),
						Node.createLiteral(beneficiary.getPostCode()));

				/*
				 * Spatial hierarchy (city, country)
				 */
				Node cityNode = emit(sink, beneficiaryNode,
						Vocab.city.asNode(), "http://fts.publicdata.eu/city/",
						beneficiary.getCity());
				Node countryNode = emit(sink, beneficiaryNode,
						Vocab.country.asNode(),
						"http://ftc.publicdata.eu/country/",
						beneficiary.getCountry());

				emit(sink, cityNode, RDF.type.asNode(),
						SpatialHierarchy.City.asNode());
				emit(sink, countryNode, RDF.type.asNode(),
						SpatialHierarchy.Country.asNode());
				emit(sink, cityNode, SpatialHierarchy.isLocatedIn.asNode(),
						countryNode);

				/*
				 * Detail Amount
				 */
				Node da = Node.createURI("http://fts.publicdata.eu/city/da/"
						+ commitment.getPositionKey()
						+ "-"
						+ StringUtils.md5Hash(commitmentNode.toString()
								+ beneficiaryNode.toString()));
				Node daValueNode = createTypedLiteralNode(processAmount(beneficiary
						.getDetailAmount()));

				emit(sink, da, RDF.type.asNode(),
						Vocab.AmountOfDistribution.asNode());
				emit(sink, da, Vocab.commitment.asNode(), commitmentNode);
				emit(sink, da, Vocab.beneficiary.asNode(), beneficiaryNode);
				emit(sink, da, Vocab.amount.asNode(), daValueNode);
			}
		}

		/*
		 * for(Triple triple : graph.find(null, null, null).toList()) {
		 * System.out.println(triple); }
		 */
	}

}
