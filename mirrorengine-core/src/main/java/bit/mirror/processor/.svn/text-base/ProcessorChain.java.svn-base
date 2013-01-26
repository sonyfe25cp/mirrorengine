package bit.mirror.processor;

import java.net.URI;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bit.mirror.core.Coordinator;
import bit.mirror.core.CrawlUrl;
import bit.mirror.core.FetchAbortedException;
import bit.mirror.core.Processor;
/**
 * 用来保存Processor的链表
 */
public class ProcessorChain extends LinkedList<Processor> {
	private static final Logger logger = LoggerFactory
			.getLogger(ProcessorChain.class);

	private static final long serialVersionUID = 3976328884053170654L;

	public void process(CrawlUrl crawlUrl, Coordinator coordinator)
			throws FetchAbortedException, InterruptedException {
		URI url = crawlUrl.getUrl();
		for (Processor processor : this) {
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}

			logger.trace("Entering {}. URL: {}",
					processor.getClass().getName(), url);

			processor.process(crawlUrl, coordinator);

			logger.trace("Leaving {}. URL: {}", processor.getClass().getName(),
					url);
		}

	}
}
