package bit.mirror.data;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.PostLoad;
import com.google.code.morphia.annotations.PrePersist;
import com.google.code.morphia.annotations.Transient;

/**
 * This class is updated for each fetch of each URL even if fetching is not
 * successful (cannot connect or 404 error), in which case WebPage is not
 * updated.
 */
@Entity
public class FetchRecord {
	@Id
	private ObjectId id;

	@Indexed
	private String url;

	@Transient
	private URI _url;

	@Indexed
	private Date fetchDate = null;

	/**
	 * The HTTP status code, or 0 if the remote failed to respond to HTTP
	 * Requests.
	 */
	private int httpStatus = 0;

	/**
	 * Some informational error message. Usually the error message of exceptions
	 * thrown during fetching.
	 */
	private String error = null;

	@PostLoad
	public void postLoad() {
		if (this.url != null) {
			try {
				this._url = new URI(this.url);
			} catch (URISyntaxException e) {
			}
		}
	}

	@PrePersist
	public void prePersist() {
		if (this._url != null) {
			this.url = this._url.toString();
		}
	}

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public URI getUrl() {
		return this._url;
	}

	public void setUrl(URI url) {
		this._url = url;
	}

	public Date getFetchDate() {
		return fetchDate;
	}

	public void setFetchDate(Date fetchDate) {
		this.fetchDate = fetchDate;
	}

	public Integer getHttpStatus() {
		return httpStatus;
	}

	public void setHttpStatus(Integer httpStatus) {
		this.httpStatus = httpStatus;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

}
