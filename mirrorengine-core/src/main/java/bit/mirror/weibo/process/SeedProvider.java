package bit.mirror.weibo.process;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bit.mirror.dao.MirrorEngineDao;
import bit.mirror.data.Seed;
import bit.mirror.weibo.core.Manager;
import bit.mirror.weibo.core.Producer;


/**
 * 为Login提供seed，纯粹的生产者，产生Seed
 * 
 * @author lins
 * @date 2012-6-19
 **/
public class SeedProvider implements Producer<Seed>, Runnable {
	private Logger logger = LoggerFactory.getLogger(SeedProvider.class);
	private MirrorEngineDao dao;// = new MongoDao();
	private Manager manager;

	public Manager getManager() {
		return manager;
	}

	public void setManager(Manager manager) {
		this.manager = manager;
	}

	public MirrorEngineDao getDao() {
		return dao;
	}

	public void setDao(MirrorEngineDao dao) {
		this.dao = dao;
	}

	LinkedBlockingQueue<Seed> seeds;

	/**
	 * 发派种子，每次从队列头部弹出一个种子。由于provider由consumer持有，所以种子的更新交由consumer处理
	 * 
	 * @see bit.mirror.weibo.core.Producer#produce()
	 */
	public Seed produce() {
		if (seeds == null || seeds.size() == 0)
			return null;
		try {
			return seeds.poll(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.error("Interrupted while getting seeds...");
			return null;
		}
	}

	/**
	 * 发派一堆种子，但是不会清空队列
	 * 
	 * @see bit.mirror.weibo.core.Producer#produceMega()
	 */
	public Collection<Seed> produceMega() {
		return seeds;
	}

	/**
	 * 从mongo数据库读入所有seed
	 * 
	 */
	public synchronized void loadSeeds() {
		seeds = new LinkedBlockingQueue<Seed>();
		Iterator<Seed> it = dao.getSeeds().iterator();
		while (it.hasNext()) {
			Seed s = it.next();
			if (s.getType().equals("WEIBO")) {
				seeds.add(s);
				logger.info(
						"Seed provider found a seed whose account is '{}' and password is '{}'",
						s.getAccount(), s.getPassword());
			}
		}
	}

	public void run() {
		loadSeeds();
	}
	
	public void dump(){
		if(seeds!=null){
			seeds.clear();
		}else{
			seeds = new LinkedBlockingQueue<Seed>();
		}
	}
}
