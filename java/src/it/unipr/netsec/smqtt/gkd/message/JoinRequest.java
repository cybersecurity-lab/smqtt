package it.unipr.netsec.smqtt.gkd.message;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.zoolu.util.Bytes;
import org.zoolu.util.json.Json;
import org.zoolu.util.json.ObjectInspector;

public class JoinRequest {

	public static int NONCE_LEN= 10;

	public String member; // member identifier
	public String group; // group identifier
	public int expires= -1; // expiration time [secs]
	public long seq; // sequence number
	public byte[] auth; // message authentication code

	
	protected JoinRequest() {
	}

	
	/** Creates a JoinRequest.
	 * @param member member identifier
	 * @param group group identifier
	 * @param seq sequence number
	 * @param clientKey client long-term key
	 * @throws IOException
	 */
	public JoinRequest(String member, String group, long seq, byte[] clientKey) throws IOException {
		this(member,group,-1,seq,clientKey);
	}
	
	
	/** Creates a JoinRequest.
	 * @param member member identifier
	 * @param group group identifier
	 * @param expires expiration time [secs]
	 * @param seq sequence number
	 * @param clientKey client long-term key
	 * @throws IOException 
	 */
	public JoinRequest(String member, String group, int expires, long seq, byte[] clientKey) throws IOException {
		this.member= member;
		this.group= group;
		this.expires= expires;
		this.seq= seq;
		try {
			this.auth= new MAC(clientKey).doFinal(authData());
		}
		catch (Exception e) {
			throw new IOException("Error computing message authentication code",e);
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
		return Json.toJSON(this,attributes);
	}
	
	
	private byte[] authData() {
		var sb= new StringBuffer();
		if (expires>=0) sb.append(String.valueOf(expires));
		return Bytes.concat(member.getBytes(),group.getBytes(),sb.toString().getBytes(),String.valueOf(seq).getBytes());
	}
}
