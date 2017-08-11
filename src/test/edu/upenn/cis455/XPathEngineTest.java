package test.edu.upenn.cis455;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import edu.upenn.cis455.xpathengine.XPathEngineImpl;
import junit.framework.TestCase;

public class XPathEngineTest extends TestCase{
	XPathEngineImpl engine;
	@Override
	protected void setUp() throws Exception {
		 engine = new XPathEngineImpl();
	}
	
	public void testIsValid() {
		String[] strs = {"/foo/bar/xyz",
				"/foo/bar[@att=\"123\"]", 
				 "/xyz/abc[contains(text(),\"someSubstring\")]", 
				 "/a/b/c[text()=\"theEntireText\"]", 
				 "/blah[anotherElement]", 
				 "/this/that[something/else]", 
				 "/d/e/f[foo[text()=\"something\"]][bar]", 
				 "/a/b/c[text() =    \"whiteSpacesShouldNotMatter\"]"};	
		engine.setXPaths(strs);
		assertTrue(engine.isValid(0));
		assertTrue(engine.isValid(1));
		assertTrue(engine.isValid(2));
		assertTrue(engine.isValid(3));
		assertTrue(engine.isValid(4));
		assertTrue(engine.isValid(5));
		assertTrue(engine.isValid(6));
		assertTrue(engine.isValid(7));
	}
	
	public void testNotValid() {
		String[] strs = {"", "/",  "/foo/bar[att=\"123\"]", 
				 "/foo/bar[contains(text(),\"someSubstring\")", 
				 "/a/b/chiald::c[text()=\"theEntireText\"]", 
				 "/this/bar*[something/else]","foo/bar/xyz", 
				 "/d/e/f[foo[@text()=\"something\"]][bar]"
						};
		engine.setXPaths(strs);
		assertFalse(engine.isValid(0));
		assertFalse(engine.isValid(1));
		assertFalse(engine.isValid(2));
		assertFalse(engine.isValid(3));
		assertFalse(engine.isValid(4));
		assertFalse(engine.isValid(5));
		assertFalse(engine.isValid(6));
		assertFalse(engine.isValid(7));
	}
	
	public void testEvaluate(){
		try{
		BufferedReader br = new BufferedReader(new FileReader(new File("test.xml")));
		String line;	
		StringBuilder sb = new StringBuilder();
		while((line = br.readLine()) != null){
		    sb.append(line.trim());
		}
		br.close();
		String content = sb.toString();
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbFactory.newDocumentBuilder();
		Document doc = db.parse(new ByteArrayInputStream(content.getBytes()));
		
		String str1 = "/company[staff]/staff[firstname[text()=\"low\"]][lastname]";
		String str2 = "/company[staff]/staff[firstname[text()=\"low\"]][lastname[text()=\"yin fong\"]]";
		String str3 = "/company[staff]";
		String str4 = "/company[staff]/staff[@id= \"2001\"]";
		String str5 = "/company[staff]/staff/firstname[text()=\"low\"]";
		String str6 = "/company[staff]/staff/firstname[text()=\"high\"]";
		String[] s = { str1,str2,str3,str4,str5,str6 };
		engine.setXPaths(s);
		assertTrue(engine.evaluate(doc)[0]);
		assertTrue(engine.evaluate(doc)[1]);
		assertTrue(engine.evaluate(doc)[2]);
		assertTrue(engine.evaluate(doc)[3]);
		assertTrue(engine.evaluate(doc)[4]);
		assertFalse(engine.evaluate(doc)[5]);
		}catch(Exception e){
			
		}
	}

}
