package edu.upenn.cis455.xpathengine;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.util.regex.Matcher;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XPathEngineImpl implements XPathEngine {
	
	List<String> xpaths = new ArrayList<String>(); 
	
	public XPathEngineImpl() {
	    // Do NOT add arguments to the constructor!!
	}
		
	public void setXPaths(String[] s) {
		xpaths.clear();
		for(String str : s){
			xpaths.add(str);
		}
	}
	
	public boolean isValid(int i){
		System.out.println("Check if XPath "+i+" expression is valid" );
		return isValid(xpaths.get(i));
	}
	
	public boolean isValid(String xpath) {
	    
	    //replace all the whitespace
	    xpath = xpath.trim().replaceAll(" ", "");
	    //valid xpath must start with axis and have step
	    if(!xpath.startsWith("/") || xpath.length() <= 1){
	    	return false;
	    }
	    //extract first step
	    xpath = xpath.substring(1);
	    
	    List<String> parts = new ArrayList<String>();  
	    //extract steps
	    String[] segments = xpath.split("/");
	    for (int j = 0; j < segments.length; j++){
	    	//calculate the number of [ and ] is equal 
	    	String seg = segments[j];
	    	if(seg.contains("[")){
	    		int left = seg.length() - seg.replace("[", "").length();
	    		int right = seg.length() - seg.replace("]", "").length();
	    		while(left > right && j < segments.length - 1){
	    			seg = seg + "/" + segments[++j];
	    			left = seg.length() - seg.replace("[", "").length();
		    		right = seg.length() - seg.replace("]", "").length();
	    		}
	    		if(left != right){
	    			return false;
	    		}
	    	}
	    	parts.add(seg);
	    }
	    
	    for(String part : parts){
	    	System.out.println("Segment: "+ part);
	    	if(!isStepValid(part)){
	    		return false;
	    	}
	    }
	    return true;
	}
	
	public boolean isStepValid(String step){
		//step-> nodename([test])*(axis step)?
		//nodename must start with a letter or underscore
		String regex = "^([a-z_][a-zA-Z0-9_]*)(\\[.+\\])*$";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(step);
		
		if(matcher.find()){
			String nodeName = matcher.group(1);
			String test = matcher.group(2);
			
			if(test != null){
				System.out.println("test: " + test);
				//split parallel text strings:[A[text()="ds"]][bar]
				//into two text string and check each 
				
				Stack<Integer> stack = new Stack<Integer>(); //store idx of '['
				
				for(int i = 0; i < test.length(); i++){
					if(test.charAt(i) == '['){
						stack.push(i);
					}else if(test.charAt(i) == ']'){
						if(stack.size() == 1){
							int startIdx = stack.pop();
							if(!checkTestValid(test.substring(startIdx+1,i))){
								return false;
							}
						}else if(stack.size() == 0){
							return false; // invalid parathesis
						}else{
							stack.pop();
						}
					}
				}
			}
		}else{
			return false;
		}
		return true;
	}
	
	public boolean checkTestValid(String test){
		//check if [test] satisfy the four condition:
		//-> text() = "..."
		//-> contains(text(),"...")
		//-> @attrname = "..."
		//-> test is step (recursive call this function)
		System.out.println("Check test String: "+test);
		String testRegex1 = "^text\\(\\)=\"[^>]*\"$";
		String testRegex2 = "^contains\\(text\\(\\),\"[^>]*\"\\)$";
		String testRegex3 = "^\\@[a-z_A-Z][A-Z_a-z0-9-.]*=\"[^>]*\"$";
		String[] testRegex = {testRegex1,testRegex2,testRegex3};
		
		for(int i = 0; i < 3; i++){
			String regex = testRegex[i];
			Pattern pattern = Pattern.compile(regex);
			Matcher m = pattern.matcher(test);
			if(m.find()){
				System.out.println("test match regex pattern "+regex);
				return true;
			}
		}
		
		System.out.println("test is step (recursive call this function)");
	    return isValid("/"+test);
	}
		
	public boolean[] evaluate(Document d) { 
	    /* TODO: Check whether the document matches the XPath expressions */
		boolean[] isMatch = new boolean[xpaths.size()];
		//initialize the vector to all false
		for(int i = 0; i < isMatch.length; i++){
			isMatch[i] = false;
		}
		if(d == null){
			System.out.println("In Evaluate: document is null");
			return isMatch;
		}
		NodeList childs = d.getChildNodes();
		ArrayList<Node> nodes = new ArrayList<Node>();
		for(int i = 0; i<childs.getLength();i++){
			nodes.add(childs.item(i));
		}
		//for each xpath, check whether doc matches the path
		for(int j = 0; j < xpaths.size(); j++){
			// check if xpath valid first
			if(isValid(j)){
				isMatch[j] = evaluate(xpaths.get(j),nodes);
			}
		}	

	    return isMatch; 
	}
	
	public boolean evaluate(String xpath,ArrayList<Node> nodes){
		System.out.println("*****************Evaluate xpath:"+xpath);
	    //replace all the whitespace
	    xpath = xpath.trim();
	    //extract first step
	    xpath = xpath.substring(1);
	    
	    List<String> parts = new ArrayList<String>();  
	    //extract steps
	    String[] segments = xpath.split("/");
	    for (int j = 0; j < segments.length; j++){
	    	//calculate the number of [ and ] is equal 
	    	String seg = segments[j];
	    	if(seg.contains("[")){
	    		int left = seg.length() - seg.replace("[", "").length();
	    		int right = seg.length() - seg.replace("]", "").length();
	    		while(left > right && j < segments.length - 1){
	    			seg = seg + "/" + segments[++j];
	    			left = seg.length() - seg.replace("[", "").length();
		    		right = seg.length() - seg.replace("]", "").length();
	    		}
	    	}
	    	parts.add(seg);
	    }
	    
	    return evaluateStepMatch(nodes,parts,0);
	}
	
	// dfs helper function to check if segment matches document nodes
	private boolean evaluateStepMatch(ArrayList<Node> nodes,
									 List<String> steps,
									 int segIdx){
		// all segment has been checked
		if(segIdx >= steps.size()){
			return true;
		}
		if(nodes == null){
			return false;
		}
		ArrayList<Node> newNodes = new ArrayList<Node>();
		String step = steps.get(segIdx);
		System.out.println("Evaluate match for segment: "+step);
		
		String regex = "^([a-z_][a-zA-Z0-9_]*)(\\[.+\\])*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(step);
		
		if(matcher.find()){
			//extract nodeName and test from xpath
			String nodeName = matcher.group(1).trim();
			String test = matcher.group(2);
			
			//If one node matches, return true
			System.out.println("Print node List for step:"+step);
			System.out.println("Candidate Node number:"+nodes.size());
	
			for(int i = 0; i < nodes.size();i++){
				Node node = nodes.get(i);
				System.out.println("Node # "+i+" name:"+node.getNodeName());
				System.out.println("Node type:"+node.getNodeType());
				if(node.getNodeName().equals(nodeName)){
					System.out.println("Nodename "+node.getNodeName()+" matches " + nodeName);
					boolean isMatch = true;
					//node name matches segment
					if(test != null){
						Stack<Integer> stack = new Stack<Integer>(); //store idx of '['
						
						for(int j = 0; j < test.length(); j++){
							if(test.charAt(j) == '['){
								stack.push(j);
							}else if(test.charAt(j) == ']'){
								if(stack.size() == 1){
									int startIdx = stack.pop();
									if(!matchTest(node,test.substring(startIdx+1,j))){
										isMatch = false;
										break;
									}
								}else{
									stack.pop();
								}
							}
						}
					}
					//step contains only nodeName
					if(isMatch){
						NodeList childNodes = node.getChildNodes();
						for(int k=0; k < childNodes.getLength();k++){
							newNodes.add(childNodes.item(i));
						}
					}
				}
			}
		}
		return evaluateStepMatch(newNodes,steps,segIdx+1);
	}
	
	
	private boolean matchTest(Node node, String test) {
		System.out.println("Check test match "+test);
		test = test.trim();
		int testPattern = checkTestPattern(test);
		System.out.println("Test pattern #"+testPattern);
		if(testPattern == 1){//text() = ""
			int idx1 = -1;
			String targetText = "";
			for(int i = 0; i < test.length(); i++){
				if(test.charAt(i) == '"'){
					if(idx1 == -1 ){
						idx1 = i;
					}else{
						targetText = test.substring(idx1+1,i);
						System.out.println("target text: "+targetText);
					}
				}
			}
			if(node.getTextContent().equals(targetText)){
				System.out.println("Text match pattern "+test);
				return true;
			}else{
				System.out.println("Text not chat text()="+node.getTextContent());
			}
		}else if(testPattern == 2){//contains(text(),"")
			int idx1 = -1;
			String targetText = "";
			for(int i = 0; i < test.length(); i++){
				if(test.charAt(i) == '"'){
					if(idx1 == -1 ){
						idx1 = i;
					}else{
						targetText = test.substring(idx1+1,i);
						System.out.println("target contains: "+targetText);
					}
				}
			}
			if(node.getTextContent().contains(test)){
				System.out.println("Text match pattern contains(text(),"+test+")");
				return true;
			}
		}else if(testPattern == 3){//@attrname = ""
			System.out.println("Test attrribute matches ..");
			String regex = "^\\@([a-z_A-Z][A-Z_a-z0-9-.]*)=\"([^>]*)\"$";
			Pattern pattern = Pattern.compile(regex);
			Matcher m = pattern.matcher(test);
			
			if(m.find()){
				String testKey = m.group(1);
				String testValue = m.group(2);

				NamedNodeMap attMap = node.getAttributes();
				if(attMap!= null){
					Node keyNode = attMap.getNamedItem(testKey);
					if(keyNode != null && keyNode.getNodeValue().equals(testValue)){
						System.out.println("Text match pattern "+testKey +" = "+testValue);
						return true;
					}
				}
			}
		}else{//test is a step recursively call 
			System.out.println("Evaluate test is a step, need further evaluate");
			ArrayList<Node> newNo = new ArrayList<>();
			NodeList childNodes = node.getChildNodes();
			for(int k=0; k < childNodes.getLength();k++){
				newNo.add(childNodes.item(k));
			}
			return evaluate("/"+test,newNo);
		}
		return false;
	}
	
	private int checkTestPattern(String test){
		System.out.println("In checkTest Pattern + test string " + test);
		int result = 0;
		String testRegex1 = "^text\\(\\)=\"([^>]*)\"$";
		String testRegex2 = "^contains\\(text\\(\\),\"[^>]*\"\\)$";
		String testRegex3 = "^\\@[a-z_A-Z][A-Z_a-z0-9-.]*=\"[^>]*\"$";
		String[] testRegex = {testRegex1,testRegex2,testRegex3};
		
		for(int i = 0; i < 3; i++){
			String regex = testRegex[i];
			Pattern pattern = Pattern.compile(regex);
			Matcher m = pattern.matcher(test);
			if(m.find()){
				System.out.println("test match regex pattern "+regex);
				result = i+1;
			}
		}
		return result;
	}

	@Override
	public boolean isSAX() {
		return false;
	}
	
	@Override
	public boolean[] evaluateSAX(InputStream document, DefaultHandler handler) {
		return null;
	}
	
	public static void main(String[] args){
		String xpath1 = "/foo/bar/xyz/name[@attrb=\"cute\"]";
		String xpath2 = "/foo/bar/f[doo[text() = \"like\"]][bar]";
		String xpath4 = "/foo/bar/xyz/name/blah";
		String xpath3 = "/this/that[something/else]";
		String str = "/company/staff/firstname[text()=\"low\"]";
		String[] s = {str};
		XPathEngineImpl engine = new XPathEngineImpl();
		engine.setXPaths(s);
//		System.out.println(engine.isValid(0));
//		System.out.println(engine.isValid(1));
//		System.out.println(engine.isValid(2));
//		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
//				+ "<foo><bar>"
//				+ "<f><doo>like</doo><bar></bar></f>"
//				+ "<xyz>"
//				+ "<name attrb = \"cute\">like</name>"
//				+ "</xyz></bar></foo>";
//		Tidy tidy = new Tidy();
//		tidy.setXmlTags(true);
//		
//		try {
//			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//			DocumentBuilder db = dbFactory.newDocumentBuilder();
//			Document doc = db.parse(new ByteArrayInputStream(xml.getBytes()));
//			
//			boolean[] result = engine.evaluate(doc);
//			for(boolean res : result){
//				System.out.println(res);
//			}
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
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
			
			System.out.println(engine.evaluate(doc)[0]);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
        
}
