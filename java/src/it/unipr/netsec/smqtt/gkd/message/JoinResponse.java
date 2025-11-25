package it.unipr.netsec.smqtt.gkd.message;

import java.io.IOException;
import java.util.HashSet;

import org.zoolu.util.Base64;
import org.zoolu.util.Bytes;
import org.zoolu.util.json.Json;
import org.zoolu.util.json.ObjectInspector;

public class JoinResponse {
	
	public String member; // member identifier
	public String group; // group identifier
	public int expires= -1; // expiration time [secs]
	public int slot= -1; // slot time [secs]
	public int depth= -1; // tree depth
	public long time= -1; // current time [millisecs]
	public long seq; // sequence number
	public String encryptedKeyMaterial; // encrypted key material
	public byte[] auth; // message authentication code
	

	/** Create a JoinResponse.
	 */
	protected JoinResponse() {
	}

	
	/** Create a JoinResponse.
	 * @param member member identifier
	 * @param group group identifier
	 * @param seq sequence number
	 * @param clientKey client long-term key
	 * @param keyMaterial key material
	 * @throws IOException 
	 */
	public JoinResponse(String member, String group, long seq, byte[] clientKey, String keyMaterial) throws IOException {
		this(member,group,-1,-1,-1,-1,seq,clientKey,keyMaterial);
	}
	
	
	/** Create a JoinResponse.
	 * @param member member identifier
	 * @param group group identifier
	 * @param expires expiration time [secs]
	 * @param slot slot time [secs]
	 * @param depth tree depth
	 * @param time current time [millisecs]
	 * @param seq sequence number
	 * @param clientKey client long-term key
	 * @param keyMaterial key material
	 * @throws IOException 
	 */
	public JoinResponse(String member, String group, int expires, int slot, int depth, long time, long seq, byte[] clientKey, String keyMaterial) throws IOException {
		this.member= member;
		this.group= group;
		this.expires= expires;
		this.slot= slot;
		this.depth= depth;
		this.time= time;
		this.seq= seq;
		try {
			this.encryptedKeyMaterial= Base64.encode(new Encryption(clientKey).encrypt(keyMaterial.getBytes()));
		}
		catch (Exception e) {
			throw new IOException("Error encrypting key material",e);
		}
		try {
			this.auth= new MAC(clientKey).doFinal(authData());
		}
		catch (Exception e) {
			throw new IOException("Error computing message authentication code",e);
		}
	}
	
	
	/**
	 * @return the key material
	 * @param clientKey
	 * @throws IOException
	 */
	public String getKeyMaterial(byte[] clientKey) throws IOException {
		try {
			var decryptedKeyMaterial= new Encryption(clientKey).decrypt(Base64.decode(encryptedKeyMaterial));
			return new String(decryptedKeyMaterial);
		}
		catch (Exception e) {
			throw new IOException("Error decrypting key material",e);
		}
	}
	
	
	/** Verifies the message authentication.
	 * It throws an IOException in case of check failure.
	 * @param clientKey
	 * @throws IOException
	 */
	public void verify(byte[] clientKey) throws IOException {
		try {
			var success= Bytes.compare(new MAC(clientKey).doFinal(authData()),auth)==0;
			if (!success) throw new SecurityException("MAC missmatch: message may have been tampered or the key is incorrect.");
		}
		catch (Exception e) {
			throw new IOException("MAC verification failed",e);
		}
	}
	
	
	/** 
	 * @return returns the JSON of this object without empty or invalid fields
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
	
	
	private byte[] authData() {
		var sb= new StringBuffer();
		if (expires>=0) sb.append(String.valueOf(expires));
		if (slot>=0) sb.append(String.valueOf(slot));
		if (depth>=0) sb.append(String.valueOf(depth));
		if (time>=0) sb.append(String.valueOf(time));
		return Bytes.concat(member.getBytes(),group.getBytes(),sb.toString().getBytes(),toString().valueOf(seq).getBytes(),Base64.decode(encryptedKeyMaterial));
	}

}
