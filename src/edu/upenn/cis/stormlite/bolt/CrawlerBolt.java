package edu.upenn.cis.stormlite.bolt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.spout.CrawlerQueueSpout;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis.stormlite.tuple.Values;
import edu.upenn.cis455.crawler.Client;
import edu.upenn.cis455.crawler.RobotProcessor;
import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.storage.FileContent;

public class CrawlerBolt implements IRichBolt {
	static Logger log = Logger.getLogger(CrawlerBolt.class);
	
	Fields schema = new Fields("document","curtUrl","fileType");
	
	String executorId = UUID.randomUUID().toString();
	
	private OutputCollector collector;

	private Map<String, RobotProcessor> robotMap;

	private Map<String, Long> modifiedMap;
	
	// static field
	private static int maxPageSize;
	private static int maxNumPages;
	private static DBWrapper db;
	private Set<String> urlSet;
	
	public CrawlerBolt() {
	}
	
	public static void setParameter(int maxSize, int numPages, String dbDir){
		maxPageSize = maxSize;
		maxNumPages = numPages;
		db = new DBWrapper(dbDir);
	}
	
	//static function
	public static DBWrapper getDB(){
		return db;
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
	public void prepare(Map<String, String> stormConf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		this.urlSet = CrawlerQueueSpout.getUrlSet();
		robotMap = new HashMap<>();
		modifiedMap = new HashMap<>();
	}

	@Override
	public void execute(Tuple input) {
		try {
			String curtUrl = input.getStringByField("url");
			
			// Do downloading and extract the links in url
			if(urlSet.size() > maxNumPages){
				System.out.println("Crawled page number larger than max limit");
				return;
			}
			// create a fake client to send head/get request to grab the content in url
			Client client = new Client(curtUrl);
			// create robotProcessor
			String hostName = client.getHostName();
			RobotProcessor robot;
			if(!robotMap.containsKey(hostName)){
				//hostname has not been crawled before,create new robotProcessor
					robot = new RobotProcessor(curtUrl);
					robotMap.put(hostName, robot);
			}else{
				robot = robotMap.get(hostName);
			}
			// check is url is in robot disallow list
			if(!robot.checkDisallow(curtUrl)){
				System.out.println(curtUrl + ":Not downloading(robot disallow)");
				return;
			}//check if in crawl delay 
		
//			else if(!checkDelay(hostName,robot)){
//				System.out.println(curtUrl + ":Not downloading(delay time not pass)");
//				CrawlerQueueSpout.frontierQueue.add(curtUrl);
//				return;
//			}
		
			//send a headrequest and check if the response is 200
			if(!client.sendHeadRequest()){
				System.out.println(curtUrl+ ":Not downloading(head request failed)");
				return;
			}
			if(!client.checkContentType()){
				System.out.println(curtUrl+ ":Not downloading(not correct content type)");
				return;
			}
			if(!client.checkContentSize(maxPageSize)){
				System.out.println(curtUrl+ ":Not downloading(content exedes max size)");
				return;
			}
			
			byte[] content = client.getUrlContent();
			if(content != null){
				
				//put the content into database
				String fileType = client.getContentType();
				long lastModified = client.getLastModified();
				if(db.containsFile(curtUrl) && checkModified(curtUrl,lastModified)){
					System.out.println(curtUrl+": Not Modified");
					System.out.println(db.containsFile(curtUrl));
					FileContent file = db.getFileContent(curtUrl);
					collector.emit(new Values<Object>(file,curtUrl,fileType));
				}else{
					
					FileContent newFile = new FileContent(curtUrl,fileType,
							content,lastModified);
					modifiedMap.put(curtUrl, lastModified);
					robot.markLastVisitTime();
					collector.emit(new Values<Object>(newFile,curtUrl,fileType));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	

	public boolean checkModified(String url,long curtModify){
		if(!modifiedMap.containsKey(url)) return true;
		long lastCrawledTime = modifiedMap.get(url);
		if(lastCrawledTime >= curtModify){
			return false;
		}
		return true;
	}
	
	public boolean checkDelay(String hostName,RobotProcessor robot){
		int minDelay = robot.getCrawlDelay();
		if(minDelay > 0){
			long lastTime = robot.getLastVisitTime();
			long curtTime = System.currentTimeMillis();
			if(minDelay*1000 > (curtTime - lastTime)) 
				return false;
		}
		return true;
	}
	
	@Override
	public void cleanup() {
		System.out.println("clear field in crawler bolt");
		robotMap.clear();
		modifiedMap.clear();
		urlSet.clear();
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
