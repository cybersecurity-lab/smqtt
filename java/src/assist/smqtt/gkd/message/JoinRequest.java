package assist.smqtt.gkd.message;

public class JoinRequest {

	protected JoinRequest() {
	}

	public JoinRequest(String member, String group) {
		this.member= member;
		this.group= group;
	}
	
	public JoinRequest(String member, String group, int intBegin, int intLen) {
		this.member= member;
		this.group= group;
		this.intBegin= intBegin;
		this.intLen= intLen;
	}

	public String member;
	public String group;
	public int intBegin= -1;
	public int intLen= 0;
}
