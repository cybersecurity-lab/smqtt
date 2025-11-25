package it.unipr.netsec.smqtt.gkd.method;

import java.util.HashMap;
import java.util.function.Consumer;

import org.zoolu.util.Bytes;
import org.zoolu.util.Random;
import org.zoolu.util.log.DefaultLogger;
import org.zoolu.util.log.LoggerLevel;

import it.unipr.netsec.smqtt.gkd.KeyServer;
import it.unipr.netsec.smqtt.gkd.GKDService;
import it.unipr.netsec.smqtt.gkd.KeyRing;
import it.unipr.netsec.smqtt.gkd.message.JoinRequest;
import it.unipr.netsec.smqtt.gkd.message.JoinResponse;


public class UpdateGKDService implements GKDService {
	
	private void log(String str) {
		DefaultLogger.log(LoggerLevel.INFO,this.getClass(),str);
	}

	
	private class GroupInfo {
		public HashMap<String,Long> members= new HashMap<>(); // member sequence numbers
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
		try {
			if (!groups.containsKey(joinReq.group)) {
				groups.put(joinReq.group,new GroupInfo(new byte[KeyServer.KEY_LENGTH]));
				if (KeyServer.VERBOSE) log("handleJoinRequest(): new group: "+joinReq.group);
			}
			var groupInfo= groups.get(joinReq.group);
			var seq= groupInfo.members.get(joinReq.member);
			if (seq!=null) {
				// client is already a group member: update sequence number and send the group key only to this client
				groupInfo.members.put(joinReq.member,joinReq.seq);
				responder.accept(new JoinResponse(joinReq.member,joinReq.group,joinReq.expires,-1,-1,-1,seq,KeyRing.getKey(joinReq.member),Bytes.toHex(groupInfo.key)));
			}
			else {
				// new member
				groupInfo.key= Random.nextBytes(KeyServer.KEY_LENGTH);
				if (KeyServer.VERBOSE) log("handleJoinRequest(): new group key: "+Bytes.toHex(groupInfo.key));
				groupInfo.members.put(joinReq.member,joinReq.seq);
				// sends the new key to all clients
				for (var m : groupInfo.members.keySet()) {
					var mSeq= groupInfo.members.get(m)+1;
					groupInfo.members.put(m,mSeq);
					responder.accept(new JoinResponse(m,joinReq.group,joinReq.expires,-1,-1,-1,mSeq,KeyRing.getKey(m),Bytes.toHex(groupInfo.key)));
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
