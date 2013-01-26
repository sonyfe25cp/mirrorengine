package bit.mirror.weibo.core;

import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

import bit.mirror.dao.MirrorEngineDao;
import bit.mirror.dao.mongo.MongoDao;
import bit.mirror.weibo.process.Fetcher;
import bit.mirror.weibo.process.Login;
import bit.mirror.weibo.process.RedundancyFilter;
import bit.mirror.weibo.process.Saver;
import bit.mirror.weibo.process.SeedProvider;
import bit.mirror.weibo.process.Fetcher.FetchThread;

/**
 * 管理所有的processor的一个类。持有爬虫的各个模块，线程池，并且管理着他们.
 * 
 * @author lins
 * @date 2012-6-21
 **/
public class Manager {
	/*** 提供seed ***/
	public SeedProvider seedProvider;
	/*** 根据帐号获得登录cookie ***/
	public Login weiboLogin;
	/*** 爬取微博 ***/
	public Fetcher weiboFetcher;
	/*** 去重 ***/
	public RedundancyFilter redundancyFilter;
	/*** 入库 ***/
	public Saver weiboSaver;
	/*** 各种标志位 ***/
	private boolean loadWithoutExceptions = false;
	private boolean pauseWithoutExceptions = false;
	private boolean stopWithoutExceptions = false;
	private boolean goonWithoutExceptions = false;
	/*** dao ***/
	MirrorEngineDao dao;
	/*** 整个crawler的线程池 ***/
	public static ThreadPoolExecutor exec;
//	= (ThreadPoolExecutor) Executors
//			.newCachedThreadPool();
//	static {
//		exec.setCorePoolSize(50);
//		exec.setMaximumPoolSize(80);
//	}

	public MirrorEngineDao getDao() {
		return dao;
	}

	public void setDao(MirrorEngineDao dao) {
		this.dao = dao;
	}

	/**
	 * 纯粹的初始化，至于由于错误调用导致的错误完全留给持有manager的crawler处理
	 */
	public void init() {
		exec = (ThreadPoolExecutor) Executors
				.newCachedThreadPool();
		exec.setCorePoolSize(50);
		exec.setMaximumPoolSize(80);
		
		// dao.setMongo(mongo);
		if (dao == null) {
			Mongo mongo;
			try {
				mongo = new Mongo("10.1.0.171", 27017);
				Morphia morphia = new Morphia();
				Datastore datastore = morphia.createDatastore(mongo, "genius");
				dao = new MongoDao();
				((MongoDao) dao).setMongo(mongo);
				((MongoDao) dao).setDatastore(datastore);
				dao.start();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (MongoException e) {
				e.printStackTrace();
			}
		}
		loadWithoutExceptions = true;

		seedProvider = new SeedProvider();
		seedProvider.setDao(dao);
		seedProvider.setManager(this);

		weiboLogin = new Login();
		weiboLogin.setManager(this);
		weiboLogin.setProducer(seedProvider);

		weiboFetcher = new Fetcher();
		weiboFetcher.setManager(this);
		weiboFetcher.setProducer(weiboLogin);

		redundancyFilter = new RedundancyFilter();
		redundancyFilter.setManager(this);
		redundancyFilter.setDao(dao);
		redundancyFilter.setProducer(weiboFetcher);

		weiboSaver = new Saver();
		weiboSaver.setDao(dao);
		weiboSaver.setManager(this);
		weiboSaver.setProducer(redundancyFilter);

		exec.execute(seedProvider);
		// try {
		// Thread.sleep(1000);
		// } catch (InterruptedException e) {
		// }
		exec.execute(weiboLogin);
		// try {
		// Thread.sleep(2000);
		// } catch (InterruptedException e) {
		// }
		exec.execute(weiboFetcher);
		// try {
		// Thread.sleep(2000);
		// } catch (InterruptedException e) {
		// }
		exec.execute(redundancyFilter);
		// try {
		// Thread.sleep(2000);
		// } catch (InterruptedException e) {
		// }
		exec.execute(weiboSaver);

		/*** 每一小时对爬虫进行一次dump ***/
		crawlerAutoDump();
	}

	/**
	 * 每一小时对爬虫进行一次dump
	 */
	private void crawlerAutoDump() {
		Thread t = new Thread() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(3600 * 1000);
						try {
							// 必须反向dump
							weiboSaver.dump();
							redundancyFilter.dump();
							weiboFetcher.dump();
							weiboLogin.dump();
							seedProvider.dump();
							exec.execute(seedProvider);// 由于seedprovider不是死循环在跑
						} catch (Exception e) {
							e.printStackTrace();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		t.start();
	}

	public boolean stop() {
		stopWithoutExceptions = true;

		// 先停再清空，防止清空后运行的processor重新写入数据。
		weiboLogin.stop();
		weiboLogin.dump();

		weiboFetcher.stop();
		weiboFetcher.dump();

		redundancyFilter.stop();
		redundancyFilter.dump();

		weiboSaver.stop();
		weiboSaver.dump();

		exec.shutdownNow();

		return stopWithoutExceptions;
	}

	/**
	 * 暂未实现
	 */
	public boolean pause(long millis) {
		pauseWithoutExceptions = true;
		return pauseWithoutExceptions;
	}

	/**
	 * 暂未实现
	 */
	public boolean goon() {
		goonWithoutExceptions = true;
		return goonWithoutExceptions;
	}

	/**
	 * 当前效果等价与fireSeedReloadEvent()，处理需要重载cookie的事件
	 */
	public void fireCookieReloadEvent() {
		try {
			// 必须反向dump
			weiboFetcher.dump();
			weiboLogin.dump();
			seedProvider.dump();
			exec.execute(seedProvider);// 由于seedprovider不是死循环在跑
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 处理重置fetcher的事件，它发生在出现连续5条微博重复的时候。
	 * 由于fetcher里面一个线程处理一个ajaxUrl，所以可以根据ajaxUrl来操作fetch的某一线程
	 */
	public void fireFetcherResetEvent(String ajaxUrl) {
		FetchThread t = weiboFetcher.getFetchThread(ajaxUrl);
		if (t != null)
			t.resetAjaxUrl();
	}

	/**
	 * 处理需要重载种子的事件
	 */
	public void fireSeedReloadEvent() {
		try {
			// 必须反向dump
			weiboFetcher.dump();
			weiboLogin.dump();
			seedProvider.dump();
			exec.execute(seedProvider);// 由于seedprovider不是死循环在跑
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 处理添加新的用于爬虫的url的事件
	 */
	public void fireNewUrlsEvent(List<String> captureNewUrls) {
		if (captureNewUrls == null || captureNewUrls.size() == 0
				|| weiboFetcher.getQueuedUrlsCount() > 512)
			return;
		weiboFetcher.addAjaxUrls(captureNewUrls);
	}

	public boolean isLoadWithoutExceptions() {
		return loadWithoutExceptions;
	}

	public void setLoadWithoutExceptions(boolean loadWithoutExceptions) {
		this.loadWithoutExceptions = loadWithoutExceptions;
	}

	public boolean isPauseWithoutExceptions() {
		return pauseWithoutExceptions;
	}

	public void setPauseWithoutExceptions(boolean pauseWithoutExceptions) {
		this.pauseWithoutExceptions = pauseWithoutExceptions;
	}

	public boolean isStopWithoutExceptions() {
		return stopWithoutExceptions;
	}

	public void setStopWithoutExceptions(boolean stopWithoutExceptions) {
		this.stopWithoutExceptions = stopWithoutExceptions;
	}

	public boolean isGoonWithoutExceptions() {
		return goonWithoutExceptions;
	}

	public void setGoonWithoutExceptions(boolean goonWithoutExceptions) {
		this.goonWithoutExceptions = goonWithoutExceptions;
	}

}
