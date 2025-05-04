package laeven.mpoa.virtualplayerdata.data;

import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.google.gson.JsonObject;

import laeven.mpoa.MPOA;
import laeven.mpoa.exception.DeserialiseException;
import laeven.mpoa.exception.SerialiseException;
import laeven.mpoa.utils.Logg;
import laeven.mpoa.utils.json.JUtils;
import laeven.mpoa.utils.json.PersistJson;
import laeven.mpoa.utils.security.HashingUtils;
import laeven.mpoa.utils.structs.UID4;
import laeven.mpoa.utils.tools.Deserialise;
import laeven.mpoa.virtualplayerdata.VirtualPlayerDataCtrl;
import laeven.mpoa.virtualplayerdata.clocks.PlayerDataBackupClock;
import laeven.mpoa.virtualplayerdata.clocks.PlayerDataDegradedClock;
import laeven.mpoa.virtualplayerdata.clocks.PlayerDataSaveClock;

/**
 * An adapted variant of the PlayerData module from Dape
 */
public class VirtualPlayerData implements PersistJson
{
	public static final DateFormat SaveFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	public static final DateFormat DisplayFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
	private PlayerDataBackupClock backupClock = null;
	private PlayerDataSaveClock saveClock = null;
	private PlayerDataDegradedClock degradeClock = null;
	
	private boolean degradedState = false;
	private boolean silenceDegradedAlarm = false;
	private boolean locked = false;
	private String password;
	private byte[] salt;
	
	private Player actingPlayer = null;
	
	private UID4 owner = null;
	private String username = null;
	private Date joinDate = null;
	private Date lastJoin = null;
	
	private VirtualPlayerInventory inv = null;
	private VirtualPlayerEnderchest echest = null;
	private VirtualPlayerEntityData entityData = null;
	private VirtualPlayerTimeLimit timelimit = null;
	
	private UUID linkedAccount = null;
	
	private JsonObject cachedSerialisedData = null;
	
	public VirtualPlayerData(UID4 uuid)
	{
		this.owner = uuid;
		this.username = "unknown";
		this.joinDate = new Date();
		this.lastJoin = new Date();
		
		setNewPassword(VirtualPlayerDataCtrl.getResetPassword());
	}
	
	/**
	 * Indicates this player data file is a temporary fix.
	 * This data file was a auto-restore from a backup when their main data file 
	 * could not be read correctly!
	 * @return
	 */
	public boolean isInDegradedState()
	{
		return degradedState;
	}

	public void setDegradedState(boolean tempData)
	{
		this.degradedState = tempData;
	}
	
	/**
	 * Indicates that this player has silenced the degraded alarm for themselves
	 * @return If the player has silenced the degraded alarm
	 */
	public boolean isSilenceDegradedAlarm()
	{
		return silenceDegradedAlarm;
	}

	public void setSilenceDegradedAlarm(boolean silenceDegradedAlarm)
	{
		this.silenceDegradedAlarm = silenceDegradedAlarm;
	}

	public Player getActingPlayer()
	{
		return actingPlayer;
	}

	public void setActingPlayer(Player actingPlayer)
	{
		this.actingPlayer = actingPlayer;
	}

	public boolean isLocked()
	{
		return locked;
	}

	public void setLocked(boolean locked)
	{
		this.locked = locked;
	}
	
	public String getPassword()
	{
		return password;
	}

	public void setNewPassword(Material[] materialPattern)
	{
		Objects.requireNonNull(materialPattern,"Material pattern cannot be null!");
		if(materialPattern.length != 4)
		{
			throw new IllegalArgumentException("Material pattern can only be a size of 4");
		}
		
		for(Material mat : materialPattern)
		{
			Objects.requireNonNull(mat,"Material of material pattern cannot be null!");
		}
		
		String password = 
				materialPattern[0].toString() + "," +
				materialPattern[1].toString() + "," + 
				materialPattern[2].toString() + "," + 
				materialPattern[3].toString();
		
		this.salt = HashingUtils.generateSalt();
		this.password = HashingUtils.hashToString(password,this.salt);
	}

	public byte[] getSalt()
	{
		return salt;
	}

	public void setSalt(byte[] salt)
	{
		this.salt = salt;
	}

	/**
	 * Owner of account
	 * @return
	 */
	public UID4 getOwner()
	{
		return owner;
	}
	
	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public Date getJoinDate()
	{
		return joinDate;
	}
	
	public void setJoinDate(Date joinDate)
	{
		this.joinDate = joinDate;
	}
	
	public Date getLastJoin()
	{
		return lastJoin;
	}
	
	public void setLastJoin(Date lastJoin)
	{
		this.lastJoin = lastJoin;
	}
	
	public VirtualPlayerInventory getInventory()
	{
		if(this.inv == null) { this.inv = new VirtualPlayerInventory(this); }		
		return inv;
	}
	
	public void setInventory(VirtualPlayerInventory inv)
	{
		this.inv = inv;
	}
	
	public VirtualPlayerEnderchest getEnderchest()
	{
		if(this.echest == null) { this.echest = new VirtualPlayerEnderchest(this); }		
		return echest;
	}
	
	public void setEnderchest(VirtualPlayerEnderchest echest)
	{
		this.echest = echest;
	}
	
	public VirtualPlayerEntityData getEntityData()
	{
		if(this.entityData == null) { this.entityData = new VirtualPlayerEntityData(this); }		
		return entityData;
	}
	
	public void setEntityData(VirtualPlayerEntityData entityData)
	{
		this.entityData = entityData;
	}
	
	public VirtualPlayerTimeLimit getTimelimit()
	{
		if(this.timelimit == null) { this.timelimit = new VirtualPlayerTimeLimit(this); }		
		return timelimit;
	}

	public void setTimelimit(VirtualPlayerTimeLimit timelimit)
	{
		this.timelimit = timelimit;
	}

	public UUID getLinkedAccount()
	{
		return linkedAccount;
	}

	public void setLinkedAccount(UUID linkedAccount)
	{
		this.linkedAccount = linkedAccount;
	}

	public void printDataToConsole()
	{
		try
		{
			String json = JUtils.toJsonString(serialise(),true);
			Logg.info("Player Data Json > " + username);
			Logg.info("&e" + json);
			return;
		}
		catch(SerialiseException e)
		{
			Logg.error("Could not serialise PlayerData " + owner.toString() + " (" + username + ")",e);
		}
	}

	/**
	 * Get the file path of where a players data file is stored.
	 * @return Path of a players data file.
	 */
	public Path getPlayerDataPath()
	{
		return MPOA.internalFilePath("playerdata" + File.separator + getOwner().toString() + File.separator + "playerdata.json");
	}
	
	/**
	 * Get the file path of where backups of a players data backups are stored.
	 * @return Path of a players data backups
	 */
	public Path getPlayerDataBackupDirectory()
	{
		return MPOA.internalFilePath("playerdata" + File.separator + getOwner().toString() + File.separator + "backups");
	}
	
	/**
	 * Get the file path of where backups of corrupt player data files are stored.
	 * @return Path of a corrupt player data files
	 */
	public Path getPlayerDataCorruptDataDirectory()
	{
		return MPOA.internalFilePath("playerdata" + File.separator + getOwner().toString() + File.separator + "corrupted");
	}
	
	public void scheduleSave()
	{
		// Check if clock is null. If it is, create a new clock
		if(this.saveClock == null) { this.saveClock = new PlayerDataSaveClock(this); this.saveClock.start(); return; }
		
		// Check if clock is not enabled because it has already ran
		if(!this.saveClock.isEnabled())
		{
			this.saveClock.start();
			return;
		}
		
		// If the clock is already running, delay the save
		this.saveClock.delay();
	}
	
	public void stopSaveClock()
	{
		// Clock is null so there is no clock to stop
		if(this.saveClock == null) { return; }
		
		// Clock is not null but has already stopped
		if(!this.saveClock.isEnabled()) { return; }
		
		this.saveClock.stop();
	}
	
	public void startBackupClock()
	{
		// Check if clock is null. If it is, create a new clock
		if(this.backupClock == null) { this.backupClock = new PlayerDataBackupClock(this); this.backupClock.start(); return; }
		
		// Check if clock is not enabled because it has already ran
		if(!this.backupClock.isEnabled())
		{
			this.backupClock.start();
			return;
		}
		
		Logg.warn("Cannot start backup clock for " + username + ". It's already enabled!");
	}
	
	public void stopBackupClock()
	{
		// Clock is null so there is no clock to stop
		if(this.backupClock == null) { return; }
		
		// Clock is not null but has already stopped
		if(!this.backupClock.isEnabled()) { return; }
		
		this.backupClock.stop();
	}
	
	public void stopBackupClockNow()
	{
		// Clock is null so there is no clock to stop
		if(this.backupClock == null) { return; }
		
		// Clock is not null but has already stopped
		if(!this.backupClock.isEnabled()) { return; }
		
		this.backupClock.kill();
	}
	
	public void startDegradedClock()
	{
		if(!this.degradedState) { return; }
		
		// Check if clock is null. If it is, create a new clock
		if(this.degradeClock == null) { this.degradeClock = new PlayerDataDegradedClock(this); this.degradeClock.start(); return; }
		
		// Check if clock is not enabled because it has already ran
		if(!this.degradeClock.isEnabled())
		{
			this.degradeClock.start();
			return;
		}
		
		Logg.warn("Cannot start degraded clock for " + username + ". It's already enabled!");
	}
	
	public void stopDegradedClock()
	{
		if(!this.degradedState) { return; }
		
		// Clock is null so there is no clock to stop
		if(this.degradeClock == null) { return; }
		
		// Clock is not null but has already stopped
		if(!this.degradeClock.isEnabled()) { return; }
		
		this.degradeClock.stop();
	}
	
	public void stopDegradedClockNow()
	{
		if(!this.degradedState) { return; }
		
		// Clock is null so there is no clock to stop
		if(this.degradeClock == null) { return; }
		
		// Clock is not null but has already stopped
		if(!this.degradeClock.isEnabled()) { return; }
		
		this.degradeClock.kill();
	}
	
	/**
	 * Gets the serialised representation of this instance of PlayerData the last time it was serialised
	 * <p>
	 * Depending on how recently {@link #serialise()} was called this data may up very new or old
	 * @return JsonObject of serialised player data
	 */
	public JsonObject getMostRecentSerialisedData()
	{
		return cachedSerialisedData;
	}
	
	// A flag to indicate that this data was auto-restored from backup as the players main data file was corrupted and the server attempted a temp fix
	public static final String IS_IN_DEGRADED_STATE = "is_in_degraded_state";
	public static final String LOCKED = "locked";
	public static final String PASSWORD = "password";
	public static final String SALT = "salt";
	
	public static final String OWNER = "owner";
	public static final String USERNAME = "username";
	public static final String JOIN_DATE = "join_date";
	public static final String LAST_JOIN = "last_join";
	public static final String PLAYER_INVENTORY = "player_inventory";
	public static final String PLAYER_ENDERCHEST = "player_enderchest";
	public static final String PLAYER_ENTITY = "player_entity";
	public static final String TIMELIMIT = "timelimit";
	public static final String LINKED_ACCOUNT = "linked_account";

	@Override
	public JsonObject serialise() throws SerialiseException
	{
		JsonObject obj = new JsonObject();
		
		obj.addProperty(IS_IN_DEGRADED_STATE,degradedState);
		obj.addProperty(LOCKED,locked);
		obj.addProperty(PASSWORD,password);
		obj.addProperty(SALT,Base64.getEncoder().encodeToString(salt));
		obj.addProperty(OWNER,owner.toString());
		obj.addProperty(USERNAME,username);
		obj.addProperty(JOIN_DATE,VirtualPlayerData.SaveFormat.format(joinDate));
		obj.addProperty(LAST_JOIN,VirtualPlayerData.SaveFormat.format(lastJoin));
		obj.add(PLAYER_INVENTORY,getInventory().serialise());
		obj.add(PLAYER_ENDERCHEST,getEnderchest().serialise());
		obj.add(PLAYER_ENTITY,getEntityData().serialise());
		obj.add(TIMELIMIT,getTimelimit().serialise());
		
		if(linkedAccount != null)
		{
			obj.addProperty(LINKED_ACCOUNT,linkedAccount.toString());
		}
		
		cachedSerialisedData = obj;
		return obj;
	}

	@Override
	public void deserialise(JsonObject obj) throws DeserialiseException
	{
		degradedState = Deserialise.assertAndGetProperty(IS_IN_DEGRADED_STATE,Deserialise.Type.BOOLEAN,obj).getAsBoolean();
		locked = Deserialise.assertAndGetProperty(LOCKED,Deserialise.Type.BOOLEAN,obj).getAsBoolean();
		password = Deserialise.assertAndGetProperty(PASSWORD,Deserialise.Type.STRING,obj).getAsString();
		salt = Base64.getDecoder().decode(Deserialise.assertAndGetProperty(SALT,Deserialise.Type.STRING,obj).getAsString());
		owner = UID4.fromString(Deserialise.assertAndGetProperty(OWNER,Deserialise.Type.STRING,obj).getAsString());
		username = Deserialise.assertAndGetProperty(USERNAME,Deserialise.Type.STRING,obj).getAsString();
		
		try
		{
			joinDate =  VirtualPlayerData.SaveFormat.parse(Deserialise.assertAndGetProperty(JOIN_DATE,Deserialise.Type.STRING,obj).getAsString());
			lastJoin =  VirtualPlayerData.SaveFormat.parse(Deserialise.assertAndGetProperty(LAST_JOIN,Deserialise.Type.STRING,obj).getAsString());
		}
		catch(ParseException e)
		{
			e.printStackTrace();
			throw new DeserialiseException("VirtualPlayerData failed to parse JoinDate and/or LastJoinDate");
		}
		
		getInventory().deserialise(Deserialise.assertAndGetProperty(PLAYER_INVENTORY,Deserialise.Type.JSON_OBJECT,obj).getAsJsonObject());
		getEnderchest().deserialise(Deserialise.assertAndGetProperty(PLAYER_ENDERCHEST,Deserialise.Type.JSON_OBJECT,obj).getAsJsonObject());
		getEntityData().deserialise(Deserialise.assertAndGetProperty(PLAYER_ENTITY,Deserialise.Type.JSON_OBJECT,obj).getAsJsonObject());
		getTimelimit().deserialise(Deserialise.assertAndGetProperty(TIMELIMIT,Deserialise.Type.JSON_OBJECT,obj).getAsJsonObject());
		
		if(obj.has(LINKED_ACCOUNT))
		{
			Deserialise.assertType(Deserialise.Type.STRING,obj.get(LINKED_ACCOUNT));
			linkedAccount = Deserialise.uuid(obj.get(LINKED_ACCOUNT));
		}
	}
}