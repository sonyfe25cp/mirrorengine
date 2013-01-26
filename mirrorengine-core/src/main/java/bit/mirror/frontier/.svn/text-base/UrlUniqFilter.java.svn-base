package bit.mirror.frontier;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Records which URLs are visited.
 * 
 * Note: Not thread safe. Requires external synchronization.
 */
public class UrlUniqFilter {
	private Set<UrlVisitRecord> records = new HashSet<UrlVisitRecord>();

	private Map<URI, UrlVisitRecord> byUrl = new HashMap<URI, UrlVisitRecord>();
	private Map<String, List<UrlVisitRecord>> bySeed = new HashMap<String, List<UrlVisitRecord>>();

	/**
	 * Try visiting an URL if it is not yet visited. Do not visit if it is
	 * already visited by this seed or by another seed.
	 * 
	 * @param seedName
	 *            The name of the seed that the URL comes from.
	 * @param url
	 *            The URL to visit.
	 * @return true if successfully visited. false otherwise.
	 */
	public boolean visitIfPossible(String seedName, URI url) {
		UrlVisitRecord oldRecord = byUrl.get(url);
		if (oldRecord != null) {
			return false;
		}

		UrlVisitRecord record = new UrlVisitRecord();
		record.setSeedName(seedName);
		record.setUrl(url);
		record.setVisitTime(new Date());

		records.add(record);
		byUrl.put(url, record);

		List<UrlVisitRecord> seedVisitRecords = bySeed.get(seedName);
		if (seedVisitRecords == null) {
			seedVisitRecords = new ArrayList<UrlVisitRecord>();
			bySeed.put(seedName, seedVisitRecords);
		}
		seedVisitRecords.add(record);
		return true;
	}

	/**
	 * Remove all records created by a seed.
	 * 
	 * @param seedName
	 *            The Seed's name.
	 */
	public void removeSeed(String seedName) {
		List<UrlVisitRecord> myRecords = bySeed.remove(seedName);
		if (myRecords == null) {
			return;
		}

		for (UrlVisitRecord rec : myRecords) {
			records.remove(rec);
			byUrl.remove(rec.getUrl());
		}
	}
}
