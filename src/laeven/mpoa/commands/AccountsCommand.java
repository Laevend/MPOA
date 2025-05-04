package laeven.mpoa.commands;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import laeven.mpoa.MPOA;
import laeven.mpoa.utils.Logg;
import laeven.mpoa.utils.PlayerUtils;
import laeven.mpoa.utils.PrintUtils;
import laeven.mpoa.utils.TimeUtils;
import laeven.mpoa.utils.structs.TabTree;
import laeven.mpoa.utils.structs.TabTree.Node;
import laeven.mpoa.utils.structs.UID4;
import laeven.mpoa.utils.tools.Deserialise;
import laeven.mpoa.virtualplayerdata.VirtualPlayerDataCtrl;
import laeven.mpoa.virtualplayerdata.data.VirtualPlayerData;

/**
 * Class representing implementation of Accounts command
 * If we only had a command parser...
 */
public class AccountsCommand extends MPOACommand
{
	private static final String ADD = "add";
	private static final String REMOVE = "remove";
	
	private static final String MANUAL_SAVE = "manual-save";
	private static final String MANUAL_LOAD = "manual-load";
	
	private static final String RESTORE = "restore";
	private static final String ALL = "all";
	private static final String INVENTORY_ONLY = "inventory-only";
	private static final String ENDERCHEST_ONLY = "enderchest-only";
	private static final String ENTITY_DATA_ONLY = "entity-data-only";
	
	private static final String FORCE_LOGIN = "force-login";
	private static final String FORCE_LOGOUT = "force-logout";
	
	private static final String LOCK = "lock";
	private static final String UNLOCK = "unlock";
	
	private static final String RESET_PASSWORD = "reset-password";
	
	private static final String LINK = "link";
	private static final String UNLINK = "unlink";
	
	private static final String TIME_LIMITS = "time_limits";
	private static final String ENABLE = "enable";
	private static final String DISABLE = "disable";
	private static final String SET_COOLDOWN_DURATION = "set-cooldown-duration";
	private static final String SET_PLAYTIME_DURATION = "set-playtime-duration";
	private static final String RESET_COOLDOWN = "reset-cooldown";
	private static final String RESET_PLAYTIME = "reset-playtime";
	
	private static final String NEW_ACCOUNT_NAME = "<new-account-name>";
	private static final String ACCOUNT_NAME = "<account-name>";
	private static final String BACKUP_FILE = "<backup-file>";
	private static final String ACTING_PLAYER_NAME = "<acting-player-name>";
	private static final String MINECRAFT_ACCOUNT_UUID = "<minecraft-account-uuid>";
	private static final String DURATION = "<duration>";
	
	private static final String NEW_PASSWORD = "<new-password>";
	
	private static Pattern newAccountName = Pattern.compile("^([a-zA-z0-9_-]|[ ])*$");

	// accounts add <account-name>
	
	// accounts remove <account-name>
	
	// accounts manual-save <account-name>
	
	// accounts manual-load <account-name>
	
	// accounts restore <account-name> all <backup-file> 
	
	// accounts restore <account-name> inventory-only <backup-file> 
	
	// accounts restore <account-name> enderchest-only <backup-file> 
	
	// accounts restore <account-name> entitydata-only <backup-file> 
	
	// accounts force-login <account-name> <acting-player-name>
	
	// accounts force-logout <account-name>
	
	// accounts lock <account-name>
	
	// accounts unlock <account-name>
	
	// accounts set-new-password <account-name>
	
	// accounts link <account-name> <minecraft-account-uuid>
	
	// accounts unlink <account-name>
	
	// accounts time-limits <account-name> enable
	
	// accounts time-limits <account-name> disable
	
	// accounts time-limits <account-name> set-cooldown-duration (^\d*[h|m|s]{1}$)
	
	// accounts time-limits <account-name> set-playtime-duration (^\d*[h|m|s]{1}$)
	
	// accounts time-limits <account-name> reset-cooldown
	
	// accounts time-limits <account-name> reset-playtime
	
	public void onCommand(CommandSender sender,String[] args)
	{
		if(args.length < 2)
		{
			PrintUtils.error(sender,"Not enough arguments!");
			return;
		}
		
		assertArgument(args[0],ADD,REMOVE,MANUAL_SAVE,MANUAL_LOAD,RESTORE,FORCE_LOGIN,FORCE_LOGOUT,LOCK,UNLOCK,RESET_PASSWORD,LINK,UNLINK,TIME_LIMITS);
		
		switch(args[0])
		{
			case ADD -> add(sender,args[1]);
			case REMOVE -> remove(sender,args[1]);
			case MANUAL_SAVE -> manualSave(sender,args[1]);
			case MANUAL_LOAD -> manualLoad(sender,args[1]);
			case RESTORE ->
			{
				if(args.length < 4)
				{
					PrintUtils.error(sender,"Not enough arguments!");
					return;
				}
				
				restore(sender,args[1],args[2],args[3]);
			}
			case FORCE_LOGIN ->
			{
				if(args.length < 3)
				{
					PrintUtils.error(sender,"Not enough arguments!");
					return;
				}
				
				forceLogin(sender,args[1],args[2]);
			}
			case FORCE_LOGOUT -> forceLogout(sender,args[1]);
			case LOCK -> lock(sender,args[1]);
			case UNLOCK -> unlock(sender,args[1]);
			case RESET_PASSWORD ->
			{
				if(args.length < 2)
				{
					PrintUtils.error(sender,"Not enough arguments!");
					return;
				}
				
				resetPassword(sender,args[1]);
			}
			case LINK ->
			{
				if(args.length < 3)
				{
					PrintUtils.error(sender,"Not enough arguments!");
					return;
				}
				
				link(sender,args[1],args[2]);
			}
			case UNLINK -> unlink(sender,args[1]);
			case TIME_LIMITS ->
			{
				if(args.length < 3)
				{
					PrintUtils.error(sender,"Not enough arguments!");
					return;
				}
				
				timeLimits(sender,args[1],args[2],args);
			}
		}
	}
	
	private void add(CommandSender sender,String newUsername)
	{
		if(!newAccountName.matcher(newUsername).matches())
		{
			PrintUtils.error(sender,"Username can only contain number, letters, regular spaces, '_' and '-' symbols!");
			return;
		}
		
		UID4 uid = UID4.randomUID(newUsername);
		
		if(VirtualPlayerDataCtrl.getVirtualAccountNames().contains(uid.toString()))
		{
			PrintUtils.error(sender,"Error occured creating new virtual account for" + newUsername + " name already exists! Collision occured!");
			return;
		}
		
		VirtualPlayerDataCtrl.loadPlayerData(uid);
		VirtualPlayerDataCtrl.getPlayerData(uid).setUsername(newUsername);
		
		if(!VirtualPlayerDataCtrl.savePlayerData(uid,false))
		{
			PrintUtils.error(sender,"Error occured creating new virtual account for" + newUsername);
			return;
		}
		
		PrintUtils.info(sender,"Created virtual account for " + uid.toString());
	}
	
	private void remove(CommandSender sender,String accountName)
	{
		UID4 account = getAccountName(sender,accountName);
		if(account == null) { return; }
		
		VirtualPlayerDataCtrl.deletePlayerData(account);
		PrintUtils.info(sender,"Removed account for " + account.toString());
	}
	
	private void manualSave(CommandSender sender,String accountName)
	{
		UID4 account = getAccountName(sender,accountName);
		if(account == null) { return; }
		
		if(!VirtualPlayerDataCtrl.savePlayerData(account,false))
		{
			PrintUtils.error(sender,"Error occured performing manual save for virtual account " + account.toString());
			return;
		}
		
		PrintUtils.info(sender,"Manual save account for " + account.toString() + " succeeded!");
	}
	
	private void manualLoad(CommandSender sender,String accountName)
	{
		UID4 account = getAccountName(sender,accountName);
		if(account == null) { return; }
		
		VirtualPlayerDataCtrl.loadPlayerData(account);
		
		PrintUtils.info(sender,"Attempted a manual load of virtual player data for " + account.toString());
	}
	
	private void restore(CommandSender sender,String accountName,String restoreType,String backupFilename)
	{
		UID4 account = getAccountName(sender,accountName);
		if(account == null) { return; }
		
		Path backupPath = MPOA.internalFilePath("playerdata" + File.separator + account.toString() + File.separator + "backups" + File.separator + backupFilename);
		
		if(!Files.exists(backupPath))
		{
			PrintUtils.error(sender,"Backup file " + backupFilename + " does not exist in " + backupPath.toAbsolutePath().toString() + "!");
			return;
		}
		
		assertArgument(restoreType,ALL,INVENTORY_ONLY,ENDERCHEST_ONLY,ENTITY_DATA_ONLY);
		
		switch(restoreType)
		{
			case ALL ->
			{
				if(!VirtualPlayerDataCtrl.restore(VirtualPlayerDataCtrl.getPlayerData(account),backupPath))
				{
					PrintUtils.error(sender,"An error occured attempting to restore from this backup file!");
					return;
				}
			}
			case INVENTORY_ONLY ->
			{
				if(!VirtualPlayerDataCtrl.restoreInventoryOnly(VirtualPlayerDataCtrl.getPlayerData(account),backupPath))
				{
					PrintUtils.error(sender,"An error occured attempting to restore inventory from this backup file!");
					return;
				}
			}
			case ENDERCHEST_ONLY ->
			{
				if(!VirtualPlayerDataCtrl.restoreEnderchestOnly(VirtualPlayerDataCtrl.getPlayerData(account),backupPath))
				{
					PrintUtils.error(sender,"An error occured attempting to restore enderchest from this backup file!");
					return;
				}
			}
			case ENTITY_DATA_ONLY ->
			{
				if(!VirtualPlayerDataCtrl.restoreEntityDataOnly(VirtualPlayerDataCtrl.getPlayerData(account),backupPath))
				{
					PrintUtils.error(sender,"An error occured attempting to restore entity data from this backup file!");
					return;
				}
			}
		}
		
		PrintUtils.info(sender,"Backup restore successful for " + account.toString());
	}
	
	private void forceLogin(CommandSender sender,String accountName,String actingAccountName)
	{
		UID4 account = getAccountName(sender,accountName);
		if(account == null) { return; }
		VirtualPlayerData pData = VirtualPlayerDataCtrl.getPlayerData(account);
		
		if(pData.isLocked())
		{
			PrintUtils.error(sender,"Cannot login, this account is locked!");
			return;
		}
		
		// Check for cooldown
		if(pData.getTimelimit().isEnabled() && !pData.getTimelimit().hasCooldownExpired())
		{
			long timeRemaining = pData.getTimelimit().getMaxCooldownTime() - (System.currentTimeMillis() - pData.getTimelimit().getCooldownStartTime());
			PrintUtils.error(sender,"Cannot login, Cooldown has not expired! Remaining time: " + TimeUtils.millisecondsToHoursMinutesSeconds(timeRemaining));
			return;
		}
		
		// Reset play time if it is 0 and cooldown has expired
		if(pData.getTimelimit().isEnabled() && pData.getTimelimit().getPlayTimeLeft() == 0)
		{
			pData.getTimelimit().resetPlayTime();
		}
		
		if(pData.getActingPlayer() != null)
		{
			PrintUtils.error(sender,"This virtual account is already logged in using account " + pData.getActingPlayer().getName());
			return;
		}
		
		Player actingPlayer = null;
		
		for(Player p : Bukkit.getOnlinePlayers())
		{
			if(p.getName().equals(actingAccountName))
			{
				actingPlayer = p;
				break;
			}
		}
		
		if(actingPlayer == null)
		{
			PrintUtils.error(sender,"This player does not exist or is not online!");
			return;
		}
		
		if(!VirtualPlayerDataCtrl.getActingAccounts().contains(actingPlayer.getUniqueId()))
		{
			PrintUtils.error(sender,"This player is not designated as an acting player! It cannot be used to login to a virtual account!");
			return;
		}
		
		if(VirtualPlayerDataCtrl.isPuppetingVirtualAccount(actingPlayer))
		{
			PrintUtils.error(sender,"This account is already puppeting a virtual account! You cannot login to another account!");
			return;
		}
		
		if(!VirtualPlayerDataCtrl.login(account,actingPlayer))
		{
			PrintUtils.error(sender,"An error occured attempting to login to account " + accountName);
			return;
		}
		
		PrintUtils.info(sender,"Minecraft account " + actingPlayer.getName() + " has been forcefully logged into virtual account " + account.toString());
	}
	
	private void forceLogout(CommandSender sender,String accountName)
	{
		UID4 account = getAccountName(sender,accountName);
		if(account == null) { return; }
		VirtualPlayerData pData = VirtualPlayerDataCtrl.getPlayerData(account);
		
		if(pData.getActingPlayer() == null)
		{
			PrintUtils.error(sender,"This virtual account is not logged in!");
			return;
		}
		
		Player actingPlayer = pData.getActingPlayer();
		
		if(!VirtualPlayerDataCtrl.logout(actingPlayer))
		{
			PrintUtils.error(sender,"An error occured attempting to logout account " + accountName);
			return;
		}
		
		PrintUtils.info(sender,"Minecraft account " + actingPlayer.getName() + " has been forcefully logged out of virtual account " + account.toString());
	}
	
	private void lock(CommandSender sender,String accountName)
	{
		UID4 account = getAccountName(sender,accountName);
		if(account == null) { return; }
		
		VirtualPlayerDataCtrl.getPlayerData(account).setLocked(true);
		VirtualPlayerDataCtrl.savePlayerData(account,false);
		
		if(VirtualPlayerDataCtrl.getLoggedInVirtualAccounts().containsValue(account))
		{
			VirtualPlayerDataCtrl.logout(VirtualPlayerDataCtrl.getPlayerData(account).getActingPlayer());
		}
		
		PrintUtils.info(sender,"Virtual account " + account.toString() + " has been locked!");
	}
	
	private void unlock(CommandSender sender,String accountName)
	{
		UID4 account = getAccountName(sender,accountName);
		if(account == null) { return; }
		
		VirtualPlayerDataCtrl.getPlayerData(account).setLocked(false);
		VirtualPlayerDataCtrl.savePlayerData(account,false);
		
		PrintUtils.info(sender,"Virtual account " + account.toString() + " has been unlocked!");
	}
	
	private void resetPassword(CommandSender sender,String accountName)
	{
		UID4 account = getAccountName(sender,accountName);
		if(account == null) { return; }
		
		VirtualPlayerDataCtrl.getPlayerData(account).setNewPassword(VirtualPlayerDataCtrl.getResetPassword());
		VirtualPlayerDataCtrl.savePlayerData(account,false);
		
		PrintUtils.info(sender,"Virtual account " + account.toString() + " has had their password reset to the default 'reset_password_pattern'");
	}
	
	private void link(CommandSender sender,String accountName,String accountToLinkTo)
	{
		UID4 account = getAccountName(sender,accountName);
		if(account == null) { return; }
		
		if(VirtualPlayerDataCtrl.getPlayerData(account).getLinkedAccount() != null)
		{
			PrintUtils.error(sender,"Virtual account " + account.toString() + " is already linked to another Minecraft account! (" + PlayerUtils.getName(VirtualPlayerDataCtrl.getPlayerData(account).getLinkedAccount()) + ")");
			return;
		}
		
		if(!Deserialise.uuidPattern.matcher(accountToLinkTo).matches())
		{
			PrintUtils.error(sender,"'" + accountToLinkTo + "' is not a valid UUID!");
			return;
		}
		
		UUID linkedAccount = UUID.fromString(accountToLinkTo);
		
		if(VirtualPlayerDataCtrl.getActingAccounts().contains(linkedAccount))
		{
			PrintUtils.error(sender,"This player is designated as an acting player! It cannot be used to link to a virtual account!");
			return;
		}
		
		if(PlayerUtils.isOnline(linkedAccount))
		{
			PlayerUtils.getPlayer(linkedAccount).kickPlayer("Account has been linked to " + account.toString() + "\nYou may login now");
		}
		
		VirtualPlayerDataCtrl.getLinkedAccountPlayers().put(linkedAccount,account);
		VirtualPlayerDataCtrl.saveLinkedAccountPlayers();
		
		VirtualPlayerDataCtrl.getPlayerData(account).setLinkedAccount(linkedAccount);
		VirtualPlayerDataCtrl.savePlayerData(account,false);
		
		PrintUtils.info(sender,"Linked Minecraft account " + PlayerUtils.getName(linkedAccount) + " (" + linkedAccount.toString() + ") to virtual account " + account.toString());
	}
	
	private void unlink(CommandSender sender,String accountName)
	{
		UID4 account = getAccountName(sender,accountName);
		if(account == null) { return; }
		
		if(VirtualPlayerDataCtrl.getPlayerData(account).getLinkedAccount() == null)
		{
			PrintUtils.error(sender,"Virtual account " + account.toString() + " is not linked to another Minecraft account!");
			return;
		}
		
		UUID oldLinkedAccount = VirtualPlayerDataCtrl.getPlayerData(account).getLinkedAccount();
		
		if(PlayerUtils.isOnline(oldLinkedAccount))
		{
			VirtualPlayerDataCtrl.logout(PlayerUtils.getPlayer(oldLinkedAccount));
		}
		else
		{
			VirtualPlayerDataCtrl.getOldLinkedAccountPlayers().add(oldLinkedAccount);
		}
		
		VirtualPlayerDataCtrl.getPlayerData(account).setLinkedAccount(null);
		VirtualPlayerDataCtrl.savePlayerData(account,false);
		VirtualPlayerDataCtrl.getLinkedAccountPlayers().remove(oldLinkedAccount);
		VirtualPlayerDataCtrl.saveLinkedAccountPlayers();
		
		PrintUtils.info(sender,"Unlinked Minecraft account " + PlayerUtils.getName(oldLinkedAccount) + " (" + oldLinkedAccount.toString() + ") from virtual account " + account.toString());
	}
	
	private Pattern durationPattern = Pattern.compile("^\\d*[h|m|s]{1}$");
	
	private void timeLimits(CommandSender sender,String accountName,String arg3,String[] args)
	{
		UID4 account = getAccountName(sender,accountName);
		if(account == null) { return; }
		
		assertArgument(arg3,ENABLE,DISABLE,SET_COOLDOWN_DURATION,SET_PLAYTIME_DURATION,RESET_COOLDOWN,RESET_PLAYTIME);
		
		switch(arg3)
		{
			case ENABLE -> 
			{
				VirtualPlayerDataCtrl.getPlayerData(account).getTimelimit().setEnabled(true);
				VirtualPlayerDataCtrl.getPlayerData(account).getTimelimit().startTimer();
				VirtualPlayerDataCtrl.savePlayerData(account,false);
				PrintUtils.info(sender,"Enabled time limit system for account " + account.toString());
				return;
			}
			case DISABLE -> 
			{
				VirtualPlayerDataCtrl.getPlayerData(account).getTimelimit().setEnabled(false);
				VirtualPlayerDataCtrl.getPlayerData(account).getTimelimit().stopTimer();
				VirtualPlayerDataCtrl.savePlayerData(account,false);
				PrintUtils.info(sender,"Disabled time limit system for account " + account.toString());
				return;
			}
			case SET_COOLDOWN_DURATION -> 
			{
				if(args.length < 4)
				{
					PrintUtils.error(sender,"Not enough arguments!");
					return;
				}
				
				String[] durations = new String[args.length - 3];
				
				for(int i = 0; (i+3) < args.length; i++)
				{
					durations[i] = args[i+3];
				}
				
				long duration = calculateDuration(sender,durations);
				if(duration == -1) { return; }
				
				VirtualPlayerDataCtrl.getPlayerData(account).getTimelimit().setMaxCooldownTime(duration);
				VirtualPlayerDataCtrl.getPlayerData(account).getTimelimit().setCooldownStartTime(0);
				VirtualPlayerDataCtrl.savePlayerData(account,false);
				PrintUtils.info(sender,"Cooldown time limit set to " + TimeUtils.millisecondsToHoursMinutesSeconds(duration) + " for account " + account.toString());
				return;
			}
			case SET_PLAYTIME_DURATION ->
			{
				if(args.length < 4)
				{
					PrintUtils.error(sender,"Not enough arguments!");
					return;
				}
				
				String[] durations = new String[args.length - 3];
				
				for(int i = 0; (i+3) < args.length; i++)
				{
					durations[i] = args[i+3];
				}
				
				long duration = calculateDuration(sender,durations);
				if(duration == -1) { return; }
				
				VirtualPlayerDataCtrl.getPlayerData(account).getTimelimit().setMaxPlayTime(duration);
				
				if(VirtualPlayerDataCtrl.isOnline(account))
				{
					VirtualPlayerDataCtrl.getPlayerData(account).getTimelimit().restartTimer();
				}
				
				VirtualPlayerDataCtrl.savePlayerData(account,false);
				PrintUtils.info(sender,"Playtime limit set to " + TimeUtils.millisecondsToHoursMinutesSeconds(duration) + " for account " + account.toString());
				return;
			}
			case RESET_COOLDOWN ->
			{
				VirtualPlayerDataCtrl.getPlayerData(account).getTimelimit().resetCooldown();
				VirtualPlayerDataCtrl.savePlayerData(account,false);
				PrintUtils.info(sender,"Cooldown timer reset for " + account.toString());
				return;
			}
			case RESET_PLAYTIME ->
			{
				VirtualPlayerDataCtrl.getPlayerData(account).getTimelimit().resetPlayTime();
				VirtualPlayerDataCtrl.savePlayerData(account,false);
				PrintUtils.info(sender,"Playtime timer reset for " + account.toString());
				return;
			}
		}
	}
	
	private long calculateDuration(CommandSender sender,String[] durations)
	{
		long finalDuration = 0;
		
		for(int i = 0; i < durations.length; i++)
		{
			if(!durationPattern.matcher(durations[i]).matches())
			{
				PrintUtils.error(sender,"Bad argument '" + durations[i] + "'. Enter a number with a 'h', 'm', or 's' at the end to denote hours, minutes and seconds.");
				return -1;
			}
			
			char durationDenotion = Character.toLowerCase(durations[i].charAt(durations[i].length() - 1));
			String digits = durations[i].substring(0,durations[i].length() - 1);
			
			// Prevent from entering a massive number
			if(digits.length() > 9)
			{
				PrintUtils.error(sender,"You cannot exceed 999,999,999! We also don't think you need this much of a cooldown... that's cruel.");
				return -1;
			}
			
			long numberOfUnit = Long.parseLong(digits);
			
			switch(durationDenotion)
			{
				case 'h' -> finalDuration += TimeUnit.HOURS.toMillis(numberOfUnit);
				case 'm' -> finalDuration += TimeUnit.MINUTES.toMillis(numberOfUnit);
				case 's' -> finalDuration += TimeUnit.SECONDS.toMillis(numberOfUnit);
				default -> Logg.warn("Unrecognised character " + durationDenotion);
			}
		}
		
		return finalDuration;
	}
	
	private UID4 getAccountName(CommandSender sender,String arg)
	{
		Set<String> accounts = VirtualPlayerDataCtrl.getVirtualAccountNames();
		
		if(!accounts.contains(arg))
		{
			PrintUtils.error(sender,arg + " is already loaded or is not a real virtual account!");
			return null;
		}
		
		UID4 uid = UID4.fromString(arg);
		VirtualPlayerDataCtrl.loadPlayerData(uid);
		return uid;
	}
	
	private static TabTree tree = new TabTree();
	
	public AccountsCommand()
	{
		tree.getRoot().addBranch(ADD).addBranch(NEW_ACCOUNT_NAME);
		tree.getRoot().addBranch(REMOVE).addBranch(ACCOUNT_NAME);
		tree.getRoot().addBranch(MANUAL_SAVE).addBranch(ACCOUNT_NAME);
		tree.getRoot().addBranch(MANUAL_LOAD).addBranch(ACCOUNT_NAME);
		
		Node restore = tree.getRoot().addBranch(RESTORE).addBranch(ACCOUNT_NAME);
		restore.addBranch(ALL).addBranch(BACKUP_FILE);
		restore.addBranch(INVENTORY_ONLY).addBranch(BACKUP_FILE);
		restore.addBranch(ENDERCHEST_ONLY).addBranch(BACKUP_FILE);
		restore.addBranch(ENTITY_DATA_ONLY).addBranch(BACKUP_FILE);
		
		tree.getRoot().addBranch(FORCE_LOGIN).addBranch(ACCOUNT_NAME).addBranch(ACTING_PLAYER_NAME);
		tree.getRoot().addBranch(FORCE_LOGOUT).addBranch(ACCOUNT_NAME);
		tree.getRoot().addBranch(LOCK).addBranch(ACCOUNT_NAME);
		tree.getRoot().addBranch(UNLOCK).addBranch(ACCOUNT_NAME);
		tree.getRoot().addBranch(RESET_PASSWORD).addBranch(ACCOUNT_NAME);
		tree.getRoot().addBranch(LINK).addBranch(ACCOUNT_NAME).addBranch(MINECRAFT_ACCOUNT_UUID);
		tree.getRoot().addBranch(UNLINK).addBranch(ACCOUNT_NAME);
		
		Node timelimits = tree.getRoot().addBranch(TIME_LIMITS).addBranch(ACCOUNT_NAME);
		timelimits.addBranch(ENABLE);
		timelimits.addBranch(DISABLE);
		timelimits.addBranch(SET_COOLDOWN_DURATION).addBranch(DURATION);
		timelimits.addBranch(SET_PLAYTIME_DURATION).addBranch(DURATION);
		timelimits.addBranch(RESET_COOLDOWN);
		timelimits.addBranch(RESET_PLAYTIME);
	}
	
	@Override
	public List<String> onTab(CommandSender sender,String[] args)
	{
		Node nextNode = tree.getRoot();
		
		if(args == null) { return new ArrayList<>(nextNode.branches.keySet()); }
		
		MainLoop:
		for(int i = 0; i < args.length; i++)
		{
			if(nextNode.branches.keySet().contains(args[i]))
			{
				nextNode = nextNode.branches.get(args[i]);
				continue;
			}
			
			for(String branch : nextNode.branches.keySet())
			{
				switch(branch)
				{
					case NEW_ACCOUNT_NAME -> 
					{
						if(newAccountName.matcher(args[i]).matches() && !args[i].isBlank() && !args[i].isEmpty())
						{
							nextNode = nextNode.branches.get(branch);
							continue MainLoop;
						}
					}
					case ACCOUNT_NAME -> 
					{
						if(UID4.UID4Pattern.matcher(args[i]).matches())
						{
							nextNode = nextNode.branches.get(branch);
							continue MainLoop;
						}
					}
					case BACKUP_FILE -> 
					{
						if(args[i].endsWith(".json"))
						{
							nextNode = nextNode.branches.get(branch);
							continue MainLoop;
						}
					}
					case ACTING_PLAYER_NAME -> 
					{
						if(PlayerUtils.isOnline(args[i]))
						{
							nextNode = nextNode.branches.get(branch);
							continue MainLoop;
						}
					}
					case MINECRAFT_ACCOUNT_UUID -> 
					{
						if(Deserialise.uuidPattern.matcher(args[i]).matches())
						{
							nextNode = nextNode.branches.get(branch);
							continue MainLoop;
						}
					}
					case DURATION ->
					{
						if(durationPattern.matcher(args[i]).matches())
						{
							nextNode = nextNode.branches.get(branch);
							continue MainLoop;
						}
					}
					case NEW_PASSWORD ->
					{
						if(!args[i].isBlank() && !args[i].isEmpty())
						{
							nextNode = nextNode.branches.get(branch);
							continue MainLoop;
						}
					}
				}
			}
			
			break MainLoop;
		}
		
		// Return suggestions
		List<String> suggestions = new ArrayList<>();
		
		for(String branch : nextNode.branches.keySet())
		{
			switch(branch)
			{
				case ACCOUNT_NAME -> 
				{
					suggestions.addAll(VirtualPlayerDataCtrl.getVirtualAccountNames());
				}
				case BACKUP_FILE -> 
				{
					String accountName = args[args.length - 3];
					
					if(!UID4.UID4Pattern.matcher(accountName).matches()) { return Collections.emptyList(); }
					
					UID4 accountNameUID = UID4.fromString(accountName);
					suggestions.addAll(VirtualPlayerDataCtrl.getPlayerDataBackupFiles(accountNameUID));
				}
				case ACTING_PLAYER_NAME -> 
				{
					for(Player p : Bukkit.getOnlinePlayers())
					{
						suggestions.add(p.getName());
					}
				}
				case MINECRAFT_ACCOUNT_UUID -> 
				{
					suggestions.add(branch);
				}
				case DURATION ->
				{
					suggestions.add("<00h> <00m> <00s>");
				}
				case NEW_PASSWORD ->
				{
					suggestions.add(branch);
				}
				default -> suggestions.add(branch);
			}
		}
		
		return suggestions;
	}
}
