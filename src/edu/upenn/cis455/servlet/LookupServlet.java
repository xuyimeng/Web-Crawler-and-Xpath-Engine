package edu.upenn.cis455.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.storage.FileContent;

@SuppressWarnings("serial")
public class LookupServlet extends HttpServlet{

	public static String DBrootDir = null;
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException 
	{
		System.out.println("In LookupServlet doGet...");
		
		if(DBrootDir == null){
			DBrootDir = getServletConfig().getServletContext().getInitParameter("BDBstore");
		}
		
		DBWrapper db = new DBWrapper(DBrootDir);
		String url = request.getParameter("url");
		System.out.println("url get:"+url);
	
		if(!db.containsFile(url)){
			response.setContentType("text/html");
			PrintWriter writer = response.getWriter();
			writer.println("<html><body>");
			writer.println("<h2>Look up Page</h2>");
			writer.println("<h3>Database does not contain "+url+" file</h3>");
			writer.println("</body></html>");
			writer.flush();
			writer.close();
		}else{
			FileContent fileContent = db.getFileContent(url);
			response.setContentType(fileContent.getType());
			response.setContentLength(fileContent.getRawContent().length);
			ServletOutputStream os = response.getOutputStream();
			os.write(fileContent.getRawContent());
		}
	}
}
