package com.semsun.spring.TestSpringBootShareSession;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.semsun.redis.RedisLock;
import com.semsun.redis.RedisUtil;

/**
 * Hello world!
 *
 */
@Controller
@EnableAutoConfiguration
public class App
{
	
	@Autowired
	private RedisTemplate<String, Integer> redisTemplate;
	
	@RequestMapping("redisTest")
	@ResponseBody
	public String redisTest(HttpServletRequest request) throws Exception {
		
		Lock lock = RedisLock.getInstance("lock1", redisTemplate);
		RedisUtil.init(redisTemplate);
		int val = 0;
		RedisConnection conn = RedisUtil.waitConnect(10 * 1000);
		try {
			if( lock.tryLock(10, TimeUnit.SECONDS) ) {
				try{
					val = Integer.parseInt( new String(conn.get("test0001".getBytes())) );
				} catch(Exception e) {
					e.printStackTrace();
					val = 0;
				}
				val++;
				conn.set("test0001".getBytes(), String.format("%d", val).getBytes());
				conn.close();
				conn = null;
				lock.unlock();
			} else {
				val = -11111;
				throw new Exception("Can't lock");
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println( "锁定失败！" + e.getMessage() );
		}
		
		if( null != conn && !conn.isClosed() ) conn.close();
//		redisTemplate.delete("test0001");
		String ret = String.format("{\"message\": \"Redis Test[%d]\"}", val);
		return ret;
	}
	
	@RequestMapping("redisIncrby")
	@ResponseBody
	public String redisIncrby(HttpServletRequest request, Double val) {
//		Double val = (Double) request.getAttribute("val");
		if( null == val ) {
			return String.format( "{\"message\": \"Failed of get value\"}" );
		}
		RedisConnection redisCnt = redisTemplate.getConnectionFactory().getConnection();
		Double result = redisCnt.incrBy("rank".getBytes(), val.doubleValue());
		String ret = null;
		
		if( result < 0 ) {
			redisCnt.incrBy("rank".getBytes(), val.doubleValue() * -1);

			ret = String.format("{\"message\": \"Failed [%f, %f]\"}", val.doubleValue(), result.doubleValue());
		} else {
			ret = String.format("{\"message\": \"Successful [%f, %f]\"}", val.doubleValue(), result.doubleValue());
		}
		
		redisCnt.close();
		
		return ret;
	}
	
	@RequestMapping("redisDesValue")
	@ResponseBody
	public String redisDesValue(HttpServletRequest request) {
		long curTime = System.currentTimeMillis();
		double val = Math.random();
		long divTime = curTime & 0x000000ff;
		long res = Math.round(divTime * val) * -1;
		
		long result = redisTemplate.getConnectionFactory().getConnection().incrBy("rank".getBytes(), res);
		String ret = null;
		
		if( result < 0 ) {
			redisTemplate.getConnectionFactory().getConnection().incrBy("rank".getBytes(), res * -1);

			ret = String.format("{\"message\": \"Failed [%d, %d]\"}", res, result);
		} else {
			ret = String.format("{\"message\": \"Successful [%d, %d]\"}", res, result);
		}
		
		return ret;
	}
	
	@RequestMapping("redisAddValue")
	@ResponseBody
	public String redisAddValue(HttpServletRequest request) {
		long curTime = System.currentTimeMillis();
		double val = Math.random();
		long divTime = curTime & 0x000000ff;
		long res = Math.round(divTime * val);
		
		long result = redisTemplate.getConnectionFactory().getConnection().incrBy("rank".getBytes(), res);
		String ret = String.format("{\"message\": \"Add[%d] Current [%d]\"}", res, result);
		
		return ret;
	}
	
	@RequestMapping("redisGetLock")
	@ResponseBody
	public String redisGetLock(HttpServletRequest request) {
		
		boolean result = redisTemplate.getConnectionFactory().getConnection().setNX("lock".getBytes(), "TRUE".getBytes());
		String ret = null;
		
		if( result ) {
			ret = String.format("{\"message\": \"Success\"}");
		} else {
			ret = String.format("{\"message\": \"Failed\"}");
		}
		
		return ret;
	}
	
	@RequestMapping("increace")
	@ResponseBody
	public String increace() {
		redisTemplate.getConnectionFactory().getConnection().incr("test0001".getBytes());
		return "";
	}
	
	@RequestMapping("session")
	@ResponseBody
	public String session(HttpServletRequest request) {
		return request.getSession().getId();
	}
	
    public static void main( String[] args )
    {
//    	SpringApplication.run(App.class, args);
    	SpringApplication app = new SpringApplication(App.class);
		app.setWebEnvironment(true);
		Set<Object> set = new HashSet<Object>();  
        set.add("classpath:applicationContext.xml");  
        app.setSources(set);  
        app.run(args);
        System.out.println( "-------------------- Redis Test Server Started ------------------" );
    }

//	@Override  implements EmbeddedServletContainerCustomizer
//	public void customize(ConfigurableEmbeddedServletContainer container) {
//		// TODO Auto-generated method stub
//		container.setPort(8090);
//	}
}
