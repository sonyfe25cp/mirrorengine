package bit.mirror.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bit.mirror.weibo.facade.SinaWeiboCrawler;

import bit.mirror.dao.MirrorEngineDao;
import bit.mirror.data.Seed;
import bit.mirror.frontier.MongoFrontier;
import bit.mirror.processor.DataSaver;
import bit.mirror.processor.HtmlDecoder;
import bit.mirror.processor.HtmlLinkExtractor;
import bit.mirror.processor.HttpFetcher;
import bit.mirror.processor.ProcessorChain;
import bit.mirror.processor.WebPageLoader;
import bit.mirror.processor.WebPageReporter;
import bit.mirror.scoping.CandidateUrlFilterChain;
import bit.mirror.scoping.InitialUrlFilter;
import bit.mirror.scoping.SameHostFilter;
import bit.mirror.scoping.SeedInterestFilter;
import bit.mirror.scoping.UrlSchemeFilter;

/**
 * A central controller of the crawling process. At run time users or UIs should
 * interact with this class to control the seeds, toe-threads and so on.
 */
public class Coordinator implements Lifecycle {
	private static final Logger logger = LoggerFactory
			.getLogger(Coordinator.class);

	/* ************* PROCESSOR CHAIN ************* */

	private ProcessorChain preFetchChain = new ProcessorChain();
	private ProcessorChain fetchChain = new ProcessorChain();
	private ProcessorChain postFetchChain = new ProcessorChain();

	public ProcessorChain getPreFetchChain() {
		return preFetchChain;
	}

	public ProcessorChain getFetchChain() {
		return fetchChain;
	}

	public ProcessorChain getPostFetchChain() {
		return postFetchChain;
	}

	private WebPageLoader webPageLoader = new WebPageLoader();
	private HttpFetcher httpFetcher = new HttpFetcher();
	private HtmlDecoder htmlDecoder = new HtmlDecoder();
	private HtmlLinkExtractor htmlLinkExtractor = new HtmlLinkExtractor();
	private DataSaver dataSaver = new DataSaver();
	private WebPageReporter webPageReporter = new WebPageReporter();

	{
		preFetchChain.add(webPageLoader);

		fetchChain.add(httpFetcher);
		fetchChain.add(htmlDecoder);
		fetchChain.add(htmlLinkExtractor);

		postFetchChain.add(dataSaver);
		postFetchChain.add(webPageReporter);
	}

	public WebPageLoader getWebPageLoader() {
		return webPageLoader;
	}

	public HttpFetcher getHttpFetcher() {
		return httpFetcher;
	}

	public HtmlDecoder getHtmlDecoder() {
		return htmlDecoder;
	}

	public HtmlLinkExtractor getHtmlLinkExtractor() {
		return htmlLinkExtractor;
	}

	public DataSaver getDataSaver() {
		return dataSaver;
	}

	public WebPageReporter getWebPageReporter() {
		return webPageReporter;
	}

	/* ************* CANDIDATE URL FILTER CHAIN ************* */

	private CandidateUrlFilterChain candidateUrlFilterChain = new CandidateUrlFilterChain();

	public CandidateUrlFilterChain getCandidateUrlFilterChain() {
		return candidateUrlFilterChain;
	}

	private UrlSchemeFilter urlSchemeFilter = new UrlSchemeFilter();
	private InitialUrlFilter initialUrlFilter = new InitialUrlFilter();
	private SeedInterestFilter seedInterestFilter = new SeedInterestFilter();;
	private SameHostFilter sameHostFilter = new SameHostFilter();

	{
		candidateUrlFilterChain.add(urlSchemeFilter);
		candidateUrlFilterChain.add(initialUrlFilter);
		candidateUrlFilterChain.add(seedInterestFilter);
		candidateUrlFilterChain.add(sameHostFilter);
	}

	public InitialUrlFilter getInitialUrlFilter() {
		return initialUrlFilter;
	}

	public UrlSchemeFilter getUrlSchemeFilter() {
		return urlSchemeFilter;
	}

	public SeedInterestFilter getSeedInterestFilter() {
		return seedInterestFilter;
	}

	public SameHostFilter getSameHostFilter() {
		return sameHostFilter;
	}

	/* ************* ACCESSORIES ************* */

	private MirrorEngineDao dao;

	public MirrorEngineDao getDao() {
		return dao;
	}

	public void setDao(MirrorEngineDao dao) {
		this.dao = dao;
	}
	
	private MongoFrontier frontier = new MongoFrontier();

	{
		frontier.setCoordinator(this);
	}

	public MongoFrontier getFrontier() {
		return frontier;
	}

	public void setFrontier(MongoFrontier frontier) {
		this.frontier = frontier;
	}

	private ToePool toePool = new ToePool();

	{
		toePool.setCoordinator(this);
	}

	public ToePool getToePool() {
		return toePool;
	}


	/* ************* CONFIGURE PROPERTIES ************* */

	/**
	 * Time (in milliseconds) for a ToeThread to sleep after processing each
	 * CrawlUrl
	 */
	private long sleepAfterCrawling = 10;

	public long getSleepAfterCrawling() {
		return sleepAfterCrawling;
	}

	public void setSleepAfterCrawling(long sleepAfterCrawling) {
		this.sleepAfterCrawling = sleepAfterCrawling;
	}

	/**
	 * Time (in seconds) after which all seeds should be loaded from the
	 * database. If negative, seeds will not be loaded until manually asked to.
	 */
	private long loadSeedsOnStartup = 0;

	public long getLoadSeedsOnStartup() {
		return loadSeedsOnStartup;
	}

	public void setLoadSeedsOnStartup(long loadSeedsOnStartup) {
		this.loadSeedsOnStartup = loadSeedsOnStartup;
	}

	/* ************* LIFECYCLE ************* */

	SinaWeiboCrawler weiboCrawler;
	public void start() {
		if (dao == null) {
			throw new IllegalStateException("DAO must not be null");
		}
		logger.info("Starting coordinator...");
		dao.start();
		frontier.start();
		toePool.start();

		/*** 初始化weibo爬中 ***/
		weiboCrawler = new SinaWeiboCrawler();
		weiboCrawler.setDao(dao);
		
		if (loadSeedsOnStartup >= 0) {
			executor.schedule(new Runnable() {
				
				public void run() {
					frontier.reloadSeeds();
				}
			}, loadSeedsOnStartup, TimeUnit.SECONDS);
		}
		logger.info("Coordinator started.");
	}

	
	public void stop() {
		logger.info("Stopping coordinator...");
		executor.shutdownNow();
		toePool.stop();
		frontier.stop();
		dao.stop();
		logger.info("Coordinator sstopped.");
	}

	/* ************* CRAWL CONTROLLING ************* */

	public boolean isSuspended() {
		return frontier.isSuspended();
	}

	public void setSuspended(boolean suspended) {
		frontier.setSuspended(suspended);
	}

	/* ************* LONG JOB SCHEDULING ************* */
	private ScheduledExecutorService executor = Executors
			.newScheduledThreadPool(2);

	/* ************* PUBLIC DATA ACCESS FACILITIES ************* */

	/* ******* OPERATIONS ******* */

	public void submitSeed(Seed seed) {
		dao.saveSeed(seed);
		frontier.reloadSeed(seed.getName());
	}

	public void deleteSeed(String seedName) {
		dao.deleteSeed(seedName);
		frontier.reloadSeed(seedName);
	}

	public void setSeedEnabled(String seedName, boolean enabled) {
		dao.setSeedEnabled(seedName, enabled);
		frontier.reloadSeed(seedName);
	}

	public void refreshSeed(String seedName) {
		frontier.reloadSeed(seedName);
	}

	/* ******* QUERIES ******* */

	public Iterable<Seed> getSeeds() {
		return dao.getSeeds();
	}

	public Seed getSeed(String seedName) {
		return dao.getSeed(seedName);
	}

	public Iterable<Seed> getSeeds(int skip, int count) {
		return dao.getSeeds(skip, count);
	}

}
