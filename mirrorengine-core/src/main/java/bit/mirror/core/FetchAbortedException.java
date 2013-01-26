package bit.mirror.core;

public class FetchAbortedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public FetchAbortedException(String message) {
		super(message);
	}

}
