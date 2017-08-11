package edu.upenn.cis455.storage;

import java.util.Date;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class FileContent {

@PrimaryKey
private String url;
private String type;
private byte[] content;
private long lastcrawl;

public FileContent(){}

public FileContent(String url, String type, byte[] raw, long time)
{
	this.url=url;
	this.type=type;
	this.content=raw;
	this.lastcrawl=time;
}

public String getUrl()
{
	return url;
}

public void setUrl(String url){
	this.url = url;
}

public String getType()
{
	return type;
}

public void setType(String type){
	this.type = type;
}

public byte[] getRawContent()
{
	return content;
}

public void setContent(byte[] content){
	this.content = content;
}

public long getLastCrawled()
{
	return lastcrawl;
}

public void setLastCrawled(long curtime){
	this.lastcrawl = curtime;
}

}
