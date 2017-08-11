package edu.upenn.cis.stormlite.bolt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.RunCrawlerStorm;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis.stormlite.tuple.Values;
import edu.upenn.cis455.storage.Channel;
import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.storage.FileContent;

public class DocumentParserBolt implements IRichBolt {
	
	Fields schema = new Fields("urls");
	String executorId = UUID.randomUUID().toString();
	private List<String> urls;
	private DBWrapper db;
	private String curtUrl;
	private FileContent file;
	private String fileType;
	
	private OutputCollector collector;
	
	public DocumentParserBolt(){
		;
	}

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
		curtUrl = input.getStringByField("curtUrl");
		file = (FileContent) input.getObjectByField("document");
		fileType = input.getStringByField("fileType");
		// download file to db
		System.out.println(curtUrl+": Downloading");
		RunCrawlerStorm.numCrawledPage++;
		db.addFile(file);
		// extract url from file
		if(fileType.equals("html")){
			urls = findLinksFromUrl(curtUrl);
			collector.emit(new Values<Object>(urls));
		}
	}
	
	private List<String> findLinksFromUrl(String url){
		List<String> list = new ArrayList<>();
		Document doc;
		try {
			doc = Jsoup.connect(url).get();
			Elements elems = doc.select("a[href]");
			for(Element e : elems){
				list.add(e.attr("abs:href"));
			}
		} catch (IOException e) {
		}
		return list;
	}

	@Override
	public void prepare(Map<String, String> stormConf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		this.db = CrawlerBolt.getDB();
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
