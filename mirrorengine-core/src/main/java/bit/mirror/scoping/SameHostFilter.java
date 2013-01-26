package bit.mirror.scoping;

import java.net.URI;

import bit.mirror.core.CrawlUrl;
import bit.mirror.data.Seed;

/**
 * If the seed did not define any interest, accept the candicateUrls that is on
 * the same host of any initial urls. Otherwise pass through.
 */
public class SameHostFilter implements CandidateUrlFilter {

	
	public DecideResult isInScope(CrawlUrl crawlUrl, URI candidateUrl) {
		Seed seed = crawlUrl.getSeed();

		if (seed.getInterests().isEmpty()) {
			String urlHost = candidateUrl.getHost();
			if (urlHost.startsWith("www.")) {
				urlHost = urlHost.substring(4);
			}

			for (URI initUrl : seed.getInitialUrls()) {
				String initUrlHost = initUrl.getHost();
				if (initUrlHost.startsWith("www.")) {
					initUrlHost = initUrlHost.substring(4);
				}
				if (urlHost.equals(initUrlHost)) {
					return DecideResult.ACCEPT;
				}
			}

			return DecideResult.REJECT;
		} else {
			return DecideResult.NONE;
		}
	}

}
