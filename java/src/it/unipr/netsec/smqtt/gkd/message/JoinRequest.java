package it.unipr.netsec.smqtt.gkd.message;

public class JoinRequest {

	public String member; // member identifier
	public String group; // group identifier
	public int expires= -1; // expiration time [secs]

	
	protected JoinRequest() {
	}

	public JoinRequest(String member, String group) {
		this.member= member;
		this.group= group;
	}
	
	public JoinRequest(String member, String group, int expires) {
		this.member= member;
		this.group= group;
		this.expires= expires;
	}
}
