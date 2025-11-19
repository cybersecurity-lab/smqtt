package it.unipr.netsec.smqtt.gkd.message;

import java.util.HashSet;

import org.zoolu.util.ArrayUtils;
import org.zoolu.util.json.Json;
import org.zoolu.util.json.ObjectInspector;

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
	
	/** 
	 * @return returns a JSON without empty or invalid fields
	 */
	public String toJson() {
		var attributes= new HashSet<String>();
		for (var a: ObjectInspector.getObjectAttributes(this,false)) attributes.add(a.name);
		if (expires<0) if (!attributes.remove("expires")) throw new RuntimeException("Attribute 'expires' doesn't exist");
		if (slot<0) if (!attributes.remove("slot")) throw new RuntimeException("Attribute 'slot' doesn't exist");
		if (depth<0) if (!attributes.remove("depth")) throw new RuntimeException("Attribute 'depth' doesn't exist");
		if (time<0) if (!attributes.remove("time")) throw new RuntimeException("Attribute 'time' doesn't exist");
		return Json.toJSON(this,attributes);
	}

}
