package laeven.mpoa.utils;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;

/**
 * @author Laeven
 * @since 1.0.0
 */
public class WorldUtils
{
	/**
	 * Gets the directory path all worlds are in
	 * @return
	 */
	public static String getWorldContainerPath()
	{
		// Substring as by default you get "C:\Server Dir\."
		// We don't need or want the extra slash or dot on the end so substring to remove it!
		return Bukkit.getServer().getWorldContainer().getAbsolutePath().substring(0,Bukkit.getServer().getWorldContainer().getAbsolutePath().length() - 2);
	}
	
	/**
	 * Gets the world directory path
	 * @param worldName Name of world to get world directory path for
	 * @return World directory path
	 */
	public static String getWorldPath(String worldName)
	{
		return getWorldContainerPath() + File.separator + worldName;
	}
	
	/**
	 * Checks if a world is already loaded
	 * @param worldName Name of world
	 * @return true if world is already loaded, otherwise false
	 */
	public static boolean isWorldLoaded(String worldName)
	{
		World w = Bukkit.getWorld(worldName);
		return w != null;
	}
	
	/**
	 * Retrieves a world, loaded or not.
	 * 
	 * <p>If world is already loaded, world it fetched from memory.
	 * <p>If world is not loaded, world is automatically loaded and returned.
	 * Given that a main world always has the folder 'playerdata',
	 * the nether world always has the folder 'DIM-1', and
	 * the end world always has the folder 'DIM1'.
	 * These assumptions are what determine what environment is used to load the world with.
	 * 
	 * @param worldName Name of the world to load
	 * @return returns null if world could not be found or environment could not be determined
	 */
	public static World getWorld(String worldName)
	{
		if(WorldUtils.isWorldLoaded(worldName))
		{
			return Bukkit.getWorld(worldName);
		}
		
		Path worldDirectoryPath = Paths.get(WorldUtils.getWorldPath(worldName));
		
		if(!Files.isDirectory(worldDirectoryPath)) { return null; }
		
		Path netherPath = Paths.get(worldDirectoryPath.toString() + File.separator + "DIM-1");
		Path endPath = Paths.get(worldDirectoryPath.toString() + File.separator + "DIM1");
		
		Environment env;
		
		// Both DIM-1 (nether) and DIM1 (end) are found in a single player world directory
		if(Files.isDirectory(netherPath) && Files.isDirectory(endPath))
		{
			env = Environment.NORMAL;
		}
		// If DIM-1 is found but not DIM1 then it's a server nether world
		else if(Files.isDirectory(netherPath) && !Files.isDirectory(endPath))
		{
			env = Environment.NETHER;
		}
		// If DIM1 is found but not DIM-1 then it's a server end world
		else if(Files.isDirectory(endPath) && !Files.isDirectory(netherPath))
		{
			env = Environment.THE_END;
		}
		// If neither DIM-1 or DIM1 is found then it's an additional over world that's not the default
		else
		{
			env = Environment.NORMAL;
		}
		
		Logg.info("World " + worldName + " was force loaded using environment " + env.toString() + " due to a subsystem request.");
		
		return new WorldCreator(worldName).environment(env).createWorld();
	}
	
	public static World createNewWorld(String worldName,Environment env)
	{
		if(WorldUtils.isWorldLoaded(worldName))
		{
			return Bukkit.getWorld(worldName);
		}
		
		Path worldDirectoryPath = Paths.get(WorldUtils.getWorldPath(worldName));
		
		if(Files.isDirectory(worldDirectoryPath)) { return getWorld(worldName); }
		
		return new WorldCreator(worldName).environment(env).keepSpawnInMemory(false).createWorld();
	}
	
	public static String getDefaultWorldName()
	{
		Properties serverProp = new Properties();
		String levelName = null;
		
		try(InputStream is = Files.newInputStream(Paths.get("server.properties")))
		{
			serverProp.load(is);
			levelName = serverProp.getProperty("level-name");
			is.close();
			
			if(levelName == null || levelName.isEmpty() || levelName.isBlank())
			{
				Logg.warn("Could not fetch default world name from server.properties! Defaulting to 'world'");
				levelName = "world";
			}
		}
		catch(Exception e)
		{
			Logg.error("Error reading server.properties!",e);
		}
		
		return "world";
	}
	
	/**
	 * Edge of a Minecraft world a player can go without special tricks to bypass the border
	 * @return
	 */
	public static int getEdge()
	{
		return 29_999_984;
	}
}
