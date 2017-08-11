package test.edu.upenn.cis455;

import edu.upenn.cis455.storage.Users;
import junit.framework.TestCase;

public class UserTest extends TestCase {

	Users user;
	protected void setUp() throws Exception {	
		user = new Users();
	}

	public void testSetUsername() {
		user.setUsername("helen");
		assertEquals(user.getUsername(), "helen");
	}

	public void testSetPassword() {
		user.setPassword("123");
		assertEquals(user.getPassword(), "123");
		user.setPassword("abc");
		assertFalse(user.getPassword().equals("123"));
		assertTrue(user.getPassword().equals("abc"));
	}

}
