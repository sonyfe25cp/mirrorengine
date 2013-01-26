package bit.mirror.data;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.PostLoad;
import com.google.code.morphia.annotations.PrePersist;
import com.google.code.morphia.annotations.Transient;

@Entity
public class Seed {
	private static final Logger logger = LoggerFactory.getLogger(Seed.class);

	@Id
	private String name;

	@Indexed
	private List<String> initialUrls = new ArrayList<String>();

	@Transient
	private List<URI> _initialUrls = new ArrayList<URI>();

	@Embedded
	private List<Interest> interests = new ArrayList<Interest>();

	@Indexed
	private boolean enabled = true;
	private int depth = 2;

	private String account = null;
	private String password = null;

	// 默认设置为news
	private String type = "NEWS";

	// 用来标志seed健康程度的标志
	// 0 健康； 1 微薄帐号过期；余下的待补充
	private int healthy = 0;
	
	/**
	 * Number (in seconds) between re-checking the same page.
	 */
	private long refresh = 3600;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@PostLoad
	public void postLoad() {
		for (String urlString : this.initialUrls) {
			try {
				URI url = new URI(urlString);
				this._initialUrls.add(url);
			} catch (URISyntaxException e) {
				logger.error("Initial URL {} of seed {} is illegal.",
						this.getName(), urlString);
			}
		}
	}

	@PrePersist
	public void prePersist() {
		this.initialUrls.clear();
		for (URI url : this._initialUrls) {
			String urlString = url.toString();
			this.initialUrls.add(urlString);
		}

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<URI> getInitialUrls() {
		return _initialUrls;
	}

	public void setInitialUrls(List<URI> initialUrls) {
		this._initialUrls = initialUrls;
	}

	public List<Interest> getInterests() {
		return interests;
	}

	public void setInterests(List<Interest> interests) {
		this.interests = interests;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public long getRefresh() {
		return refresh;
	}

	public void setRefresh(long refresh) {
		this.refresh = refresh;
	}

	public int getHealthy() {
		return healthy;
	}

	public void setHealthy(int healthy) {
		this.healthy = healthy;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
