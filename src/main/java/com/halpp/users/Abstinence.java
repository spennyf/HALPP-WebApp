package com.halpp.users;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.halpp.Authentication;

/**
 * Servlet implementation class Abstinence
 */
@WebServlet("/api/v1/users/abstinence")
public class Abstinence extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Abstinence() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Get if user did or did not abstain or a specific day
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		Connection con = null;
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		JSONObject resObj = new JSONObject();
		
		//TODO: HACK FOR DEV
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "PUT");
		response.setHeader("Access-Control-Allow-Methods", "POST");
		response.setHeader("Access-Control-Allow-Methods", "GET");
		
		try {
			
			InitialContext initialContext = new InitialContext();
			javax.sql.DataSource ds = (javax.sql.DataSource) initialContext.lookup("jdbc/db2");
			con = ds.getConnection();
			
			String authToken = request.getParameter("authentication_token");
			if (Authentication.authenticate(authToken)) {
								
				Integer userId = Authentication.getUserId(authToken);
				String date = request.getParameter("date");
				
				PreparedStatement getYesterdayLog = con.prepareStatement("SELECT * FROM HALPP.ABSTINENCE_LOGS"
						+ " WHERE USER_ID = ? AND DATE = ?");
				getYesterdayLog.setInt(1, userId);
				getYesterdayLog.setString(2, date);
				ResultSet yesterdayLogSet = getYesterdayLog.executeQuery();
				if (yesterdayLogSet.next()) {
					resObj.put("DATE", date);
					resObj.put("ABSTAINED", yesterdayLogSet.getString("ABSTAINED"));
				} else {
					resObj.put("DATE", date);
					resObj.put("ABSTAINED", "");
				}
				out.print(resObj);
				out.flush(); 
				
				
			} else {
				//not authenticated
				System.out.println("user not authenticated");
			}
	
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (con != null) con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 * Save if user did or did not abstain or a specific day
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		System.out.println("Enter Abstinence.doPost()");
		
		Connection con = null;
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		JSONObject resObj = new JSONObject();
		
		//TODO: HACK FOR DEV
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "POST");
		response.setHeader("Access-Control-Allow-Methods", "GET");
		
		try {
			
			InitialContext initialContext = new InitialContext();
			javax.sql.DataSource ds = (javax.sql.DataSource) initialContext.lookup("jdbc/db2");
			con = ds.getConnection();
			
			String authToken = request.getParameter("authentication_token");
			System.out.println("authToken: " + authToken);
			
			if (Authentication.authenticate(authToken)) {
				Integer userId = Authentication.getUserId(authToken);
				String date = request.getParameter("date");
				String abstained = request.getParameter("abstained");
				if (abstained.equals("Y") || abstained.equals("N")) {
					PreparedStatement insertAbstinenceLog = con.prepareStatement("INSERT INTO HALPP.ABSTINENCE_LOGS"
							+ " (USER_ID, DATE, ABSTAINED) VALUES (?,?,?)");
					insertAbstinenceLog.setInt(1, userId);
					insertAbstinenceLog.setString(2, date);
					insertAbstinenceLog.setString(3, abstained);
					insertAbstinenceLog.execute();
				} else {
					PreparedStatement deleteAbstinenceLog = con.prepareStatement("DELETE FROM HALPP.ABSTINENCE_LOGS "
							+ " WHERE USER_ID = ? AND DATE = ?");
					deleteAbstinenceLog.setInt(1, userId);
					deleteAbstinenceLog.setString(2, date);
					deleteAbstinenceLog.execute();
				}
				
				
				
				
				resObj.put("ABSTAINED", abstained);
				out.print(resObj);
				out.flush(); 
				
				
			} else {
				//not authenticated
				System.out.println("user not authenticated");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (con != null) con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		
		
		
	}
	
	
}
