package com.halpp.users;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import redis.clients.jedis.Jedis;

/**
 * Servlet implementation class Signup
 */
@WebServlet("/api/v1/users/signup")
public class Signup extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZqwertyuiopasdfghjklzxcvbnm0123456789";


       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Signup() {
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
		try {
			
			InitialContext initialContext = new InitialContext();
			javax.sql.DataSource ds = (javax.sql.DataSource) initialContext.lookup("jdbc/db2");
			con = ds.getConnection();
			con.setAutoCommit(false);
			
			StringBuilder buffer = new StringBuilder();
		    BufferedReader reader = request.getReader();
		    String line;
		    while ((line = reader.readLine()) != null) {
		        buffer.append(line);
		    }
		    String data = buffer.toString();
		    JSONObject obj = new JSONObject(data);
		    
		    
		    int chapterId = -1;
		    if (obj.getString("CHAPTER_NAME") != null) {
		    		PreparedStatement insertChapter = con.prepareStatement("INSERT INTO HALPP.CHAPTERS (CHAPTER_NAME, DATE_CREATED,ABSTAIN_GOAL) VALUES (?,?,?)", 
		    				PreparedStatement.RETURN_GENERATED_KEYS);
				insertChapter.setString(1, obj.getString("CHAPTER_NAME"));
				insertChapter.setTimestamp(2, new Timestamp(new Date().getTime()));
				insertChapter.setString(3, obj.getString("ABSTAIN_GOAL"));
				insertChapter.execute();
				ResultSet chapterKeys = insertChapter.getGeneratedKeys();
				if (chapterKeys != null && chapterKeys.next()) {
					chapterId = chapterKeys.getInt(1);
				}
		    } else {
		    		chapterId = obj.getInt("CHAPTER_ID");
		    }
			
			String password = obj.getString("PASSWORD");
			String salt = genSalt();
			String passAndSalt = password + salt;
			
			MessageDigest m = MessageDigest.getInstance("MD5");
		    m.update(passAndSalt.getBytes(),0,passAndSalt.length());
		    String hashedPass = new BigInteger(1,m.digest()).toString(16);
			
		    String dbPass = hashedPass + "|" + salt;
		    
			PreparedStatement insertUser = con.prepareStatement("INSERT INTO HALPP.USERS (EMAIL_ADDRESS, PASSWORD, DATE_CREATED, CHAPTER_ID, USER_UUID) VALUES (?,?,?,?)", 
					PreparedStatement.RETURN_GENERATED_KEYS);
			insertUser.setString(1, obj.getString("EMAIL_ADDRESS").toLowerCase());
			insertUser.setString(2, dbPass);
			insertUser.setTimestamp(3, new Timestamp(new Date().getTime()));
			insertUser.setInt(4, chapterId);
			insertUser.setString(5, UUID.randomUUID().toString());
			insertUser.execute();
			ResultSet userKeys = insertUser.getGeneratedKeys();
			int userId = -1;
			if (userKeys != null && userKeys.next()) {
				userId = userKeys.getInt(1);
			}
			
			
			jedis = new Jedis("localhost");
			String authToken = UUID.randomUUID().toString();
		    jedis.set(authToken, userId + "");
		    jedis.set(authToken + "_EMAIL_ADDRESS", obj.getString("EMAIL_ADDRESS"));
		    jedis.set(authToken + "_CHAPTER_ID", chapterId + "");
//		    jedis.set(authToken + "_CHAPTER_NAME", obj.getString("CHAPTER_NAME"));
			
		    con.commit();
		    
		    response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			JSONObject resObj = new JSONObject();
			resObj.put("AUTHENTICATION_TOKEN", authToken);
			out.print(resObj);
			out.flush();
			
			
		} catch (Exception e) {
			e.printStackTrace();
			try {
				con.rollback();
			} catch (SQLException ignored) { }
		} finally {
			try {
				if (con != null) con.close();
				if (jedis != null) jedis.close();
			}
			catch (SQLException ignored) { }
	    }
		
		
	}
	
	public static String genSalt() {
		Random random = new Random();
		StringBuilder builder = new StringBuilder(16);
		for (int i = 0; i < 16; i++) {
			builder.append(ALPHA_NUMERIC_STRING.charAt(random.nextInt(ALPHA_NUMERIC_STRING.length())));
		}
		return builder.toString();

	}

}
