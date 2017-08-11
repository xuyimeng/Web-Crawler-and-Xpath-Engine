package edu.upenn.cis455.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.http.*;
import javax.servlet.*;

import com.sleepycat.persist.PrimaryIndex;

import edu.upenn.cis455.storage.Channel;
import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.storage.Users;

@SuppressWarnings("serial")
public class XPathServlet extends HttpServlet {
	
	/* TODO: Implement user interface for XPath engine here */
	
	/* You may want to override one or both of the following methods */
	public static String dbDir = null;
	DBWrapper db;
	
//	@Override
//	public void init(ServletConfig config) throws ServletException{
//		super.init(config);
//		String dbDir= config.getServletContext().getInitParameter("BDBstore");
//		db = new DBWrapper(dbDir);
//	}
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws IOException
    {
		/* TODO: Implement user interface for XPath engine here */
		System.out.println("In XpathServlet doPost...");
		if(dbDir == null){
			dbDir = getServletConfig().getServletContext().getInitParameter("BDBstore");
		}
		//initialize db wrapper
		System.out.println(dbDir);
		DBWrapper db = new DBWrapper(dbDir);
		String uri = request.getServletPath();
		
		if(uri.equals("/register.jsp")){
			String username = request.getParameter("username");
			String password = request.getParameter("password");
			String type = request.getParameter("type");
			
			if(type.equals("login")){
				System.out.println("Register with login...");
				if(!db.containsUser(username)){
					PrintWriter writer;
					try {
						writer = response.getWriter();
						writer.println("<html><body><h3>");
						writer.println("The username does't exist<br>");
						writer.println("<a href=" + request.getContextPath() + "/signup> Sign Up </a>");
						writer.println("</h3></body></html>");
						writer.flush();
						writer.close();
						db.closeDB();
					}catch (IOException e) {
						e.printStackTrace();
					}
				}else if(!db.varifyPassword(username,password)){
					PrintWriter writer;
					try {
						writer = response.getWriter();
						writer.println("<html><body><h3>");
						writer.println("The password is incorrect<br>");
						writer.println("<a href=" + request.getContextPath() + "/login> Try Again </a>");
						writer.println("</h3></body></html>");
						writer.flush();
						writer.close();
						db.closeDB();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}else{
					HttpSession session = request.getSession();
					session.setAttribute("username", username);
					db.closeDB();
					response.sendRedirect(request.getContextPath()+"/xpath");
				}
			}
			else if(type.equals("signup")){
				System.out.println("Register with signup...");
				if(db.containsUser(username)){
					PrintWriter writer = response.getWriter();
					writer.println("<html><body><h3>");
					writer.println("This username already exists!<br>");
					writer.println("<a href=" + request.getContextPath() + "/signup> Try Again </a>");
					writer.println("</h3></body></html>");
					writer.flush();
					writer.close();
					db.closeDB();
				}else{
					db.addUser(username, password);
					db.syncDB();
					db.closeDB();
					response.sendRedirect(request.getContextPath()+"/xpath");
				}
			}
		}
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException 
	{
		System.out.println("In XpathServlet doGet...");
		
		if(dbDir == null){
			dbDir = getServletConfig().getServletContext().getInitParameter("BDBstore");
		}
		//initialize db wrapper
		System.out.println("Initialize db at:"+dbDir);
		DBWrapper db = new DBWrapper(dbDir);

		/* TODO: Implement user interface for XPath engine here */
		String uri = request.getServletPath();
		System.out.println(uri);
		if(uri.equals("/")||uri.equals("/xpath")){
			System.out.println("In homepage");
			HttpSession session = request.getSession(false);
			if(session == null){
				//no user sessions before, 2 condition : login/sign up
				//display home page with two buttons lead to each url
				response.setContentType("text/html");
				PrintWriter writer = response.getWriter();
				writer.println("<html><body>");
				writer.println("<h3>Home Page</h3>");
				writer.println("<a href="+request.getContextPath()+"/login>Log in</a><br/>");
				writer.println("<a href="+request.getContextPath()+"/signup>Create a new account</a>");
				writer.println("</body></html>");
				writer.flush();
				writer.close();
			}else{
				//user has already logged in, present a page showing user's name
				String userName = (String) session.getAttribute("username");
				Users user = db.getUser(userName);
				System.out.println("In servlet: " + user == null);
				PrimaryIndex<String, Channel> channels = db.getChannels();
				response.setContentType("text/html");
				PrintWriter writer = response.getWriter();
				writer.println("<html><body>");
				writer.println("<h2>Home Page</h2>");
				writer.println("<h3>Hi! "+userName+"</h3>");
				writer.println("<h3>Channels in DB:</h3>");
				
				for(String channelName : channels.keys()){
					writer.println("<p>");
					writer.print(channelName);
					if(!user.hasSubscribed(channelName)){
						writer.println(" <a href="+request.getContextPath()+"/subscribe?name="+channelName+">subscribe</a>");
					}else{
						writer.println(" <a href="+request.getContextPath()+"/unsubscribe?name="+channelName+">unsubscribe</a>");
						writer.println(" <a href="+request.getContextPath()+"/show?name="+channelName+">show</a>");
					}
					if(db.checkOwner(userName, channelName)){
						writer.println(" <a href="+request.getContextPath()+"/delete?name="+channelName+">delete</a>");
					}
					writer.println("</p>");
				}
				writer.println("<h3>Create new channel:</h3>");
				writer.println("<form method=\"get\" action=\"create\">");
				writer.println("Channel Name: ");
				writer.println("<input type=\"text\" name = \"name\"><br/>");
				writer.println("Xpath: ");
				writer.println("<input type=\"text\" name = \"xpath\"><br/>");
				writer.println("<input type=\"submit\" value=\"Create Channel\"><br>");
				writer.println("<a href="+request.getContextPath()+"/logout>Log out</a>");
				writer.println("</body></html>");
				writer.flush();
				writer.close();
			}
		}
		else if(uri.equals("/register.jsp")){
			response.setContentType("text/html");
			PrintWriter writer = response.getWriter();
			writer.println("<html><body>");
			writer.println("<h3>Register with username and password</h3>");
			writer.println("<form method=\"post\" action=\"register.jsp\"");
			writer.println("<p>username:</p>");
			writer.println("<input type=\"text\" name = \"username\"><br/>");
			writer.println("<p>password:</p>");
			writer.println("<input type=\"text\" name = \"password\"><br/>");
			writer.println("</form></body></html>");
			writer.flush();
			writer.close();
		}
		else if(uri.equals("/logout")){
			HttpSession session = request.getSession(false);
			if (session != null)
				session.invalidate();
			response.sendRedirect(request.getContextPath() + "/xpath");
		}
		else if(uri.equals("/login")){
			//no user sessions before, present login page
			response.setContentType("text/html");
			PrintWriter writer = response.getWriter();
			writer.println("<html><body>");
			writer.println("<h3>Log in with username and password</h3>");
			writer.println("<form action=\"register.jsp?type=login\" method=\"post\">");
			writer.println("Username: <br>");
			writer.println("<input type=\"text\" name=\"username\"><br>");
			writer.println("Password: <br>");
			writer.println("<input type=\"text\" name=\"password\"><br>");
			writer.println("<input type=\"submit\" value=\"Submit\">");
			writer.println("</form></body></html>");
			writer.flush();
			writer.close();
		}
		else if(uri.equals("/signup")){
			response.setContentType("text/html");
			PrintWriter writer = response.getWriter();
			writer.println("<html><body>");
			writer.println("<h3>Sign up with username and password</h3>");
			writer.println("<form action=\"register.jsp?type=signup\" method=\"post\">");
			writer.println("Username: <br>");
			writer.println("<input type=\"text\" name=\"username\"><br>");
			writer.println("Password: <br>");
			writer.println("<input type=\"text\" name=\"password\"><br>");
			writer.println("<input type=\"submit\" value=\"Submit\">");
			writer.println("</form></body></html>");
			writer.flush();
			writer.close();
		}
	}

}









