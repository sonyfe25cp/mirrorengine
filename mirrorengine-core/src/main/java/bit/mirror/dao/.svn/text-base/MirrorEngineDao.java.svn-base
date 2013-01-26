package bit.mirror.dao;

import java.net.URI;
import java.util.Date;
import bit.mirror.core.Lifecycle;
import bit.mirror.data.FetchRecord;
import bit.mirror.data.Seed;
import bit.mirror.data.WebPage;

public interface MirrorEngineDao extends Lifecycle {
	Iterable<Seed> getSeeds();
	Iterable<Seed> getSeeds(int skip, int count);
	public long getSeedCount();
	public long getSeedCount(String type);
	Iterable<Seed> getEnabledSeeds();
	Seed getSeed(String seedName);
	Seed getEnabledSeed(String seedName);
	void saveSeed(Seed seed);
	void deleteSeed(String seedName);
	void setSeedEnabled(String seedName, boolean enabled);
	
	public long getWebPageCount();
	WebPage getWebPageByIdString(String idString);
	WebPage getWebPageByUrl(URI url);
	WebPage getWebPageByUrl(String url);
	WebPage getWebPageByUrlMetaOnly(URI url);
	WebPage getWebPageByUrlMetaOnly(String url);
	void saveWebPage(WebPage webPage);
	
	Iterable<WebPage> getRecentWebPagesMetaOnly(int offset, int limit);
	
	FetchRecord getFetchRecordByUrl(URI url);
	FetchRecord getFetchRecordByUrl(String url);
	void saveFetchRecord(FetchRecord fetchRecord);
	Iterable<FetchRecord> getRecentFetchRecords(int skip, int count);
	WebPage getWebPageByIdStringMetaOnly(String idString);
	Iterable<WebPage> getRecentWebPages(Date earlyBound);
	Iterable<WebPage> getWebPagesBetweenDate(Date earlyBound, Date lateBound);
}
