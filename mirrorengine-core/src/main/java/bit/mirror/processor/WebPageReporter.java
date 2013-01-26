package bit.mirror.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bit.mirror.core.Coordinator;
import bit.mirror.core.CrawlUrl;
import bit.mirror.core.FetchAbortedException;
import bit.mirror.core.Processor;
/**
 * 向coordinator的frontier添加外部链接
 */
public class WebPageReporter implements Processor {
	private static final Logger logger = LoggerFactory
			.getLogger(WebPageReporter.class);

	
	public void process(CrawlUrl crawlUrl, Coordinator coordinator)
			throws FetchAbortedException, InterruptedException {

		if (!crawlUrl.isShouldPushBack()) {
			logger.debug("Reporting page to frontier. URL: {}",
					crawlUrl.getUrl());
			coordinator.getFrontier().updatePage(crawlUrl);
		} else {
			// TODO: Not able to push back unless frontier can do so.
		}
	}

}
