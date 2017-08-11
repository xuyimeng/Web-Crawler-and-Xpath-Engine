package edu.upenn.cis.stormlite.spout;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;

import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Values;

public class CrawlerQueueSpout implements IRichSpout{
	static Logger log = Logger.getLogger(CrawlerQueueSpout.class);
	
	Fields schema = new Fields("url");
	
	//for debug generate ID for each queue spout
	String executorId = UUID.randomUUID().toString();
	//Collector is the destination for tuples to emit
	SpoutOutputCollector collector;
	public static Queue<String> frontierQueue;
	public static Set<String> urlSet;
	public static String seedUrl;
	
	public CrawlerQueueSpout() {
		log.debug("Starting crawler queue spout");
	}

	@Override
	public void open(Map<String, String> config, TopologyContext topo, SpoutOutputCollector collector) {
		this.collector = collector;
		frontierQueue = new LinkedList<String>();
		urlSet = new HashSet<String>();
		frontierQueue.add(seedUrl);
		urlSet.add(seedUrl);
	}
	
	public static void setParameter(String url){
		seedUrl = url;
	}
	
	public static Set<String> getUrlSet(){
		return urlSet;
	}
	
	public static Queue<String> getFrontierQueue(){
		return frontierQueue;
	}

	@Override
	public void close() {
		frontierQueue.clear();
		urlSet.clear();
	}

	@Override
	public void nextTuple() {
		if(!frontierQueue.isEmpty()){
			String url = frontierQueue.poll();
			if(url != null){
				System.out.println("queue emit url:" + url);
				collector.emit(new Values<Object>(url));
			}else{
//				System.out.println("Url is null from queue");
			}
		}
	}

	@Override
	public void setRouter(IStreamRouter router) {
		this.collector.setRouter(router);
	}
	@Override
	public String getExecutorId() {
		return executorId;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(schema);
	}

}
