package edu.upenn.cis455.crawler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.storage.FileContent;

public class XPathCrawler {
	
	private String seedUrl;
	private int maxPageSize;
	private int maxNumPages;
	private DBWrapper db;
	private Queue<String> frontierQueue = new LinkedList<String>();
	private Set<String> urlSet = new HashSet<String>();
	private HashMap<String,RobotProcessor> robotMap = new HashMap<>();
	//map host name to robot
	private HashMap<String,Long> modifiedMap = new HashMap<>();
	private Set<String> downloadSet = new HashSet<>();
	public XPathCrawler(){
		;
	}
	
	public XPathCrawler(String seedurl,String dbDir,int maxSize){
		this.seedUrl = seedurl;
		this.maxPageSize = maxSize;
		this.maxNumPages = 1000;
		this.db = new DBWrapper(dbDir);
	}
	
	public XPathCrawler(String seedurl,String dbDir,
						int maxSize,int maxNum){
		this(seedurl,dbDir,maxSize);
		this.maxNumPages = maxNum;
	}
	
	public static void main(String args[]) throws InterruptedException
	{
	    /* TODO: Implement crawler */
		  if(args.length <= 2){
			  System.out.println("YimengXu (xuyimeng)"); 
			  return;
		  }
		  // initialize crawler parameter given the command arguments
		  XPathCrawler crawler;
		  if(args.length == 3){
			  crawler = new XPathCrawler(args[0],args[1],Integer.parseInt(args[2]));
		  }else if(args.length == 4){
			  crawler = new XPathCrawler(args[0],args[1],
					  Integer.parseInt(args[2]),Integer.parseInt(args[3]));
		  }else{
			  System.out.println("Too many numbers of arguments!");
			  return;
		  }
		  
		  //crawl and update frontier queue
		  try {
			crawler.crawl();
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
		  
	 }
	
	public void crawl() throws IOException, InterruptedException{
		frontierQueue.add(seedUrl);
		urlSet.add(seedUrl);
		int numPage = 0;
		//using bfs to crawl urls from seed url, check if queue is empty
		while(!frontierQueue.isEmpty()){
			//dequeue an url page
			String curtUrl = frontierQueue.poll();
			//limit number of pages crawled
			if(urlSet.size() > maxNumPages){
				System.out.println("Crawled page number larger than max limit");
				break;
			}
			// Do downloading and extract the links in url
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
				continue;
			}//check if in crawl delay 
			else if(!checkDelay(hostName,robot)){
//				System.out.println(curtUrl + ":Not downloading(robot crawl delay haven't pass)");
				
				frontierQueue.offer(curtUrl);
				continue;
			}
		
			//send a headrequest and check if the response is 200
			if(!client.sendHeadRequest()){
				System.out.println(curtUrl+ ":Not downloading(head request failed)");
				continue;
			}
			if(!client.checkContentType()){
				System.out.println(curtUrl+ ":Not downloading(not correct content type)");
				continue;
			}
			if(!client.checkContentSize(maxPageSize)){
				System.out.println(curtUrl+ ":Not downloading(content exedes max size)");
				continue;
			}
			
			//client send a get request and get the page content
			//stored into a byte array, return null if the input stream is null
			
			TimeUnit.SECONDS.sleep(1);
			
			byte[] content = client.getUrlContent();
			if(content != null){
				
				//put the content into database
				String fileType = client.getContentType();
				long lastModified = client.getLastModified();
				if(db.containsFile(curtUrl) && checkModified(curtUrl,lastModified)){
					System.out.println(curtUrl+": Not Modified");
					System.out.println(db.containsFile(curtUrl));
				}else{
					
					FileContent newFile = new FileContent(curtUrl,fileType,
							content,lastModified);
					System.out.println(curtUrl+": Downloading");
					modifiedMap.put(curtUrl, lastModified);
					numPage++;
					robot.markLastVisitTime();
					db.addFile(newFile);
				
				}
				
				//if content is html, extract the links in html and put into queue
				if(fileType.equals("html")){
					List<String> links = findLinksFromUrl(curtUrl);
					for(String link : links){
						if(!urlSet.contains(link)){
							urlSet.add(link);
							frontierQueue.offer(link);
						}
					}
				}
			}
		}	
		if(frontierQueue.isEmpty()){
			System.out.println("Finish crawling, Frontier queue is empty now");
			System.out.println("Total "+numPage+" add to db");
			System.out.println("==============In set===============");
			for(String s:urlSet){
				System.out.println(s);
			}
			
		}
			
	}
	
	public boolean checkModified(String url,long curtModify){
		if(!modifiedMap.containsKey(url)) return true;
		long lastCrawledTime = modifiedMap.get(url);
//		System.out.println(lastCrawledTime );
//		System.out.println(curtModify);
		if(lastCrawledTime >= curtModify){
			return false;
		}
		return true;
	}
	
	public boolean checkDelay(String hostName,RobotProcessor robot){
		int minDelay = robot.getCrawlDelay();
		if(minDelay > 0){
			long lastTime = robot.getLastVisitTime();
//			System.out.println("lasttime:"+lastTime);
			long curtTime = System.currentTimeMillis();
//			System.out.println("curtTime:" + curtTime);
//			System.out.println(minDelay);
			if(minDelay*1000 > (curtTime - lastTime)) 
				return false;
		}
		return true;
	}
	
	public List<String> findLinksFromUrl(String url){
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
	
	
}
