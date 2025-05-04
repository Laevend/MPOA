package laeven.mpoa.virtualplayerdata;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import laeven.mpoa.MPOA;
import laeven.mpoa.config.Configurable;
import laeven.mpoa.config.Configure;
import laeven.mpoa.exception.DeserialiseException;
import laeven.mpoa.exception.SerialiseException;
import laeven.mpoa.loginworld.LoginWorldCtrl;
import laeven.mpoa.utils.ChecksumUtils;
import laeven.mpoa.utils.ColourUtils;
import laeven.mpoa.utils.Cooldown;
import laeven.mpoa.utils.FUtils;
import laeven.mpoa.utils.Logg;
import laeven.mpoa.utils.TimeUtils;
import laeven.mpoa.utils.json.JUtils;
import laeven.mpoa.utils.structs.UID4;
import laeven.mpoa.utils.tools.Deserialise;
import laeven.mpoa.virtualplayerdata.data.DefaultVirtualPlayerData;
import laeven.mpoa.virtualplayerdata.data.VirtualPlayerData;

/**
 * 
 * @author Laeven
 *
 */
public class VirtualPlayerDataCtrl implements Listener, Configurable
{
	private static ConcurrentHashMap<UID4,VirtualPlayerData> playerData = new ConcurrentHashMap<>();
	private static BiMap<UUID,UID4> loggedInVirtualAccounts = HashBiMap.create();
	private static Set<UUID> actingAccounts = new HashSet<>();
	
	private static Map<UUID,UID4> linkedAccountPlayers = new HashMap<>();
	private static Set<UUID> oldLinkedAccountPlayers = new HashSet<>();
	private static Path linkedAccountPlayersPath = MPOA.internalFilePath("playerdata" + File.separator + "linked_account_players.json");
	
	public static LocalDateTime BACKUP_CUTOFF_DATE = LocalDateTime.now().minusDays(7);
	public static final int MAX_BACKUPS_PER_PLAYER = 300; // 1 backup per 10 minutes
	
	private static final UUID dropItemCooldown = UUID.randomUUID();
	private static final UUID pickupItemCooldown = UUID.randomUUID();
	private static final UUID clickAndDragCooldown = UUID.randomUUID();
	
	private static Material[] resetPassword = new Material[4];
	
	public static void loadActingAccounts()
	{
		Set<UUID> actingAccounts = new HashSet<>();
		
		for(String uuid : MPOA.getConfigFile().getStringList("virtual_player_accounts.acting_accounts"))
		{
			if(!Deserialise.uuidPattern.matcher(uuid).matches())
			{
				throw new IllegalArgumentException("UUID '" + uuid + " is not a valid UUID!");
			}
			
			UUID playerUUID = UUID.fromString(uuid);
			actingAccounts.add(playerUUID);
			
			Logg.info("Added acting account (" + uuid + ")");
		}
		
		VirtualPlayerDataCtrl.actingAccounts = actingAccounts;
	}
	
	public static void loadLinkedAccountPlayers()
	{
		if(!Files.exists(linkedAccountPlayersPath)) { return; }
		
		try
		{
			JsonObject data = JUtils.readToObject(linkedAccountPlayersPath);
		
			for(Entry<String,JsonElement> entry : data.entrySet())
			{
				Deserialise.assertProperty(entry.getKey(),Deserialise.Type.STRING,data);
				
				if(!Deserialise.uuidPattern.matcher(entry.getKey()).matches())
				{
					throw new IllegalArgumentException("LinkedAccountPlayer json key is not a UUID type!");
				}
				
				if(!UID4.UID4Pattern.matcher(entry.getValue().getAsString()).matches())
				{
					if(entry.getValue().getAsString().equals("pending_clearing"))
					{
						oldLinkedAccountPlayers.add(UUID.fromString(entry.getKey()));
						continue;
					}
					
					throw new IllegalArgumentException("LinkedAccountPlayer json value is not a UID4 type! or 'pending_clearing'");
				}
				
				linkedAccountPlayers.put(UUID.fromString(entry.getKey()),UID4.fromString(entry.getValue().getAsString()));
			}
		}
		catch(IllegalArgumentException e)
		{
			Logg.error("Could not Deserialise LinkedAccountPlayers!",e);
		}
	}
	
	public static void loadResetPassword()
	{
		String resetPassword = MPOA.getConfigFile().getString("virtual_player_accounts.reset_password_pattern");
		String[] pattern = resetPassword.split(",");
		
		if(pattern.length != 4)
		{
			VirtualPlayerDataCtrl.resetPassword = new Material[] {Material.CRAFTING_TABLE,Material.GRASS_BLOCK,Material.DIAMOND_SWORD,Material.GOLD_BLOCK};
			throw new IllegalArgumentException("Reset password pattern must only have 3 ',' seperators! Defaulting to CRAFTING_TABLE,GRASS_BLOCK,DIAMOND_SWORD,GOLD_BLOCK");
		}
		
		String nextMat = "";
		
		try
		{
			for(int i = 0; i < pattern.length; i++)
			{
				nextMat = pattern[i];
				Material m = Material.valueOf(pattern[i]);
				VirtualPlayerDataCtrl.resetPassword[i] = m;
			}
			
			Logg.info("Loaded reset pattern password as " + 
					VirtualPlayerDataCtrl.resetPassword[0].toString() + " > " + 
					VirtualPlayerDataCtrl.resetPassword[1].toString() + " > " + 
					VirtualPlayerDataCtrl.resetPassword[2].toString() + " > " + 
					VirtualPlayerDataCtrl.resetPassword[3].toString());
		}
		catch(Exception e)
		{
			Logg.error("Material " + nextMat + " is not a valid material type! Check https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html for valid material types!");
			VirtualPlayerDataCtrl.resetPassword = new Material[] {Material.CRAFTING_TABLE,Material.GRASS_BLOCK,Material.DIAMOND_SWORD,Material.GOLD_BLOCK};
		}
	}
	
	public static boolean saveLinkedAccountPlayers()
	{
		JsonObject data = new JsonObject();
		
		for(Entry<UUID,UID4> entry : linkedAccountPlayers.entrySet())
		{
			data.addProperty(entry.getKey().toString(),entry.getValue().toString());
		}
		
		for(UUID entry : oldLinkedAccountPlayers)
		{
			data.addProperty(entry.toString(),"pending_clearing");
		}
		
		String json = JUtils.toJsonString(data,true);
		JUtils.write(linkedAccountPlayersPath,data,true);
		
		long checksumOfMemory = ChecksumUtils.getChecksum(json.getBytes());
		long checksumOfFile = FUtils.checksumFile(linkedAccountPlayersPath);
		
		if(checksumOfMemory == checksumOfFile)
		{
			return true;
		}
		else
		{
			Logg.error("LinkedAccountPlayers saved on disk does not match data in memory! (Checksum of file) " + checksumOfFile + " != " + checksumOfMemory + " (Checksum of memory)");
			return false;
		}
	}
	
	/**
	 * Loads player data from file (or creates new player data if they're not online)
	 * @param uuid UUID of a player to load data for
	 */
	public static void loadPlayerData(UID4 uid4)
	{
		// Prevents reloading while already in memory
		if(playerData.containsKey(uid4)) { return; }
		
		// Player data path
		Path playerDataFile = MPOA.internalFilePath("playerdata" + File.separator + uid4.toString() + File.separator + "playerdata.json");
		VirtualPlayerData data = null;
		
		// Check if an existing player data file exists
		if(!Files.exists(playerDataFile))
		{
			// No existing file exists so create a new one
			data = new VirtualPlayerData(uid4);
			playerData.put(uid4,data);
			return;
		}
		
		// Existing player data exists, attempt to load it
		JsonObject dataObj = JUtils.readToObject(playerDataFile);
		
		// If loading did not fail, attempt to deserialise
		if(dataObj != null)
		{
			data = new VirtualPlayerData(uid4);
			
			try
			{
				// Attempt to deserialise data. If successful save to map!
				data.deserialise(dataObj);
				data.startDegradedClock();
				playerData.put(uid4,data);
				return;
			}
			catch(DeserialiseException e)
			{
				// Could not deserialise data so treat as corrupted
				Logg.error("Could not deserialise PlayerData " + uid4.toString() + " (" + uid4.getPrefix() + ")",e);
			}
		}
		
		// Loading failed, player data may be corrupted?
		Logg.fatal("An error occured attempting to load playerdata for " + uid4.getPrefix() + "!");
		Logg.warn("Attempting a temporary data fix...");
		
		// Fix corrupted data by moving the current 'corrupted' data to a folder for later examining and manual fixing.
		// Restore from the most recent good backup
		fixCorruptedData(uid4);
		
		// Check if a backup was able to be retrieved and used as a substitute
		if(Files.exists(playerDataFile))
		{
			// Existing player data exists, attempt to load it
			JsonObject restoredDataObj = JUtils.readToObject(playerDataFile);
			
			// Check if the restored backup was loaded
			if(restoredDataObj != null)
			{
				data = new VirtualPlayerData(uid4);
				
				try
				{
					// Attempt to deserialise data. If successful save to map!
					data.deserialise(restoredDataObj);
					data.setDegradedState(true);
					data.startDegradedClock();
					Logg.info("Data restored from backup!");
					playerData.put(uid4,data);
					return;
				}
				catch(DeserialiseException e)
				{
					// Could not deserialise data so treat as corrupted
					Logg.error("Could not deserialise restored PlayerData " + uid4.toString() + " (" + uid4.getPrefix() + ")",e);
				}
			}
			
			Logg.fatal("Attempted fix failed! Failed to load playerdata for " + uid4.getPrefix() + ", backups may be corrupted! Investigate immediately!");
		}
		
		// Reaching this point means:
		// - Player had data and that data is corrupted
		// - A backup restore was attempted and either a backup does not exist or the backup failed to load or failed to deserialise
		
		// Blank player data is used as a placeholder
		
		Logg.info("Creating placeholder blank player data...");
		data = new VirtualPlayerData(uid4);
		Date date = new Date();
		data.setJoinDate(date);
		data.setLastJoin(date);
		data.setDegradedState(true);
		data.startDegradedClock();
		playerData.put(uid4,data);
	}
	
	/**
	 * Removes player data from the map
	 * 
	 * <p> @see #savePlayerData(Player) to save player data
	 * before removing it
	 * @param uid4 Players uid4 who's data to remove
	 */
	public static void removePlayerData(UID4 uid4)
	{
		playerData.remove(uid4);
	}
	
	/**
	 * Saves player data
	 * @param uid4 Players uid4 who's data to save
	 * @param removeFromMap If player data should be removed from the map after being saved
	 * @return True if data was saved successfully, false otherwise
	 */
	public static boolean savePlayerData(UID4 uid4,boolean removeFromMap)
	{
		boolean saveSuccessful = false;
		
		if(!playerData.containsKey(uid4))
		{
			Logg.error("Player " + uid4.getPrefix() + " does not exist in the playerdata map!");
			return saveSuccessful;
		}
		
		if(playerData.get(uid4).getActingPlayer() != null)
		{
			playerData.get(uid4).getInventory().updateInventory();
			playerData.get(uid4).getEnderchest().updateEnderchest();
			playerData.get(uid4).getEntityData().updatePlayerEntity();
		}
		
		try
		{
			JsonObject data = playerData.get(uid4).serialise();
			String json = JUtils.toJsonString(data,true);
			
			JUtils.write(playerData.get(uid4).getPlayerDataPath(),data,true);
			
			long checksumOfMemory = ChecksumUtils.getChecksum(json.getBytes());
			long checksumOfFile = FUtils.checksumFile(playerData.get(uid4).getPlayerDataPath());
			
			if(checksumOfMemory == checksumOfFile)
			{
				saveSuccessful = true;
			}
			else
			{
				Logg.error("PlayerData " + uid4.getPrefix() + " saved on disk does not match data in memory! (Checksum of file) " + checksumOfFile + " != " + checksumOfMemory + " (Checksum of memory)");
				return false;
			}
		}
		catch(SerialiseException e)
		{
			Logg.error("Could not serialise PlayerData " + uid4.toString() + " (" + uid4.getPrefix() + ")",e);
			return false;
		}
		
		if(!removeFromMap) { return saveSuccessful; }
		playerData.get(uid4).stopBackupClock();
		playerData.get(uid4).stopSaveClock();
		playerData.get(uid4).stopDegradedClock();
		removePlayerData(uid4);
		return saveSuccessful;
	}
	
	/**
	 * Saves all player data in the map to disk
	 * @param removeFromMap If player data should be removed from the map after being saved
	 * @return True if all data was saved successfully, false otherwise
	 */
	public static boolean saveAllPlayerData(boolean removeFromMap)
	{
		boolean allSavesSuccessful = true;
		
		for(UID4 uid4 : playerData.keySet())
		{
			if(!savePlayerData(uid4,removeFromMap))
			{
				allSavesSuccessful = false;
			}
		}
		
		return allSavesSuccessful;
	}
	
	/**
	 * Logs out the virtual account (if logged in), removes from memory and deletes data on disk
	 * @param uid4 Account to delete
	 */
	public static void deletePlayerData(UID4 uid4)
	{
		if(!playerData.containsKey(uid4)) { return; }
		if(isOnline(uid4))
		{
			logout(playerData.get(uid4).getActingPlayer());
		}
		
		if(VirtualPlayerDataCtrl.getPlayerData(uid4).getLinkedAccount() != null)
		{
			UUID oldLinkedAccount = VirtualPlayerDataCtrl.getPlayerData(uid4).getLinkedAccount();
			
			VirtualPlayerDataCtrl.getOldLinkedAccountPlayers().add(oldLinkedAccount);
			VirtualPlayerDataCtrl.getPlayerData(uid4).setLinkedAccount(null);
			VirtualPlayerDataCtrl.savePlayerData(uid4,false);
			VirtualPlayerDataCtrl.getLinkedAccountPlayers().remove(oldLinkedAccount);
			VirtualPlayerDataCtrl.saveLinkedAccountPlayers();
		}
		
		Path pathToData = MPOA.internalFilePath("playerdata" + File.separator + uid4.toString());
		playerData.remove(uid4);
		FUtils.delete(pathToData);
	}
	
	/**
	 * Checks if PlayerData for this player is loaded in the map
	 * @param uid4 Players uid4
	 * @return True if their PlayerData is loaded in the map, false otherwise
	 */
	public static boolean playerDataExistsInMap(UID4 uid4)
	{
		return playerData.containsKey(uid4);
	}
	
	/**
	 * Retrieves a players data.
	 * <p>
	 * It's assumed that you expected the data to be present. {@link #isPuppetingVirtualAccount(Player)} should be called before calling this method
	 * @param uid4 Players uid4 who's data to retrieve
	 * @return PlayerData when retrieved successfully, otherwise null
	 */
	public static VirtualPlayerData getPlayerData(Player actingPlayer)
	{
		return playerData.get(loggedInVirtualAccounts.get(actingPlayer.getUniqueId()));
	}
	
	/**
	 * Retrieves a players data.
	 * @param uid4 Players uid4 who's data to retrieve
	 * @return PlayerData when retrieved successfully, otherwise null
	 */
	public static VirtualPlayerData getPlayerData(UID4 uid4)
	{
		if(!playerData.containsKey(uid4))
		{			
			loadPlayerData(uid4);
			
			if(!playerData.containsKey(uid4))
			{
				Logg.error("An autoload was performed as " + uid4.toString() + "'s data did not exist. This autoload failed!");
				return null;
			}
		}
		
		return playerData.get(uid4);
	}
	
	private static DateFormat fileFormat = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
	
	/**
	 * Attempts to restore players data to a known working version
	 * @param player Player who's data to restore
	 */
	public static void fixCorruptedData(UID4 player)
	{
		Objects.requireNonNull(player,"Player uid4 cannot be null!");
		
		Path dataBackups = MPOA.internalFilePath("playerdata" + File.separator + player.toString() + File.separator + "backups");
		Path playerDataFile = MPOA.internalFilePath("playerdata" + File.separator + player.toString() + File.separator + "playerdata.json");
		Path corruptDataDir = MPOA.internalFilePath("playerdata" + File.separator + player.toString() + File.separator + "corrupted");
		
		// Make copy of corrupted player data to 'corrupted' directory
		if(!FUtils.createDirectories(corruptDataDir))
		{
			Logg.error("Could not create directories for corrupted player data location!");			
			return;
		}
		
		Path archivedCorruptedPlayedData = Path.of(corruptDataDir.toString() + File.separator + "corrupted_playerdata_" + UUID.randomUUID().toString() + ".json");
		
		// Copy corrupted player data to corrupted dir
		FUtils.copyFile(playerDataFile,archivedCorruptedPlayedData);
		
		if(!Files.exists(archivedCorruptedPlayedData))
		{
			Logg.error("Could not archive corrupted player data! Please check file permissions!");
			return;
		}
		
		// Delete original
		FUtils.delete(playerDataFile);
		
		if(!FUtils.createDirectoriesForFile(dataBackups))
		{
			Logg.error("Could not create directories for player data backup location!");
			return;
		}
		
		if(!Files.exists(dataBackups))
		{
			Logg.info("No backups exist for " + player.toString());
			return;
		}
		
		// Attempt to find a backup that is not corrupted
		TreeMap<Long,Path> backupFiles = FUtils.getPathsInDirectorySortedByAge(dataBackups);
		
		// Check no more than 24 backup files as we could be here for a while
		// If more than 24 are corrupted I'm sure there is something very wrong with the code...
		int MAX_FILES_TO_CHECK = 24;
		int filesChecked = 0;
		
		for(Path backupFile : backupFiles.values())
		{
			if(filesChecked > MAX_FILES_TO_CHECK) { return; }
			
			// Check that the backup can be read from (if it cannot be read it is corrupted)
			if(JUtils.readToObject(backupFile) != null)
			{
				Logg.info("Attempting to restore from backup: " + backupFile.getFileName());
				FUtils.copyFile(backupFile,playerDataFile);				
				Logg.fatal("Player " + player.toString() + " has degraded player data!");
				return;
			}
		}
	}
	
	/**
	 * Backups player data
	 * @param data PlayerData to backup
	 * @return Path to this backup file
	 */
	public static Path backup(VirtualPlayerData data)
	{
		if(!FUtils.createDirectoriesForFile(data.getPlayerDataBackupDirectory()))
		{
			Logg.error("Could not create directories for player data backup location!");
			return null;
		}
		
		// Prevents backing up data that's already in a degraded state
		if(data.isInDegradedState()) { return null; }
		
		Date backupDate = new Date();
		Path backupPath = Paths.get(data.getPlayerDataBackupDirectory().toString() + File.separator + fileFormat.format(backupDate) + ".json");
		
		try
		{
			JUtils.write(backupPath,data.serialise(),true);
		}
		catch(SerialiseException e)
		{
			Logg.error("Could not serialise PlayerData " + data.getOwner().toString() + " (" + data.getUsername() + ")",e);
		}
		
		return backupPath;
	}
	
	/**
	 * Verifies the backup that just took place
	 * This should be called immediately after {@link #backup(VirtualPlayerData)}
	 * @param pathToBackup Path to backup file
	 * @param data PlayerData held in memory to compare against
	 * @return True if backup was saved successfully, false otherwise
	 */
	public static boolean verifyBackup(Path pathToBackup,VirtualPlayerData data)
	{
		if(data.getMostRecentSerialisedData() == null)
		{
			Logg.error("No data has been serialised!");
			return false;
		}
		
		String json = JUtils.toJsonString(data.getMostRecentSerialisedData(),false);
		
		long checksumOfMemory = ChecksumUtils.getChecksum(json.getBytes());
		long checksumOfFile = FUtils.checksumFile(pathToBackup);
		
		if(checksumOfMemory == checksumOfFile)
		{
			return true;
		}
		else
		{
			Logg.error("Backup of playerData " + data.getUsername() + " saved on disk does not match data in memory! " + checksumOfFile + " != " + checksumOfMemory);
		}
		
		return false;
	}
	
	/**
	 * Deletes backup files older than a week
	 * @param data PlayerData to check the backup files of
	 */
	public static void checkAndDeleteOldBackups(VirtualPlayerData data)
	{
		if(!FUtils.createDirectoriesForFile(data.getPlayerDataBackupDirectory()))
		{
			Logg.error("Could not create directories for player data backup location!");
			return;
		}
		
		Path backupPath = Paths.get(data.getPlayerDataBackupDirectory().toString());
		TreeMap<Long,Path> backupFiles = new TreeMap<>();
		BACKUP_CUTOFF_DATE = LocalDateTime.now().minusDays(7);
		
		// NOTE:
		// If backups have exactly the same creation date, then they will not be picked up
		// They will however be eventually cleared with enough backup clear checks
		
		// Cull files that are older than 'BACKUP_CUTOFF_DATE'		
		try(DirectoryStream<Path> stream = Files.newDirectoryStream(backupPath))
		{
			for(Path backupFilePath : stream)
			{
				if(Files.isDirectory(backupFilePath)) { continue; }
				
				LocalDateTime date = FUtils.getFileCreationDate(backupFilePath);
				
				// If file is older than 7 days, delete
				if(BACKUP_CUTOFF_DATE.isAfter(date))
				{
					FUtils.delete(backupFilePath);
					continue;
				}
				
				long timeEpoch = TimeUtils.getMilliFromLocalDateTime(date);
				backupFiles.put(timeEpoch,backupFilePath);
			}
		}
		catch(Exception e)
		{
			Logg.error("Could not stream directory " + backupPath.toString(),e);
			Logg.fatal("Could not clean up old backup files!");
			return;
		}
		
		// We don't need to delete extra backup files if there are less than 'MAX_BACKUPS_PER_PLAYER' 
		if(backupFiles.size() <= MAX_BACKUPS_PER_PLAYER)
		{
			// Delete empty directories left over from culling
			deleteEmptyBackupDirs(backupPath);
			return;
		}
		
		// Oldest file is at the top of the list (has the smallest epoch time)
		while(backupFiles.size() > MAX_BACKUPS_PER_PLAYER)
		{
			Path backupFilePath = backupFiles.remove(backupFiles.firstKey());
			FUtils.delete(backupFilePath);
		}
		
		// Delete empty directories left over from culling
		deleteEmptyBackupDirs(backupPath);
	}
	
	private static void deleteEmptyBackupDirs(Path backupPath)
	{
		if(!Files.exists(backupPath)) { return; }
		
		// Delete empty directories left over from culling
		for(File backupDir : backupPath.toFile().listFiles())
		{
			if(backupDir.isDirectory())
			{
				if(backupDir.listFiles().length == 0)
				{
					Path backupDirPath = Paths.get(backupDir.getAbsolutePath());
					FUtils.delete(backupDirPath);
				}
			}
		}
	}
	
	/**
	 * Reads a player data file or player data backup file
	 * @param path Path to player data file / player data backup file
	 * @return JsonObject representing the serialised player data
	 */
	private static JsonObject readPlayerData(Path path)
	{
		return JUtils.readToObject(path);
	}
	
	/**
	 * Restores a players PlayerData from an existing backup
	 * @param data PlayerData to overwrite
	 * @param backupPath Backup path to restore from
	 * @return True if restore was successful, false otherwise
	 */
	public static boolean restore(VirtualPlayerData data,Path backupPath)
	{
		Objects.requireNonNull(data,"PlayerData cannot be null!");
		Objects.requireNonNull(backupPath,"Backup path cannot be null!");
		
		UID4 owner = data.getOwner();
		String playerName = data.getUsername();
		JsonObject dataObj = readPlayerData(backupPath);
		
		if(dataObj == null)
		{
			Logg.fatal("Failed to restore player data from backup for player " + playerName);
			Logg.fatal("Could not read from " + backupPath.toAbsolutePath().toString());			
			return false;
		}
		
		try
		{
			data.deserialise(dataObj);
		}
		catch(DeserialiseException e)
		{
			Logg.error("Could not deserialise restored PlayerData " + owner.toString() + " (" + data.getUsername() + ")",e);
			Logg.error("Backup " + backupPath.toString() + " is possibly corrupt or missing attributes!");
			return false;
		}
		
		data.getInventory().loadInventoryToPlayer();
		data.getEnderchest().loadEnderchestToPlayer();
		data.getEntityData().loadEntityDataToPlayer();
		if(!savePlayerData(owner,false))
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Restores a players inventory from an existing backup
	 * @param data PlayerData to overwrite
	 * @param backupPath Backup path to restore from
	 * @return True if restore was successful, false otherwise
	 */
	public static boolean restoreInventoryOnly(VirtualPlayerData data,Path backupPath)
	{
		Objects.requireNonNull(data,"PlayerData cannot be null!");
		Objects.requireNonNull(backupPath,"Backup path cannot be null!");
		
		UID4 owner = data.getOwner();
		String playerName = data.getUsername();
		JsonObject dataObj = readPlayerData(backupPath);
		
		if(dataObj == null)
		{
			Logg.fatal("Failed to restore player data from backup for player " + playerName);
			Logg.fatal("Could not read from " + backupPath.toAbsolutePath().toString());			
			return false;
		}
		
		try
		{
			data.getInventory().deserialise(dataObj.get(VirtualPlayerData.PLAYER_INVENTORY).getAsJsonObject());
		}
		catch(DeserialiseException e)
		{
			Logg.error("Could not deserialise restored Inventory PlayerData " + owner.toString() + " (" + data.getUsername() + ")",e);
			Logg.error("Backup " + backupPath.toString() + " is possibly corrupt or missing attributes!");
			return false;
		}
		
		data.getInventory().loadInventoryToPlayer();
		if(!savePlayerData(owner,false))
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Restores a players enderchest from an existing backup
	 * @param data PlayerData to overwrite
	 * @param backupPath Backup path to restore from
	 * @return True if restore was successful, false otherwise
	 */
	public static boolean restoreEnderchestOnly(VirtualPlayerData data,Path backupPath)
	{
		Objects.requireNonNull(data,"PlayerData cannot be null!");
		Objects.requireNonNull(backupPath,"Backup path cannot be null!");
		
		UID4 owner = data.getOwner();
		String playerName = data.getUsername();
		JsonObject dataObj = readPlayerData(backupPath);
		
		if(dataObj == null)
		{
			Logg.fatal("Failed to restore player data from backup for player " + playerName);
			Logg.fatal("Could not read from " + backupPath.toAbsolutePath().toString());			
			return false;
		}
		
		try
		{
			data.getEnderchest().deserialise(dataObj.get(VirtualPlayerData.PLAYER_ENDERCHEST).getAsJsonObject());
		}
		catch(DeserialiseException e)
		{
			Logg.error("Could not deserialise restored Enderchest PlayerData " + owner.toString() + " (" + data.getUsername() + ")",e);
			Logg.error("Backup " + backupPath.toString() + " is possibly corrupt or missing attributes!");
			return false;
		}
		
		data.getEnderchest().loadEnderchestToPlayer();
		if(!savePlayerData(owner,false))
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Restores a players entity data from an existing backup
	 * @param data PlayerData to overwrite
	 * @param backupPath Backup path to restore from
	 * @return True if restore was successful, false otherwise
	 */
	public static boolean restoreEntityDataOnly(VirtualPlayerData data,Path backupPath)
	{
		Objects.requireNonNull(data,"PlayerData cannot be null!");
		Objects.requireNonNull(backupPath,"Backup path cannot be null!");
		
		UID4 owner = data.getOwner();
		String playerName = data.getUsername();
		JsonObject dataObj = readPlayerData(backupPath);
		
		if(dataObj == null)
		{
			Logg.fatal("Failed to restore player data from backup for player " + playerName);
			Logg.fatal("Could not read from " + backupPath.toAbsolutePath().toString());			
			return false;
		}
		
		try
		{
			data.getEntityData().deserialise(dataObj.get(VirtualPlayerData.PLAYER_ENTITY).getAsJsonObject());
		}
		catch(DeserialiseException e)
		{
			Logg.error("Could not deserialise restored Enderchest PlayerData " + owner.toString() + " (" + data.getUsername() + ")",e);
			Logg.error("Backup " + backupPath.toString() + " is possibly corrupt or missing attributes!");
			return false;
		}
		
		data.getEntityData().loadEntityDataToPlayer();
		if(!savePlayerData(owner,false))
		{
			return false;
		}
		
		return true;
	}
	
	@EventHandler(priority = EventPriority.LOWEST,ignoreCancelled = false)
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		UUID uuid = e.getPlayer().getUniqueId();
		
		// Reset previously linked players that were not online at the time their account was unlinked from them
		if(getOldLinkedAccountPlayers().contains(uuid))
		{
			DefaultVirtualPlayerData.resetPlayerData(e.getPlayer());
			getOldLinkedAccountPlayers().remove(uuid);
			VirtualPlayerDataCtrl.saveLinkedAccountPlayers();
		}
		
		if(linkedAccountPlayers.containsKey(uuid))
		{
			UID4 uid = linkedAccountPlayers.get(uuid);
			loginWithLinked(uid,e.getPlayer());
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST,ignoreCancelled = false)
	public void onPlayerQuit(PlayerQuitEvent e)
	{
		if(!isPuppetingVirtualAccount(e.getPlayer())) { return; }
		e.setQuitMessage(null);
		logout(e.getPlayer());
	}
	
	/**
	 * Login to a virtual player account
	 * @param uuid UID of the player data to log in with
	 * @param actingPlayer
	 * @return
	 */
	public static boolean login(UID4 uid4,Player actingPlayer)
	{
		loadPlayerData(uid4);
		boolean isActingPlayer = actingAccounts.contains(actingPlayer.getUniqueId());
		boolean isLinkedPlayer = playerData.get(uid4).getLinkedAccount() != null && playerData.get(uid4).getLinkedAccount().equals(actingPlayer.getUniqueId());
		
		if(!playerData.containsKey(uid4))
		{
			Logg.error("An error occured! Could not log into account " + uid4.toString());
			return false;
		}
		
		// Check the acting player is a player listed for use as acting player or if the player is linked to this virtual account
		if(!isActingPlayer && !isLinkedPlayer)
		{
			Logg.error("Could not login to account " + uid4.toString() + " using player " + actingPlayer.getName() + " because this player is not listed as an acting player or a player linked to this virtual account!");
			return false;
		}
		
		// Check account is not already logged into another account
		if(loggedInVirtualAccounts.inverse().containsKey(uid4))
		{
			Logg.error("Could not login to account " + uid4.toString() + " as this account is already logged in by player " + playerData.get(uid4).getActingPlayer().getName());
			return false;
		}
		
		playerData.get(uid4).setActingPlayer(actingPlayer);
		loggedInVirtualAccounts.put(actingPlayer.getUniqueId(),uid4);
		
		Logg.verb("Loading account for " + uid4.toString(),Logg.VerbGroup.PLAYER_DATA);
		
		VirtualPlayerData pData = getPlayerData(uid4);
		
		pData.setLastJoin(new Date());
		pData.getInventory().loadInventoryToPlayer();
		pData.getEnderchest().loadEnderchestToPlayer();
		pData.getEntityData().loadEntityDataToPlayer();
		pData.startBackupClock();
		
		if(pData.getTimelimit().isEnabled())
		{
			pData.getTimelimit().startTimer();
		}
		
		if(!isLinkedPlayer)
		{
			LoginWorldCtrl.removeWaiting(actingPlayer);
		}
		return true;
	}
	
	public static void loginWithLinked(UID4 uid4,Player linkedPlayer)
	{
		Path playerDataFile = MPOA.internalFilePath("playerdata" + File.separator + uid4.toString() + File.separator + "playerdata.json");
		
		// Checks that the linked account in linked_account_players.json points to an account that exists
		if(!Files.exists(playerDataFile))
		{
			// No existing file exists so create a new one
			linkedPlayer.kickPlayer(ColourUtils.transCol("&cCannot login, this account no longer exists!"));
			VirtualPlayerDataCtrl.getOldLinkedAccountPlayers().add(linkedPlayer.getUniqueId());
			VirtualPlayerDataCtrl.getLinkedAccountPlayers().remove(linkedPlayer.getUniqueId());
			VirtualPlayerDataCtrl.saveLinkedAccountPlayers();
			Logg.warn("Player " + linkedPlayer.getName() + " attempted to log into a virtual account that no longer exists! (" + uid4.toString() + ")");
			return;
		}
		
		VirtualPlayerData pData = VirtualPlayerDataCtrl.getPlayerData(uid4);
		
		// Checks that the linked account in linked_account_players.json points to an account they are still linked to
		if(pData.getLinkedAccount() == null || !pData.getLinkedAccount().equals(linkedPlayer.getUniqueId()))
		{
			linkedPlayer.kickPlayer(ColourUtils.transCol("&cCannot login, you are no longer linked to this account!"));
			VirtualPlayerDataCtrl.getOldLinkedAccountPlayers().add(linkedPlayer.getUniqueId());
			VirtualPlayerDataCtrl.getLinkedAccountPlayers().remove(linkedPlayer.getUniqueId());
			VirtualPlayerDataCtrl.saveLinkedAccountPlayers();
			Logg.warn("Player " + linkedPlayer.getName() + " attempted to log into a virtual account that they should no longer be linked to! (" + uid4.toString() + ")");
			return;
		}
		
		if(pData.isLocked())
		{
			linkedPlayer.kickPlayer(ColourUtils.transCol("&cCannot login, this account is locked!"));
			return;
		}
		
		// Check for cooldown
		if(pData.getTimelimit().isEnabled() && !pData.getTimelimit().hasCooldownExpired())
		{
			long timeRemaining = pData.getTimelimit().getMaxCooldownTime() - (System.currentTimeMillis() - pData.getTimelimit().getCooldownStartTime());
			linkedPlayer.kickPlayer(ColourUtils.transCol("&cCannot login, Cooldown has not expired! Remaining time: " + TimeUtils.millisecondsToHoursMinutesSeconds(timeRemaining)));
			return;
		}
		
		// Reset play time if it is 0 and cooldown has expired
		if(pData.getTimelimit().isEnabled() && pData.getTimelimit().getPlayTimeLeft() == 0)
		{
			pData.getTimelimit().resetPlayTime();
		}
		
		if(pData.getActingPlayer() != null)
		{
			linkedPlayer.kickPlayer(ColourUtils.transCol("&cThis virtual account is already logged in using account " + pData.getActingPlayer().getName()));
			return;
		}
		
		if(!VirtualPlayerDataCtrl.login(uid4,linkedPlayer))
		{
			linkedPlayer.kickPlayer(ColourUtils.transCol("&cAn error occured attempting to login to account " + uid4.toString()));
			return;
		}
	}
	
	/**
	 * Logout of a virtual account
	 * @param actingPlayer Acting player that is logged into a virtual account
	 * @return True if logout was successful
	 */
	public static boolean logout(Player actingPlayer)
	{
		if(!loggedInVirtualAccounts.containsKey(actingPlayer.getUniqueId()))
		{
			Logg.error("Cannot logout acting player " + actingPlayer.getName() + " as they're not listed as logged in.");
			return false;
		}
		
		UID4 uid4 = loggedInVirtualAccounts.get(actingPlayer.getUniqueId());
		
		boolean isLinkedPlayer = playerData.get(uid4).getLinkedAccount() != null && playerData.get(uid4).getLinkedAccount().equals(actingPlayer.getUniqueId());
		
		VirtualPlayerData pData = getPlayerData(uid4);
		
		pData.getTimelimit().getDisplayBar().removeAll();
		pData.getTimelimit().resetDisplayBar();
		pData.getTimelimit().stopTimer();
		
		backup(pData);
		
		if(!savePlayerData(pData.getOwner(),true)) { return false; }
		pData.stopBackupClock();
		pData.stopSaveClock();
		pData.stopDegradedClock();
		pData.setActingPlayer(null);
		loggedInVirtualAccounts.remove(actingPlayer.getUniqueId());
		
		if(pData.getTimelimit().isEnabled())
		{
			pData.getTimelimit().stopTimer();
		}
		
		if(isLinkedPlayer)
		{
			DefaultVirtualPlayerData.resetPlayerData(actingPlayer);
			actingPlayer.kickPlayer(ColourUtils.transCol("&cYou have been logged out."));
		}
		else
		{
			LoginWorldCtrl.setWaiting(actingPlayer);
		}
		return true;
	}
	
	public static BiMap<UUID,UID4> getLoggedInVirtualAccounts()
	{
		return loggedInVirtualAccounts;
	}
	
	public static Set<UUID> getActingAccounts()
	{
		return actingAccounts;
	}

	public static boolean isOnline(UID4 uuid)
	{
		return loggedInVirtualAccounts.values().contains(uuid);
	}
	
	/**
	 * Checks if a player is puppeting a virtual account
	 * @param p Player that may be an acting player for a virtual account
	 * @return True if this player is puppeting a virtual account
	 */
	public static boolean isPuppetingVirtualAccount(Player p)
	{
		return loggedInVirtualAccounts.containsKey(p.getUniqueId());
	} 
	
	// =========================================
	// Events to trigger a scheduled save
	// =========================================

	@EventHandler
	public void onDropItem(PlayerDropItemEvent e)
	{
		if(!isPuppetingVirtualAccount(e.getPlayer())) { return; }
		
		// Drop item events happen a lot, this prevents spam scheduling
		if(Cooldown.isCooling(e.getPlayer(),dropItemCooldown,true)) { return; }
		
		VirtualPlayerData data = getPlayerData(e.getPlayer());
		if(data == null) { return; }
		data.scheduleSave();
		
		Cooldown.setCooldown(e.getPlayer(),dropItemCooldown,10000);
	}
	
	@EventHandler
	public void onItemBreak(PlayerItemBreakEvent e)
	{
		if(!isPuppetingVirtualAccount(e.getPlayer())) { return; }
		VirtualPlayerData data = getPlayerData(e.getPlayer());
		if(data == null) { return; }
		data.scheduleSave();
	}
	
	@EventHandler
	public void onConsume(PlayerItemConsumeEvent e)
	{
		if(!isPuppetingVirtualAccount(e.getPlayer())) { return; }
		VirtualPlayerData data = getPlayerData(e.getPlayer());
		if(data == null) { return; }
		data.scheduleSave();
	}
	
	@EventHandler
	public void onPickupItem(EntityPickupItemEvent e)
	{
		if(!(e.getEntity() instanceof Player p)) { return; }		
		if(!isPuppetingVirtualAccount(p)) { return; }
		
		// Pickup item events happen a lot, this prevents spam scheduling
		if(Cooldown.isCooling(p,pickupItemCooldown,true)) { return; }
		
		Logg.verb("Scheduled Save -> " + p.getName(),Logg.VerbGroup.PLAYER_DATA);
		VirtualPlayerData data = getPlayerData(p);
		if(data == null) { return; }
		data.scheduleSave();
		
		Cooldown.setCooldown(p,pickupItemCooldown,10000);
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent e)
	{
		if(!isPuppetingVirtualAccount(e.getEntity())) { return; }
		VirtualPlayerData data = getPlayerData(e.getEntity());
		if(data == null) { return; }
		data.scheduleSave();
	}
	
	@EventHandler
	public void onClickInInventory(InventoryClickEvent e)
	{
		if(e.getView().getBottomInventory().getType() != InventoryType.PLAYER) { return; }
		if(e.getRawSlot() == -999 || e.getRawSlot() == -1) { return; }
		if(!isPuppetingVirtualAccount((Player) e.getWhoClicked())) { return; }
		
		// Click and Drag events happen a lot, this prevents spam scheduling
		if(Cooldown.isCooling((Player) e.getWhoClicked(),clickAndDragCooldown,true)) { return; }
		
		VirtualPlayerData data = getPlayerData((Player) e.getWhoClicked());
		if(data == null) { return; }
		data.scheduleSave();
		
		Cooldown.setCooldown((Player) e.getWhoClicked(),clickAndDragCooldown,10000);
	}
	
	@EventHandler
	public void onDragInInventory(InventoryDragEvent e)
	{
		if(e.getView().getBottomInventory().getType() != InventoryType.PLAYER) { return; }
		
		// Click and Drag events happen a lot, this prevents spam scheduling
		if(Cooldown.isCooling((Player) e.getWhoClicked(),clickAndDragCooldown,true)) { return; }
		if(!isPuppetingVirtualAccount((Player) e.getWhoClicked())) { return; }
		
		VirtualPlayerData data = getPlayerData((Player) e.getWhoClicked());
		if(data == null) { return; }
		data.scheduleSave();
		
		Cooldown.setCooldown((Player) e.getWhoClicked(),clickAndDragCooldown,10000);
	}
	
	@EventHandler
	public void onLevelChange(PlayerLevelChangeEvent e)
	{
		if(!isPuppetingVirtualAccount(e.getPlayer())) { return; }
		VirtualPlayerData data = getPlayerData(e.getPlayer());
		if(data == null) { return; }
		data.scheduleSave();
	}
	
	private static final List<String> emptyList = List.of("<none>");
	private static final Set<String> emptySet = Set.of("<none>");
	
	private static List<String> cachedVirtualAccountBackupFiles = null;
	private static long getPlayerDataBackupFiles_lastCallTime = 0L;
	
	public static List<String> getPlayerDataBackupFiles(UID4 uid)
	{
		// Prevents a lot of directory scanning when tab complete for the accounts command rapidly calls this to provide estimates
		// Virtual account backups are cached for 7.5 seconds before checking again
		if(cachedVirtualAccountBackupFiles != null && System.currentTimeMillis() - getPlayerDataBackupFiles_lastCallTime < 7500L)
		{
			return cachedVirtualAccountBackupFiles;
		}
		
		List<String> backupFileNames = new ArrayList<>();
		Path dataBackups = VirtualPlayerDataCtrl.getPlayerData(uid).getPlayerDataBackupDirectory();
		
		if(!Files.exists(dataBackups)) { return emptyList; }
		
		try(DirectoryStream<Path> stream = Files.newDirectoryStream(dataBackups))
		{
			for(Path entry : stream)
			{
				if(Files.isDirectory(entry)) { continue; }
				
				backupFileNames.add(entry.getFileName().toString());
			}
		}
		catch(Exception e)
		{
			Logg.error("Could not stream directory " + dataBackups.toString(),e);
			return emptyList;
		}
		
		cachedVirtualAccountBackupFiles = backupFileNames;
		return backupFileNames;
	}
	
	private static Set<String> cachedVirtualAccountNamesOnFile = null;
	private static long getVirtualAccountNames_lastCallTime = 0L;
	
	public static Set<String> getVirtualAccountNames()
	{
		// Prevents a lot of directory scanning when tab complete for the accounts command rapidly calls this to provide estimates
		// Virtual accounts are cached for 7.5 seconds before checking again
		if(cachedVirtualAccountNamesOnFile != null && System.currentTimeMillis() - getVirtualAccountNames_lastCallTime < 7500L)
		{
			return cachedVirtualAccountNamesOnFile;
		}
		
		Set<String> accountNames = new HashSet<>();
		Path playerDataDir = MPOA.internalFilePath("playerdata");
		
		if(!Files.exists(playerDataDir)) { return emptySet; }
		
		try(DirectoryStream<Path> stream = Files.newDirectoryStream(playerDataDir))
		{
			for(Path entry : stream)
			{
				if(!Files.isDirectory(entry)) { continue; }
				
				accountNames.add(entry.getFileName().toString());
			}
		}
		catch(Exception e)
		{
			Logg.error("Could not stream directory " + playerDataDir.toString(),e);
			return emptySet;
		}
		
		cachedVirtualAccountNamesOnFile = accountNames;
		return accountNames;
	}
	
	public static Set<String> getOfflineVirtualAccountNames()
	{
		Set<String> accountNames = getVirtualAccountNames();
		
		for(String onlineName : getOnlineVirtualAccountNames())
		{
			accountNames.remove(onlineName);
		}
		
		return accountNames;
	}
	
	public static Set<String> getOnlineVirtualAccountNames()
	{
		Set<String> accountNames = new HashSet<>();
		
		for(UID4 key : playerData.keySet())
		{
			accountNames.add(key.toString());
		}
		
		return accountNames;
	}
	
	public static Map<UUID,UID4> getLinkedAccountPlayers()
	{
		return linkedAccountPlayers;
	}

	public static Set<UUID> getOldLinkedAccountPlayers()
	{
		return oldLinkedAccountPlayers;
	}
	
	public static Material[] getResetPassword()
	{
		return resetPassword;
	}

	@Configure
	public static Map<String,Object> getDefaults()
	{
		return Map.of("virtual_player_accounts.acting_accounts",new ArrayList<String>(),
				  "virtual_player_accounts.default_playtime","30m",
				  "virtual_player_accounts.default_cooldown","8h",
				  "virtual_player_accounts.save_advancements",true,
				  "virtual_player_accounts.save_statistics",true,
				  "virtual_player_accounts.reset_password_pattern","CRAFTING_TABLE,GRASS_BLOCK,DIAMOND_SWORD,GOLD_BLOCK");
	}
}