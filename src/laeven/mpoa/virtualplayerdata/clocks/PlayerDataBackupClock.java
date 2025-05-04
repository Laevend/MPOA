package laeven.mpoa.virtualplayerdata.clocks;

import java.util.Random;

import org.bukkit.entity.Player;

import laeven.mpoa.utils.Logg;
import laeven.mpoa.utils.PrintUtils;
import laeven.mpoa.utils.clocks.FixedRateAsyncClock;
import laeven.mpoa.virtualplayerdata.VirtualPlayerDataCtrl;
import laeven.mpoa.virtualplayerdata.data.VirtualPlayerData;

/**
 * 
 * @author Laeven
 *
 */
public class PlayerDataBackupClock extends FixedRateAsyncClock
{
	private VirtualPlayerData parent;
	
	public PlayerDataBackupClock(VirtualPlayerData data)
	{
		// 10 minutes + 1 to 60 seconds for variance.
		super("Player Data Backup Clock",600_000L + new Random().nextLong(1000,60_000));
		this.parent = data;
	}

	@Override
	public void execute() throws Exception
	{
		if(parent.isInDegradedState())
		{
			Logg.fatal("Player " + parent.getOwner().toString() + " has degraded player data!");
			
			if(!VirtualPlayerDataCtrl.isOnline(parent.getOwner())) { return; }
			Player p = parent.getActingPlayer();
			
			PrintUtils.sendAlert(p,"Alert! Your PlayerData was found to be corrupted and could not be read correctly!"
					+ " Your data will not be saved!"
					+ " Please contact an administrator immediately! DO NOT IGNORE!");
			return;
		}
		
		Logg.info("Backing up PData from clock for -> " + parent.getUsername());
		VirtualPlayerDataCtrl.backup(parent);
		VirtualPlayerDataCtrl.checkAndDeleteOldBackups(parent);
	}
}