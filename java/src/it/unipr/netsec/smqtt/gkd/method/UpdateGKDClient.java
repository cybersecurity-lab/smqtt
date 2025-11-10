package it.unipr.netsec.smqtt.gkd.method;

import java.io.IOException;
import java.util.HashMap;

import org.zoolu.util.Bytes;

import it.unipr.netsec.smqtt.gkd.GKDClient;
import it.unipr.netsec.smqtt.gkd.ThrowingConsumer;
import it.unipr.netsec.smqtt.gkd.message.JoinRequest;
import it.unipr.netsec.smqtt.gkd.message.JoinResponse;


public class UpdateGKDClient implements GKDClient {

	/** Client identifier */
	String clientId;

	/** Client secret key */
	byte[] secretKey;
	
	/** Group keys */
	HashMap<String,byte[]> groupKeys= new HashMap<>();

	
	/** Create a new GKDClient.
	 * @param clientId client identifier
	 * @param secretKey client secret key
	 */
	public UpdateGKDClient(String clientId, byte[] secretKey) {
		this.clientId= clientId;
		this.secretKey= secretKey;
	}

	@Override
	public void join(String group, ThrowingConsumer<JoinRequest> sender) throws IOException {
		var join= new JoinRequest(clientId,group);
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
	public byte[] getGroupKey(String group) {
		return groupKeys.get(group);
	}

	@Override
	public byte[] getGroupKey(String group, Object obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void handleJoinResponse(JoinResponse resp) {
		groupKeys.put(resp.group,Bytes.fromHex(resp.key));
	}

}
