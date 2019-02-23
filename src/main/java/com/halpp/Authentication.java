package com.halpp;

import redis.clients.jedis.Jedis;

public class Authentication {
	
	public static boolean authenticate(String authToken) {
		Jedis jedis = null;
		try {
			jedis = new Jedis("localhost");
			if (jedis.get(authToken) != null) {
				return true;
			}
			
		}  catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (jedis != null) jedis.close();
		}
		
		return false;
	}
	
	public static Integer getUserId(String authToken) {
		Jedis jedis = null;
		try {
			jedis = new Jedis("localhost");
			if (jedis.get(authToken) != null) {
				return Integer.parseInt(jedis.get(authToken));
			}
			
		}  catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (jedis != null) jedis.close();
		}
		
		return -1;
	}

}
