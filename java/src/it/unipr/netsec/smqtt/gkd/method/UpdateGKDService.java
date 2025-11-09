package it.unipr.netsec.smqtt.gkd.method;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Consumer;

import org.zoolu.util.Bytes;
import org.zoolu.util.Random;

import it.unipr.netsec.smqtt.gkd.GKDServer;
import it.unipr.netsec.smqtt.gkd.GKDService;
import it.unipr.netsec.smqtt.gkd.message.JoinRequest;
import it.unipr.netsec.smqtt.gkd.message.JoinResponse;


public class UpdateGKDService implements GKDService {

	private class GroupInfo {

		public HashSet<String> members= new HashSet<>();
		public byte[] key;

		public GroupInfo(byte[] key) {
			this.key= key;
		}
	}

	private HashMap<String, GroupInfo> groups= new HashMap<>();

	
	public UpdateGKDService() {
	}

	@Override
	public void handleJoinRequest(JoinRequest joinReq, Consumer<JoinResponse> responder) {
		if (!groups.containsKey(joinReq.group)) {
			groups.put(joinReq.group,new GroupInfo(new byte[GKDServer.KEY_LENGTH]));
		}
		var group= groups.get(joinReq.group);
		if (group.members.contains(joinReq.member)) {
			// sends the current key only to the client
			responder.accept(new JoinResponse(joinReq.member,joinReq.group,joinReq.intBegin,joinReq.intLen,Bytes.toHex(group.key)));
		}
		else {
			group.key= Random.nextBytes(GKDServer.KEY_LENGTH);
			group.members.add(joinReq.member);
			// sends the new key to all clients
			for (var member : group.members) {
				responder.accept(new JoinResponse(member,joinReq.group,joinReq.intBegin,joinReq.intLen,Bytes.toHex(group.key)));
			}
		}
	}

}
