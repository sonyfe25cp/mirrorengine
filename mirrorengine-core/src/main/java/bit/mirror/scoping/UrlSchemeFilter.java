package bit.mirror.scoping;

import java.net.URI;
import bit.mirror.core.CrawlUrl;

/**
 * Non-HTTP/HTTPS URLs are rejected. Others are passed through.
 */
public class UrlSchemeFilter implements CandidateUrlFilter {

	
	public DecideResult isInScope(CrawlUrl crawlUrl, URI candidateUrl) {
		String scheme = candidateUrl.getScheme();
		if (!(scheme.equals("http") || scheme.equals("https"))) {
			return DecideResult.REJECT;
		} else {
			return DecideResult.NONE;
		}
	}

}
