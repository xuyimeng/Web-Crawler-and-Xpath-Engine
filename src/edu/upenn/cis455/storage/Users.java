package edu.upenn.cis455.storage;

import java.util.ArrayList;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class Users {
	@PrimaryKey
	private String username;
	private String password;
	private ArrayList<String> subscribeChannels;
	
	public Users(){
	}
	
	public Users(String username,String password){
		this.username = username;
		this.password = password;
		this.subscribeChannels = new ArrayList<String>();
	}
	
	public String getUsername()
	{
		return username;
	}
	
	public String getPassword()
	{
		return password;
	}
	
	public void setUsername(String username){
		this.username = username;
	}
	
	public void setPassword(String password){
		this.password = password;
	}
	
	public boolean hasSubscribed(String channelName){
		return subscribeChannels.contains(channelName);
	}
	
	public boolean subscribe(String channelname){
		
		System.out.println("**********Users subscribe successfully*********");
		subscribeChannels.add(channelname);
		for(String cha : subscribeChannels){
			System.out.println(cha);
		}
		return true;
	}
	
	public boolean unsubscribe(String channel){
		if(!hasSubscribed(channel)) return false;
		System.out.println(username + "unsubscribe"+channel);
		subscribeChannels.remove(channel);
		return true;
	}
}
