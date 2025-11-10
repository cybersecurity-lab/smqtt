package test;

import java.io.IOException;

import org.zoolu.util.Flags;
import org.zoolu.util.Random;
import org.zoolu.util.SystemUtils;
import org.zoolu.util.log.DefaultLogger;
import org.zoolu.util.log.WriterLogger;

import it.unipr.netsec.smqtt.SecureMqttClient;
import it.unipr.netsec.smqtt.gkd.GKDServer;
import it.unipr.netsec.smqtt.gkd.method.SlottedGKDClient;


/** Simple SMQTT client.
 * <p>
 * It requires a standard MQTT broker and a GKD server.
 * The addresses of the broker can be passed at command-line using the '-b' option.
 */
public class SMqttClientMain {
	private SMqttClientMain() {}

	/** Default MQTT broker */
	private static String DEFAULT_BROKER= "127.0.0.1:1883";

	/** Default duration */
	private static int DEFAULT_EXPIRES= 60;

	/** Default MQTT QoS */
	private static final int PUBLISH_QOS= 2;


	/** Main method.
	 * @param args command-line options
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		var flags= new Flags(args);
		var verbose= flags.getBoolean("-v","verbose mode");
		var broker= flags.getString("-b",DEFAULT_BROKER,"broker","socket address of the MQTT broker");
		var gkdType= flags.getInteger("-m",2,"method","group key distribution method (default is 2=update)"); // 1=static, 2=update, 3=slotted
		var topic= flags.getString("-t","test","topic","topic name to join and subscribe (default is 'test')");
		var expires= flags.getInteger("-x",-1,"secs","duration of the membership");
		var help= flags.getBoolean("-h","shows this help and extits");

		if (help) {
			System.out.println(flags.toUsageString(SMqttClientMain.class));
			return;
		}
		
		if (verbose) {
			DefaultLogger.setLogger(new WriterLogger(System.out));
			//PahoClient.VERBOSE= true;
			SecureMqttClient.VERBOSE= true;
			//SecureMqttClient.DEBUG= true;
			SlottedGKDClient.VERBOSE= true;
		}
		
		SecureMqttClient.GKD_TYPE= gkdType;
				
		var clientId= "client-"+(Random.nextInt(90000)+10000);
		var Key= Random.nextBytes(GKDServer.KEY_LENGTH);
		var client= new SecureMqttClient(clientId,Key,broker,null);
		client.connect();	
		SystemUtils.sleep(1000);	
		if (expires>0) SystemUtils.runAfter(expires*1000,()->System.exit(0));

		if (gkdType==3) {
			if (expires<0) expires= DEFAULT_EXPIRES;
			client.join(topic,expires);
		}
		else client.join(topic);
			
		SystemUtils.sleep(1000);
		client.subscribe(topic,2);
		
		String message;
		while (true) {
			System.out.println("Type a message to send (ENTER to exit):");
			message= SystemUtils.readLine();
			if (message.length()>0) client.publish(topic,PUBLISH_QOS,message.getBytes());
			else System.exit(0);
		}
	}

}
