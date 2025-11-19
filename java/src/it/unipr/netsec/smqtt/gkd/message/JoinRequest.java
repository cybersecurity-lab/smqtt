package it.unipr.netsec.smqtt.gkd.message;

import java.util.HashSet;

import org.zoolu.util.json.Json;
import org.zoolu.util.json.ObjectInspector;

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
	
	/** 
	 * @return returns the JSON of this object
	 */
	public String toJson() {
		var attributes= new HashSet<String>();
		for (var a: ObjectInspector.getObjectAttributes(this,false)) attributes.add(a.name);
		if (expires<0) if (!attributes.remove("expires")) throw new RuntimeException("Attribute 'expires' doesn't exist");
		return Json.toJSON(this,attributes);
	}
}
