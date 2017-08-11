package edu.upenn.cis455.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.List;

import edu.upenn.cis455.crawler.info.RobotsTxtInfo;
import edu.upenn.cis455.crawler.info.URLInfo;

public class RobotProcessor {
	private String robotUrl; //hostname+robots.txt
	private String host;   //hostname
	private RobotsTxtInfo robotInfo; //There is one robots.txt per host
	private Client client; //fake client to grab content in robots.txt
	private long lastvisitTime = 0;
	
	public RobotProcessor(String url) throws IOException{
		URLInfo info = new URLInfo(url);
		this.host = info.getHostName();
		this.robotUrl = "http://"+info.getHostName()+"/robots.txt";
		this.client = new Client(robotUrl);
		this.robotInfo = new RobotsTxtInfo();
		formulateRobotInfo();
	}
	
	public void formulateRobotInfo() throws IOException{
		InputStream is = (InputStream) client.sendGetRequest();
		if(is == null){
			return;
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = "";
		String tempAgent = "";
		while((line = br.readLine())!= null){
			line = line.toLowerCase();
			if(line.startsWith("user-agent: *")){
				tempAgent = "*";
				robotInfo.addUserAgent(tempAgent);
				while((line = br.readLine())!= null && line.length() != 0){
					line = line.toLowerCase();
					if(line.startsWith("disallow: ")){
						robotInfo.addDisallowedLink(tempAgent, line.substring(10));
					}else if(line.startsWith("crawl-delay: ")){
						robotInfo.addCrawlDelay(tempAgent, 
								Integer.parseInt(line.substring(13)));
					}
					if(line.startsWith("user-agent: ")) break;
				}
			}else if(line.startsWith("user-agent: cis455crawler")){
				tempAgent = "cis455crawler";
				robotInfo.addUserAgent(tempAgent);
				while((line = br.readLine())!= null && line.length() != 0){
					line = line.toLowerCase();
					if(line.startsWith("disallow: ")){
						robotInfo.addDisallowedLink(tempAgent, line.substring(10));
					}else if(line.startsWith("crawl-delay: ")){
						
						robotInfo.addCrawlDelay(tempAgent, 
								Integer.parseInt(line.substring(13)));
					}
					if(line.startsWith("user-agent: ")) break;
				}
			}
		}
	}
	
	public boolean checkDisallow(String url){//true : allow false: disallow
		URLInfo info = new URLInfo(url);
		String filePath = info.getFilePath();
		String matchAgent = "";
		if(robotInfo.containsUserAgent("cis455crawler")){
			matchAgent = "cis455crawler";
		}else if(robotInfo.containsUserAgent("*")){
			matchAgent = "*";
		}
		if(matchAgent.equals("")){//no disallow restriction
			return true;
		}
		List<String> disallowedPaths =  robotInfo.getDisallowedLinks(matchAgent);
		for(String disallowedPath : disallowedPaths){
			if(filePath.equals(disallowedPath)){
				return false;
			}
			if(disallowedPath.endsWith("/") && filePath.contains(disallowedPath)){
				return false;
			}
		}
		return true;
	}
	
	public int getCrawlDelay(){
		String matchAgent = "";
		if(robotInfo.containsUserAgent("cis455crawler")){
			matchAgent = "cis455crawler";
		}else if(robotInfo.containsUserAgent("*")){
			matchAgent = "*";
		}
		if(matchAgent.equals("")){//no delay restriction
			return 0;
		}
		return robotInfo.getCrawlDelay(matchAgent);
	}
	
	public long getLastVisitTime(){
		return lastvisitTime;
	}
	
	public void markLastVisitTime() {
		this.lastvisitTime = Calendar.getInstance().getTimeInMillis();
	}

	public static void main(String[] args) throws IOException {
		String url = "http://crawltest.cis.upenn.edu";
		RobotProcessor rp = new RobotProcessor(url);
		
	}

}
