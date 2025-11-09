package test;

import java.io.IOException;

import org.zoolu.util.Flags;
import org.zoolu.util.Random;
import org.zoolu.util.Range;
import org.zoolu.util.SystemUtils;
import org.zoolu.util.log.DefaultLogger;
import org.zoolu.util.log.WriterLogger;

import assist.smqtt.SecureMqttClient;
import assist.smqtt.gkd.GKDServer;
import assist.smqtt.gkd.method.SlottedGKDClient;
import assist.smqtt.gkd.method.SlottedGKDService;


/** Simple test program that runs a GKD server and a specified number of SMQTT clients.
 * <p>
 * It requires a standard MQTT broker.  The address of the broker can be configured using the default
 * static attribute {@link DEFAULT_GKD_TYPE} or at command-line using the '-b' option.
 */
public class GKDTest {
	private GKDTest() {}

	/** Default MQTT broker */
	private static String DEFAULT_BROKER= "127.0.0.1:1883";

	/** Default GKD method type (current values: 1=static, 2=update, 3=slotted) */
	private static int DEFAULT_GKD_TYPE= 3; // 1=static, 2=update, 3=slotted
	
	/** Default number of SMQTT clients */
	private static final int DEFAULT_NUMBER_OF_CLIENTS= 8;
	
	/** Default MQTT QoS */
	private static final int PUBLISH_QOS= 2;

	/** Default number of time slots (used only in GKD method type 3) */
	private static int N= 128;
	
	private static int log2(int n) {
		return 31 - Integer.numberOfLeadingZeros(n);
	}

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
		SystemUtils.sleep(Random.nextLong(2000));
		var Key= Random.nextBytes(GKDServer.KEY_LENGTH);
		var client= new SecureMqttClient(id,Key,broker,null);
		client.connect();
		var joinTime= Random.nextLong(2000);
		SystemUtils.sleep(joinTime);
		int intBegin= 0;
		int intLen= (int)(16000/SlottedGKDService.SLOT_TIME);
		if (N>0) {
			intBegin= Random.nextInt(N/2);
			intLen= Random.nextInt(N/2-intBegin)+1;
			//client.join("test",intBegin,intLen);
			client.join("test",0,intBegin+intLen);
		}
		else client.join("test");
		var subscribeTime= Random.nextLong(2000);
		SystemUtils.sleep(subscribeTime);
		client.subscribe("test",2);
		SystemUtils.sleep(Random.nextLong((intBegin+intLen)*SlottedGKDService.SLOT_TIME - joinTime - subscribeTime));
		client.publish("test",PUBLISH_QOS,("Hello from "+id).getBytes());
	}

	/** Main method.
	 * @param args command-line options
	 */
	public static void main(String[] args) {
		var flags= new Flags(args);
		var verbose= flags.getBoolean("-v","verbose mode");
		var broker= flags.getString("-b",DEFAULT_BROKER,"broker","socket address of the MQTT broker");
		var clientNum= flags.getInteger("-n",DEFAULT_NUMBER_OF_CLIENTS,"num","number of clients");
		var gkdType= flags.getInteger("-t",DEFAULT_GKD_TYPE,"type","group key distribution type");
		var help= flags.getBoolean("-h","shows this help and extits");

		if (help) {
			System.out.println(flags.toUsageString(GKDTest.class));
			return;
		}
		
		verbose= true;
		if (verbose) DefaultLogger.setLogger(new WriterLogger(System.out));
		//PahoClient.VERBOSE= true;
		//GKDServer.VERBOSE= true;
		SlottedGKDService.VERBOSE= true;
		SecureMqttClient.VERBOSE= true;
		//SecureMqttClient.DEBUG= true;
		SlottedGKDClient.VERBOSE= true;
		
		SecureMqttClient.GKD_TYPE= gkdType;
		
		if (gkdType==3) {
			SlottedGKDService.DEPTH= log2(N-1)+1;
			System.out.println("Number of slots: "+N);
			System.out.println("Tree depth: "+SlottedGKDService.DEPTH);
		}
		else N= 0;
		
		SystemUtils.run(() -> server(broker));

		for (var i: new Range(clientNum)) {
			SystemUtils.run(() -> client("client-"+i,broker));
		}
	}

}
