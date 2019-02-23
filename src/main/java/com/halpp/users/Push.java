package com.halpp.users;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.GetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.GetEndpointAttributesResult;
import com.amazonaws.services.sns.model.NotFoundException;
import com.amazonaws.services.sns.model.SetEndpointAttributesRequest;
import com.halpp.Authentication;

/**
 * Servlet implementation class Push
 */
@WebServlet("/api/v1/users/push")
public class Push extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Push() {
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
			con.setAutoCommit(false);
			
			String authToken = request.getParameter("authentication_token");
			
			if (Authentication.authenticate(authToken)) {
				Integer userId = Authentication.getUserId(authToken);
				String registrationId = request.getParameter("registrationId");	
				String deviceType = request.getParameter("deviceType");	
								
				String userArn = null;
				PreparedStatement getArn = con.prepareStatement("SELECT * FROM HALPP.USERS WHERE USER_ID = ?");
				getArn.setInt(1, userId);
				ResultSet userArnSet = getArn.executeQuery();
				if (userArnSet.next()) {
					userArn = userArnSet.getString("ENDPOINT_ARN");
				}
				
				BasicAWSCredentials creds = new BasicAWSCredentials("***", "***"); 
				AmazonSNS client = AmazonSNSClientBuilder.standard()
					.withCredentials(new AWSStaticCredentialsProvider(creds))
					.withRegion(Regions.US_EAST_1).build(); 
				
				if (userArn != null) {
					boolean updateNeeded = false;
					try {
						GetEndpointAttributesRequest geaReq = new GetEndpointAttributesRequest().withEndpointArn(userArn);
					    GetEndpointAttributesResult geaRes = client.getEndpointAttributes(geaReq);
					      
					    updateNeeded = !geaRes.getAttributes().get("Token").equals(registrationId) || !geaRes.getAttributes().get("Enabled").equalsIgnoreCase("true");
					    
					    if (updateNeeded) {
					    		System.out.println("updateNeeded: " + updateNeeded);
						    	Map<String,String> attribs = new HashMap<String, String>();
						    	attribs.put("Token", registrationId);
						    	attribs.put("Enabled", "true");
						    	SetEndpointAttributesRequest saeReq = new SetEndpointAttributesRequest().withEndpointArn(userArn).withAttributes(attribs);
						    	client.setEndpointAttributes(saeReq);
					    }
					} catch (NotFoundException nfe) {
						CreatePlatformEndpointRequest createReq = new CreatePlatformEndpointRequest();
						if (deviceType.equals("iOS")) {
							createReq.setPlatformApplicationArn("arn:aws:sns:us-east-1:286614459112:app/APNS/HALPP");
						} else if (deviceType.equals("Android")) {
							createReq.setPlatformApplicationArn("arn:aws:sns:us-east-1:712515477573:app/GCM/ngDesk");
		
						}
						createReq.setToken(registrationId);
						CreatePlatformEndpointResult createRes = client.createPlatformEndpoint(createReq);
						userArn = createRes.getEndpointArn();
					}
				} else {
					CreatePlatformEndpointRequest createReq = new CreatePlatformEndpointRequest();
					if (deviceType.equals("iOS")) {
						createReq.setPlatformApplicationArn("arn:aws:sns:us-east-1:286614459112:app/APNS/HALPP");
					} else if (deviceType.equals("Android")) {
						createReq.setPlatformApplicationArn("arn:aws:sns:us-east-1:712515477573:app/GCM/ngDesk");
	
					}
					createReq.setToken(registrationId);
					CreatePlatformEndpointResult createRes = client.createPlatformEndpoint(createReq);
					userArn = createRes.getEndpointArn();
				}
				
				PreparedStatement insertAbstinenceLog = con.prepareStatement("UPDATE HALPP.USERS"
						+ " SET PUSH_TOKEN = ?, ENDPOINT_ARN = ? WHERE USER_ID = ?");
				insertAbstinenceLog.setString(1, registrationId);
				insertAbstinenceLog.setString(2, userArn);
				insertAbstinenceLog.setInt(3, userId);
				insertAbstinenceLog.execute();
				
				con.commit();
				
				resObj.put("REGISTRTION_TOKEN", registrationId);
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
