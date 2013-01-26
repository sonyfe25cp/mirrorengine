package bit.mirror.frontier;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bit.mirror.core.Coordinator;
import bit.mirror.core.CrawlUrl;
import bit.mirror.core.Frontier;
import bit.mirror.dao.MirrorEngineDao;
import bit.mirror.data.Seed;
import bit.mirror.data.WebPage;

public class MongoFrontier implements Frontier {
	private static final Logger logger = LoggerFactory
			.getLogger(MongoFrontier.class);

	private Coordinator coordinator;

	public Coordinator getCoordinator() {
		return coordinator;
	}

	public void setCoordinator(Coordinator coordinator) {
		this.coordinator = coordinator;
	}

	public MirrorEngineDao getDao() {
		return coordinator.getDao();
	}

	/**
	 * Create a MongoFrontier without initializing it.
	 */
	public MongoFrontier() {
		super();
	}

	/* ************* CONFIG PROPERTIES ************* */

	private boolean suspended;

	
	public boolean isSuspended() {
		return suspended;
	}

	
	public void setSuspended(boolean suspended) {
		this.suspended = suspended;
		if (suspended == false) {
			notifyNewPages();
		}
	}

	/* ************* INTERNAL DATA STRUCTURES ************* */

	private class SeedSubfrontier {
		private Seed seed;
		private PriorityBlockingQueue<CrawlUrl> queue = new PriorityBlockingQueue<CrawlUrl>();

		private ScheduledFuture<?> refreshFuture = null;

		public ScheduledFuture<?> getRefreshFuture() {
			return refreshFuture;
		}

		public void setRefreshFuture(ScheduledFuture<?> refreshFuture) {
			this.refreshFuture = refreshFuture;
		}

		public SeedSubfrontier(Seed seed) {
			this.seed = seed;
		}

		public void init() {
			for (URI url : seed.getInitialUrls()) {
				enqueueUrl(url, 0);
			}
		}

		private void enqueueUrl(URI url, int newDistance) {
			boolean addable = urlUniqFilter
					.visitIfPossible(seed.getName(), url);
			if (!addable) {
				return;
			}

			CrawlUrl ph = new CrawlUrl();
			ph.setDistance(newDistance);
			ph.setSeed(seed);
			ph.setUrl(url);
			queue.add(ph);

			logger.debug("CrawlUrl enqueued at distance {} URL: {}",
					newDistance, url);

			notifyNewPages();
		}

		public boolean isEmpty() {
			return queue.isEmpty();
		}

		public CrawlUrl next() {
			return queue.poll();
		}

		public void updatePage(CrawlUrl crawlUrl) {
			WebPage webPage = crawlUrl.getWebPage();

			if (crawlUrl.getDistance() < seed.getDepth()) {
				for (URI url : webPage.getOutLinks()) {
					boolean isInScope = coordinator
							.getCandidateUrlFilterChain().isInScope(crawlUrl,
									url);
					if (isInScope) {
						enqueueUrl(url, crawlUrl.getDistance() + 1);
					}
				}
			}
		}
	}

	private Map<String, SeedSubfrontier> seedSubfrontiers = new HashMap<String, SeedSubfrontier>();

	private UrlUniqFilter urlUniqFilter = new UrlUniqFilter();

	private class ScheduledSeedRefresher implements Runnable {
		private final Seed seed;

		public ScheduledSeedRefresher(Seed seed) {
			super();
			this.seed = seed;
		}

		
		public void run() {
			putSeed(seed);
		}

	}

	private ScheduledExecutorService refreshScheduler = Executors
			.newScheduledThreadPool(2);

	private ReadWriteLock seedsAccessLock = new ReentrantReadWriteLock();

	/* ************* CORE ACCESS METHODS ************* */

	/**
	 * Put the seed into seedSubfrontiers, urlUniqFilters and refreshSchedulers.
	 * It assumes that the seed is appropriate (enabled and is at the
	 * appropriate time), but does not assume any old seeds with the same name
	 * is already added or not.
	 * <p>
	 * After calling this, the seed will be actually put.
	 * 
	 * @param seed
	 *            The seed to put.
	 */
	private void putSeed(Seed seed) {
		seedsAccessLock.writeLock().lock();
		try {
			SeedSubfrontier ssf = new SeedSubfrontier(seed);
			SeedSubfrontier oldSsf = seedSubfrontiers.put(seed.getName(), ssf);

			if (oldSsf != null) {
				ScheduledFuture<?> oldRefreshFuture = oldSsf.getRefreshFuture();
				oldRefreshFuture.cancel(false);
			}

			urlUniqFilter.removeSeed(seed.getName());

			ScheduledFuture<?> refreshFuture = refreshScheduler.schedule(
					new ScheduledSeedRefresher(seed), seed.getRefresh(),
					TimeUnit.SECONDS);

			logger.debug("Next refresh of {} scheduled {} seconds later.",
					seed.getName(), refreshFuture.getDelay(TimeUnit.SECONDS));

			ssf.setRefreshFuture(refreshFuture);

			ssf.init();

			logger.info("Seed reloaded: {}", seed.getName());
		} finally {
			seedsAccessLock.writeLock().unlock();
		}
	}

	/**
	 * Actually remove a seed from internal data structures (seedSubfrontiers,
	 * etc.). Also cleanup the urlUniqFilter and cancel scheduled task(s).
	 * 
	 * @param seedName
	 */
	private void removeSeed(String seedName) {
		seedsAccessLock.writeLock().lock();
		try {
			SeedSubfrontier oldSeedSubfrontier = seedSubfrontiers
					.remove(seedName);

			if (oldSeedSubfrontier != null) {
				ScheduledFuture<?> future = oldSeedSubfrontier
						.getRefreshFuture();
				if (future != null) {
					future.cancel(false);
				}
			}

			urlUniqFilter.removeSeed(seedName);

			logger.info("Seed removed: {}", seedName);
		} finally {
			seedsAccessLock.writeLock().unlock();
		}
	}

	/* ************* SEEDS RELOADING ************* */

	
	public void reloadSeeds() {
		seedsAccessLock.writeLock().lock();
		try {
			logger.info("Reloading all seeds...");
			HashSet<String> currentSeeds = new HashSet<String>(
					seedSubfrontiers.keySet());

			for (Seed seed : getDao().getEnabledSeeds()) {
				putSeed(seed);
				currentSeeds.remove(seed.getName());
			}

			for (String seedName : currentSeeds) {
				removeSeed(seedName);
			}
		} finally {
			seedsAccessLock.writeLock().unlock();
		}
	}

	
	public void reloadSeed(String seedName) {
		seedsAccessLock.writeLock().lock();
		try {
			Seed seed = getDao().getEnabledSeed(seedName);
			if (seed == null) {
				removeSeed(seedName);
			} else {
				putSeed(seed);
			}
		} finally {
			seedsAccessLock.writeLock().unlock();
		}
	}

	/* ************* RETRIEVING CRAWLURLS ************* */

	private Condition newWebPageArriveCondition = seedsAccessLock.writeLock()
			.newCondition();

	
	public CrawlUrl next() throws InterruptedException {
		CrawlUrl crawlUrl;
		logger.trace("next(): Locking seedsAccessLock...");
		seedsAccessLock.writeLock().lock();
		logger.trace("next(): Locked seedsAccessLock.");
		try {
			while (true) {
				logger.trace("next(): polling for url...");
				crawlUrl = poll();
				logger.trace("next(): polled for url.");
				if (crawlUrl != null) {
					logger.trace("next(): polled url is not null. break.");
					break;
				}

				logger.trace("next(): awaiting newWebPageArriveCondition...");
				newWebPageArriveCondition.await();
				logger.trace("next(): awaited newWebPageArriveCondition.");

			}
		} finally {
			logger.trace("next(): Unlocking seedsAccessLock...");
			seedsAccessLock.writeLock().unlock();
			logger.trace("next(): Unlocked seedsAccessLock.");
		}

		logger.debug("Yielding CrawlUrl: {}", crawlUrl.getUrl());
		return crawlUrl;
	}

	private static final Random rnd = new Random();

	/**
	 * Get the next CrawlUrl or return null when not available.
	 * 
	 * @return The next CrawlUrl to crawl or return null when not available.
	 * @throws InterruptedException
	 *             Thrown when the current thread is interrupted while acquiring
	 *             read lock.
	 */
	private CrawlUrl poll() throws InterruptedException {
		if (suspended) {
			return null;
		}

//		logger.trace("poll(): Locking seedsAccessLock...");
//		seedsAccessLock.writeLock().lockInterruptibly();
//		logger.trace("poll(): Locked seedsAccessLock.");

		try {
			List<SeedSubfrontier> nonEmptyLists = new ArrayList<SeedSubfrontier>();
			
			for (SeedSubfrontier sf : seedSubfrontiers.values()) {
				if (!sf.isEmpty()) {
					nonEmptyLists.add(sf);
				}
			}

			if (nonEmptyLists.isEmpty()) {
				return null;
			}

			SeedSubfrontier sf = nonEmptyLists.get(rnd.nextInt(nonEmptyLists
					.size()));

			return sf.next();
		} finally {
//			logger.trace("poll(): Unlocking seedsAccessLock...");
//			seedsAccessLock.writeLock().unlock();
//			logger.trace("poll(): Unlocked seedsAccessLock.");
		}
	}

	/* ************* NEW WEB PAGE AVAILABLILTY NOTIFY ************* */

	/**
	 * Equivalent to notifyNewPages(false).
	 */
	private void notifyNewPages() {
		notifyNewPages(false);
	}

	/**
	 * Notify that some new URLs are available.
	 * 
	 * @param all
	 *            True if notify all waiting toe threads. Useful when the
	 *            frontier is unsuspended.
	 */
	private void notifyNewPages(boolean all) {
		logger.trace("notifyNewPages(): Locking nwpaLock...");
		seedsAccessLock.writeLock().lock();
		logger.trace("notifyNewPages(): Unlocking nwpaLock.");
		try {
			logger.trace("notifyNewPages(): signaling newWebPageArriveCondition...");
			if (all) {
				newWebPageArriveCondition.signalAll();
			} else {
				newWebPageArriveCondition.signal();
			}
			logger.trace("notifyNewPages(): signaled newWebPageArriveCondition...");
		} finally {
			logger.trace("notifyNewPages(): Unlocking nwpaLock...");
			seedsAccessLock.writeLock().unlock();
			logger.trace("notifyNewPages(): Unlocked nwpaLock...");
		}
	}

	
	public void updatePage(CrawlUrl crawlUrl) throws InterruptedException {
		seedsAccessLock.writeLock().lockInterruptibly();
		try {
			String seedName = crawlUrl.getSeed().getName();
			SeedSubfrontier seedSubfrontier = seedSubfrontiers.get(seedName);
			if (seedSubfrontier == null) {
				logger.debug("Seed {} not available. URL {} not added.",
						seedName, crawlUrl.getWebPage().getUrl());
				return;
			} else {
				seedSubfrontier.updatePage(crawlUrl);
			}
		} finally {
			seedsAccessLock.writeLock().unlock();
		}
	}

	/* ************* LIFECYCLE ************* */

	
	public void start() {
		// Do nothing
	}

	
	public void stop() {
		refreshScheduler.shutdownNow();
	}
}
