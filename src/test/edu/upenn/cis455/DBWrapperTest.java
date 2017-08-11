package test.edu.upenn.cis455;

import com.sleepycat.persist.PrimaryIndex;

import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.storage.FileContent;
import edu.upenn.cis455.storage.Users;
import junit.framework.TestCase;

public class DBWrapperTest extends TestCase {
	DBWrapper dbWrapper;
	PrimaryIndex<String, Users> userIndex;
	
	protected void setUp() throws Exception {
		String path = "./database";
		dbWrapper = new DBWrapper(path);
		userIndex = dbWrapper.getUserIndex();
	}

	public void testContainFile() {
		FileContent content = new FileContent("www.facebook.com","text/html",
				     "string".getBytes(),System.currentTimeMillis());
		dbWrapper.addFile(content);
		assertTrue(dbWrapper.containsFile("www.facebook.com"));
	}

	public void testGetUser() {
		dbWrapper.addUser("Yimeng", "12345");
		assertEquals(userIndex.get("Yimeng").getUsername(), "Yimeng");
		assertEquals(userIndex.get("Yimeng").getPassword(), "12345");
	}

	public void testCheckPassword() {
		dbWrapper.addUser("Yimeng", "12345");
		assertTrue(dbWrapper.varifyPassword("Yimeng", "12345"));
		assertFalse(dbWrapper.varifyPassword("Yimeng", "123"));
	}
}
