package edu.upenn.cis455.storage;

import java.io.File;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;

public class DBWrapper {
	
	private static String envDirectory = null;
	
	private static Environment myEnv;
	private static EntityStore store;
	
	private static String rootDir = null; 
	private static PrimaryIndex<String,Users> userIndex;
	private static PrimaryIndex<String,FileContent> fileIndex;
	private static PrimaryIndex<String,Channel> channelIndex;
	
	public DBWrapper(String rootDir){
		this.rootDir = rootDir;
		System.out.println("db dir is: " + rootDir);
		//prepare and create enviornment 
		EnvironmentConfig envConfig = new EnvironmentConfig();
		envConfig.setAllowCreate(true);	
		envConfig.setTransactional(true);
		// check if the file exists in given root directory
		File myfile = new File(rootDir);
		if(!myfile.exists()) {
			myfile.mkdirs();
			System.out.println("new directory created for DB...");
		}
		myEnv = new Environment(myfile,envConfig);
		
		//prepare and create entity store
		StoreConfig stConfig = new StoreConfig();
		stConfig.setAllowCreate(true);
		stConfig.setTransactional(true);
		store = new EntityStore(myEnv,"EntityStore",stConfig);
		
		//initialize user index
		userIndex = store.getPrimaryIndex(String.class, Users.class);
		fileIndex = store.getPrimaryIndex(String.class, FileContent.class);
		channelIndex = store.getPrimaryIndex(String.class, Channel.class);
		
		System.out.println("Berkeley DB has setted up...");
	}
	
	public void syncDB() {
		if(store != null){
			store.sync();
		}
		if(myEnv != null){
			myEnv.sync();
		}
	}
	
	public void closeDB(){
		if(store != null){
			store.close();
		}
		if(myEnv != null){
			myEnv.close();
		}
	}
	// user functions
	public void addUser(String username,String password){
		Users user = new Users(username,password);
		userIndex.put(user);
		System.out.println("User added to DB");
	}
	
	public PrimaryIndex<String,FileContent> getFileIndex(){
		return fileIndex;
	}
	public Users getUser(String username){
		
		return userIndex.get(username);
	}
	
	public boolean containsUser(String username){
		return userIndex.contains(username);
	}
	
	public void deleteUser(String username){
		userIndex.delete(username);
	}
	
	public boolean varifyPassword(String username,String password){
		if(!containsUser(username)){
			System.out.println("DB does not contain the user");
			return false;
		}
		Users user = userIndex.get(username);
		return user.getPassword().equals(password);
	}
	public  PrimaryIndex<String,Users> getUserIndex(){
		return userIndex;
	}
	
	public void userSubscribe(String username, String channelname){
		Users user = userIndex.get(username);
		user.subscribe(channelname);
		userIndex.put(user);
	}
	
	public void userUnsubscribe(String username, String channelname){
		Users user = userIndex.get(username);
		user.unsubscribe(channelname);
		userIndex.put(user);
	}
	
	public boolean checkSubscribe(String username, String channelName){
		System.out.println("**********DB check subscribe**************");
		Users user = userIndex.get(username);
		System.out.println("In dbwrapper class: " + user);
		return user.hasSubscribed(channelName);
	}
	
	// file content functions
	public void addFile(FileContent file){
		fileIndex.put(file);
	}
	
	public FileContent getFileContent(String url){
		return fileIndex.get(url);
	}
	
	public void deleteFileContent(String url){
		fileIndex.delete(url);
	}
	
	public boolean containsFile(String url){
		return fileIndex.contains(url);
	}
	
	// channel functions
	public void addChannel(Channel channel){
		channelIndex.put(channel);
	}
	
	public void addChannelDoc(String channelName,String docUrl){
		Channel channel = channelIndex.get(channelName);
		channel.addMatchedDoc(docUrl);
		channelIndex.put(channel);
	}
	
	public Channel getChannelByName(String channelName){
		return channelIndex.get(channelName);
	}
	
	public void deletChannel(String channelName){
		System.out.println("***********Channel deleted***********");
		channelIndex.delete(channelName);
//		for(String username : userIndex.keys()){
//			userUnsubscribe(username, channelName);
//		}
	}
	
	public boolean containsChannel(String channelName){
		return channelIndex.contains(channelName);
	}
	
	public PrimaryIndex<String,Channel> getChannels(){
		return channelIndex;
	}
	
	public boolean checkOwner(String userName, String channelName){
		Channel channel = channelIndex.get(channelName);
		return channel.getCreatorName().equals(userName);
	}
	
	public static void main(String[] args){
		String DBrootDir = "./database";
		DBWrapper db = new DBWrapper(DBrootDir);
		if(!db.containsChannel("sport")){
			Channel channel = new Channel("sport","xuyimeng","/xpath");
			System.out.println("channel: sport add to db");
			db.addChannel(channel);
		}
	}
}
