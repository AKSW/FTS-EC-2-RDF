package org.aksw.fts;

import java.io.File;

import org.junit.Test;
import org.openjena.atlas.lib.Sink;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.util.ModelUtils;

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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void send(Triple triple) {
		Statement stmt = ModelUtils.tripleToStatement(model, triple);
		model.add(stmt);		
	}
	
}

public class FtsTest {
	
	@Test
	public void test()
		throws Exception
	{
		SinkModel sink = new SinkModel();
		
		File sourceFile = new File("src/test/resources/fts-excerpt-2009-en.xml");
		
		Main.process(sourceFile, sink);
		
		sink.getModel().write(System.out, "TTL");
	}
}
