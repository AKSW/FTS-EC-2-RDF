package org.aksw.fts;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.aksw.commons.jena.util.SinkModel;
import org.junit.Assert;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;


public class FtsTest {
	
	@Test
	public void test1()
		throws Exception
	{
		SinkModel sink = new SinkModel();
		
		File sourceFile = new File("src/test/resources/test1-fts-excerpt-2009-en.xml");
		
		Main.process(sourceFile, sink);
		
		Model actual = sink.getModel(); 
		//actual.write(System.out, "TTL");		
		
		Model expected = ModelFactory.createDefaultModel();
		InputStream in = new FileInputStream(new File("src/test/resources/test1-expected.ttl"));
		try {
			expected.read(in, "http://ex.org/", "TTL");
		} finally {
			in.close();
		}

		Assert.assertTrue("Isomorphy of expected and actual model", expected.isIsomorphicWith(actual));
	}
}
