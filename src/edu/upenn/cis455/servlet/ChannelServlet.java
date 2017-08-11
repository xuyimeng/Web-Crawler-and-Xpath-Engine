package edu.upenn.cis455.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import edu.upenn.cis455.storage.Channel;
import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.storage.FileContent;

@SuppressWarnings("serial")
public class ChannelServlet extends HttpServlet{
	
	public static String DBrootDir = null;
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException 
	{
		System.out.println("In ChannelServlet doGet...");
		String uri = request.getServletPath();
		System.out.println(uri);
		//Initialize DB
		if(DBrootDir == null){
			DBrootDir = getServletConfig().getServletContext().getInitParameter("BDBstore");
		}
		DBWrapper db = new DBWrapper(DBrootDir);
		
		// get Printwriter and session 
		response.setContentType("text/html");
		PrintWriter writer = response.getWriter();
		HttpSession session = request.getSession(false);
		
		if(uri.equals("/create")){
			System.out.println("create channel");
			if(session == null){
				sendErrorPage(writer,401,"No user login in when create new channel");
				return;
			}else{
				String channelName = request.getParameter("name");
				String xpath = request.getParameter("xpath");
				String userName = (String) session.getAttribute("username");
				if(userName == null){
					System.out.println("No user name from session");
				}
				if(db.containsChannel(channelName)){
					sendErrorPage(writer,409,"Channel already exits");
					return;
				}else{
					Channel channel = new Channel(channelName,userName,xpath);
					db.addChannel(channel);
					System.out.println(userName);
					System.out.println(channelName);
					db.userSubscribe(userName, channelName);
					writer.println("<html><body>");
					writer.println("<h3>Channel "+channelName+" added to DB</h3>");
					writer.println("<h3>Creator: "+userName+"</h3>");
					writer.println("<h3>Channel path: "+xpath+"</h3>");
					
				}
			}
		}
		else if(uri.equals("/delete")){
			if(session == null){
				sendErrorPage(writer,401,"No user login in when delete channel");
				return;
			}else{
				String channelName = request.getParameter("name");
				if(!db.containsChannel(channelName)){
					sendErrorPage(writer,404,"No channel with the specified name exists");
					return;
				}else{
					Channel channel = db.getChannelByName(channelName);
					String userName = (String) session.getAttribute("username");
					if(!channel.getCreatorName().equals(userName)){
						sendErrorPage(writer,403,"Login user is different from channel's creator");
						return;
					}else{
						db.deletChannel(channelName);
						writer.println("<html><body>");
						writer.println("<h3>Channel "+channelName+" deleted from DB</h3>");
					
					}
				}
			}
		}
		else if(uri.equals("/subscribe")){
			if(session == null){
				sendErrorPage(writer,401,"No user login in when subscribe channel");
				return;
			}else{
				String channelName = request.getParameter("name");
				if(!db.containsChannel(channelName)){
					sendErrorPage(writer,404,"No channel with the specified name exists");
					return;
				}else{
					Channel channel = db.getChannelByName(channelName);
					String userName = (String) session.getAttribute("username");
					if(db.checkSubscribe(userName, channelName)){
						sendErrorPage(writer,409,"User is already subscribed to channel " + channelName);
						return;
					}else{
						db.userSubscribe(userName, channelName);
						writer.println("<html><body>");
						writer.println("<h3>User: "+userName+" subscribe to Channel:"+channelName+" successfully</h3>");
					}
				}
			}
		}
		else if(uri.equals("/unsubscribe")){
			if(session == null){
				sendErrorPage(writer,401,"No user login in when unsubscribe channel");
				return;
			}else{
				String channelName = request.getParameter("name");
				String userName = (String) session.getAttribute("username");
				if(!db.checkSubscribe(userName, channelName)){
					sendErrorPage(writer,404,"User is not subscribe to channel,can not unsubscribe");
					return;
				}else{
					db.userUnsubscribe(userName, channelName);
					writer.println("<html><body>");
					writer.println("<h3>User: "+userName+" unsubscribe to Channel:"+channelName+" successfully</h3>");
				}
			}
		}
		else if(uri.equals("/show")){
			if(session == null){
				sendErrorPage(writer,401,"No user login in when subscribe channel");
				return;
			}else{
				String channelName = request.getParameter("name");
				if(!db.containsChannel(channelName)){
					sendErrorPage(writer,404,"No channel with the specified name exists");
					return;
				}else{
					String userName = (String) session.getAttribute("username");
					if(!db.checkSubscribe(userName, channelName)){
						sendErrorPage(writer,404,"User is not subscribe to channel,can not show");
						return;
					}else{
						System.out.println("******************In Show******");
						Channel channel = db.getChannelByName(channelName);
						String creator = channel.getCreatorName();
						List<String> matchUrls = channel.getMatchedDoc();
						System.out.println(matchUrls);
						writer.println("<html><body>");
						writer.println("<div class = \"channelheader\">Channel name:"+channelName+",created by "+creator+"</div>");
						if(matchUrls == null || matchUrls.size() == 0){
							writer.println("<div>No document match to channel</div>");
							System.out.println("No matched document for channel path "+channel.getXpath() );
						}else{
							for(String url : matchUrls){
								System.out.println("Find match url for channel:" + channelName+ " "+url);
								FileContent file = db.getFileContent(url);
								long dateValue = file.getLastCrawled();
								Date date = new Date(dateValue);
								SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
								String dateText = sdf.format(date);
								writer.println("Crawled on:"+dateText);
								writer.println("Location:"+url);
								writer.println("<div class=\"document\">");
								writer.println("<a href=\""+file.getUrl()+"\">"+file.getUrl()+"</a><br>");
								writer.println("</div>");
							}
						}
						
					}
				}
			}
		}
		writer.println("<a href="+request.getContextPath()+"/>Back to home</a>");
		writer.println("</body></html>");
		writer.flush();
		writer.close();
	}
	
	
	public void sendErrorPage(PrintWriter writer, int code, String msg){
		writer.println("<html><body>");
		writer.println("<h2>Error: "+code+" </h2>");
		writer.println("<h3>Message: "+msg+"</h3>");
		writer.println("</body></html>");
		writer.flush();
		writer.close();
	}

}
