package jrds;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import javax.xml.parsers.ParserConfigurationException;

import jrds.mockobjects.MockGraph;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestGraphTree {
	static final private Logger logger = Logger.getLogger(TestGraphTree.class);
	
	@BeforeClass
	static public void configure() throws IOException, ParserConfigurationException {
		Tools.configure();
		Tools.prepareXml();
		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {"jrds.GraphTree"}, logger.getLevel());
	}
	
	private LinkedList<String> doList(String... pathelems) {
		return new LinkedList<String>( Arrays.asList(pathelems));
	}
	
	@Test
	public void test1() {
		GraphTree gt1 = GraphTree.makeGraph("root");
		
		GraphNode gn = new MockGraph();
		gt1.addGraphByPath(doList("a", "b", gn.getName()), gn);
		
		Assert.assertNotNull("Graph node not found" , gt1.getByPath("root", "a", "b"));
	}

	@Test
	public void test2() {
		GraphTree gt1 = GraphTree.makeGraph("root");
		
		GraphNode gn = new MockGraph();
		gt1.addPath("a", "b");
		
		Assert.assertNotNull("Graph node not found" , gt1.getByPath("root", "a", "b"));
	}

}