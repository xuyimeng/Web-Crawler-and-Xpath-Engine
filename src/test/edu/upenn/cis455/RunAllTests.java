package test.edu.upenn.cis455;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class RunAllTests extends TestCase 
{
  public static Test suite() 
  {
    try {
      Class[]  testClasses = {
        /* TODO: Add the names of your unit test classes here */
        // Class.forName("your.class.name.here") 
  
    	      Class.forName("test.edu.upenn.cis455.RobotProcessorTest"),
    	      Class.forName("test.edu.upenn.cis455.XPathCrawlerTest"),
    	      Class.forName("test.edu.upenn.cis455.UserTest"),
    	      Class.forName("test.edu.upenn.cis455.DBWrapperTest"),
    	      Class.forName("test.edu.upenn.cis455.XPathEngineTest")
      };   
      
      return new TestSuite(testClasses);
    } catch(Exception e){
      e.printStackTrace();
    } 
    
    return null;
  }
}
