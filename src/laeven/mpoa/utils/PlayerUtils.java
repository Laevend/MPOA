package laeven.mpoa.utils;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


/**
 * 
 * @author Laeven
 * 
 */
public class PlayerUtils
{
	/**
	 * Checks if the player is online
	 * @param playerName Name of the player to check if it is online
	 * @return true if player is online, false otherwise
	 */
	public static boolean isOnline(String playerName)
	{
		Player p = Bukkit.getPlayerExact(playerName);
		return p != null;
	}
	
	/**
	 * Checks if the player is online
	 * @param UUID uuid of the player
	 * @return true if player is online, false otherwise
	 */
	public static boolean isOnline(UUID uuid)
	{
		Player p = Bukkit.getPlayer(uuid);
		return p != null;
	}
	
	/**
	 * Gets a player object if this player exists and is online
	 * @param UUID uuid of the player 
	 * @return Player object, null otherwise
	 */
	public static Player getPlayer(UUID uuid)
	{
		if(!isOnline(uuid)) { Logg.error("Player is not online!"); return null; }
		return Bukkit.getPlayer(uuid);
	}
	
	/**
	 * Gets the name of a player regardless if they're online
	 * @param p Player
	 * @return Name of the player, null otherwise
	 */
	public static String getName(Player p)
	{
		if(p.isOnline()) { return p.getName(); }
		UUID uuid = p.getUniqueId();		
		return getName(uuid);
	}
	
	/**
	 * Gets the name of a player regardless if they're online
	 * @param uuid UUID of the player
	 * @return Name of the player, null otherwise
	 */
	public static String getName(UUID uuid)
	{
		if(isOnline(uuid)) { return Bukkit.getPlayer(uuid).getName(); }
		Bukkit.getOfflinePlayer(uuid).getName();
		return null;
	}
}