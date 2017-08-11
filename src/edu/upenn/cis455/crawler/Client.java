package edu.upenn.cis455.crawler;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

import edu.upenn.cis455.crawler.info.URLInfo;

public class Client {
	private String url;
	private String host;
	private String filePath;
	private int portNum;
	
	private String contentType;
	private int contentLength;
	private long lastModified;
	
	public Client(String url){
		this.url = url;
		URLInfo urlinfo = new URLInfo(url);
		this.host = urlinfo.getHostName();
		this.filePath = urlinfo.getFilePath();
		this.portNum = urlinfo.getPortNo();
		this.contentLength = -1;
		this.lastModified = -1;
		this.contentType = "text/html";
	}
	
	public boolean sendHeadRequest(){
		if(url.startsWith("https")){
			try {
				CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
				URL httpsURL = new URL(url);
				HttpsURLConnection urlConnection = (HttpsURLConnection) httpsURL.openConnection();	
				urlConnection.setRequestMethod("HEAD");
				urlConnection.addRequestProperty("User-Agent", "cis455crawler");
				urlConnection.addRequestProperty("Host", host);
				urlConnection.connect();
				int statusCode = urlConnection.getResponseCode();
				if(statusCode >= 200 && statusCode <= 400){
					contentLength = urlConnection.getContentLength();
					lastModified = urlConnection.getLastModified();
					contentType = urlConnection.getContentType();
					return true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if(url.startsWith("http")){
			try {
				Socket socket = new Socket(host,portNum);
				//client send head request to host 
				PrintWriter out = new PrintWriter(socket.getOutputStream());
				out.println("HEAD "+ url + " HTTP/1.1");
				out.println("User-Agent: cis455crawler");
				out.println("Host: " + host);
				out.println();
				out.flush();
				//client read response from host 
				BufferedReader br = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				String initLine = br.readLine();
				System.out.println("initline:"+initLine);
				String[] init = initLine.split(" ");
				int statusCode = Integer.parseInt(init[1]);
				//check if the response is ok
				if(statusCode >= 200 && statusCode <= 400){
					String line = null;
					while((line = br.readLine())!=null && line.length()!=0){
						String[] pairs = line.split(":",2);
						String key = pairs[0].trim().toLowerCase();
						String val = pairs[1].trim();
						if(key.equals("content-length")){
							try{
								contentLength = Integer.parseInt(val);
							}catch(Exception e){
								
							}
						}else if(key.equals("content-type")){
							contentType = val;
						}else if(key.equals("last-modified")){
							SimpleDateFormat dateFormatter = new SimpleDateFormat(
									"EEE, dd MMM yyyy HH:mm:ss z");
							Date date;
							try {
								date = dateFormatter.parse(val);
								lastModified = date.getTime();
							} catch (ParseException e) {
							}
						}
					}
					socket.close();
					return true;
				}
				socket.close();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	public byte[] getUrlContent() throws IOException{
		InputStream is = sendGetRequest();
		if(is == null){
			System.out.println("Inputstream by sending get request is null");
			return null;
		}
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		int bit;
		while(( bit = is.read()) != -1){
			bos.write(bit);
		}
		bos.flush();
		return bos.toByteArray();
	}
	
	public InputStream sendGetRequest(){
		if(url.startsWith("https")){
			try{
				CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
				URL httpsURL = new URL(url);
				HttpsURLConnection connect = (HttpsURLConnection) httpsURL.openConnection();	
				connect.setRequestMethod("GET");
				connect.addRequestProperty("User-Agent", "cis455crawler");
				connect.addRequestProperty("Host", host);
				connect.connect();
				return connect.getInputStream();
				
			}catch(IOException e){
	
			}
		}
		else if(url.startsWith("http")){
			try{
				URL httpURL = new URL(url);
				HttpURLConnection connect = (HttpURLConnection) httpURL.openConnection();	
				connect.setRequestMethod("GET");
				connect.addRequestProperty("User-Agent", "cis455crawler");
				connect.addRequestProperty("Host", host);
				connect.connect();
				return connect.getInputStream();
			}catch(Exception e){
				
			}
		}
		return null;
	}
	public String getContentType(){
		if(contentType.toLowerCase().contains("html")){
			return "html";
		}else if(contentType.toLowerCase().contains("xml")){
			return "xml";
		}
		return contentType;
	}
	
	public long getLastModified() {
		return lastModified;
	}
	
	public int getContentLength() {
		return contentLength;
	}
	
	public String getHostName(){
		return host;
	}
	
	public boolean checkContentSize(int maxSize) {
		if (contentLength > maxSize * 1024 * 1024)
			return false;
		return true;			
	}
	
	public boolean checkContentType(){
		if (!contentType.startsWith("text/xml") && !contentType.startsWith("text/html")
				&& !contentType.startsWith("application/xml") && !contentType.endsWith("+xml"))
			return false;
		else 
			return true;
	}
}
