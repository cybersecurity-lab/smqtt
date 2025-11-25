package it.unipr.netsec.smqtt.gkd.message;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.zoolu.util.Bytes;
import org.zoolu.util.Random;

public class MAC {
	
	public static String MAC_ALGO= "HmacSHA256";

	public static int AUTH_LEN= 16;

	private SecretKey macKey;

	private Mac mac;
	
	
	/** Creates a new instance of the mac.
	 * @param key the secret key
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 */
	public MAC(byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		mac= Mac.getInstance(MAC_ALGO);
		macKey= new SecretKeySpec(key,MAC_ALGO);
	}
	
	
	public synchronized byte[] doFinal(byte[] message) throws InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		mac.init(macKey);
		byte[] auth= mac.doFinal(message);
		return auth.length>AUTH_LEN? Bytes.copy(auth,0,AUTH_LEN) : auth;
	}

}
