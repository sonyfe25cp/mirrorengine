package bit.mirror.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bit.mirror.core.Coordinator;
import bit.mirror.core.CrawlUrl;
import bit.mirror.core.FetchAbortedException;
import bit.mirror.core.Processor;
import bit.mirror.dao.MirrorEngineDao;

/**
 * Saves the FetchRecord (and the WebPage if permitted) to the database.
 */
public class DataSaver implements Processor {
	private static final Logger logger = LoggerFactory
			.getLogger(DataSaver.class);

	
	public void process(CrawlUrl crawlUrl, Coordinator coordinator)
			throws FetchAbortedException, InterruptedException {
		MirrorEngineDao dao = coordinator.getDao();

		if (crawlUrl.isShouldSaveFetchRecord()) {
			logger.debug("Saving FetchRecord. URL: {}", crawlUrl
					.getFetchRecord().getUrl());
			dao.saveFetchRecord(crawlUrl.getFetchRecord());
		}

		if (crawlUrl.isShouldSaveWebPage()) {
			logger.debug("Saving WebPage. URL: {}", crawlUrl.getWebPage()
					.getUrl());
			dao.saveWebPage(crawlUrl.getWebPage());
			logger.info("WebPage saved. URL: {}", crawlUrl.getWebPage()
					.getUrl());
		}
	}
}
