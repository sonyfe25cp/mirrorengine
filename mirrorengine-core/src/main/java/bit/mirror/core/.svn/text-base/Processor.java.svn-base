package bit.mirror.core;

public interface Processor {
	/**
	 * A processor should work on a given CrawlUrl object, which is provided
	 * adequate informations by its creator or previous processors.
	 * <p>
	 * A processor could decide to skip the current chain by throwing a
	 * FetchAbortedException. Indicators should be set be throwing exception to
	 * notify later processors if the current page should be saved, or reported
	 * to frontier. This exception can be used in both normal conditions or when
	 * errors occur.
	 * <p>
	 * When the current thread is interrupted, a processor should throw an
	 * InterruptedException as soon as possible.
	 * <p>
	 * Other unchecked exceptions indicates programming errors.
	 * 
	 * @param crawlUrl
	 *            The CrawlUrl to work on.
	 * @param coordinator
	 *            The current Coordinator of this system.
	 * @throws FetchAbortedException
	 *             Thrown when decided to skip the current chain.
	 * @throws InterruptedException
	 *             Thrown when the current Thread is interrupted.
	 */
	void process(CrawlUrl crawlUrl, Coordinator coordinator)
			throws FetchAbortedException, InterruptedException;
}
