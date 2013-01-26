package bit.mirror.dao.mongo;

import java.net.URI;
import java.util.Date;

import org.bson.types.ObjectId;

import bit.mirror.MirrorEngineConfigException;
import bit.mirror.dao.MirrorEngineDao;
import bit.mirror.data.FetchRecord;
import bit.mirror.data.Interest;
import bit.mirror.data.Seed;
import bit.mirror.data.WebPage;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Key;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.mongodb.Mongo;

/**
 * A DAO based on MongoDB and Morphia.
 * <p>
 * If the user does not provide a Datastore, this class helps creating a
 * Datastore instance using the "mirrorengine" collection and the current Mongo
 * instance in the "mongo" property. In this case, if the mongo property is
 * still null, then this class also creates a Mongo instance using the default
 * settings (localhost, default port, no authentication).
 */
public class MongoDao implements MirrorEngineDao {

	/* ************* DEFAULT DATABASE CONFIGURATIONS ************* */

	/* ******* MORPHIA ******* */
	private Morphia morphia = new Morphia();

	{
		morphia.map(Interest.class);
		morphia.map(Seed.class);
		morphia.map(WebPage.class);
		morphia.map(FetchRecord.class);
	}

	/* ******* MONGO DB ******* */
	private Mongo mongo = null;

	public Mongo getMongo() {
		return mongo;
	}

	public void setMongo(Mongo mongo) {
		this.mongo = mongo;
	}

	private String mongoDbName = "mirrorengine";
	private String mongoUserName = null;
	private String mongoPassword = null;

	public String getMongoDbName() {
		return mongoDbName;
	}

	public void setMongoDbName(String mongoDbName) {
		this.mongoDbName = mongoDbName;
	}

	public String getMongoUserName() {
		return mongoUserName;
	}

	public void setMongoUserName(String mongoUserName) {
		this.mongoUserName = mongoUserName;
	}

	public String getMongoPassword() {
		return mongoPassword;
	}

	public void setMongoPassword(String mongoPassword) {
		this.mongoPassword = mongoPassword;
	}

	/* ******* MORPHIA DATASTORE ******* */
	private Datastore datastore = null;

	public Datastore getDatastore() {
		return datastore;
	}

	public void setDatastore(Datastore datastore) {
		this.datastore = datastore;
		this.mongo = datastore.getMongo();
	}

	/* ************* LIFECYCLE ************* */

	public void start() {
		if (datastore == null) {
			if (mongo == null) {
				try {
					mongo = new Mongo();
				} catch (Exception e) {
					throw new MirrorEngineConfigException(
							"Error configuring MongoDb", e);
				}
			}

			try {
				char[] pwd = null;
				if (mongoPassword != null) {
					pwd = mongoPassword.toCharArray();
				}
				datastore = morphia.createDatastore(mongo, mongoDbName,
						mongoUserName, pwd);
			} catch (Exception e) {
				throw new MirrorEngineConfigException(
						"Error creating datastore", e);
			}
		}
	}

	public void stop() {
		mongo.close();
	}

	public Iterable<Seed> getSeeds() {
		return datastore.find(Seed.class).order("_id");
	}

	public Iterable<Seed> getSeeds(int skip, int count) {
		Query<Seed> q = datastore.find(Seed.class).order("_id").offset(skip);
		if (count >= 0) {
			q = q.limit(count);
		}

		return q;
	}

	public long getSeedCount() {
		return datastore.find(Seed.class).countAll();
	}

	public long getSeedCount(String type) {
		return datastore.find(Seed.class).filter("type", type).countAll();
	}

	public Iterable<Seed> getEnabledSeeds() {
		return datastore.find(Seed.class).filter("enabled ==", true)
				.order("_id");
	}

	public Seed getSeed(String seedName) {
		return datastore.getByKey(Seed.class, new Key<Seed>(Seed.class,
				seedName));
	}

	public Seed getEnabledSeed(String seedName) {
		return datastore.find(Seed.class).filter("name ==", seedName)
				.filter("enabled ==", true).get();
	}

	public void saveSeed(Seed seed) {
		datastore.save(seed);
		System.out.println("save seed over~");
	}

	public void deleteSeed(String seedName) {
		datastore.delete(Seed.class, seedName);
	}

	public void setSeedEnabled(String seedName, boolean enabled) {
		Key<Seed> key = new Key<Seed>(Seed.class, seedName);
		datastore.update(
				key,
				datastore.createUpdateOperations(Seed.class).set("enabled",
						enabled));
	}

	public long getWebPageCount() {
		return datastore.find(WebPage.class).countAll();
	}

	public WebPage getWebPageByIdString(String idString) {
		ObjectId id;
		try {
			id = new ObjectId(idString);
		} catch (IllegalArgumentException e) {
			return null;
		}
		return datastore.get(WebPage.class, id);
	}

	public WebPage getWebPageByIdStringMetaOnly(String idString) {
		ObjectId id;
		try {
			id = new ObjectId(idString);
		} catch (IllegalArgumentException e) {
			return null;
		}
		return datastore.find(WebPage.class).filter("id ==", id)
				.retrievedFields(false, "entity", "content").get();
	}

	public WebPage getWebPageByUrl(URI url) {
		return getWebPageByUrl(url.toString());
	}

	public WebPage getWebPageByUrl(String url) {
//		if (datastore.find(WebPage.class).filter("url ==", url).countAll() > 0)
			return datastore.find(WebPage.class).filter("url ==", url).get();
//		else
//			return null;
	}

	public WebPage getWebPageByUrlMetaOnly(URI url) {
		return getWebPageByUrlMetaOnly(url.toString());
	}

	public WebPage getWebPageByUrlMetaOnly(String url) {
		return datastore.find(WebPage.class).filter("url ==", url)
				.retrievedFields(false, "entity", "content").get();
	}

	public void saveWebPage(WebPage webPage) {
		datastore.updateFirst(
				datastore.find(WebPage.class).filter("url ==",
						webPage.getUrl().toString()), webPage, true);
	}

	public Iterable<WebPage> getRecentWebPages(Date earlyBound) {
		return datastore.find(WebPage.class).filter("fetchDate >=", earlyBound);
	}

	public Iterable<WebPage> getWebPagesBetweenDate(Date earlyBound,
			Date lateBound) {

		return datastore.find(WebPage.class).filter("fetchDate >=", earlyBound)
				.filter("fetchDate <", lateBound);
	}

	public Iterable<WebPage> getRecentWebPagesMetaOnly(int offset, int limit) {
		return datastore.find(WebPage.class)
				.retrievedFields(false, "entity", "content").offset(offset)
				.limit(limit);
	}

	public FetchRecord getFetchRecordByUrl(URI url) {
		return getFetchRecordByUrl(url.toString());
	}

	public FetchRecord getFetchRecordByUrl(String url) {
		return datastore.find(FetchRecord.class).filter("url ==", url).get();
	}

	public Iterable<FetchRecord> getRecentFetchRecords(int skip, int count) {
		return datastore.find(FetchRecord.class).order("-fetchDate")
				.offset(skip).limit(count);
	}

	public void saveFetchRecord(FetchRecord fetchRecord) {
		datastore.save(fetchRecord);
	}

}
