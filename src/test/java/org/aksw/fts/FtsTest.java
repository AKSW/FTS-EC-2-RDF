package org.aksw.fts;

import java.io.File;

import org.junit.Test;

public class FtsTest {
	
	@Test
	public void test()
		throws Exception
	{
		File sourceFile = new File("src/main/test/resources/fts-exceprt-2009-en.xml");
		
		Main.process(sourceFile);
	}
}
