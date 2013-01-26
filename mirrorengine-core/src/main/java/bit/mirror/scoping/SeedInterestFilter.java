package bit.mirror.scoping;

import java.net.URI;

import bit.mirror.core.CrawlUrl;
import bit.mirror.data.Interest;
import bit.mirror.data.Seed;

/**
 * If the seed has defined at least one interest, accept the candicateUrls that
 * matches any interest's regexp. Otherwise pass through.
 */
public class SeedInterestFilter implements CandidateUrlFilter {

	
	public DecideResult isInScope(CrawlUrl crawlUrl, URI candidateUrl) {
		Seed seed = crawlUrl.getSeed();

		if (!seed.getInterests().isEmpty()) {
			String urlString = candidateUrl.toString();

			for (Interest interest : seed.getInterests()) {
				if (interest.getRegexp().matcher(urlString).matches()) {
					return DecideResult.ACCEPT;
				}
			}

			return DecideResult.REJECT;
		} else {
			return DecideResult.NONE;
		}
	}
}
