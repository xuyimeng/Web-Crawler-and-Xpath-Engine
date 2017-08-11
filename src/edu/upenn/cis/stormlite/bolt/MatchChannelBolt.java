package edu.upenn.cis.stormlite.bolt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xml.sax.SAXException;

import com.sleepycat.persist.PrimaryIndex;

import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis.stormlite.tuple.Values;
import edu.upenn.cis455.storage.Channel;
import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.storage.FileContent;
import edu.upenn.cis455.xpathengine.XPathEngine;
import edu.upenn.cis455.xpathengine.XPathEngineFactory;
import edu.upenn.cis455.xpathengine.XPathEngineImpl;

public class MatchChannelBolt implements IRichBolt {
	
	Fields schema = new Fields("document","curtUrl","fileType");
	String executorId = UUID.randomUUID().toString();
	
	private DBWrapper db;
	private OutputCollector collector;
	private XPathEngine engine;

	@Override
	public String getExecutorId() {
		return executorId;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(schema);
	}

	@Override
	public void cleanup() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void execute(Tuple input) {
		//looks up for all the channels in db
		//find if the document content matches the each channel
		try {
			String curtUrl = input.getStringByField("curtUrl");
			FileContent file = (FileContent) input.getObjectByField("document");
			String fileType = input.getStringByField("fileType");
			// get all channels in database
			Map<String,Channel> channels = db.getChannels().map();
			ArrayList<String> xpaths = new ArrayList<>();//store all xpath
			HashMap<String,String> pathChannelMap = new HashMap<>();//map xpath to channelName
			
			for(Channel channel: channels.values()){
				xpaths.add(channel.getXpath());
				pathChannelMap.put(channel.getXpath(), channel.getChannelName());
			}
			// find matched channel of the document
			boolean[] channelMatch = matchChannel(file,xpaths,fileType);
			// if doc matches channel, mark it to channel
			// store the url of file(key of fileContent) to channel
			for(int i = 0; i < channelMatch.length;i++){
				if(channelMatch[i] == true){
					String channelname = pathChannelMap.get(xpaths.get(i));
//					//add current url to doc
					db.addChannelDoc(channelname,curtUrl);
				}
			}
			
			// emit argument to another bolt
			collector.emit(new Values<Object>(file,curtUrl,fileType));
			
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
		}
		
	}
	
	private boolean[] matchChannel(FileContent file,
									ArrayList<String> xpaths,
									String fileType) 
			throws SAXException, IOException, ParserConfigurationException{
		boolean[] result = new boolean[xpaths.size()];
		for(int i = 0; i < result.length; i++) result[i] = false;
		// set engine's xpaths to evaluate
		String[] xpathArr = xpaths.toArray(new String[xpaths.size()]);
		engine.setXPaths(xpathArr);
		
		// parse file content into document:
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbFactory.newDocumentBuilder();
		Document doc = null;
		if(fileType.equals("html")){
			Tidy tidy = new Tidy();
			tidy.setXHTML(true);
			tidy.setXmlTags(false);
			tidy.setDocType("omit");
			tidy.setEncloseText(true);
			ByteArrayInputStream is = new ByteArrayInputStream(file.getRawContent());
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			tidy.parseDOM(is, os);
			doc = db.parse(new ByteArrayInputStream(os.toString("UTF-8").getBytes()));
			result = engine.evaluate(doc);
		}else if(fileType.equals("xml")){
			doc = db.parse(new ByteArrayInputStream(file.getRawContent()));
			result = engine.evaluate(doc);
		}else{
			System.out.println("File type invalid (not xml/html),not match any channel");
		}
		return result;
	}

	@Override
	public void prepare(Map<String, String> stormConf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		this.db = CrawlerBolt.getDB();
		this.engine = XPathEngineFactory.getXPathEngine();
	}

	@Override
	public void setRouter(IStreamRouter router) {
		this.collector.setRouter(router);
	}

	@Override
	public Fields getSchema() {
		return schema;
	}

}
