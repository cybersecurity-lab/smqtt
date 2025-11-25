package test;

import java.io.IOException;

import org.zoolu.util.Flags;
import org.zoolu.util.Random;
import org.zoolu.util.Range;
import org.zoolu.util.SystemUtils;
import org.zoolu.util.log.DefaultLogger;
import org.zoolu.util.log.WriterLogger;

import it.unipr.netsec.smqtt.SecureMqttClient;
import it.unipr.netsec.smqtt.gkd.KeyRing;
import it.unipr.netsec.smqtt.gkd.KeyServer;
import it.unipr.netsec.smqtt.gkd.method.SlottedGKDClient;
import it.unipr.netsec.smqtt.gkd.method.SlottedGKDService;


/** Simple test program that runs a Key Server and a specified number of SMQTT clients.
 * <p>
 * It requires a standard MQTT broker. The address of the broker can be configured using the default
 * static attribute {@link DEFAULT_GKD_TYPE} or at command-line using the '-b' option.
 */
public class SMqttTest {
	private SMqttTest() {}

	/** Default MQTT broker */
	private static String DEFAULT_BROKER= "127.0.0.1:1883";

	/** Default GKD method type (current values: 1=static, 2=update, 3=slotted) */
	private static int DEFAULT_GKD_TYPE= 3; // 1=static, 2=update, 3=slotted
	
	/** Number of SMQTT clients */
	private static int NUM_CLIENTS= 3;
	
	/** Default MQTT QoS */
	private static final int PUBLISH_QOS= 2;

	/** Default time slot duration [s] (used in GKD method type 3) */
	private static int TIMESLOT= 10;

	/** Default number of time slots (used in GKD method type 3) */
	private static int TREE_DEPTH=10;

	/** Number of published messages */
	private static int MSG_COUNTER=0;


	/**
	 * @param id
	 * @param broker
	 * @throws IOException
	 */
	public static void client(String id, String broker) throws IOException {
		long startDelay= Random.nextLong(SlottedGKDService.TIMESLOT*500*NUM_CLIENTS); // 1000*
		long joinDelay= Random.nextLong(2000);
		long subscribeDelay= Random.nextLong(1000);
		long publishDelay= Random.nextLong(NUM_CLIENTS*SlottedGKDService.TIMESLOT*500);
		long cleaningDalay= Random.nextLong(4000);
		
		SystemUtils.sleep(startDelay);
		var client= new SecureMqttClient(id,KeyRing.DEFAULT_KEY,broker,null);
		client.connect();
		SystemUtils.sleep(joinDelay);
		if (SecureMqttClient.GKD_TYPE==3) {
			var expires= (int)((1<<TREE_DEPTH)*TIMESLOT);
			client.join("test",expires);
		}
		else client.join("test");
		SystemUtils.sleep(subscribeDelay);
		client.subscribe("test",2);
		SystemUtils.sleep(publishDelay);
		client.publish("test",PUBLISH_QOS,("Hello from "+id).getBytes());
		if (++MSG_COUNTER==NUM_CLIENTS) {
			SystemUtils.sleep(cleaningDalay);
			System.exit(0);
		}
	}

	/** Main method.
	 * @param args command-line options
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		var flags= new Flags(args);
		var veryVerbose= flags.getBoolean("-vv","verbose mode");
		var verbose= flags.getBoolean("-v","verbose mode");
		if (veryVerbose) verbose= true;
		var broker= flags.getString("-b",DEFAULT_BROKER,"broker","socket address of the MQTT broker (default is "+DEFAULT_BROKER+")");
		var slot= flags.getInteger("-slot",SlottedGKDService.TIMESLOT,"sec","time slot duration [s] (default is "+SlottedGKDService.TIMESLOT+"s)");
		var depth= flags.getInteger("-depth",SlottedGKDService.TREE_DEPTH,"depth","tree depth (default is "+SlottedGKDService.TREE_DEPTH+")");
		var gkdType= flags.getInteger("-m",DEFAULT_GKD_TYPE,"method","group key distribution method (default is "+DEFAULT_GKD_TYPE+")"); // 1=static, 2=update, 3=slotted
		NUM_CLIENTS= flags.getInteger("-n",NUM_CLIENTS,"num","number of clients (default is "+NUM_CLIENTS+")");
		var help= flags.getBoolean("-h","shows this help and extits");			

		if (help) {
			System.out.println(flags.toUsageString(SMqttTest.class));
			return;
		}

		// @@@@@@@@@@@@@@@@@@@@@@@ DEBUG MODE @@@@@@@@@@@@@@@@@@@@@@@
		veryVerbose= true;
		verbose= true;
		
		if (verbose) {
			DefaultLogger.setLogger(new WriterLogger(System.out));
			//PahoClient.VERBOSE= true;
			if (veryVerbose) KeyServer.VERBOSE= true;
			SecureMqttClient.VERBOSE= true;
			if (veryVerbose) SecureMqttClient.DEBUG= true;
		}
		
		SecureMqttClient.GKD_TYPE= gkdType;
		if (gkdType==3) {
			SlottedGKDService.TIMESLOT= slot;
			SlottedGKDService.TREE_DEPTH= depth;
			System.out.println("Timeslot: "+slot+"s");
			System.out.println("Tree depth: "+depth);
			var slotNum= 1L<<depth;
			System.out.println("Number of slots: "+slotNum);
			var maxTime= slotNum*slot;
			System.out.println("Maximum time: "+(maxTime<60? maxTime+"s" : maxTime<3600? maxTime/60+"min" : maxTime<(3600*24)? maxTime/3600+"h" :  maxTime/3600/24+" days"));
		}
		
		// SERVER
		SystemUtils.run(() -> new KeyServer("server",broker));

		// CLIENTS
		for (var i: new Range(NUM_CLIENTS)) {
			SystemUtils.run(() -> client("client-"+i,broker));
		}
		
		System.out.println("Press 'ENTER' to exit");
		System.in.read();
		System.exit(0);
	}

}
