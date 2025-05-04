package laeven.mpoa.utils;

import net.md_5.bungee.api.ChatColor;

/**
 * @author Laeven
 * @since 1.0.0
 */
public class ColourUtils
{	
	// The colour code char used to represent a colour code in the following character
	public static final char COLOUR_CODE_CHARACTER = '&';
	
	/**
	 * Translates '&' symbols to section symbol/section sign
	 * @param s String to format
	 * @return String with colour code symbols translated
	 */
	public static String transCol(String s)
	{
		return ChatColor.translateAlternateColorCodes(COLOUR_CODE_CHARACTER,s);
	}
}