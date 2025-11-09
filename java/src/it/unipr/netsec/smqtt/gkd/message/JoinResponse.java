package it.unipr.netsec.smqtt.gkd.message;


public class JoinResponse {

	/** Create a JoinResponse.
	 */
	protected JoinResponse() {
	}

	/** Create a JoinResponse.
	 * @param member member identifier
	 * @param group group identifier
	 * @param intBegin begin of validity interval
	 * @param intLen length of validity interval
	 * @param key key material
	 */
	public JoinResponse(String member, String group, int intBegin, int intLen, String key) {
		this.member= member;
		this.group= group;
		this.intBegin= intBegin;
		this.intLen= intLen;
		this.key= key;
	}

	public String member; // member identifier
	public String group; // group identifier
	public int intBegin; // begin of validity interval
	public int intLen; // length of validity interval
	public String key; // key material
	public long time= 0; // elapsed time [ms]
}
