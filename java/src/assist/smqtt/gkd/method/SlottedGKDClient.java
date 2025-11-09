package assist.smqtt.gkd.method;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.zoolu.util.ArrayUtils;
import org.zoolu.util.Bytes;
import org.zoolu.util.Range;
import org.zoolu.util.log.DefaultLogger;
import org.zoolu.util.log.LoggerLevel;

import assist.smqtt.gkd.GKDClient;
import assist.smqtt.gkd.ThrowingConsumer;
import assist.smqtt.gkd.message.JoinRequest;
import assist.smqtt.gkd.message.JoinResponse;
import assist.smqtt.gkd.method.keytree.KeyNode;


public class SlottedGKDClient implements GKDClient {
	
	/** Verbose mode */
	public static boolean VERBOSE= false;

	private void log(String str) {
		DefaultLogger.log(LoggerLevel.INFO,this.getClass(),str);
	}


	/** Client identifier */
	String clientId;

	/** Client secret key */
	byte[] secretKey;
	
	/** Starting time */
	private long startT;
	
	/** Group key nodes */
	HashMap<String,ArrayList<KeyNode>> groupKeyNodes= new HashMap<>();

	
	/** Create a new GKDClient.
	 * @param clientId client identifier
	 * @param secretKey client secret key
	 */
	public SlottedGKDClient(String clientId, byte[] secretKey) {
		this.clientId= clientId;
		this.secretKey= secretKey;
	}

	@Override
	public void join(String group, ThrowingConsumer<JoinRequest> sender) throws IOException {
		throw new RuntimeException("Currently not supported");
	}
	

	@Override
	public void join(String group, long start, long duration, ThrowingConsumer<JoinRequest> sender) throws IOException {
		var join= new JoinRequest(clientId,group,(int)start,(int)duration);
		sender.accept(join);
	}
	
	@Override
	public void leave(String group) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Currently not supported");
	}


	@Override
	public byte[] getGroupKey(String group) {
		long time= System.currentTimeMillis() - startT;
		var slot= (int)(time/SlottedGKDService.SLOT_TIME);
		return getGroupKey(group,Integer.valueOf(slot));
	}

	@Override
	public byte[] getGroupKey(String group, Object obj) {
		int slot= (Integer)obj;
		var keyNodes= groupKeyNodes.get(group);
		if (keyNodes==null || keyNodes.size()==0) return null;
		// else		
		for (var x: keyNodes) {
			if (x.leaves(SlottedGKDService.DEPTH).contains(slot)) {
				return x.successor(SlottedGKDService.DEPTH,slot).key;
			}
		}
		return null;
	}

	@Override
	public void handleJoinResponse(JoinResponse joinResp) {
		startT= System.currentTimeMillis() - joinResp.time;
		var keyMaterial= joinResp.key.split("&");
		var k2= Bytes.fromHex(keyMaterial[0]);
		if (VERBOSE) log("handleJoinResponse(): k2: "+Bytes.toHex(k2));
		
		var selectedKeyNodes= new ArrayList<KeyNode>();
		for (int i: new Range(1,keyMaterial.length)) selectedKeyNodes.add(KeyNode.fromJson(keyMaterial[i]));				
		if (VERBOSE) log("handleJoinResponse(): key nodes ("+selectedKeyNodes.size()+"): "+ArrayUtils.toString(selectedKeyNodes));
		
		groupKeyNodes.put(joinResp.group,selectedKeyNodes);
	}

}
