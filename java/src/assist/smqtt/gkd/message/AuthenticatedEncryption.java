package assist.smqtt.gkd.message;

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

public class AuthenticatedEncryption {
	
	public static String ALGO= "AES";

	public static int BLOCK_SIZE= 16;

	public static String MODE= "CBC";
	
	public static String PADDING= "PKCS5Padding"; // e.g. PKCS5Padding, NoPadding
			
	public static String MAC_ALGO= "HmacSHA256";

	public static int AUTH_LEN= 16;

	private SecretKey secretKey;
	
	private Cipher cipher;
	
	private SecretKey macKey;

	private Mac mac;
	
	
	/** Creates a new instance of the cipher.
	 * @param key the secret key
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 */
	public AuthenticatedEncryption(byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		// cipher
		cipher= Cipher.getInstance(ALGO+"/"+MODE+"/"+PADDING);
		secretKey= new SecretKeySpec(key,ALGO);
		// MAC
		mac= Mac.getInstance(MAC_ALGO);
		macKey= new SecretKeySpec(key,MAC_ALGO);
	}
	
	
	public synchronized byte[] encrypt(byte[] plaintext) throws InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		byte[] iv= !MODE.equalsIgnoreCase("EBC")? Random.nextBytes(BLOCK_SIZE) : null;
		var ivParam= iv!=null? new IvParameterSpec(iv) : null;
		cipher.init(Cipher.ENCRYPT_MODE,secretKey,ivParam);
		byte[] ciphertext= cipher.doFinal(plaintext);
		mac.init(macKey);
		byte[] auth= mac.doFinal(plaintext);
		if (auth.length>AUTH_LEN) auth= Bytes.copy(auth,0,AUTH_LEN);
		return Bytes.concat(iv,ciphertext,auth);
	}
	
	
	public synchronized byte[] decrypt(byte[] ciphertext) throws InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		IvParameterSpec iv= (MODE.equalsIgnoreCase("EBC"))? null : new IvParameterSpec(Bytes.copy(ciphertext,0,BLOCK_SIZE));
		cipher.init(Cipher.DECRYPT_MODE,secretKey,iv);
		byte[] plaintext= cipher.doFinal(ciphertext, iv==null? 0 : BLOCK_SIZE, iv==null? ciphertext.length-AUTH_LEN : ciphertext.length-BLOCK_SIZE-AUTH_LEN);
		mac.init(macKey);
		byte[] auth= mac.doFinal(plaintext);
		boolean success= Bytes.compare(ciphertext,ciphertext.length-AUTH_LEN,AUTH_LEN,auth,0,AUTH_LEN)==0;
		if (!success) throw new SecurityException("MAC verification failed: message may have been tampered with or the key is incorrect.");
		return plaintext;
	}


}
