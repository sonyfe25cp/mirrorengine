package bit.mirror.core;

import java.net.URI;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToeThread extends Thread {
	private static final Logger logger = LoggerFactory
			.getLogger(ToeThread.class);

	private static final Random random = new Random();
	
	public int no = 0;

	private Coordinator coordinator;
	private CrawlUrl currentCrawlUrl = null;

	public Coordinator getCoordinator() {
		return coordinator;
	}

	public void setCoordinator(Coordinator coordinator) {
		this.coordinator = coordinator;
	}

	public CrawlUrl getCurrentCrawlUrl() {
		return currentCrawlUrl;
	}

	public int getNo() {
		return no;
	}

	public void setNo(int no) {
		this.no = no;
	}

	public ToeThread() {
	}

	
	public void run() {
		try {
			workLoop();
		} catch (InterruptedException e) {
			logger.info("Toe thread interrupted.");
		}

	}

	private void workLoop() throws InterruptedException {
		while (true) {
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}

			try {
				workOne();
			} catch (InterruptedException e) {
				throw e;
			} catch (Exception e) {
				// A toe thread should not die on other exceptions.

				URI url = null;

				if (currentCrawlUrl != null) {
					url = currentCrawlUrl.getUrl();
				}
				e.printStackTrace();
				logger.error("Exception thrown while processing URL: {}", url,
						e);
			}
		}
	}

	private void workOne() throws InterruptedException {
		currentCrawlUrl = null;

//		logger.info(no+ " begin at "+new Date().toGMTString());
		CrawlUrl crawlUrl = coordinator.getFrontier().next();
//		logger.info(no + " end at "+new Date().toGMTString());
		// Have a random rest so that threads do not start together.
		haveARest();

		currentCrawlUrl = crawlUrl;

		URI url = crawlUrl.getUrl();

		logger.debug("Crawling URL: {}", url);

		logger.trace("Entering prefetch chain");

		try {
			coordinator.getPreFetchChain().process(crawlUrl, coordinator);
		} catch (FetchAbortedException e) {
			logger.debug("Aborted in prefetch chain. Reason: {}. URL: {}",
					e.getMessage(), url);
			// This page should not be fetched.
			return;
		}

		logger.trace("Entering fetch chain");

		try {
			coordinator.getFetchChain().process(crawlUrl, coordinator);
		} catch (FetchAbortedException e) {
			logger.debug("Aborted in fetch chain. Reason: {}. URL: {}",
					e.getMessage(), url);
		}

		logger.trace("Entering post fetch chain");
		try {
			coordinator.getPostFetchChain().process(crawlUrl, coordinator);
		} catch (FetchAbortedException e) {
			logger.debug("Aborted in post fetch chain. Reason: {}. URL: {}",
					e.getMessage(), url);
		}

		logger.debug("Processing done. URL: {}", url);

		haveARest();
	}

	private void haveARest() throws InterruptedException {
		long timeToSleep = coordinator.getSleepAfterCrawling();

		// Randomly pick a sleep time between 0.25x to 0.5x of the specified
		// time.
		timeToSleep = (long) (timeToSleep * (0.25 + 0.5 * random.nextDouble()));

		logger.debug("Sleeping for {}ms.", timeToSleep);
		Thread.sleep(timeToSleep);
		logger.debug("Wake up after {}ms sleep.", timeToSleep);

	}
}
