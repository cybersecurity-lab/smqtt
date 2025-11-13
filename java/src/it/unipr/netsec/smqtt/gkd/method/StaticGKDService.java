package it.unipr.netsec.smqtt.gkd.method;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Consumer;

import org.zoolu.util.Bytes;
import org.zoolu.util.Random;

import it.unipr.netsec.smqtt.gkd.KeyServer;
import it.unipr.netsec.smqtt.gkd.GKDService;
import it.unipr.netsec.smqtt.gkd.message.JoinRequest;
import it.unipr.netsec.smqtt.gkd.message.JoinResponse;


public class StaticGKDService implements GKDService {

	private class GroupInfo {

		public HashSet<String> members= new HashSet<>();
		public byte[] key;

		public GroupInfo(byte[] key) {
			this.key= key;
		}
	}

	private HashMap<String, GroupInfo> groups= new HashMap<>();

	
	public StaticGKDService() {
	}

	@Override
	public void handleJoinRequest(JoinRequest joinReq, Consumer<JoinResponse> sender) {
		if (!groups.containsKey(joinReq.group)) {
			groups.put(joinReq.group,new GroupInfo(Random.nextBytes(KeyServer.KEY_LENGTH)));
		}
		var groupInfo= groups.get(joinReq.group);
		if (!groupInfo.members.contains(joinReq.member)) groupInfo.members.add(joinReq.member);
		sender.accept(new JoinResponse(joinReq.member,joinReq.group,Bytes.toHex(groupInfo.key)));
	}

}
