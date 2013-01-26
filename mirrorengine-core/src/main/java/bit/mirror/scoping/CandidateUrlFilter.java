package bit.mirror.scoping;

import java.net.URI;

import bit.mirror.core.CrawlUrl;

public interface CandidateUrlFilter {
	public DecideResult isInScope(CrawlUrl crawlUrl, URI candidateUrl);
}
