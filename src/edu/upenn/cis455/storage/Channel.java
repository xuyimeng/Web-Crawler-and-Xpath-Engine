package edu.upenn.cis455.storage;

import java.util.ArrayList;
import java.util.List;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class Channel {
	@PrimaryKey
	private String channelName;
	private String creatorName;
	private String xpath;
	private List<String> docUrls;
	
	public Channel(){
		;
	}
	
	public Channel(String channelName,String creatorName,String xpath){
		this.channelName = channelName;
		this.creatorName = creatorName;
		this.xpath = xpath;
		this.docUrls = new ArrayList<String>();
	}
	
	public String getChannelName() {
		return channelName;
	}

	public String getCreatorName() {
		return creatorName;
	}

	public String getXpath() {
		return xpath;
	}
	
	public List<String> getMatchedDoc(){ 
		return docUrls;
	}
	
	public void addMatchedDoc(String url){
		if(!docUrls.contains(url)){
			System.out.println("Add url "+url+" to channel "+ channelName);
			docUrls.add(url);
		}
	}
}
