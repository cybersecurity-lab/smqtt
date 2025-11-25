package it.unipr.netsec.smqtt.gkd.method;

import java.io.IOException;
import java.util.HashMap;

import org.zoolu.util.Bytes;
import org.zoolu.util.log.DefaultLogger;
import org.zoolu.util.log.LoggerLevel;

import it.unipr.netsec.smqtt.SecureMqttClient;
import it.unipr.netsec.smqtt.gkd.GKDClient;
import it.unipr.netsec.smqtt.gkd.IndexKeyPair;
import it.unipr.netsec.smqtt.gkd.ThrowingConsumer;
import it.unipr.netsec.smqtt.gkd.message.JoinRequest;
import it.unipr.netsec.smqtt.gkd.message.JoinResponse;


public class StaticGKDClient implements GKDClient {
	
	private void log(String str) {
		DefaultLogger.log(LoggerLevel.INFO,this.getClass(),str);
	}

	
	/** Client identifier */
	String clientId;

	/** Client long-term secret key */
	byte[] clientKey;
	
	/** Sequence number */
	private long seq= 0;

	/** Group keys */
	HashMap<String,byte[]> groupKeys= new HashMap<>();

	
	/** Create a new GKDClient.
	 * @param clientId client identifier
	 * @param secretKey client long-term secret key
	 */
	public StaticGKDClient(String clientId, byte[] clientKey) {
		this.clientId= clientId;
		this.clientKey= clientKey;
	}

	@Override
	public void join(String group, ThrowingConsumer<JoinRequest> sender) throws IOException {
		var join= new JoinRequest(clientId,group,seq,clientKey);
		sender.accept(join);
	}

	@Override
	public void join(String group, int expires, ThrowingConsumer<JoinRequest> sender) throws IOException {
		join(group,sender);
	}

	@Override
	public void leave(String group) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Currently not supported");
	}

	@Override
	public IndexKeyPair getGroupKey(String group) {
		return new IndexKeyPair(0,groupKeys.get(group));
	}

	@Override
	public byte[] getGroupKey(String group, int index) {
		if (index==0) return groupKeys.get(group);
		else return null;
	}

	@Override
	public void handleJoinResponse(JoinResponse resp) {
		if (resp.seq<seq) {
			if (SecureMqttClient.DEBUG) log("handleJoinResponse(): seq "+resp.seq+" < "+seq+": message is old, discarded");
			return;
		}
		seq= resp.seq+1;
		try {
			groupKeys.put(resp.group,Bytes.fromHex(resp.getKeyMaterial(clientKey)));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
