package it.unipr.netsec.smqtt.gkd;

import org.zoolu.util.Bytes;

/** Database containing long-term keys of clients.
 */
public class KeyRing {
	private KeyRing() {}
	
	public static byte[] DEFAULT_KEY= Bytes.copy(Bytes.fromHex("aaaaaaaabbbbbbbbccccccccddddddddaaaaaaaabbbbbbbbccccccccdddddddd"),0,KeyServer.KEY_LENGTH);
	
	public static byte[] getKey(String clientId) {
		return DEFAULT_KEY;
	}

}
