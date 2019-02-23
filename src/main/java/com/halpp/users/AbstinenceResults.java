package com.halpp.users;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.halpp.Authentication;

/**
 * Servlet implementation class AbstinenceResults
 */
@WebServlet("/api/v1/users/abstinence_results")
public class AbstinenceResults extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AbstinenceResults() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		System.out.println("Enter AbstinenceResults.doGet()");
		Connection con = null;
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		JSONObject resObj = new JSONObject();
		JSONArray resArr = new JSONArray();
		JSONArray resMonthArr = new JSONArray();
		
		//TODO: HACK FOR DEV
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "POST");
		response.setHeader("Access-Control-Allow-Methods", "GET");
		
		try {
			
			InitialContext initialContext = new InitialContext();
			javax.sql.DataSource ds = (javax.sql.DataSource) initialContext.lookup("jdbc/db2");
			con = ds.getConnection();
			
			String authToken = request.getParameter("authentication_token");
			if (Authentication.authenticate(authToken)) {
								
				int year = Calendar.getInstance().get(Calendar.YEAR);
				PreparedStatement getResults = con.prepareStatement("SELECT A.USER_ID, B.EMAIL_ADDRESS, COUNT(*) AS DAYS_ABSTAINED"
						+ " FROM HALPP.ABSTINENCE_LOGS AS A"
						+ " JOIN HALPP.USERS AS B ON A.USER_ID = B.USER_ID"
						+ " WHERE YEAR(DATE) = ? AND ABSTAINED = 'Y' GROUP BY A.USER_ID, B.EMAIL_ADDRESS ORDER BY A.USER_ID");
				getResults.setInt(1, year);
				ResultSet resultSet = getResults.executeQuery();
				while (resultSet.next()) {
					JSONObject obj = new JSONObject();
					obj.put("EMAIL", resultSet.getString("EMAIL_ADDRESS"));
					obj.put("DAYS_ABSTAINED", resultSet.getString("DAYS_ABSTAINED"));
					resArr.put(obj);
				} 
				getResults.close();
				resultSet.close();
				
				int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
				PreparedStatement getMonthResults = con.prepareStatement("SELECT A.USER_ID, B.EMAIL_ADDRESS, COUNT(*) AS DAYS_ABSTAINED"
						+ " FROM HALPP.ABSTINENCE_LOGS AS A"
						+ " JOIN HALPP.USERS AS B ON A.USER_ID = B.USER_ID"
						+ " WHERE MONTH(DATE) = ? AND ABSTAINED = 'Y' GROUP BY A.USER_ID, B.EMAIL_ADDRESS ORDER BY A.USER_ID");
				getMonthResults.setInt(1, month);
				ResultSet monthResultSet = getMonthResults.executeQuery();
				while (monthResultSet.next()) {
					JSONObject obj = new JSONObject();
					obj.put("EMAIL", monthResultSet.getString("EMAIL_ADDRESS"));
					obj.put("DAYS_ABSTAINED", monthResultSet.getString("DAYS_ABSTAINED"));
					resMonthArr.put(obj);
				} 
				getMonthResults.close();
				monthResultSet.close();
				
				
				resObj.put("RESULTS", resArr);
				resObj.put("RESULTS_MONTH", resMonthArr);
				out.print(resObj);
				out.flush(); 
				System.out.println("Exit AbstinenceResults.doGet()");
				
				
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
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
