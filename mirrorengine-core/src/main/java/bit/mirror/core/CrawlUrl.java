package bit.mirror.core;

import java.net.URI;

import bit.mirror.data.FetchRecord;
import bit.mirror.data.Seed;
import bit.mirror.data.WebPage;

public class CrawlUrl implements Comparable<CrawlUrl> {

	private Seed seed = null;
	private int distance;
	private URI url = null;

	private FetchRecord fetchRecord = null;
	private WebPage webPage = null;

	private boolean shouldSaveFetchRecord = false;
	private boolean shouldSaveWebPage = false;
	private boolean shouldPushBack = false;

	public Seed getSeed() {
		return seed;
	}

	public void setSeed(Seed seed) {
		this.seed = seed;
	}

	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public URI getUrl() {
		return url;
	}

	public void setUrl(URI url) {
		this.url = url;
	}

	public FetchRecord getFetchRecord() {
		return fetchRecord;
	}

	public void setFetchRecord(FetchRecord fetchRecord) {
		this.fetchRecord = fetchRecord;
	}

	public WebPage getWebPage() {
		return webPage;
	}

	public void setWebPage(WebPage webPage) {
		this.webPage = webPage;
	}

	public boolean isShouldSaveFetchRecord() {
		return shouldSaveFetchRecord;
	}

	public void setShouldSaveFetchRecord(boolean shouldSaveFetchRecord) {
		this.shouldSaveFetchRecord = shouldSaveFetchRecord;
	}

	public boolean isShouldSaveWebPage() {
		return shouldSaveWebPage;
	}

	public void setShouldSaveWebPage(boolean shouldSaveWebPage) {
		this.shouldSaveWebPage = shouldSaveWebPage;
	}

	public boolean isShouldPushBack() {
		return shouldPushBack;
	}

	public void setShouldPushBack(boolean shouldPushBack) {
		this.shouldPushBack = shouldPushBack;
	}

	
	public int compareTo(CrawlUrl o) {
		return distance - o.distance;
	}

}
