package bit.mirror.scoping;

import java.net.URI;
import java.util.LinkedList;

import bit.mirror.core.CrawlUrl;

public class CandidateUrlFilterChain extends LinkedList<CandidateUrlFilter> {

	private static final long serialVersionUID = 6286588522040268544L;

	public boolean isInScope(CrawlUrl crawlUrl, URI candidateUrl) {
		DecideResult result = DecideResult.NONE;
		for (CandidateUrlFilter cuf : this) {
			result = cuf.isInScope(crawlUrl, candidateUrl);
			if (result != DecideResult.NONE) {
				break;
			}
		}

		if (result == DecideResult.ACCEPT) {
			return true;
		} else {
			return false;
		}
	}
}
