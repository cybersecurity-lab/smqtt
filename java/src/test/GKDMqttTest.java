package test;

import java.io.IOException;

import org.zoolu.util.Flags;
import org.zoolu.util.Random;
import org.zoolu.util.Range;
import org.zoolu.util.SystemUtils;
import org.zoolu.util.log.DefaultLogger;
import org.zoolu.util.log.WriterLogger;

import it.unipr.netsec.smqtt.SecureMqttClient;
import it.unipr.netsec.smqtt.gkd.GKDServer;
import it.unipr.netsec.smqtt.gkd.method.SlottedGKDClient;
import it.unipr.netsec.smqtt.gkd.method.SlottedGKDService;


/** Simple test program that runs a GKD server and a specified number of SMQTT clients.
 * <p>
 * It requires a standard MQTT broker.  The address of the broker can be configured using the default
 * static attribute {@link DEFAULT_GKD_TYPE} or at command-line using the '-b' option.
 */
public class GKDMqttTest {
	private GKDMqttTest() {}

	/** Default MQTT broker */
	private static String DEFAULT_BROKER= "127.0.0.1:1883";

	/** Default GKD method type (current values: 1=static, 2=update, 3=slotted) */
	private static int DEFAULT_GKD_TYPE= 2; // 1=static, 2=update, 3=slotted
	
	/** Default number of SMQTT clients */
	private static final int DEFAULT_NUMBER_OF_CLIENTS= 3;
	
	/** Default MQTT QoS */
	private static final int PUBLISH_QOS= 2;

	/** Default slot time [s] (used in GKD method type 3) */
	private static int SLOT_TIME= 10;

	/** Default number of time slots (used in GKD method type 3) */
	private static int TREE_DEPTH=0;
	
	private static int log2(int n) { return 31 - Integer.numberOfLeadingZeros(n); }

	/**
	 * @param broker
	 * @throws IOException
	 */
	public static void server(String broker) throws IOException {
		new GKDServer("server",broker);
	}

	/**
	 * @param id
	 * @param broker
	 * @throws IOException
	 */
	public static void client(String id, String broker) throws IOException {
		long startDelay= Random.nextLong(2000);
		long joinDelay= Random.nextLong(2000);
		long subscribeDelay= Random.nextLong(1000);
		long publishDelay= Random.nextLong(5000);
		
		SystemUtils.sleep(startDelay);
		var key= Random.nextBytes(GKDServer.KEY_LENGTH);
		var client= new SecureMqttClient(id,key,broker,null);
		client.connect();
		SystemUtils.sleep(joinDelay);
		if (TREE_DEPTH>0) {
			var expires= (int)((1<<TREE_DEPTH)*SLOT_TIME);
			client.join("test",expires);
		}
		else client.join("test");
		SystemUtils.sleep(subscribeDelay);
		client.subscribe("test",2);
		SystemUtils.sleep(publishDelay);
		client.publish("test",PUBLISH_QOS,("Hello from "+id).getBytes());
	}

	/** Main method.
	 * @param args command-line options
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		var flags= new Flags(args);
		var verbose= flags.getBoolean("-v","verbose mode");
		var broker= flags.getString("-b",DEFAULT_BROKER,"broker","socket address of the MQTT broker");
		var clientNum= flags.getInteger("-n",DEFAULT_NUMBER_OF_CLIENTS,"num","number of clients");
		var gkdType= flags.getInteger("-t",DEFAULT_GKD_TYPE,"type","group key distribution type");
		var help= flags.getBoolean("-h","shows this help and extits");

		if (help) {
			System.out.println(flags.toUsageString(GKDMqttTest.class));
			return;
		}
		
		verbose= true;
		if (verbose) {
			DefaultLogger.setLogger(new WriterLogger(System.out));
			//PahoClient.VERBOSE= true;
			//GKDServer.VERBOSE= true;
			SlottedGKDService.VERBOSE= true;
			SecureMqttClient.VERBOSE= true;
			//SecureMqttClient.DEBUG= true;
			SlottedGKDClient.VERBOSE= true;
		}
		
		SecureMqttClient.GKD_TYPE= gkdType;
		
		if (gkdType==3) {
			System.out.println("Tree depth: "+TREE_DEPTH);
			System.out.println("Number of slots: "+(1<<TREE_DEPTH));
			System.out.println("Slot time: "+SLOT_TIME);
			SlottedGKDService.TREE_DEPTH= TREE_DEPTH;
			SlottedGKDService.TIMESLOT= SLOT_TIME;
		}
		
		SystemUtils.run(() -> server(broker));

		for (var i: new Range(clientNum)) {
			SystemUtils.run(() -> client("client-"+i,broker));
		}
		
		System.out.println("Press 'ENTER' to exit");
		System.in.read();
		System.exit(0);
	}

}
