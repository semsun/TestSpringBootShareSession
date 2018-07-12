package com.semsun.redis;

import java.util.Random;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

public class RedisUtil {

	private static RedisTemplate redisTemplate;
	
	public static void init(RedisTemplate redisTemplate) {
		RedisUtil.redisTemplate = redisTemplate;
	}
	
	/**
	 * 获取 Redis 连接
	 * @return
	 */
	private static RedisConnection getRedisConnect() {
        RedisConnection redisConn = null;
		try {
			redisConn = redisTemplate.getConnectionFactory().getConnection();
		} catch (Exception e) {
			System.out.println( "获取连接异常" + e.getMessage() );
			return null;
		}
		
		return redisConn;
	}
	
	/**
	 * 在指定时间范围等待Redis连接
	 * @param timeout
	 * @return
	 */
	public static RedisConnection waitConnect(long timeout) {
		long beginTime = System.currentTimeMillis();
		
		final Random ran = new Random();
		
		long divTime = System.currentTimeMillis() - beginTime;
		
        RedisConnection redisConn = null;
		while( divTime < timeout ) {
			redisConn = getRedisConnect();
			if( null != redisConn ) return redisConn;
			
			try {
				Thread.sleep(3, ran.nextInt(500));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			divTime = System.currentTimeMillis() - beginTime;
		}
		
		return null;
	}
}
