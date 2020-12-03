import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.util.Arrays;
import java.util.Base64;

public class Encryptor {
	
	private static final int RSA_BYTE_COUNT = 2048;

	//key for decrypting RSA messages
	private static PrivateKey rsaPrivateKey;
	private static PublicKey rsaPublicKey;
	
	//key for encrypting and decrypting AES messages
	private static volatile byte[] aesCipherKey = null;

	
	public static void generateRsaKeys() throws Exception {
		//generate public and private keys for RSA
		KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
		keyPairGen.initialize(RSA_BYTE_COUNT, new SecureRandom());
		KeyPair pair = keyPairGen.generateKeyPair();
		
		// set keys
		rsaPublicKey = pair.getPublic();
		rsaPrivateKey = pair.getPrivate();
	}
	
	public static String getRsaPublicKey() {
		//return encoded RSA public key as base 64 string
		return Base64.getEncoder().encodeToString(rsaPublicKey.getEncoded());		
	}

	public static byte[] rsaEncrypt(byte[] bytesToEncrypt, String b64PublicKey) throws Exception {
		
		//convert b64 string rsa key into bytes
		byte[] keyBytes = Base64.getDecoder().decode(b64PublicKey.getBytes());
		
		//create RSA public key from key bytes
		X509EncodedKeySpec rsaKeySpec =
			new X509EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PublicKey publicKey = keyFactory.generatePublic(rsaKeySpec);

		//initialize RSA cipher in encrypt mode
		Cipher rsaCipher = Cipher.getInstance("RSA");
		rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);

		//encrypt bytes
		byte[] encryptedBytes = rsaCipher.doFinal(bytesToEncrypt);

		return encryptedBytes;
	}
	
	public static byte[] rsaDecrypt(byte[] bytesToDecrypt) throws Exception {

		//initialize RSA cipher in decrypt mode
		Cipher rsaCipher = Cipher.getInstance("RSA");
		rsaCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
		
		//decrypt bytes
		byte[] decryptedBytes = rsaCipher.doFinal(bytesToDecrypt);

		return decryptedBytes;
	}

	public static byte[] generateAesKey() {
		//generate random AES key
		byte[] key = new byte[32];
		(new SecureRandom()).nextBytes(key);
		return key;
	}	

	public static void setAesKey(byte[] aesKey) {
		aesCipherKey = aesKey;
	}
	
	public static void resetAesKey() {
		aesCipherKey = null;
	}

	public static synchronized byte[] aesEncrypt(byte[] bytesToEncrypt) throws Exception {
		
		//create cipher and keyspec
		Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
		SecretKeySpec aesKeySpec = new SecretKeySpec(aesCipherKey, "AES");
		
		//generate random initialization vector (IV)
		byte[] initVector = new byte[16];
		(new SecureRandom()).nextBytes(initVector);
		IvParameterSpec ivSpec = new IvParameterSpec(initVector);
		
		//prepare cipher in encrypt mode with specs for encryption
		aesCipher.init(Cipher.ENCRYPT_MODE, aesKeySpec, ivSpec);
 
		//encrypt bytes 
		byte[] encrypted = aesCipher.doFinal(bytesToEncrypt);

		byte[] fullBytes = new byte[initVector.length + encrypted.length];
		
		//combine IV bytes with encrypted bytes
		//src, srcPos, dst, dstPos, length
        System.arraycopy(initVector, 0, fullBytes, 0, initVector.length);
        System.arraycopy(encrypted, 0, fullBytes, initVector.length, encrypted.length);
		
		//return combined IV and encrypted bytes
		return fullBytes;
	}
	
	public static synchronized byte[] aesDecrypt(byte[] bytes) throws Exception {

		//create cipher and keyspec
		Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
		SecretKeySpec aesKeySpec = new SecretKeySpec(aesCipherKey, "AES");
		
		byte[] initVector = new byte[16];
		byte[] bytesToDecrypt = new byte[bytes.length - 16];
		
		//separate initialization vector at beginning from cipher bytes
		//src, srcPos, dst, dstPos, length
		System.arraycopy(bytes, 0, initVector, 0, initVector.length);
		System.arraycopy(bytes, initVector.length, bytesToDecrypt, 0, bytesToDecrypt.length);
		
		//generate specs for decryption
		IvParameterSpec ivSpec = new IvParameterSpec(initVector);

		//prepare cipher in decrypt mode with specs for decryption
		aesCipher.init(Cipher.DECRYPT_MODE, aesKeySpec, ivSpec);
		
		//decrypt bytes
		byte[] decrypted = aesCipher.doFinal(bytesToDecrypt);
 
		return decrypted;
	}
}
