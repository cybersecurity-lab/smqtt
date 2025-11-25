package it.unipr.netsec.smqtt.gkd.message;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.zoolu.util.Bytes;
import org.zoolu.util.Random;

public class Encryption {
	
	public static String ALGO= "AES";

	public static String MODE= "CBC";
	
	public static String PADDING= "PKCS5Padding";
	
	private SecretKey secretKey;
	
	private Cipher cipher;
	
	
	/** Creates a new instance of the cipher.
	 * @param key the secret key
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 */
	public Encryption(byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		// cipher
		cipher= Cipher.getInstance(ALGO+"/"+MODE+"/"+PADDING);
		secretKey= new SecretKeySpec(key,ALGO);
	}
	
	
	public synchronized byte[] encrypt(byte[] plaintext, byte[] iv) throws InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		var ivParam= MODE.equalsIgnoreCase("EBC")? null : new IvParameterSpec(iv);
		cipher.init(Cipher.ENCRYPT_MODE,secretKey,ivParam);
		return cipher.doFinal(plaintext);
	}
	
	
	public synchronized byte[] encrypt(byte[] plaintext) throws InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		byte[] iv= MODE.equalsIgnoreCase("EBC")? null : Random.nextBytes(cipher.getBlockSize());
		var ivParam= iv!=null? new IvParameterSpec(iv) : null;
		cipher.init(Cipher.ENCRYPT_MODE,secretKey,ivParam);
		byte[] ciphertext= cipher.doFinal(plaintext);
		return iv!=null? Bytes.concat(iv,ciphertext) : ciphertext;
	}
	
	
	public synchronized byte[] decrypt(byte[] ciphertext, byte[] iv) throws InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		var ivParam= MODE.equalsIgnoreCase("EBC")? null : new IvParameterSpec(iv);
		cipher.init(Cipher.DECRYPT_MODE,secretKey,ivParam);
		return cipher.doFinal(ciphertext);
	}

	public synchronized byte[] decrypt(byte[] ciphertext) throws InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		int blockSize= cipher.getBlockSize();
		IvParameterSpec iv= (MODE.equalsIgnoreCase("EBC"))? null : new IvParameterSpec(Bytes.copy(ciphertext,0,blockSize));
		cipher.init(Cipher.DECRYPT_MODE,secretKey,iv);
		byte[] plaintext= cipher.doFinal(ciphertext, iv==null? 0 : blockSize, iv==null? ciphertext.length : ciphertext.length-blockSize);
		return plaintext;
	}


}
