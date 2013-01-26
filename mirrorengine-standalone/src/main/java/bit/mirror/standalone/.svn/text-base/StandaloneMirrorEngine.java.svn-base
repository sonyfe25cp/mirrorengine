package bit.mirror.standalone;

import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import ch.qos.logback.classic.Logger;
import bit.mirror.MirrorEngine;

public class StandaloneMirrorEngine extends MirrorEngine {
	@SuppressWarnings("unused")
	private static final Logger logger = (Logger) LoggerFactory
			.getLogger(StandaloneMirrorEngine.class);

	public static void main(String[] args) {
		CmdLineParams clp = new CmdLineParams();
		JCommander jc = new JCommander(clp);

		try {
			jc.parse(args);
		} catch (ParameterException e) {
			System.out.println(e.getMessage());
			jc.usage();
			return;
		}

		if (clp.help) {
			jc.usage();
			return;
		}

		StandaloneMirrorEngine sme = new StandaloneMirrorEngine();

		sme.getCoordinator().setLoadSeedsOnStartup(clp.loadSeeds);
		sme.getCoordinator().setSuspended(clp.suspendedOnStartup);

		sme.start();
	}
}

class CmdLineParams {
	@Parameter(names = { "-help", "-h" }, description = "Display this help and exit.")
	public boolean help = false;

	@Parameter(names = { "-port" }, description = "Port of the RESTful interface server.")
	public Integer port = 64776;

	@Parameter(names = { "-load-seeds" }, description = "After this amount of time (in seconds) "
			+ "after starting up, the server will load seeds from the database. "
			+ "If negative, the server will not load seeds unless the user tells it to.")
	public Long loadSeeds = 0L;

	@Parameter(names = { "-suspended-on-startup" }, description = "If true, the coordinator will be suspended on startup.")
	public boolean suspendedOnStartup = false;

	@Parameter(names = { "-sleep-between-fetch" }, description = "The average amount of time (in nanoseconds) between two "
			+ "consecutive fetches of each toe thread.")
	public Long sleepBetweenFetch = 3000L;

	@Parameter(names = { "-norest" }, description = "Do not use the REST interface.")
	public boolean noRest;
}