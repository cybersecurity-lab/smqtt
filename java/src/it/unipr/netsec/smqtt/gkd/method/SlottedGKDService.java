package it.unipr.netsec.smqtt.gkd.method;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

import org.zoolu.util.ArrayUtils;
import org.zoolu.util.Bytes;
import org.zoolu.util.Random;
import org.zoolu.util.log.DefaultLogger;
import org.zoolu.util.log.LoggerLevel;

import it.unipr.netsec.smqtt.gkd.KeyServer;
import it.unipr.netsec.smqtt.gkd.GKDService;
import it.unipr.netsec.smqtt.gkd.KeyRing;
import it.unipr.netsec.smqtt.gkd.message.JoinRequest;
import it.unipr.netsec.smqtt.gkd.message.JoinResponse;
import it.unipr.netsec.smqtt.gkd.method.keytree.IntRange;
import it.unipr.netsec.smqtt.gkd.method.keytree.KeyNode;


/** 
*/
public class SlottedGKDService implements GKDService {
	
	private void log(String str) {
		DefaultLogger.log(LoggerLevel.INFO,this.getClass(),str);
	}


	public static int START_DELAY= 1; // [slots]

	public static int TIMESLOT= 10; // 10s
	public static int TREE_DEPTH= 16; // 65536 slots


	private class RootKey {
		public byte[] k2; // Key k2 for managing unexpected leaving (key revoke)
		public KeyNode x00= null; // root node of the key tree
		
		public RootKey() {
			k2= Random.nextBytes(KeyServer.KEY_LENGTH);
			var k00= Random.nextBytes(KeyServer.KEY_LENGTH);
			x00= new KeyNode(0,0,k00);
			// BEGIN DEBUG
			//var keyRange= x00.leaves(DEPTH); 
			//for (int pos: new Range(keyRange.first,keyRange.last+1)) System.out.println("DEBUG: SlottedGKDService: "+x00.successor(DEPTH,pos).toJson());
			// END DEBUG
		}
	}
	
	/** Group root keys (k2 and x00) */ 
	private HashMap<String,RootKey> groupRootKeys= new HashMap<>();
	
	/** Starting time */
	private long startT;

	
	/** 
	 * @throws UnsupportedEncodingException 
	 * @throws NoSuchAlgorithmException
	 */
	public SlottedGKDService() throws NoSuchAlgorithmException, UnsupportedEncodingException {	
		startT= System.currentTimeMillis();
	}
	
	
	@Override
	public void handleJoinRequest(JoinRequest joinReq, Consumer<JoinResponse> sender) {
		try {
			if (joinReq.expires<0) joinReq.expires=0;
			var elapsedTime= System.currentTimeMillis()-startT;		
			int intBegin= (int)(elapsedTime/TIMESLOT/1000);
			int intEnd= (int)((elapsedTime+joinReq.expires*1000)/TIMESLOT/1000);
			
			var slotRange= new IntRange(intBegin,intEnd-1);
			if (KeyServer.VERBOSE) log("handleJoinRequest(): slot interval: "+slotRange);

			var selectedNodes= slotRange.generators(TREE_DEPTH);
			if (KeyServer.VERBOSE) log("handleJoinRequest(): selected nodes: "+ArrayUtils.toString(selectedNodes));
			
			var selectedKeyNodes= new ArrayList<KeyNode>();

			var rootKey= groupRootKeys.get(joinReq.group);
			if (rootKey==null) {
				rootKey= new RootKey();
				groupRootKeys.put(joinReq.group,rootKey);
				if (KeyServer.VERBOSE) log("handleJoinRequest(): new group: k2="+Bytes.toHex(rootKey.k2)+", x00="+rootKey.x00.toString());
			}
			for (var node: selectedNodes) selectedKeyNodes.add(rootKey.x00.successor(node));
			var sb= new StringBuffer();
			sb.append(Bytes.toHex(rootKey.k2));
			for (var e: selectedKeyNodes) sb.append("&").append(e.toJson());
			var keyMaterial= sb.toString();
			if (KeyServer.VERBOSE) log("handleJoinRequest(): key material ("+selectedKeyNodes.size()+"): "+keyMaterial);
			
			var joinResp= new JoinResponse(joinReq.member,joinReq.group,joinReq.expires,TIMESLOT,TREE_DEPTH,elapsedTime,joinReq.seq,KeyRing.getKey(joinReq.member),keyMaterial);
			sender.accept(joinResp);			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
