package laeven.mpoa.utils.structs;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 
 * @author Laeven
 * 
 * Unique ID 4 decimals
 * Creates a unique id of 4 numbers on the end of a prefix e.g snow_golem#5832
 * Used in place of UUID where a user is expected to interact with id's in CLI
 */
public final class UID4
{
	private static SecureRandom sr = new SecureRandom();
	public static final Pattern UID4Pattern = Pattern.compile("^[a-zA-Z0-9_-]*#\\d{4}$");
	private final String uid4Prefix;
	private final String uid4;
	
	public UID4(String uid4Prefix,String uid4)
	{
		Objects.requireNonNull(uid4Prefix,"UID4 prefix cannot be null!");
		Objects.requireNonNull(uid4,"uid4 cannot be null!");
		if(uid4Prefix.isEmpty() || uid4Prefix.isBlank()) { throw new IllegalArgumentException("Invalid UID4 prefix! Prefix cannot be empty or blank!"); }
		if(uid4.isEmpty() || uid4.isBlank()) { throw new IllegalArgumentException("Invalid UID4! uid4 cannot be empty or blank!"); }
		
		this.uid4Prefix = uid4Prefix;
		this.uid4 = uid4;
	}
	
	/**
	 * Creates a random UID4
	 * @param prefix String to prefix this UUID with
	 * @return UID4
	 */
	public static UID4 randomUID(String prefix)
	{
		Objects.requireNonNull(prefix,"UID4 prefix cannot be null!");
		if(prefix.isEmpty() || prefix.isBlank()) { throw new IllegalArgumentException("Invalid UID4 prefix! Prefix cannot be empty or blank!"); }
		
		return UID4.fromString(prefix + "#" 
				+ (sr.nextInt(0,10))
				+ (sr.nextInt(0,10))
				+ (sr.nextInt(0,10))
				+ (sr.nextInt(0,10)));
	}
	
	public static UID4 fromString(String uid4String)
	{
		Objects.requireNonNull(uid4String,"UID4 string cannot be null!");
		
		// Forces all space to be replaced with underscores
		String formatted = uid4String.toLowerCase().replaceAll("\\s+","_");
		if(!UID4Pattern.matcher(formatted).matches()) { throw new IllegalArgumentException("Invalid UID4 string: " + formatted); }
		
		String[] uid = formatted.split("#");
		return new UID4(uid[0],uid[1]);
	}
	
	public String getPrefix()
	{
		return uid4Prefix;
	}

	public String getDigits()
	{
		return uid4;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if((obj == null) || (obj.getClass() != UID4.class)) { return false; }
		UID4 uid4 = (UID4) obj;
		return uid4.toString().equals(this.toString());
	}

	@Override
	public String toString()
	{
		return uid4Prefix + "#" + uid4;
	}
	
	@Override
	public int hashCode()
	{
		return this.toString().hashCode();
	}
}
