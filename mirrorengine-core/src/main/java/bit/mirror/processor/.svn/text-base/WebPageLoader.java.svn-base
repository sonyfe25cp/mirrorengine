package bit.mirror.processor;

import java.net.URI;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bit.mirror.core.Coordinator;
import bit.mirror.core.CrawlUrl;
import bit.mirror.core.FetchAbortedException;
import bit.mirror.core.Processor;
import bit.mirror.data.FetchRecord;
import bit.mirror.data.WebPage;

/**
 * Load the WebPage record from the database, or create a new instance if it is
 * absent. Old contents (entity, content) are not fetched.
 */
public class WebPageLoader implements Processor {
	private static final Logger logger = LoggerFactory
			.getLogger(WebPageLoader.class);

	/**
	 * Each seed should be restarted on regular intervals. Seems no need to
	 * check each page using last fetch time.
	 */
	private boolean checkTime = false;

	public boolean isCheckTime() {
		return checkTime;
	}

	public void setCheckTime(boolean checkTime) {
		this.checkTime = checkTime;
	}

	
	public void process(CrawlUrl crawlUrl, Coordinator coordinator)
			throws FetchAbortedException, InterruptedException {

		URI url = crawlUrl.getUrl();

		FetchRecord fetchRecord = coordinator.getDao().getFetchRecordByUrl(url);
		if (fetchRecord == null) {
			logger.debug("FetchRecord absent in database. Create. URL: {}", url);
			fetchRecord = new FetchRecord();
			fetchRecord.setUrl(url);
		}

		if (checkTime) {
			Date lastFetch = fetchRecord.getFetchDate();
			if (lastFetch != null) {
				Date now = new Date();
				long timeToRenew = lastFetch.getTime()
						+ crawlUrl.getSeed().getRefresh() * 1000;
				if (now.getTime() < timeToRenew) {
					throw new FetchAbortedException("Web page crawled recently");
				}
			}
		}

		fetchRecord.setHttpStatus(0);
		fetchRecord.setError("Fetching not started yet.");

		crawlUrl.setFetchRecord(fetchRecord);

		WebPage webPage = coordinator.getDao().getWebPageByUrlMetaOnly(url);

		if (webPage == null) {
			logger.debug("WebPage absent in database. Create. URL: {}", url);
			webPage = new WebPage();
			webPage.setUrl(url);
		}

		webPage.setSeed(crawlUrl.getSeed());
		crawlUrl.setWebPage(webPage);

		Date now = new Date();
		fetchRecord.setFetchDate(now);
		webPage.setFetchDate(now);

		// FetchRecord's are saved as long as fetching has started.
		crawlUrl.setShouldSaveFetchRecord(true);
	}

}
