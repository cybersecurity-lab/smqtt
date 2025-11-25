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


public class StaticGKDService implements GKDService {
	
	private void log(String str) {
		DefaultLogger.log(LoggerLevel.INFO,this.getClass(),str);
	}

	
	private HashMap<String, byte[]> groupKey= new HashMap<>();

	
	public StaticGKDService() {
	}

	@Override
	public void handleJoinRequest(JoinRequest joinReq, Consumer<JoinResponse> sender) {
		try {
			if (!groupKey.containsKey(joinReq.group)) {
				groupKey.put(joinReq.group,Random.nextBytes(KeyServer.KEY_LENGTH));
				if (KeyServer.VERBOSE) log("handleJoinRequest(): new group: "+joinReq.group+", key: "+Bytes.toHex(groupKey.get(joinReq.group)));
			}
			sender.accept(new JoinResponse(joinReq.member,joinReq.group,joinReq.seq,KeyRing.getKey(joinReq.member),Bytes.toHex(groupKey.get(joinReq.group))));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
