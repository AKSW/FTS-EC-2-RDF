package org.aksw.fts;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.Test;
import org.openjena.atlas.lib.Sink;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.util.ModelUtils;


// TODO This class now exists in aksw-commons
class SinkModel
	implements Sink<Triple>
{
	private Model model;
	
	public SinkModel() {
		this.model = ModelFactory.createDefaultModel();
	}
	
	public SinkModel(Model model) {
		this.model = model;
	}
	
	public Model getModel() {
		return model;
	}
	

	@Override
	public void close() {		
	}

	@Override
	public void flush() {
	}

	@Override
	public void send(Triple triple) {
		Statement stmt = ModelUtils.tripleToStatement(model, triple);
		model.add(stmt);		
	}
	
}

public class FtsTest {
	
	@Test
	public void test1()
		throws Exception
	{
		SinkModel sink = new SinkModel();
		
		File sourceFile = new File("src/test/resources/test1-fts-excerpt-2009-en.xml");
		
		Main.process(sourceFile, sink);
		
		Model actual = sink.getModel(); 
		actual.write(System.out, "TTL");		
		
		Model expected = ModelFactory.createDefaultModel();
		InputStream in = new FileInputStream(new File("src/test/resources/test1-expected.ttl"));
		try {
			expected.read(in, "http://ex.org/", "TTL");
		} finally {
			in.close();
		}
		
		//Assert.assertEquals(expected, actual);
	}
}
