package com.halpp.users;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.halpp.Authentication;

import redis.clients.jedis.Jedis;

/**
 * Servlet implementation class Login
 */
@WebServlet("/api/v1/users/login")
public class Login extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Login() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		Connection con = null;
		Jedis jedis = null;
		
		//TODO: HACK FOR DEV
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "POST");
		
		
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		JSONObject resObj = new JSONObject();
		
		try {
			
			InitialContext initialContext = new InitialContext();
			javax.sql.DataSource ds = (javax.sql.DataSource) initialContext.lookup("jdbc/db2");
			con = ds.getConnection();
			jedis = new Jedis("localhost");
			
			
			String authToken = request.getParameter("authentication_token");
			if (authToken != null && Authentication.authenticate(authToken)) {
				String  emailAddress = jedis.get(authToken + "_EMAIL_ADDRESS");
				resObj.put("EMAIL_ADDRESS", emailAddress);
				resObj.put("AUTHENTICATION_TOKEN", authToken);
				out.print(resObj);
				out.flush(); 
				return;
			} else {
				String emailAddress = request.getParameter("email_address").toLowerCase();
				System.out.println("emailAddress: " + emailAddress);
				String password = request.getParameter("password");
				PreparedStatement getUser = con.prepareStatement("SELECT * FROM HALPP.USERS WHERE EMAIL_ADDRESS = ?");
				getUser.setString(1, emailAddress);
				ResultSet userSet = getUser.executeQuery();
				if (userSet.next()) {
					String dbPassword = userSet.getString("PASSWORD"); 
					int userId = userSet.getInt("USER_ID");
					int chapterId = userSet.getInt("CHAPTER_ID");
					System.out.println("valid email");
					if (validatePassword(password, dbPassword)) {
						System.out.println("valid password");
						authToken = UUID.randomUUID().toString();
					    jedis.set(authToken, userId + "");
					    jedis.set(authToken + "_EMAIL_ADDRESS", emailAddress);
					    jedis.set(authToken + "_CHAPTER_ID", chapterId + "");
					    
					    
						resObj.put("AUTHENTICATION_TOKEN", authToken);
						resObj.put("EMAIL_ADDRESS", emailAddress);
						out.print(resObj);
						out.flush(); 
						return;
						
					} else {
						System.out.println("invalid password");
						resObj.put("ERROR", "Invalid Password");
						out.print(resObj);
						out.flush(); 
						return;
						// Invalid Password
					}
				} else {
					System.out.println("email not found");
					// Invalid User
					return;
				}
			}
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (con != null) con.close();
				if (jedis != null) jedis.close();
			}
			catch (SQLException ignored) { ignored.printStackTrace(); }
		}
		
		
	}
	
	private boolean validatePassword(String input, String dbValue) throws Exception {
		
		String hash = dbValue.split("\\|")[0];
		String salt = dbValue.split("\\|")[1];
		int saltLength = 16;
		
		System.out.println("hash: " + hash);
		System.out.println("salt: " + salt);
		
		MessageDigest m = MessageDigest.getInstance("MD5");
	    m.update((input + salt).getBytes(),0,(input + salt).length());
	    String userHash = new BigInteger(1,m.digest()).toString(saltLength);
	    
	    System.out.println("userHash: " + userHash);
	    
		if (userHash.toString().equals(hash)) {
			return true;
		}
		
		return false;
	}

}
