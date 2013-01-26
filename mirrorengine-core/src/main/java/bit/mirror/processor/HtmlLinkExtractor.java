package bit.mirror.processor;

import java.net.URI;
import java.util.List;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bit.mirror.core.Coordinator;
import bit.mirror.core.CrawlUrl;
import bit.mirror.core.FetchAbortedException;
import bit.mirror.core.Processor;
import bit.mirror.data.WebPage;
/**
 * HtmlLinkExtractor用来获取网页里面的超链接，具体的保存留在WebPgeReporter
 */
public class HtmlLinkExtractor implements Processor {
	private static final Logger logger = LoggerFactory
			.getLogger(HtmlLinkExtractor.class);

	private HtmlCleaner htmlCleaner = new HtmlCleaner();

	
	public void process(CrawlUrl crawlUrl, Coordinator coordinator)
			throws FetchAbortedException, InterruptedException {
		WebPage webPage = crawlUrl.getWebPage();
		String content = webPage.getContent();

		TagNode root = htmlCleaner.clean(content);

		@SuppressWarnings("unchecked")
		List<TagNode> anchors = (List<TagNode>) root.getElementListByName("a",
				true);

		URI oldUri = webPage.getUrl();

		/*
		 * Here we accept valid (but not necessarily in scope) links and convert
		 * them into absolute URLs.
		 */
		for (TagNode node : anchors) {
			String href = node.getAttributeByName("href");

			if (href == null) {
				continue;
			}

			URI newUri;
			try {
				newUri = oldUri.resolve(href.trim());
			} catch (IllegalArgumentException e) {
				continue;
			}

			if (newUri == null) {
				continue;
			}

			webPage.getOutLinks().add(newUri);
		}

		logger.debug("{} links extracted from URL: {}", webPage.getOutLinks()
				.size(), webPage.getUrl());
	}

}
