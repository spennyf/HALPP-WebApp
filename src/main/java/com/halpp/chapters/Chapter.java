package com.halpp.chapters;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

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
 * Servlet implementation class Chapter
 */
@WebServlet("/api/v1/chapters")
public class Chapter extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Chapter() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		Connection con = null;
		
		//TODO: HACK FOR DEV
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "POST");
		response.setHeader("Access-Control-Allow-Methods", "GET");
		
		try {
			InitialContext initialContext = new InitialContext();
			javax.sql.DataSource ds = (javax.sql.DataSource) initialContext.lookup("jdbc/db2");
			con = ds.getConnection();
			
			PreparedStatement getChapters = con.prepareStatement("SELECT * FROM HALPP.CHAPTERS");
			ResultSet chaptersSet = getChapters.executeQuery();
			JSONArray json = new JSONArray();
			ResultSetMetaData rsmd = chaptersSet.getMetaData();
			int numColumns = rsmd.getColumnCount();
			while(chaptersSet.next()) {
				JSONObject obj = new JSONObject();
				for (int i=1; i <= numColumns; i++) {
					String column_name = rsmd.getColumnName(i);
					obj.put(column_name, chaptersSet.getObject(column_name));
				}
				json.put(obj);
			}
			
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			out.print(json);
			out.flush();
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (con != null) con.close();
			}
			catch (SQLException ignored) { }
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
