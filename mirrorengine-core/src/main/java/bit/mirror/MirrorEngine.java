package bit.mirror;

import bit.mirror.core.Coordinator;
import bit.mirror.core.Lifecycle;
import bit.mirror.dao.MirrorEngineDao;
import bit.mirror.dao.mongo.MongoDao;

/**
 * A facade class for users to use the Mirror Engine.
 * <p>
 */
public class MirrorEngine implements Lifecycle {
	private Coordinator coordinator = new Coordinator();

	public Coordinator getCoordinator() {
		return coordinator;
	}

	public void setCoordinator(Coordinator coordinator) {
		this.coordinator = coordinator;
	}

	private MirrorEngineDao dao = null;

	public MirrorEngineDao getDao() {
		return dao;
	}

	public void setDao(MirrorEngineDao dao) {
		this.dao = dao;
	}

	
	public void start() throws MirrorEngineConfigException {
		if (dao == null) {
			dao = new MongoDao();
		}
		coordinator.setDao(dao);

		coordinator.start();
	}

	
	public void stop() {
		coordinator.stop();
	}
}
