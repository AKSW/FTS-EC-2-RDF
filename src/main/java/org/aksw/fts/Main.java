package org.aksw.fts;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.graph.GraphFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class Main {
	private static Logger logger = LoggerFactory.getLogger(Main.class);

	private static Map<String, Resource> cofinancingRateTagToResource = new HashMap<String, Resource>();
	private static Map<String, Resource> expenseTypeTagToResource = new HashMap<String, Resource>();

	static {
		cofinancingRateTagToResource.put("Lump sum",
				Vocab.COFINANCING_RATE_LUMP_SUM);
		cofinancingRateTagToResource.put("Mixed financing",
				Vocab.COFINANCING_RATE_MIXED);
		cofinancingRateTagToResource.put("N.A.", Vocab.COFINANCING_RATE_NA);

		expenseTypeTagToResource.put("Operational",
				Vocab.EXPENSE_TYPE_OPERATIONAL);
		expenseTypeTagToResource.put("Administrative",
				Vocab.EXPENSE_TYPE_ADMINISTRATIVE);
	}

	public static void main(String[] args) throws Exception {
		processDir(new File("data/en"));
	}

	public static void processDir(File dir) throws Exception {
		for (File file : dir.listFiles()) {
			process(file);
		}
	}

	public static BigDecimal processCofinancingRate(String value) {
		if (value == null || value.isEmpty()) {
			return null;
		}

		try {
			if (value.endsWith("%")) {
				return new BigDecimal(value.replace("%", "").trim());
			} else {
				return null;
			}
		} catch (Exception e) {
			logger.error("Error parsing: " + value);
			return null;
		}
	}

	public static BigDecimal processAmount(String value) {
		if (value == null || value.isEmpty()) {
			return null;
		}

		try {
			return new BigDecimal(value.replace(".", "").replace(',', '.'));
		} catch (Exception e) {
			logger.error("Error parsing: " + value);
			return null;
		}
	}

	public static void emit(Sink<Triple> sink, Node s, Node p, Node o) {
		if (s == null || p == null || o == null) {
			return;
		}

		sink.send(new Triple(s, p, o));
	}

	public static Node emit(Sink<Triple> sink, Node subject, Node property,
			String prefix, String label) {
		if (label == null || label.isEmpty()) {
			return null;
		}

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
		if (!(file.isFile() && file.getName().endsWith(".xml"))) {
			return;
		}

		// InputStream in = new FileInputStream(file);
		System.out.println("Processing " + file.getAbsolutePath());

		Graph graph = GraphFactory.createDefaultGraph();
		Sink<Triple> sink = new SinkTriplesToGraph(graph);

		process(file, sink);

		Model model = ModelFactory.createModelForGraph(graph);
		
		String sourcePath = file.getAbsolutePath();
		String targetPath = sourcePath.substring(0, sourcePath.length() - 3) + "nt";
		OutputStream out = new FileOutputStream(targetPath);
		
		try {
			model.write(out, "N-TRIPLE");
			out.flush();
		} finally {
			out.close();
		}
	}

	public static String trim(String str) {
		return str == null ? null : str.trim();
	}

	/**
	 * Iterates all getters of strings and sets a new trimmed string
	 * 
	 * 
	 * @param o
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws IntrospectionException
	 */
	public static void trimStrings(Object o) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException,
			IntrospectionException {

		BeanInfo info = Introspector.getBeanInfo(o.getClass());

		for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
			if (String.class.equals(pd.getPropertyType())) {
				if (pd.getReadMethod() == null || pd.getWriteMethod() == null) {
					continue;
				}

				String trimmed = trim((String) pd.getReadMethod().invoke(o));
				pd.getWriteMethod().invoke(o, trimmed);
			}
		}
	}

	public static void process(File file, Sink<Triple> sink) throws Exception {

		JAXBContext context = JAXBContext.newInstance(Fts.class.getPackage()
				.getName());

		Unmarshaller unmarshaller = context.createUnmarshaller();
		Object result = unmarshaller.unmarshal(file);

		Fts fts = (Fts) result;

		for (Commitment commitment : fts.getCommitment()) {

			trimStrings(commitment);

			Node commitmentNode = Node.createURI("http://fts.publicdata.eu/ct/"
					+ commitment.getPositionKey());

			emit(sink, commitmentNode, RDF.type.asNode(),
					Vocab.Commitment.asNode());

			// Node yearNode = Node.createURI("http://dbpedia.org/resource/"
			// + commitment.getYear());

			Node amountNode = createTypedLiteralNode(processAmount(commitment
					.getAmount()));
			emit(sink, commitmentNode, Vocab.amount.asNode(), amountNode);

			emit(sink,
					commitmentNode,
					RDFS.label.asNode(),
					Node.createLiteral("Commitment "
							+ commitment.getPositionKey()));

			emit(sink, commitmentNode, Vocab.budgetLine.asNode(),
					Node.createLiteral(commitment.getBudgetLine()));

			if (!commitment.getGrantSubject().isEmpty()) {
				emit(sink,
						commitmentNode,
						Vocab.subject.asNode(),
						ResourceFactory.createPlainLiteral(
								commitment.getGrantSubject()).asNode());
			}

			emit(sink, commitmentNode, Vocab.actionType.asNode(),
					"http://fts.publicdata.eu/at/", commitment.getActiontype());

			emit(sink, commitmentNode, Vocab.programme.asNode(),
					"http://fts.publicdata.eu/pg/", commitment.getProgramme());
			emit(sink, commitmentNode, Vocab.responsibleDepartment.asNode(),
					"http://fts.publicdata.eu/rd/",
					commitment.getResponsibleDepartment());
			emit(sink, commitmentNode, Vocab.year.asNode(),
					"http://dbpedia.org/resource/", commitment.getYear()
							.toString());

			/*
			 * Cofinancing rate
			 */
			String cfr = commitment.getCofinancingRate();
			if (cfr != null) {
				if(cfr.isEmpty()) {
					// Skp
				}
				else if (cfr.endsWith("%")) {
					BigDecimal rate = processAmount(cfr.substring(0,
							cfr.length() - 1).trim());

					emit(sink, commitmentNode,
							Vocab.cofinancingRateType.asNode(),
							Vocab.COFINANCING_RATE_EXPLICIT.asNode());
					emit(sink, commitmentNode, Vocab.cofinancingRate.asNode(),
							createTypedLiteralNode(rate));

				} 
				else {

					Resource res = cofinancingRateTagToResource.get(cfr);
					if (res != null) {
						emit(sink, commitmentNode,
								Vocab.cofinancingRateType.asNode(),
								res.asNode());
					} else {
						logger.error("Unknown cofinancing rate: " + cfr);
					}

				}

			}

			// TODO Attach the expense type to the commitment
			/*
			 * emit(sink, commitmentNode, Vocab.expenseType.asNode(),
			 * "http://fts.publicdata.eu/et/", commitment.get());
			 */

			// http://fts.opendata.org/resource/cm/{position-key}/SI2.566788.1
			String expenseType = null;
			for (Beneficiary beneficiary : commitment.getBeneficiaries()
					.getBeneficiary()) {

				trimStrings(beneficiary);

				/*
				 * Expense type
				 */
				if (expenseType == null) {
					expenseType = beneficiary.getExpensetype();
				} else if (!expenseType.equals(beneficiary.getExpensetype())) {
					logger.error("Different expense types within same commitment: "
							+ expenseType + ", " + beneficiary.getExpensetype());
				}

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

				emit(sink, beneficiaryNode, RDF.type.asNode(),
						Vocab.Beneficiary.asNode());

				/*
				 * Link the beneficiary with the commitment
				 */
				emit(sink, commitmentNode, Vocab.beneficiary.asNode(),
						beneficiaryNode);

				/*
				 * The first name of a beneficiary becomes a rdf:label and a
				 * skos:pref label. The remaining labels became skos:alt labels
				 * 
				 * Below is deprecated. All of a beneficiary's names become
				 * rdfs:labels; the primary name additionally becomes
				 * skos:prefLabel
				 */

				for (int i = 0; i < names.length; ++i) {
					String name = names[i];
					Node nameNode = ResourceFactory.createPlainLiteral(name)
							.asNode();

					if (i == 0) {
						emit(sink, beneficiaryNode, RDFS.label.asNode(),
								nameNode);
						emit(sink, beneficiaryNode,
								Vocab.skosPrefLabel.asNode(), nameNode);
					} else {
						emit(sink, beneficiaryNode,
								Vocab.skosAltLabel.asNode(), nameNode);
					}
				}

				if (expenseType != null) {
					emit(sink, commitmentNode, Vocab.expenseType.asNode(),
							"http://fts.publicdata.eu/et/", expenseType);
				}

				// for (String name : names) {
				// emit(sink, beneficiaryNode, RDFS.label.asNode(),
				// ResourceFactory.createPlainLiteral(name).asNode());
				// }
				// emit(sink, beneficiaryNode, Vocab.skosPrefLabel.asNode(),
				// ResourceFactory.createPlainLiteral(names[0]).asNode());

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
				if (!(beneficiary.getAddress() == null || beneficiary
						.getAddress().isEmpty())) {
					emit(sink, beneficiaryNode, Vocab.street.asNode(),
							Node.createLiteral(beneficiary.getAddress()));
				}

				if (!(beneficiary.getPostCode() == null || beneficiary
						.getPostCode().isEmpty())) {
					emit(sink, beneficiaryNode, Vocab.postCode.asNode(),
							Node.createLiteral(beneficiary.getPostCode()));
				}

				/*
				 * Spatial hierarchy (city, country)
				 */
				Node cityNode = null;
				if (!(beneficiary.getCity() == null || beneficiary.getCity()
						.isEmpty())) {

					String cityStr = beneficiary.getCity() + "-"
							+ beneficiary.getCountry();
					cityNode = Node
							.createURI("http://fts.publicdata.eu/resource/ci/"
									+ cityStr);

					emit(sink, beneficiaryNode, Vocab.city.asNode(), cityNode);

					emit(sink, cityNode, RDF.type.asNode(),
							SpatialHierarchy.City.asNode());
					emit(sink, cityNode, RDFS.label.asNode(),
							Node.createLiteral(beneficiary.getCity()));
				}

				Node countryNode = emit(sink, beneficiaryNode,
						Vocab.country.asNode(),
						"http://ftc.publicdata.eu/resource/cy/",
						beneficiary.getCountry());

				emit(sink, countryNode, RDF.type.asNode(),
						SpatialHierarchy.Country.asNode());

				if (cityNode != null) {
					emit(sink, cityNode, SpatialHierarchy.isLocatedIn.asNode(),
							countryNode);
				}

				/*
				 * Detail Amount
				 */
				String detailAmount = beneficiary.getDetailAmount() == null ? ""
						: beneficiary.getDetailAmount().trim();
				if (!detailAmount.isEmpty()) {
					Node da = Node.createURI("http://fts.publicdata.eu/da/"
							+ commitment.getPositionKey()
							+ "-"
							+ StringUtils.md5Hash(commitmentNode.toString()
									+ beneficiaryNode.toString()));
					Node daValueNode = createTypedLiteralNode(processAmount(detailAmount));

					emit(sink, da, RDF.type.asNode(),
							Vocab.AmountOfDistribution.asNode());
					emit(sink, da, Vocab.commitment.asNode(), commitmentNode);
					emit(sink, da, Vocab.beneficiary.asNode(), beneficiaryNode);
					emit(sink, da, Vocab.amount.asNode(), daValueNode);
				}
			}
		}

		/*
		 * for(Triple triple : graph.find(null, null, null).toList()) {
		 * System.out.println(triple); }
		 */
	}
}
