package bit.mirror.data;

import java.util.regex.Pattern;

import com.google.code.morphia.annotations.Embedded;

@Embedded
public class Interest {

	private int refresh = 3600;

	private Pattern regexp;
	private boolean monitor = false;

	public int getRefresh() {
		return refresh;
	}

	public void setRefresh(int refresh) {
		this.refresh = refresh;
	}

	public Pattern getRegexp() {
		return regexp;
	}

	public void setRegexp(Pattern regexp) {
		this.regexp = regexp;
	}

	public boolean isMonitor() {
		return monitor;
	}

	public void setMonitor(boolean monitor) {
		this.monitor = monitor;
	}

}
