package laeven.mpoa.utils.security;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import laeven.mpoa.utils.Logg;

/**
 * @author Laeven (Zack)
 * @since 0.6.0
 */
public class HashingUtils
{
	private final static int iterationCount = 10000;
	private final static int keyLen = 512;
	
	/**
	 * Hashes a string and compares it to an existing one
	 * @param inputString String input to be hashed
	 * @param existingHash The existing hashed string
	 * @param salt The salt used to hash to existing hashed string
	 * @return True if the hashes match, false otherwise
	 * @throws Exception
	 */
	public static boolean isEqualTo(String inputString,String existingHash,byte[] salt)
	{
		byte[] hash = hash(inputString,salt);
		byte[] storedHash = Base64.getDecoder().decode(existingHash);
		
		if(salt == null) { throw new NullPointerException("Stored Salt is null!"); }
		if(hash == null) { throw new NullPointerException("New Hash is null!"); }
		if(storedHash == null) { throw new NullPointerException("Stored Hash is null!"); }
		
		return Arrays.equals(hash,storedHash);
	}
	
	/**
	 * Hashes a password
	 * @param password String to be hashed
	 * @param salt Salt to hash with the password
	 * @return Hash byte array
	 * @throws Exception
	 */
	public static byte[] hash(String inputString,byte[] salt)
	{
		try
		{
			KeySpec spec = new PBEKeySpec(inputString.toCharArray(),salt,iterationCount,keyLen);
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			return factory.generateSecret(spec).getEncoded();
		}
		catch(Exception e)
		{
			Logg.error("Error! Unable to hash " + inputString);
		}
		
		return null;
	}
	
	/**
	 * Hashes a password
	 * @param password String to be hashed
	 * @param salt Salt to hash with the password
	 * @return Hash byte array
	 * @throws Exception
	 */
	public static String hashToString(String inputString,byte[] salt)
	{
		byte[] hash = null;
		hash = hash(inputString,salt);
		if(hash == null) { return null; }
		return Base64.getEncoder().encodeToString(hash);
	}
	
	/**
	 * Generates a password salt.
	 * 
	 * <p>Salts are used to resist dictionary attacks and rainbow tables
	 * @param size
	 * @return Salt byte array
	 */
	public static byte[] generateSalt(int size)
	{
		SecureRandom rand = new SecureRandom();
		byte[] salt = new byte[size];
		rand.nextBytes(salt);
		return salt;
	}
	
	/**
	 * Generates a password salt.
	 * 
	 * <p>Salts are used to resist dictionary attacks and rainbow tables
	 * @param size
	 * @return Salt byte array
	 */
	public static byte[] generateSalt()
	{
		SecureRandom rand = new SecureRandom();
		byte[] salt = new byte[64];
		rand.nextBytes(salt);
		return salt;
	}
}
