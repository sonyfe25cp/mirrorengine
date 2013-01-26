package bit.mirror.weibo.process;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import net.sf.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bit.mirror.util.StreamFormator;
import bit.mirror.weibo.core.Manager;
import bit.mirror.weibo.core.Processor;
import bit.mirror.weibo.core.Producer;

/**
 * 抓取微博的类,获得15条微博，将获得结果传送给下一个processor
 * 消费JSONObject，产生JSONObject
 * @author lins
 * @date 2012-6-20
 **/
public class Fetcher implements Processor<JSONObject, JSONObject> {
	private static final Logger logger = LoggerFactory.getLogger(Fetcher.class);
	volatile private Manager manager;
	volatile private Producer<JSONObject> producer;

	public Fetcher() {
		ajaxUrls.add(homeAjaxUrl);
	}

	public Manager getManager() {
		return manager;
	}

	public void setManager(Manager manager) {
		this.manager = manager;
	}

	public void setProducer(Producer<JSONObject> p) {
		this.producer = p;
	}

	public Producer<JSONObject> getProducer() {
		return producer;
	}

	private volatile static LinkedBlockingQueue<JSONObject> jsonObjs = new LinkedBlockingQueue<JSONObject>();

	public JSONObject produce() {
		return jsonObjs.poll();
	}

	public Collection<JSONObject> produceMega() {
		return jsonObjs;
	}

	public void consume(Producer<JSONObject> p) throws Exception {
		this.producer = p;
		consume();
	}

	/*** 由于这些线程是常驻的，所以把他们记录下来 ***/
	private volatile ArrayList<FetchThread> fetchThreads = new ArrayList<FetchThread>();

	public ArrayList<FetchThread> getFetchThreads() {
		return fetchThreads;
	}

	public FetchThread getFetchThread(String ajaxUrl) {
		for (FetchThread fetchThread : fetchThreads) {
			if (fetchThread.ajaxUrl.equals(ajaxUrl))
				return fetchThread;
		}
		return null;
	}

	int threadCount = 5;

	int sleep = 1;

	public void consume() throws Exception {
		if (producer == null) {
			// logger.debug("Provider of weibo login unavilable.");
			return;
		}
		JSONObject cookie = null;
		alive = true;
		for (cookie = producer.produce(); alive; cookie = producer.produce()) {
			if (cookie == null || cookie.equals("")) {
				// logger.debug("Cookie for weibo login unavilable.");
				Thread.sleep(sleep * 10000);
				if (sleep < 10)
					sleep++;
				else {
					// 每过9分钟左右重置一下ajaxUrls
					ajaxUrls.clear();
					ajaxUrls.add(homeAjaxUrl);
				}
				continue;
			}
			sleep = 1;
			// 默认每个cookie开threadCount条线进行爬虫
			for (int i = 0; i < threadCount; i++) {
				// 真正在爬微博的线程
				FetchThread r = new FetchThread(i, cookie);
				Manager.exec.execute(r);
				fetchThreads.add(r);
			}
		}
	}

	/**
	 * 只有在跑起来之前设置才有效
	 */
	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}

	public void run() {
		try {
			consume();
		} catch (Exception e) {
			logger.info("Error in fetching weibo! Maybe i was interrupted!");
			e.printStackTrace();
		}
	}

	private volatile LinkedBlockingQueue<String> ajaxUrls = new LinkedBlockingQueue<String>();
	private final String homeAjaxUrl = "http://weibo.com/aj/mblog/fsearch";

	public int getQueuedUrlsCount() {
		return ajaxUrls.size();
	}

	public void addAjaxUrls(List<String> list) {
		for (String str : list) {
			if (!ajaxUrls.contains(str)) {
				// logger.debug("Found new URL: {}.", str);
				ajaxUrls.add(str);
			}
		}
	}

	public final int MAX_VOLUMN = 1024;

	/**
	 * 真正用来爬虫的线程
	 */
	public class FetchThread implements Runnable {
		private int id;
		private JSONObject cookie;
		private String ajaxUrl;

		public FetchThread(int id, JSONObject cookie) {
			this.id = id;
			this.cookie = cookie;
			page = id + 1;
		}

		public int getId() {
			return id;
		}

		public JSONObject getCookie() {
			return cookie;
		}

		private int page;
		private int count = 15;
		private int pre_page = 1;
		private int pagebar = -1;

		/**
		 * 每个page的第一个都是纯粹的AjaxUrl，接着pagebar增从0-1，然后再增加page从1开始
		 */
		private String getNextAjaxUrl() {
			if (page > 10) {
				resetAjaxUrl();
				return ajaxUrl;
			}

			if (ajaxUrl == null)
				ajaxUrl = homeAjaxUrl;
			StringBuilder sb = new StringBuilder(ajaxUrl);
			if (pagebar != -1) {
				if (ajaxUrl.contains("?")) {// http://weibo.com/aj/mblog/mbloglist?uid=1875931727
					sb.append("&");
				} else {
					// http://weibo.com/aj/mblog/fsearch
					sb.append("?");
				}
				sb.append("page=" + page);
				sb.append("&count=" + count);
				sb.append("&pre_page=" + pre_page);
				sb.append("&pagebar=" + pagebar);
				pre_page = page;
				// pagebar从0～1，page从id开始自增
				if (pagebar == 1) {
					page++;
					pagebar = -2;
				}
			}
			pagebar++;
			return sb.toString();
		}

		public void reset() {
			page = 1;
			count = 15;
			pre_page = 1;
			pagebar = -1;
		}

		public void resetAjaxUrl() {
			int tsleep = 0;
			name = null;
			uid = null;
			do {
				try {
					Thread.sleep(tsleep * 1000);
				} catch (InterruptedException e) {
					//e.printStackTrace();
				}
				if (tsleep <= 20)
					tsleep++;
				ajaxUrl = ajaxUrls.poll();
			} while (ajaxUrl == null);

			if (ajaxUrl.contains("/name=")) {// http://weibo.com/aj/mblog/mbloglist?uid=1875931727&name=lins
				int idx = ajaxUrl.lastIndexOf("/");
				name = ajaxUrl.substring(idx + 6, ajaxUrl.length());
				ajaxUrl = ajaxUrl.substring(0, idx);
				uid = ajaxUrl.substring(ajaxUrl.indexOf("?") + 1);
			}
		}

		String name;
		String uid;

		/**
		 * 一个线程保持一个HttpClient，不停地发送请求，获得微薄
		 * 
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			resetAjaxUrl();
			HttpClient httpClient = new DefaultHttpClient();
			int tsleep = 1;
			long cost = 0;
			while (true) {
				if (jsonObjs.size() > MAX_VOLUMN) {
					logger.error("Sth. may be wrong with RedundancyFilter as so many weibos in WeiboFetcher.");
					try {
						Thread.sleep(tsleep * 1000);
					} catch (InterruptedException e) {
						//e.printStackTrace();
					}
					if (tsleep <= 20)
						tsleep++;
				}
				tsleep = 1;
				String uri = getNextAjaxUrl();
				HttpGet get = new HttpGet(uri);
				get.addHeader("Cookie", cookie.getString("cookie"));
				HttpResponse hr;
				try {
					cost = new Date().getTime();
					hr = httpClient.execute(get);
					HttpEntity httpEntity = hr.getEntity();
					InputStream inputStream = httpEntity.getContent();
					cost =  new Date().getTime() - cost;
					String tmp = StreamFormator.getString(inputStream, "utf-8");

					JSONObject jsonObj = JSONObject.fromObject(tmp);
					jsonObj.accumulate("thread-id", id);
					jsonObj.accumulate("ajax-url", ajaxUrl);
					jsonObj.accumulateAll(cookie);
					if (name != null) {// postFormat,给来自其他人主页的微博加用户
						String data = jsonObj
								.getString("data")
								.replace(
										"<dd class=\"content\">",
										String.format(
												"<dd class=\"content\"><a usercard=\"%s\">%s</a>",
												uid, name));
						jsonObj.put("data", data);
					}

					jsonObjs.add(jsonObj);
					inputStream.close();
				} catch (ClientProtocolException e) {
					logger.error("Client protocol error!");
					httpClient.getConnectionManager().closeIdleConnections(0,
							TimeUnit.SECONDS);
					httpClient.getConnectionManager().shutdown();
					httpClient = new DefaultHttpClient();
					logger.info("Sleep 10 seconds for ehmm some reasons.");
					try {
						Thread.sleep((long) (Math.random() * 10000));
					} catch (InterruptedException e1) {
						//e1.printStackTrace();
					}
					e.printStackTrace();
				} catch (IOException e) {
					logger.error("IO error!");
					httpClient.getConnectionManager().closeIdleConnections(0,
							TimeUnit.SECONDS);
					httpClient.getConnectionManager().shutdown();
					httpClient = new DefaultHttpClient();
					logger.info("Sleep 10 seconds for ehmm some reasons.");
					try {
						Thread.sleep((long) (Math.random() * 10000));
					} catch (InterruptedException e1) {
						//e1.printStackTrace();
					}
					e.printStackTrace();
				} catch (Exception e) {
					logger.error("Maybe connection error!");
					httpClient.getConnectionManager().closeIdleConnections(0,
							TimeUnit.SECONDS);
					httpClient.getConnectionManager().shutdown();
					httpClient = new DefaultHttpClient();
					logger.info("Sleep 10 seconds for ehmm some reasons.");
					try {
						Thread.sleep((long) (Math.random() * 10000));
					} catch (InterruptedException e1) {
						logger.info("Sb. interrupted me!");
						//e1.printStackTrace();
					}
					e.printStackTrace();
				} finally {
					// 释放链接
					httpClient.getConnectionManager().closeIdleConnections(0,
							TimeUnit.SECONDS);
				}

				logger.debug("After this round the stacked was {} and time costing was {}.", jsonObjs.size(),cost);
				try {
					// 稍微休息一下
					Thread.sleep(jsonObjs.size() / 2 + Math.min(cost, 1000));
				} catch (InterruptedException e) {
					logger.error("Sb. interrupted me!");
					//e.printStackTrace();
				}
				if (killed) {
					httpClient.getConnectionManager().shutdown();
					break;
				}
			}
		}

		boolean killed = false;

		public void kill() {
			killed = true;
		}
	}

	public void dump() {
		ajaxUrls.clear();
		ajaxUrls.add(homeAjaxUrl);
		for (FetchThread ft : fetchThreads) {
			ft.kill();
		}
		fetchThreads.clear();
		jsonObjs.clear();
		sleep = 1;

	}

	private boolean alive = true;

	public void stop() {
		alive = false;
		for (FetchThread ft : fetchThreads) {
			ft.kill();
		}
	}
}
