package bit.mirror.scoping;

import java.net.URI;

import bit.mirror.core.CrawlUrl;

/**
 * Accept all URLs equal to any InitialUrl of a seed. Otherwise passthrough
 */
public class InitialUrlFilter implements CandidateUrlFilter {

	
	public DecideResult isInScope(CrawlUrl crawlUrl, URI candidateUrl) {
		for(URI initUrl : crawlUrl.getSeed().getInitialUrls()) {
			if (candidateUrl.equals(initUrl)) {
				return DecideResult.ACCEPT;
			}
		}
		return DecideResult.NONE;
	}

}
