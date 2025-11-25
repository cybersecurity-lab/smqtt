package it.unipr.netsec.smqtt.gkd.method;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.zoolu.util.ArrayUtils;
import org.zoolu.util.Bytes;
import org.zoolu.util.Range;
import org.zoolu.util.log.DefaultLogger;
import org.zoolu.util.log.LoggerLevel;

import it.unipr.netsec.smqtt.SecureMqttClient;
import it.unipr.netsec.smqtt.gkd.GKDClient;
import it.unipr.netsec.smqtt.gkd.IndexKeyPair;
import it.unipr.netsec.smqtt.gkd.ThrowingConsumer;
import it.unipr.netsec.smqtt.gkd.message.JoinRequest;
import it.unipr.netsec.smqtt.gkd.message.JoinResponse;
import it.unipr.netsec.smqtt.gkd.method.keytree.KeyNode;


public class SlottedGKDClient implements GKDClient {
	
	private void log(String str) {
		DefaultLogger.log(LoggerLevel.INFO,this.getClass(),str);
	}


	/** Client identifier */
	String clientId;

	/** Client secret key */
	byte[] clientKey;

	/** Sequence number */
	private long seq= 0;

	/** Start time [ms] */
	private long startT;

	/** Slot time [s] */
	private int slotTime;
	
	/** Tree depth */
	private int treeDepth;

	/** Group key nodes */
	HashMap<String,ArrayList<KeyNode>> groupKeyNodes= new HashMap<>();

	
	/** Create a new GKDClient.
	 * @param clientId client identifier
	 * @param secretKey client long-term secret key
	 */
	public SlottedGKDClient(String clientId, byte[] clientKey) {
		this.clientId= clientId;
		this.clientKey= clientKey;
	}

	@Override
	public void join(String group, ThrowingConsumer<JoinRequest> sender) throws IOException {
		throw new RuntimeException("Currently not supported");
	}
	

	@Override
	public void join(String group, int expires, ThrowingConsumer<JoinRequest> sender) throws IOException {
		var join= new JoinRequest(clientId,group,expires,seq,clientKey);
		sender.accept(join);
	}
	
	@Override
	public void leave(String group) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Currently not supported");
	}

	@Override
	public IndexKeyPair getGroupKey(String group) {
		long time= System.currentTimeMillis() - startT;
		var slot= (int)(time/slotTime/1000);
		return new IndexKeyPair(slot,getGroupKey(group,Integer.valueOf(slot)));
	}

	@Override
	public byte[] getGroupKey(String group, int index) {
		var keyNodes= groupKeyNodes.get(group);
		if (keyNodes==null || keyNodes.size()==0) return null;
		// else		
		for (var x: keyNodes) {
			if (x.leaves(treeDepth).contains(index)) {
				return x.successor(treeDepth,index).key;
			}
		}
		return null;
	}

	@Override
	public void handleJoinResponse(JoinResponse resp) {
		try {
			if (resp.seq<seq) {
				if (SecureMqttClient.DEBUG) log("handleJoinResponse(): seq "+resp.seq+" < "+seq+": message is old, discarded");
				return;
			}
			seq= resp.seq+1;
			startT= System.currentTimeMillis() - resp.time;
			slotTime= resp.slot;
			treeDepth= resp.depth;
			var keyMaterial= resp.getKeyMaterial(clientKey).split("&");
			var k2= Bytes.fromHex(keyMaterial[0]);
			if (SecureMqttClient.DEBUG) log("handleJoinResponse(): k2: "+Bytes.toHex(k2));
			
			var selectedKeyNodes= new ArrayList<KeyNode>();
			for (int i: new Range(1,keyMaterial.length)) selectedKeyNodes.add(KeyNode.fromJson(keyMaterial[i]));				
			if (SecureMqttClient.DEBUG) log("handleJoinResponse(): key nodes ("+selectedKeyNodes.size()+"): "+ArrayUtils.toString(selectedKeyNodes));
			
			groupKeyNodes.put(resp.group,selectedKeyNodes);			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
