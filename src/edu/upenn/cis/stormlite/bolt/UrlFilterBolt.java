package edu.upenn.cis.stormlite.bolt;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.spout.CrawlerQueueSpout;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;

public class UrlFilterBolt implements IRichBolt {
	
	Fields schema = new Fields();
	String executorId = UUID.randomUUID().toString();
	private OutputCollector collector;
	private List<String> links;

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
		
	}

	@Override
	public void execute(Tuple input) {
		links = (List<String>) input.getObjectByField("urls");
		for(String link : links){
			if(!CrawlerQueueSpout.urlSet.contains(link)){
				CrawlerQueueSpout.urlSet.add(link);
				CrawlerQueueSpout.frontierQueue.offer(link);
			}
		}
	}

	@Override
	public void prepare(Map<String, String> stormConf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
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
