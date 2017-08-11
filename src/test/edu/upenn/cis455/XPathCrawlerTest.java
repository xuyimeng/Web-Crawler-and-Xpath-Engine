package test.edu.upenn.cis455;

import java.util.List;

import edu.upenn.cis455.crawler.XPathCrawler;

import junit.framework.TestCase;

public class XPathCrawlerTest extends TestCase{
	XPathCrawler crawler;
	
	protected void setUp() throws Exception {
		crawler = new XPathCrawler();
	}
	
	public void testFindLinks() {
		List<String> list =crawler.findLinksFromUrl("http://crawltest.cis.upenn.edu");
		assertTrue(list.contains("http://crawltest.cis.upenn.edu/nytimes/"));
		assertTrue(list.contains("http://crawltest.cis.upenn.edu/misc/weather.xml"));
		assertTrue(list.contains("http://crawltest.cis.upenn.edu/bbc/"));
		assertFalse(list.contains("http://crawltest.cis.upenn.edu/marie/private/"));
	}
	
}
