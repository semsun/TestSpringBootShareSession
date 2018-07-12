package com.semsun.redis;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.apache.log4j.Logger;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

public class RedisLock implements Lock {
	
	private static final Logger logger = Logger.getLogger(RedisLock.class);
	
	private RedisTemplate redisTemplate;
	
	private static final String LOCKED = "TRUE";
	
	public static final long TIME_OUT = 30000;
	
	public static final int EXPIRE = 60;
	
	private String key;

    private volatile boolean locked = false;
	
//	private static ConcurrentMap<String, RedisLock> map = new ConcurrentHashMap<String, RedisLock>();
	
	private static RedisLock instance = null;
	
	public RedisLock(String key, RedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
		
		this.key = String.format("_LOCK_%s", key);
	}
	
	public static RedisLock getInstance(String key, RedisTemplate redisTemplate) {
//        return map.getOrDefault(key, new RedisLock(key, redisTemplate));
		if( null == instance ) {
			instance = new RedisLock(key, redisTemplate);
		}
		
		return instance;
    }
	
	/**
	 * 获取 Redis 连接
	 * @return
	 */
	private RedisConnection getRedisConnect() {
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
	public RedisConnection waitConnect(long timeout) {
		long beginTime = System.currentTimeMillis();
		
		final Random ran = new Random();
		
		long divTime = System.currentTimeMillis() - beginTime;
		
        RedisConnection redisConn = null;
		while( divTime < timeout ) {
			redisConn = this.getRedisConnect();
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
	
	private boolean lockOpt(long timeout) {
		long nano = System.currentTimeMillis();
        timeout *= 1000;
        final Random r = new Random();
        RedisConnection redisConn = this.waitConnect(timeout);
        
        if( null == redisConn ) return false;
        
    	long dav = System.currentTimeMillis() - nano;
        while (dav < timeout) {
        	redisConn.incr( "lock_times".getBytes() );
            if (redisConn.setNX(key.getBytes(), LOCKED.getBytes())) {
//                redisTemplate.expire(key, EXPIRE, TimeUnit.SECONDS);
                redisConn.expire(key.getBytes(), EXPIRE);
                logger.debug("add RedisLock[" + key + "].");
                System.out.println("add RedisLock[" + key + "].");
                
                redisConn.close();
                return true;
            }
            try {
				Thread.sleep(3, r.nextInt(500));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				System.out.println("Sleep 失败!");
			}
            dav = System.currentTimeMillis() - nano;
        }
		
//        return false;
//		RedisConnection redisConn = redisTemplate.getConnectionFactory().getConnection();
//		if ( redisConn.setNX(key.getBytes(), LOCKED.getBytes()) ) {
//			redisConn.expire(key.getBytes(), EXPIRE);
//			locked = true;
//			logger.debug("add RedisLock[" + key + "].");
//			System.out.println("add RedisLock[" + key + "].");
//			redisConn.close();
//			return true;
//		}
		
        if( null != redisConn && !redisConn.isClosed() ) redisConn.close();
		return false;
	}

	@Override
	public void lock() {
		// TODO Auto-generated method stub
		lockOpt(TIME_OUT);
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Condition newCondition() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean tryLock() {
		// TODO Auto-generated method stub
		return lockOpt(TIME_OUT);
//		return false;
	}

	@Override
	public boolean tryLock(long time, TimeUnit unit)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return lockOpt(unit.toSeconds(time));
//		return false;
	}

	@Override
	public void unlock() {
		// TODO Auto-generated method stub
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub

		        logger.debug("release RedisLock[" + key + "].");
		        System.out.println("release RedisLock[" + key + "].");
//		        redisTemplate.delete(key);
		        RedisConnection redisConn = RedisUtil.waitConnect(10000);
		        if( null == redisConn ) return;
		        
		        long timeout = 10;
				long nano = System.currentTimeMillis();
		        timeout *= 1000000;
		        final Random r = new Random();

		    	long dav = System.currentTimeMillis() - nano;
		        while (dav < timeout) {
		            long del = redisConn.del(key.getBytes());
		            if( del > 0 ) {
		                logger.debug(String.format("release RedisLock[%s] OK", key));
		                System.out.println(String.format("release RedisLock[%s] OK", key));
		            	locked = false;
		            	break;
		            }
		            logger.debug(String.format("release RedisLock[%s] Failed", key));
		            System.out.println(String.format("release RedisLock[%s] Failed", key));
		            
		            try {
						Thread.sleep(3, r.nextInt(500));
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		            
		            dav = System.currentTimeMillis() - nano;
		        }
		        
		        if( null != redisConn && !redisConn.isClosed() ) redisConn.close();
			}
		});
		
		System.out.println("Start Thread to unlock");
		thread.start();
	}

}
