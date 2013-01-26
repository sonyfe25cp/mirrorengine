package bit.mirror.processor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bit.mirror.core.Coordinator;
import bit.mirror.core.CrawlUrl;
import bit.mirror.core.FetchAbortedException;
import bit.mirror.core.Processor;
import bit.mirror.data.FetchRecord;
import bit.mirror.data.WebPage;
import bit.mirror.util.ErrorTolerantInputStream;

public class HttpFetcher implements Processor {
	private static final Logger logger = LoggerFactory
			.getLogger(HttpFetcher.class);

	private String userAgent = "Mozilla/5.0 (compatible; mirrorengine/0.0.1)";

	// Deflate is somehow difficult to process, so we prefer not to use it.
	// See org.apache.http.client.entity.DeflateDecompressingEntity in Apache
	// HttpClient.
	private String acceptEncoding = "gzip";
	private String accept = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getAcceptEncoding() {
		return acceptEncoding;
	}

	public void setAcceptEncoding(String acceptEncoding) {
		this.acceptEncoding = acceptEncoding;
	}

	public String getAccept() {
		return accept;
	}

	public void setAccept(String accept) {
		this.accept = accept;
	}

	public ThreadSafeClientConnManager getConnManager() {
		return connManager;
	}

	public void setConnManager(ThreadSafeClientConnManager connManager) {
		this.connManager = connManager;
	}

	private ThreadSafeClientConnManager connManager = new ThreadSafeClientConnManager();
	private HttpClient httpClient = new DefaultHttpClient(connManager);

	private long maxLength = 1048576;

	public HttpClient getHttpClient() {
		return httpClient;
	}

	public void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	public long getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(long maxLength) {
		this.maxLength = maxLength;
	}

	public void process(final CrawlUrl crawlUrl, Coordinator coordinator)
			throws FetchAbortedException, InterruptedException {

		final FetchRecord fetchRecord = crawlUrl.getFetchRecord();
		final WebPage webPage = crawlUrl.getWebPage();
		final URI url = webPage.getUrl();

		HttpGet req = new HttpGet(url);

		if (userAgent != null) {
			req.addHeader("User-Agent", userAgent);
		}

		if (accept != null) {
			req.addHeader("Accept", accept);
		}

		if (acceptEncoding != null) {
			req.addHeader("Accept-Encoding", acceptEncoding);
		}

		String oldETag = webPage.getEtag();
		if (oldETag != null) {
			logger.debug("Using ETag: '{}' URL: {}", oldETag, url);
			req.addHeader("If-None-Match", oldETag);
		}

		Date oldLastModified = webPage.getLastModified();
		if (oldLastModified != null) {
			String dateString = DateUtils.formatDate(oldLastModified);
			logger.debug("Using IfModSince: '{}' URL: {}", dateString, url);
			req.addHeader("If-Modified-Since", dateString);
		}

		ResponseHandler<Object> handler = new ResponseHandler<Object>() {

			public Object handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				logger.debug("Reading headers from URL: {}", url);

				int statusCode = response.getStatusLine().getStatusCode();

				fetchRecord.setHttpStatus(statusCode);
				fetchRecord.setError(null);

				if (statusCode == HttpStatus.SC_NOT_MODIFIED) {
					fetchRecord.setError(response.getStatusLine()
							.getReasonPhrase());
					throw new FetchAbortedException("Not modified.");
				}

				webPage.setHttpStatus(statusCode);

				Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();

				for (Header header : response.getAllHeaders()) {
					List<String> values = headers.get(header.getName());
					if (values == null) {
						values = new ArrayList<String>();
						headers.put(header.getName(), values);
					}

					values.add(header.getValue());
				}

				webPage.setHeaders(headers);

				String newETag = null;

				Header eTagHeader = response.getLastHeader("ETag");
				if (eTagHeader != null) {
					newETag = eTagHeader.getValue();
				}

				webPage.setEtag(newETag);

				Date newLastModified = null;

				Header lastModifiedHeader = response
						.getLastHeader("Last-Modified");
				if (lastModifiedHeader != null) {
					String dateString = lastModifiedHeader.getValue();
					try {
						newLastModified = DateUtils.parseDate(dateString);
					} catch (DateParseException e) {
						// leave as null
					}
				}

				webPage.setLastModified(newLastModified);

				webPage.setEntity(null);
				webPage.setContent(null);
				webPage.getOutLinks().clear();

				if (statusCode != HttpStatus.SC_OK) {
					fetchRecord.setError(response.getStatusLine()
							.getReasonPhrase());
					throw new FetchAbortedException("Bad status code: "
							+ statusCode);
				}

				logger.debug("Reading entity from URL: {}", url);

				HttpEntity entity = response.getEntity();

				if (entity == null) {
					fetchRecord.setError("Null entity.");
					throw new FetchAbortedException("Null entity.");
				}

				byte[] rawContent = null;
				InputStream contentStream = null;
				ErrorTolerantInputStream errorTolerantStream = null;
				BoundedInputStream boundedStream = null;

				try {
					contentStream = entity.getContent();
					errorTolerantStream = new ErrorTolerantInputStream(
							contentStream);
					boundedStream = new BoundedInputStream(errorTolerantStream,
							maxLength);
					rawContent = IOUtils.toByteArray(boundedStream);
				} catch (Exception e) {
					// Not sure what can cause this exception.
					// errorTolerantStream will absorb errors during
					// reading.
					fetchRecord.setError("Error reading HTTP entity:"
							+ e.toString());
					throw new FetchAbortedException(
							"Error reading HTTP entity.");
				} finally {
					if (contentStream != null && rawContent != null
							&& rawContent.length >= maxLength) {
						logger.debug("Entity too large. Closing it. URL: {}",
								url);
						IOUtils.closeQuietly(contentStream);
					}
				}

				webPage.setEntity(rawContent);

				crawlUrl.setShouldSaveWebPage(true);

				return null;
			}

		};

		logger.debug("Sending request to URL: {}", url);

		try {
			httpClient.execute(req, handler);
		} catch (ClientProtocolException e) {
			crawlUrl.getFetchRecord().setHttpStatus(0);
			crawlUrl.getFetchRecord().setError(e.toString());
			throw new FetchAbortedException("HTTP fetching error");
		} catch (IOException e) {
			crawlUrl.getFetchRecord().setHttpStatus(0);
			crawlUrl.getFetchRecord().setError(e.toString());
			throw new FetchAbortedException("HTTP fetching error");

		}
	}
}
