package test.edu.upenn.cis455;

import edu.upenn.cis455.crawler.RobotProcessor;
import edu.upenn.cis455.crawler.XPathCrawler;
import junit.framework.TestCase;

public class RobotProcessorTest extends TestCase {
	
	RobotProcessor robot;
	
	
	protected void setUp() throws Exception {
		robot = new RobotProcessor("http://crawltest.cis.upenn.edu");
	}

	public void testIsValid() {
		String url;
		url = "http://crawltest.cis.upenn.edu/marie/private/";
		assertFalse(robot.checkDisallow(url));
		url = "http://crawltest.cis.upenn.edu/foo/";
		assertFalse(robot.checkDisallow(url));
	}
}
