package bit.mirror.core;

/**
 * An interface for anything that needs proper initialization and finalizing.
 */
public interface Lifecycle {
	/**
	 * Called to let this class start working.
	 */
	void start();

	/**
	 * Implementors should prepare to stop, specifically they should stop
	 * running threads inside this object.
	 */
	void stop();
}
