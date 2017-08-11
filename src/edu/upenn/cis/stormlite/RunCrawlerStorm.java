package edu.upenn.cis.stormlite;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleepycat.persist.EntityCursor;

import edu.upenn.cis.stormlite.bolt.CrawlerBolt;
import edu.upenn.cis.stormlite.bolt.DocumentParserBolt;
import edu.upenn.cis.stormlite.bolt.MatchChannelBolt;
import edu.upenn.cis.stormlite.bolt.UrlFilterBolt;
import edu.upenn.cis.stormlite.spout.CrawlerQueueSpout;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.storage.FileContent;

public class RunCrawlerStorm {
	
	private static String seedUrl;
	// static fields used by spout and bolt
	private static int maxPageSize;
	private static int maxNumPages;
	private static String dbDir;
	public static int numCrawledPage;
	
	private static final String QUEUE_SPOUT = "QUEUE_SPOUT";
    private static final String CRAWLER_BOLT = "CRAWLER_BOLT";
    private static final String MATCHANNEL_BOLT = "MATCHCHANNEL_BOLT";
    private static final String DOCPARSER_BOLT = "DOCPARSER_BOLT";
    private static final String URLFILTER_BOLT = "URLFILTER_BOLT ";
	
	public RunCrawlerStorm(String seedurl,String dbDir,int maxSize){
		this.seedUrl = seedurl;
		this.maxPageSize = maxSize;
		this.maxNumPages = 1000;
		this.dbDir = dbDir;
		this.numCrawledPage = 0;
	}
	
	public RunCrawlerStorm(String seedurl,String dbDir,
						int maxSize,int maxNum){
		this(seedurl,dbDir,maxSize);
		this.maxNumPages = maxNum;
	}
	
	public static void main(String[] args) throws InterruptedException{
		
		 if(args.length <= 2){
			  System.out.println("YimengXu (xuyimeng)"); 
			  return;
		  }
		  // initialize crawler parameter given the command arguments
		  RunCrawlerStorm crawler;
		  if(args.length == 3){
			  crawler = new RunCrawlerStorm(args[0],args[1],Integer.parseInt(args[2]));
		  }else if(args.length == 4){
			  crawler = new RunCrawlerStorm(args[0],args[1],
					  Integer.parseInt(args[2]),Integer.parseInt(args[3]));
		  }else{
			  System.out.println("Too many numbers of arguments!");
			  return;
		  }
		  
		  Config config = new Config();
		  
		  CrawlerQueueSpout.setParameter(seedUrl);
		  CrawlerBolt.setParameter(maxPageSize,maxNumPages, dbDir);
		  
		  CrawlerQueueSpout spout = new CrawlerQueueSpout();
		  CrawlerBolt crawlerBolt = new CrawlerBolt();
		  MatchChannelBolt matchChannelBolt = new MatchChannelBolt();
		  DocumentParserBolt docParserBolt = new DocumentParserBolt();
		  UrlFilterBolt urlFilterBolt = new UrlFilterBolt();
		  
		  TopologyBuilder builder = new TopologyBuilder();
		  builder.setSpout(QUEUE_SPOUT,spout,1);
		  
		  builder.setBolt(CRAWLER_BOLT, crawlerBolt, 1).shuffleGrouping(QUEUE_SPOUT);
		  
		  builder.setBolt(MATCHANNEL_BOLT, matchChannelBolt, 1).shuffleGrouping(CRAWLER_BOLT);
	
		  builder.setBolt(DOCPARSER_BOLT, docParserBolt, 1).shuffleGrouping(MATCHANNEL_BOLT);
		  
		  builder.setBolt(URLFILTER_BOLT, urlFilterBolt, 1).shuffleGrouping(DOCPARSER_BOLT);
		  
		  LocalCluster cluster = new LocalCluster();
		  Topology topo = builder.createTopology();
		  
			
		  ObjectMapper mapper = new ObjectMapper();
	      try {
				String str = mapper.writeValueAsString(topo);
				
				System.out.println("The StormLite topology is:\n" + str);
		  } catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		  }
			
			
		  cluster.submitTopology("test", config, builder.createTopology());
		  Thread.sleep(30000);
		  if(CrawlerQueueSpout.frontierQueue.isEmpty()){
			  System.out.println("Finish crawling, total num of pages crawled:"+numCrawledPage);
//			  printFileInDb();
		  }
	      cluster.killTopology("test");
		  cluster.shutdown();
		  System.exit(0);
		
	}
//	public static void printFileInDb(){
////		System.out.println("Url list have been crawled:");
//		
////		EntityCursor<FileContent> cursor = CrawlerBolt.getDB().getFileIndex().entities();
////		try{
////			for(FileContent entity = cursor.first();entity != null ; entity = cursor.next()){
////			System.out.println(entity.getUrl());
////			}
////		}finally{
////			cursor.close();
////		}
//		
//	}

}
