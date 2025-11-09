package assist.smqtt.gkd;

import java.util.function.Consumer;

import assist.smqtt.gkd.message.JoinRequest;
import assist.smqtt.gkd.message.JoinResponse;


/** Server-side of a GKD method.
 */
public interface GKDService {
	
	/** Processes a JOIN REQUEST message.
	 * @param joinReq the JOIN REQUEST
	 * @param responder object that can be used for sending JOIN RESPONSE messages
	 */
	public void handleJoinRequest(JoinRequest joinReq, Consumer<JoinResponse> responder);
}
