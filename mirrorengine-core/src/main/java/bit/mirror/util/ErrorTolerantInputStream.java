package bit.mirror.util;

import java.io.FilterInputStream;
import java.io.InputStream;

/**
 * An input stream that silently ignore Exception's thrown while reading the
 * stream. When an Exception is thrown, it is recorded in an internal
 * exception field which can be accessed by the getException method. Then it
 * pretends that the underlying InputStream has reached its end.
 * <p>
 * This class does not automatically close the underlying InputStream. You need
 * to close this stream manually even if IOException is thrown.
 */
public class ErrorTolerantInputStream extends FilterInputStream {

	private Exception exception = null;

	public Exception getException() {
		return exception;
	}

	public ErrorTolerantInputStream(InputStream in) {
		super(in);
	}

	
	public int read() {
		if (exception != null) {
			try {
				return super.read();
			} catch (Exception e) {
				exception = e;
				return -1;
			}
		} else {
			return -1;
		}
	}
}
