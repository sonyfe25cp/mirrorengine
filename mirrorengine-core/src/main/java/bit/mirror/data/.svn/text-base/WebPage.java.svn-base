package bit.mirror.data;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.PostLoad;
import com.google.code.morphia.annotations.PrePersist;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Transient;

/**
 * This class stores a successfully fetched page.
 */
@Entity
public class WebPage {
	private static final Logger logger = LoggerFactory.getLogger(WebPage.class);

	@Id
	private ObjectId id;

	@Indexed
	private String url;

	@Transient
	private URI _url;

	private int httpStatus;

	private Map<String, List<String>> headers = new HashMap<String, List<String>>();
	private String etag;
	private Date lastModified;

	private byte[] entity;
	private String content;

	private List<String> outLinks = new ArrayList<String>();

	@Transient
	private List<URI> _outLinks = new ArrayList<URI>();

	@Indexed
	private Date fetchDate;

	@Reference
	private Seed seed;
	
	public Seed getSeed() {
		return seed;
	}

	public void setSeed(Seed seed) {
		this.seed = seed;
	}

	public boolean isParsed = false;
	
	@PostLoad
	public void postLoad() {
		if (this.url != null) {
			try {
				this._url = new URI(this.url);
			} catch (URISyntaxException e) {
			}
		}
		this._outLinks.clear();
		for (String urlString : outLinks) {
			try {
				URI url = new URI(urlString);
				this._outLinks.add(url);
			} catch (URISyntaxException e) {
				logger.debug("WebPage {} contains illegal URL: {}",
						this.getUrl(), urlString);
			}
		}
	}

	@PrePersist
	public void prePersist() {
		if (this._url != null) {
			this.url = this._url.toString();
		}
		outLinks.clear();
		for(URI uri : _outLinks) {
			outLinks.add(uri.toString());
		}
	}

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public URI getUrl() {
		if (this._url == null && this.url != null) {
			try {
				this._url = new URI(this.url);
			} catch (URISyntaxException e) {
			}
		}
		return this._url;
	}

	public void setUrl(URI url) {
		this._url = url;
		this.url = this._url.toString();
	}

	public int getHttpStatus() {
		return httpStatus;
	}

	public void setHttpStatus(int httpStatus) {
		this.httpStatus = httpStatus;
	}

	public Map<String, List<String>> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, List<String>> headers) {
		this.headers = headers;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public byte[] getEntity() {
		return entity;
	}

	public void setEntity(byte[] entity) {
		this.entity = entity;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public List<URI> getOutLinks() {
		return _outLinks;
	}

	public void setOutLinks(List<URI> outLinks) {
		this._outLinks = outLinks;
	}

	public Date getFetchDate() {
		return fetchDate;
	}

	public void setFetchDate(Date fetchDate) {
		this.fetchDate = fetchDate;
	}

	public boolean isParsed() {
		return isParsed;
	}

	public void setParsed(boolean isParsed) {
		this.isParsed = isParsed;
	}

}
