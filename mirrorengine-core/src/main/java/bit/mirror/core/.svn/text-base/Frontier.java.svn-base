package bit.mirror.core;

public interface Frontier extends Lifecycle {
	/**
	 * Get the next target to fetch. Block if no pages available.
	 * 
	 * @return A CrawlUrl object denoting a page to fetch. Never null.
	 * @throws InterruptedException
	 *             Thrown if the current thread is interrupted.
	 */
	public CrawlUrl next() throws InterruptedException;

	/**
	 * Tell the frontier that the page has been fetched.
	 * 
	 * @param crawlUrl
	 *            The CrawlUrl object returned by the next() method and
	 *            processed by a ToeThread.
	 * @throws InterruptedException
	 *             Thrown if the current thread is interrupted.
	 */
	public void updatePage(CrawlUrl crawlUrl) throws InterruptedException;

	/**
	 * If true, the frontier will not return any pages.
	 * 
	 * @return The suspend status.
	 */
	public boolean isSuspended();

	/**
	 * Set the suspend status.
	 * 
	 * @param suspended
	 */
	public void setSuspended(boolean suspended);

	/**
	 * Reload all seeds that are enabled from the database and remove all seeds
	 * not in the database.
	 */
	void reloadSeeds();

	/**
	 * Reload one seed from the database. Delete if not in.
	 * 
	 * @param seedName
	 */
	void reloadSeed(String seedName);
}
