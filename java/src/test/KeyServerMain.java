package test;

import java.io.IOException;

import org.zoolu.util.Flags;
import org.zoolu.util.SystemUtils;
import org.zoolu.util.log.DefaultLogger;
import org.zoolu.util.log.WriterLogger;

import it.unipr.netsec.smqtt.gkd.KeyServer;
import it.unipr.netsec.smqtt.gkd.method.SlottedGKDService;


/** Key Server (KS).
 * <p>
 * It requires a standard MQTT broker.
 * The address of the broker can be passed at command-line using the '-b' option.
 */
public class KeyServerMain {
	private KeyServerMain() {}

	/** Default MQTT broker */
	private static String DEFAULT_BROKER= "127.0.0.1:1883";

	/** Main method.
	 * @param args command-line options
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		var flags= new Flags(args);
		var verbose= flags.getBoolean("-v","verbose mode");
		var broker= flags.getString("-b",DEFAULT_BROKER,"broker","socket address of the MQTT broker (default is "+DEFAULT_BROKER+")");
		var slot= flags.getInteger("-slot",SlottedGKDService.TIMESLOT,"sec","time slot duration [s] (default is "+SlottedGKDService.TIMESLOT+"s)");
		var depth= flags.getInteger("-depth",SlottedGKDService.TREE_DEPTH,"depth","tree depth (default is "+SlottedGKDService.TREE_DEPTH+")");
		var prompt= flags.getBoolean("-prompt","prompts to exit");
		var help= flags.getBoolean("-h","shows this help and extits");

		if (help) {
			System.out.println(flags.toUsageString(KeyServerMain.class));
			return;
		}
		
		if (verbose) {
			DefaultLogger.setLogger(new WriterLogger(System.out));
			KeyServer.VERBOSE= true;
		}
		
		SlottedGKDService.TIMESLOT= slot;
		SlottedGKDService.TREE_DEPTH= depth;
		System.out.println("Timeslot: "+slot+"s");
		System.out.println("Tree depth: "+depth);
		var slotNum= 1L<<depth;
		System.out.println("Number of slots: "+slotNum);
		var maxTime= slotNum*slot;
		System.out.println("Maximum time: "+(maxTime<60? maxTime+"s" : maxTime<3600? maxTime/60+"min" : maxTime<(3600*24)? maxTime/3600+"h" :  maxTime/3600/24+" days"));
	
		if (prompt) SystemUtils.run(()->{
			System.out.println("Press 'ENTER' to exit");
			System.in.read();
			System.exit(0);
		});
		
		new KeyServer("server",broker);
	}

}
