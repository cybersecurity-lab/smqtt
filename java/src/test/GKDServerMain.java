package test;

import java.io.IOException;

import org.zoolu.util.Flags;
import org.zoolu.util.SystemUtils;
import org.zoolu.util.log.DefaultLogger;
import org.zoolu.util.log.WriterLogger;

import it.unipr.netsec.smqtt.gkd.GKDServer;
import it.unipr.netsec.smqtt.gkd.method.SlottedGKDService;


/** GKD server.
 * <p>
 * It requires a standard MQTT broker.
 * The address of the broker can be passed at command-line using the '-b' option.
 */
public class GKDServerMain {
	private GKDServerMain() {}

	/** Default MQTT broker */
	private static String DEFAULT_BROKER= "127.0.0.1:1883";

	private static int log2(long n) {
		return 31 - Long.numberOfLeadingZeros(n);
	}

	/** Main method.
	 * @param args command-line options
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		var flags= new Flags(args);
		var verbose= flags.getBoolean("-v","verbose mode");
		var broker= flags.getString("-b",DEFAULT_BROKER,"broker","socket address of the MQTT broker (default is "+DEFAULT_BROKER+")");
		var n= flags.getLong("-x",-1,"num","number of time slots (default is "+(1<<SlottedGKDService.TREE_DEPTH)+")");
		var prompt= flags.getBoolean("-prompt","prompts to exit");
		var help= flags.getBoolean("-h","shows this help and extits");

		if (help) {
			System.out.println(flags.toUsageString(GKDServerMain.class));
			return;
		}
		
		if (verbose) {
			DefaultLogger.setLogger(new WriterLogger(System.out));
			GKDServer.VERBOSE= true;
			SlottedGKDService.VERBOSE= true;
		}
		
		if (n>0) {
			SlottedGKDService.TREE_DEPTH= log2(n-1)+1;
			System.out.println("Number of slots: "+n);
			System.out.println("Tree depth: "+SlottedGKDService.TREE_DEPTH);
		}
		
		if (prompt) SystemUtils.run(()->{
			System.out.println("Press 'ENTER' to exit");
			System.in.read();
			System.exit(0);
		});

		
		new GKDServer("server",broker);
	}

}
