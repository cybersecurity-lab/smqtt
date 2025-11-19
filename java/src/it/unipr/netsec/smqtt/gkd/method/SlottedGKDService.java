package it.unipr.netsec.smqtt.gkd.method;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.function.Consumer;

import org.zoolu.util.ArrayUtils;
import org.zoolu.util.Bytes;
import org.zoolu.util.Random;
import org.zoolu.util.json.Json;
import org.zoolu.util.log.DefaultLogger;
import org.zoolu.util.log.LoggerLevel;

import it.unipr.netsec.smqtt.gkd.KeyServer;
import it.unipr.netsec.smqtt.gkd.GKDService;
import it.unipr.netsec.smqtt.gkd.message.JoinRequest;
import it.unipr.netsec.smqtt.gkd.message.JoinResponse;
import it.unipr.netsec.smqtt.gkd.method.keytree.IntRange;
import it.unipr.netsec.smqtt.gkd.method.keytree.KeyNode;


/** 
*/
public class SlottedGKDService implements GKDService {
	
	/** Verbose mode */
	public static boolean VERBOSE= false;

	private void log(String str) {
		DefaultLogger.log(LoggerLevel.INFO,this.getClass(),str);
	}


	public static int START_DELAY= 1; // [slots]

	public static int TIMESLOT= 10; // 10s
	public static int TREE_DEPTH= 16; // 65536 slots


	/** Key k2 for managing unexpected leaving (key revoke) */
	private byte[] k2;

	/** Root node of the key tree */
	private KeyNode x00= null;
	
	/** Starting time */
	private long startT;

	
	/** 
	 * @throws UnsupportedEncodingException 
	 * @throws NoSuchAlgorithmException
	 */
	public SlottedGKDService() throws NoSuchAlgorithmException, UnsupportedEncodingException {
		k2= Random.nextBytes(KeyServer.KEY_LENGTH);

		// root key
		var k00= Random.nextBytes(KeyServer.KEY_LENGTH);
		x00= new KeyNode(0,0,k00);
		if (VERBOSE) log("SlottedGKDService(): x00: "+x00.toString());
		
		// BEGIN DEBUG
		//System.out.println("DEBUG: SlottedGKDService: k2: "+Bytes.toHex(k2));
		//System.out.println("DEBUG: SlottedGKDService: x00: "+x00.toString());
		//var keyRange= x00.leaves(DEPTH); 
		//for (int pos: new Range(keyRange.first,keyRange.last+1)) System.out.println("DEBUG: SlottedGKDService: "+x00.successor(DEPTH,pos).toJson());
		// END DEBUG
	
		startT= System.currentTimeMillis();
	}
	
	
	@Override
	public void handleJoinRequest(JoinRequest joinReq, Consumer<JoinResponse> sender) {
		if (VERBOSE) log("handleJoinRequest(): "+joinReq.toJson());

		if (joinReq.expires<0) joinReq.expires=0;
		var elapsedTime= System.currentTimeMillis()-startT;		
		int intBegin= (int)(elapsedTime/TIMESLOT/1000);
		int intEnd= (int)((elapsedTime+joinReq.expires*1000)/TIMESLOT/1000);
		
		var slotRange= new IntRange(intBegin,intEnd-1);
		if (VERBOSE) log("handleJoinRequest(): slot interval: "+slotRange);

		var selectedNodes= slotRange.generators(TREE_DEPTH);
		if (VERBOSE) log("handleJoinRequest(): selected nodes: "+ArrayUtils.toString(selectedNodes));
		
		var selectedKeyNodes= new ArrayList<KeyNode>();
		for (var node: selectedNodes) selectedKeyNodes.add(x00.successor(node));
		var sb= new StringBuffer();
		sb.append(Bytes.toHex(k2));
		for (var e: selectedKeyNodes) sb.append("&").append(e.toJson());
		var keyMaterial= sb.toString();
		if (VERBOSE) log("handleJoinRequest(): key material ("+selectedKeyNodes.size()+"): "+keyMaterial);
		
		var joinResp= new JoinResponse(joinReq.member,joinReq.group,joinReq.expires,TIMESLOT,TREE_DEPTH,elapsedTime,keyMaterial);
		sender.accept(joinResp);
	}

}
