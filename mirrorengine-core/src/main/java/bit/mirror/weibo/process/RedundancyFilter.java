package bit.mirror.weibo.process;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.impl.client.DefaultHttpClient;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import bit.mirror.dao.MirrorEngineDao;
import bit.mirror.data.WebPage;
import bit.mirror.weibo.core.Manager;
import bit.mirror.weibo.core.Processor;
import bit.mirror.weibo.core.Producer;

import net.sf.json.JSONObject;

/**
 * 去重。该类先将一片（15条）微博分割成一条条。然后通过查询数据库来发现某一条微博是否已经存在，如果没有存在，则缓存给Saver进行保存。
 * 同时出于构建dom耗时的考虑，整合抽取评论部分。
 * 
 * @author lins
 * @date 2012-6-21
 **/
public class RedundancyFilter implements Processor<JSONObject, JSONObject> {
	private static final Logger logger = LoggerFactory
			.getLogger(RedundancyFilter.class);
	private MirrorEngineDao dao;// = new MongoDao();
	volatile private Producer<JSONObject> producer;
	volatile private Manager manager;

	/**
	 * 构造方法，主要是初始化transformer，transformer的作用是将dom转换成string
	 */
	public RedundancyFilter() {
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
					"yes");
		} catch (TransformerConfigurationException e) {
			logger.error("Fail to create transformer.");
		} catch (TransformerFactoryConfigurationError e) {
			logger.error("Fail to create transformer.");
		}
	}

	public MirrorEngineDao getDao() {
		return dao;
	}

	public void setDao(MirrorEngineDao dao) {
		this.dao = dao;
	}

	public Manager getManager() {
		return manager;
	}

	public void setManager(Manager manager) {
		this.manager = manager;
	}

	/*** 用于给saver消费的缓存 ***/
	private volatile static LinkedBlockingQueue<JSONObject> weibos = new LinkedBlockingQueue<JSONObject>();

	public JSONObject produce() {
		return weibos.poll();
	}

	public Collection<JSONObject> produceMega() {
		return weibos;
	}

	public void setProducer(Producer<JSONObject> p) {
		this.producer = p;
	}

	public Producer<JSONObject> getProducer() {
		return producer;
	}

	public void consume(Producer<JSONObject> p) throws Exception {
		this.producer = p;
		consume();
	}

	/** 出于性能考虑，只有一份，通过synchronized来解决线程安全问题 **/
	private Transformer transformer;

	int sleep = 1;
	/*** 把线程记录下来，也方便操作，主要用于限制线程数 ***/
	private volatile ArrayList<FilterThread> filterThreads = new ArrayList<FilterThread>();

	/**
	 * 消费fetcher产生的东西，具体操作留给子类FilterThread
	 * 
	 * @see bit.mirror.weibo.core.Consumer#consume()
	 */
	public void consume() throws Exception {
		if (producer == null) {
			// logger.debug("Provider of weibo login unavilable.");
			return;
		}
		JSONObject jsonObj = null;
		alive = true;
		for (jsonObj = producer.produce(); alive; jsonObj = producer.produce()) {
			// 假如没有用于消费weibo则sleep
			if (jsonObj == null) {
				// logger.debug("None of Weibos were found.");
				Thread.sleep(sleep * 1000);
				if (sleep < 10)
					sleep++;
				continue;
			}
			// 假如当前保存的产品weibo过多则sleep
			if (weibos.size() > MAX_VOLUMN) {
				logger.error("Sth. may be wrong with WeiboSaver as so many weibos in RedundancyFilter.");
				Thread.sleep(sleep * 1000);
				if (sleep <= 10)
					sleep++;
				continue;
			}
			// 假如当前的线程过多则sleep
			if (filterThreads.size() > MAX_THREAD) {
				logger.debug("I kept so many filter threads that i have to sleep.");
				Thread.sleep(sleep * 1000);
				if (sleep <= 10)
					sleep++;
				continue;
			}
			// 一切正常则进行filt
			sleep = 1;
			logger.debug("I began to filt.");
			FilterThread r = new FilterThread(jsonObj);
			Manager.exec.execute(r);
			filterThreads.add(r);
		}
	}

	/**
	 * 线程的run而已
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			consume();
		} catch (Exception e) {
			logger.error("Error in filting weibo!");
		}
	}

	public final int MAX_VOLUMN = 1024;
	public final int MAX_THREAD = 12;

	/**
	 * 用于跑过滤的线程的类,一个线程处理一个JSONObject
	 */
	public class FilterThread implements Runnable {
		JSONObject jsonObj = null;

		public FilterThread(JSONObject jsonObj) {
			this.jsonObj = jsonObj;
		}

		long hash = 0;

		private boolean DBcontains(String content) {
			/*
			 * 模拟hashcode，不同的是返回long型。hash能保证的是相同的hash相同，但无法保证不同的不同
			 * logger.debug("Caculating hash code of weibo.");
			 */
			// 剔除时间，评论等，因为这些即便对于同一条微博也是在变的；然后剔除标签，这是出于效率的考虑
			content = content
					.replaceAll(
							"<(dd|p) class=\"info W_linkb W_textb\">.*?</(dd|p)>",
							"").replaceAll("<.*?>", "")
					.replaceAll("(\n|\t)", "").trim();
			long seed = 31;// 用31是因为JVM会优化这个乘法为移位
			hash = 0;
			for (int i = 0; i < content.length(); i++) {
				hash = (hash * seed) + content.charAt(i);
			}
			// 由于hash无法保证不同的不同。所以最多查询10次数据库，假如还是没找到就放弃该条微博，认为已经存在于数据库
			WebPage webPage;
			for (int i = 0; i < 10; i++) {
				webPage = dao.getWebPageByUrl("file://mock/"
						+ jsonObj.getString("seed:type") + "/"
						+ String.valueOf(hash));
				if (webPage == null)
					return false;
				else {
					// 同样的处理剔除时间评论，然后剔除标签
					String tmp = webPage
							.getContent()
							.replaceAll(
									"<(dd|p) class=\"info W_linkb W_textb\">.*?</(dd|p)>",
									"").replaceAll("<.*?>", "")
							.replaceAll("(\n|\t)", "").trim();
					if (tmp.equals(content))
						return true;
					hash++;
				}
			}
			return true;
		}

		int count = 0;// 计数，记录到底有几个被视为重复
		boolean flag = false;// 记录是不是有需要入库的微博

		/**
		 * 先将string转换成dom，然后对每个子节点再转换成string。 之所以没有直接正则匹配
		 * <dl>
		 * ，一是想解耦合，不把代码写死，二是，我也没想出特别好的方法
		 * 
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			/***** 将string转换成dom ******/
			long cost = new Date().getTime();
			String data = jsonObj.getString("data");
			jsonObj.remove("data");
			HtmlCleaner cleaner = new HtmlCleaner();// w3c dom在这里无法使用，会抛异常
			cleaner.getProperties().setNamespacesAware(false);
			Document doc = null;
			try {
				doc = new DomSerializer(cleaner.getProperties(), true)
						.createDOM(cleaner.clean(data));// 转换成W3C
														// DOM，为的是转换成string的方便
			} catch (ParserConfigurationException e1) {
				logger.error("Error in cleaner configuration.");
				return;
			} catch (Exception e){
				logger.error("Unknow error in cleaning");
				return;
			}
			if (doc == null)
				return;

			/****** 再将dom所有的子节点分别转换成String *******/
			CommentExtractor cExtractor = new CommentExtractor(
					jsonObj.getString("seed:type"),
					jsonObj.getString("cookie"), new DefaultHttpClient());
			NodeList nodeList = doc.getElementsByTagName("body").item(0)
					.getChildNodes();
			for (int i = 0, j = 0; i < nodeList.getLength(); i++) {// 处理每一个节点，当然个别节点可能根本就不是微薄，但是也入库
				Node n = nodeList.item(i);
				if (n.getNodeType() != Node.ELEMENT_NODE)
					continue;

				DOMSource source = new DOMSource(n);
				StringWriter writer = new StringWriter();
				Result result = new StreamResult(writer);

				// 转换成string
				synchronized (transformer) {
					try {
						transformer.transform(source, result);
					} catch (TransformerException e) {
						logger.error("Fail to transform node to string.");
						return;
					} catch(Exception e){
						logger.error("Unknow error in transfering node to string.");
						return;
					}
				}

				/***** 通过对比数据库里面的String来判断冗余 ****/
				String content = writer.getBuffer().toString();
				// String raw = n.getTextContent().trim();
				if (!DBcontains(content)) {
					// 由于uri是被索引的域,但是微博没有url所以使用hascode来代替，从而提升查重的速率
					jsonObj.accumulate("uri-" + j,
							"file://mock/" + jsonObj.getString("seed:type")
									+ "/" + String.valueOf(hash));
					jsonObj.accumulate("content-" + j, content);
					j++;
					count = 0;
					flag = true;
					/*** 获得comment以及获得新的ajaxUrl并且给予fetcher ***/
					cExtractor.setNode(n);
					String comment = cExtractor.extractComment();
					if (comment != null)
						jsonObj.accumulate("comment-" + j, comment);
					manager.fireNewUrlsEvent(cExtractor.captureNewUrls());
				} else {
					count++;
					logger.debug("Found redundancy, it'll be given up!");
					if (count > 5) {
						logger.debug("Found redundancy, i'll fire fetcher reset event!");
						manager.fireFetcherResetEvent(jsonObj
								.getString("ajax-url"));
						count = 0;
						try {
							writer.close();
						} catch (IOException e) {
							logger.error("Fail to close StringWriter.");
						}
						break;
					}
				}
				try {
					writer.close();
				} catch (IOException e) {
					logger.error("Fail to close StringWriter.");
				}
			}
			cExtractor.close();
			/*** 将多个微博记录一并存进队列 ***/
			if (flag)
				weibos.add(jsonObj);
			cost = new Date().getTime() - cost;
			logger.debug("This round cost {} millis to filt weibo.", cost);
			synchronized (filterThreads) {//访问量不大，这种同步不会影响性能
				filterThreads.remove(this);
			}
		}

	}

	/**
	 * 清空缓存
	 */
	public void dump() {
		weibos.clear();
		filterThreads.clear();
		sleep = 1;
	}

	private boolean alive = true;

	public void stop() {
		alive = false;
	}
}
