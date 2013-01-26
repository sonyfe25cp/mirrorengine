package bit.mirror.processor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bit.mirror.core.Coordinator;
import bit.mirror.core.CrawlUrl;
import bit.mirror.core.FetchAbortedException;
import bit.mirror.core.Processor;
import bit.mirror.data.WebPage;
import bit.mirror.util.EncodingGuesser;
import bit.mirror.util.ErrorTolerantInputStream;

public class HtmlDecoder implements Processor {
	private static final Logger logger = LoggerFactory
			.getLogger(HtmlDecoder.class);
	private long maxLength = 1048576;

	public long getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(long maxLength) {
		this.maxLength = maxLength;
	}

	
	public void process(CrawlUrl crawlUrl, Coordinator coordinator)
			throws FetchAbortedException, InterruptedException {
		WebPage webPage = crawlUrl.getWebPage();
		byte[] rawContent = webPage.getEntity();
		Map<String, List<String>> headers = webPage.getHeaders();

		String contentEncoding = getLastHeader(headers, "Content-Encoding");
		if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
			GZIPInputStream gzipInputStream;
			try {
				gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(
						rawContent));
			} catch (IOException e) {
				throw new FetchAbortedException("Bad gzip entity");
			}
			
			ErrorTolerantInputStream errorTolerantStream = new ErrorTolerantInputStream(
					gzipInputStream);
			
			try {
				rawContent = IOUtils.toByteArray(errorTolerantStream);
			} catch (IOException e) {
				// ErrorTolerantInputStream does not throw exceptions while
				// reading. If it does, it is a programming error.
				throw new RuntimeException(e);
			}
		}

		String contentType = getLastHeader(headers, "Content-Type");
		if (contentType == null) {
			contentType = ""; // to be friendly to EncodingGuesser.
		}

		Charset charset = EncodingGuesser.guessWithContentType(rawContent,
				contentType, EncodingGuesser.ISO88591CHARSET);

		logger.debug("Encoding seems to be {}. URL: {}", charset,
				webPage.getUrl());

		String content = new String(rawContent, charset);

		webPage.setContent(content);
	}

	/**
	 * Get the last value of the specified header.
	 * 
	 * @param headers
	 *            A Map object from WebPage.getHeaders().
	 * @param headerName
	 *            The name of the desired header.
	 * @return The value of the last header field whose name is headerName, or
	 *         null if no such header.
	 */
	private String getLastHeader(Map<String, List<String>> headers,
			String headerName) {
		List<String> headerValues = headers.get(headerName);
		if (headerValues == null || headerValues.size() == 0) {
			return null;
		}

		return headerValues.get(headerValues.size() - 1);
	}
}
