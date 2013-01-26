package bit.mirror.weibo.process;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.sf.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import bit.mirror.util.StreamFormator;


/**
 * 获取评论放到json里面，然后从评论里面获得新的ajaxurl
 * 
 * @author lins 2012-6-26
 */
public class CommentExtractor {
	private static final Logger logger = LoggerFactory
			.getLogger(CommentExtractor.class);
	private HtmlCleaner cleaner;
	private HttpClient httpClient;
	private String type;
	private String cookie;
	private Node node;
	private final String WEIBO = "WEIBO", TQQ = "TQQ", TWITTER = "TWITTER";
	private String commentAjaxUrl;
	private String baseAjaxUrl4Fetch;

	public CommentExtractor(String type, String cookie, HttpClient httpClient) {
		this.httpClient = httpClient;
		this.type = type;
		this.cookie = cookie;
		cleaner = new HtmlCleaner();
		cleaner.getProperties().setNamespacesAware(false);
		if (type.equals(WEIBO)) {
			// 无论自己的首页还是他人的首页都是这个ajax
			commentAjaxUrl = "http://weibo.com/aj/comment/small?act=list&isMain=true&location=home";
			// 他人首页的ajax，注意的是获取的微薄里面都没有用户名
			baseAjaxUrl4Fetch = "http://weibo.com/aj/mblog/mbloglist?u";
		} else if (type.equals(TQQ)) {

		} else if (type.equals(TWITTER)) {

		}
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
		if (type.equals(WEIBO)) {
			// 需要赋予mid，例如&mid=3461523001760467
			commentAjaxUrl = "http://weibo.com/aj/comment/small?act=list&isMain=true&location=home";
			// 需要赋予uid，例如uid=3461523001760467
			baseAjaxUrl4Fetch = "http://weibo.com/aj/mblog/mbloglist?u";
		} else if (type.equals(TQQ)) {

		} else if (type.equals(TWITTER)) {

		}
	}

	public void setNode(Node node) {
		this.node = node;
	}

	public List<String> captureNewUrls() {
		if (node == null || comment == null)
			return null;

		List<String> ajaxUrls = new ArrayList<String>();
		if (type.equals("WEIBO")) {
			@SuppressWarnings("unchecked")
			List<TagNode> list = cleaner.clean(
					"<html><body>" + comment + "</body></html>")
					.getElementListHavingAttribute("usercard", true);
			for (TagNode tagNode : list) {
				String usercard = tagNode.getAttributeByName("usercard").trim();
				String name = tagNode.getText().toString().trim();
				if (usercard != null && name != null && !usercard.equals("")
						&& !name.equals("")) {
					// 添加name用于指示这是来自其他人主页的微薄
					ajaxUrls.add(baseAjaxUrl4Fetch + usercard + "/name=" + name);
				}
			}
		}
		return ajaxUrls;
	}

	private String comment;

	public String extractComment() {
		if (node == null)
			return null;

		if (type.equals("WEIBO")) {
			/*** 先用mid补全ajaxUrl ***/
			Node mid = node.getAttributes().getNamedItem("mid");
			if (mid == null)
				return null;
			String url = commentAjaxUrl + "&mid="
					+ mid.getNodeValue().replaceAll("\"|\\\\", "");
			/*** 然后使用url发送请求 ***/
			HttpGet get = new HttpGet(url);
			get.addHeader("Cookie", cookie);
			HttpResponse hr;
			try {
				hr = httpClient.execute(get);
				HttpEntity httpEntity = hr.getEntity();
				InputStream inputStream = httpEntity.getContent();
				String tmp = StreamFormator.getString(inputStream, "utf-8");
				JSONObject jsonObj = JSONObject.fromObject(tmp);
				if (jsonObj.containsKey("data")) {
					JSONObject tmpJSON = ((JSONObject) jsonObj.get("data"));
					if (tmpJSON.containsKey("html")) {
						comment = tmpJSON.getString("html");
					}
				}
				// logger.debug(comment);
				inputStream.close();
			} catch (ClientProtocolException e) {
				logger.error("ClientProtocolException on getting comments.");
				httpClient.getConnectionManager().closeIdleConnections(0,
						TimeUnit.SECONDS);
				httpClient.getConnectionManager().shutdown();
				httpClient = new DefaultHttpClient();
				logger.info("Sleep 10 seconds for ehmm some reasons.");
				try {
					Thread.sleep((long) (Math.random()*10000));
				} catch (InterruptedException e1) {
					//e1.printStackTrace();
				}
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				logger.error("IO error  on getting comments.");
				httpClient.getConnectionManager().closeIdleConnections(0,
						TimeUnit.SECONDS);
				httpClient.getConnectionManager().shutdown();
				httpClient = new DefaultHttpClient();
				logger.info("Sleep 10 seconds for ehmm some reasons.");
				try {
					Thread.sleep((long) (Math.random()*10000));
				} catch (InterruptedException e1) {
					//e1.printStackTrace();
				}
				e.printStackTrace();
				return null;
			} catch(Exception e){
				logger.error("Maybe connection error on getting comments!");
				httpClient.getConnectionManager().closeIdleConnections(0,
						TimeUnit.SECONDS);
				httpClient.getConnectionManager().shutdown();
				httpClient = new DefaultHttpClient();
				logger.info("Sleep 10 seconds for ehmm some reasons.");
				try {
					Thread.sleep((long) (Math.random()*10000));
				} catch (InterruptedException e1) {
					//e1.printStackTrace();
				}
				e.printStackTrace();
			} finally {
				// 释放链接
				httpClient.getConnectionManager().closeIdleConnections(1,
						TimeUnit.SECONDS);
			}
		}
		return comment;
	}

	public void close() {
		httpClient.getConnectionManager().closeIdleConnections(0,
				TimeUnit.SECONDS);
		httpClient.getConnectionManager().shutdown();
	}

}
