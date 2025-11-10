package it.unipr.netsec.smqtt.gkd.message;


public class JoinResponse {
	
	public String member; // member identifier
	public String group; // group identifier
	public int expires= -1; // expiration time [secs]
	public int slot= -1; // slot time [secs]
	public int depth= -1; // tree depth
	public long time= -1; // current time [millisecs]
	public String key; // key material
	

	/** Create a JoinResponse.
	 */
	protected JoinResponse() {
	}

	/** Create a JoinResponse.
	 * @param member member identifier
	 * @param group group identifier
	 * @param key key material
	 */
	public JoinResponse(String member, String group, String key) {
		this(member,group,-1,-1,-1,-1,key);
	}
	
	/** Create a JoinResponse.
	 * @param member member identifier
	 * @param group group identifier
	 * @param expires expiration time [secs]
	 * @param slot slot time [secs]
	 * @param depth tree depth
	 * @param time current time [millisecs]
	 * @param key key material
	 */
	public JoinResponse(String member, String group, int expires, int slot, int depth, long time, String key) {
		this.member= member;
		this.group= group;
		this.expires= expires;
		this.slot= slot;
		this.depth= depth;
		this.time= time;
		this.key= key;
	}

}
