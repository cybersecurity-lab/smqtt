package it.unipr.netsec.smqtt.gkd;

import java.io.IOException;

import it.unipr.netsec.smqtt.gkd.message.JoinRequest;
import it.unipr.netsec.smqtt.gkd.message.JoinResponse;


/** Client-side of a GKD method.
  */
public interface GKDClient {
	
	/** Requests to join a group for a given interval of time.
	 * @param group the group
	 * @param sender object that can be used for sending JOIN REQUST messages
	 * @throws IOException
	 */
	public void join(String group, ThrowingConsumer<JoinRequest> sender) throws IOException;

	/** Requests to join a group for a given interval of time.
	 * @param group the group
	 * @param start starting time [sec]
	 * @param duration duration [sec]
	 * @param sender object that can be used for sending JOIN REQUST messages
	 * @throws IOException
	 */
	public void join(String group, long start, long duration, ThrowingConsumer<JoinRequest> sender) throws IOException;
	
	/** Gets the current group key.
	 * @param group the group
	 * @return the current group key
	 */
	public byte[] getGroupKey(String group);
	
	/** Gets the current group key.
	 * @param group the group
	 * @param obj additional information (like a sequence number or a challenge)
	 * @return the current group key
	 */
	public byte[] getGroupKey(String group, Object obj);

	
	/** Processes a JOIN RESPONSE message.
	 * @param resp the response message
	 */
	public void handleJoinResponse(JoinResponse resp);
	
	/** Leaves a group.
	 * @param group the group
	 */
	public void leave(String group);
}
