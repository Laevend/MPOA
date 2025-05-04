package laeven.mpoa.virtualplayerdata.clocks;

import org.bukkit.entity.Player;

import laeven.mpoa.utils.Logg;
import laeven.mpoa.utils.PrintUtils;
import laeven.mpoa.utils.clocks.FixedRateAsyncClock;
import laeven.mpoa.virtualplayerdata.VirtualPlayerDataCtrl;
import laeven.mpoa.virtualplayerdata.data.VirtualPlayerData;

/**
 * 
 * @author Laeven
 * Alerts the console and player that the players data is in a degraded state
 */
public class PlayerDataDegradedClock extends FixedRateAsyncClock
{
	private VirtualPlayerData parent;
	
	public PlayerDataDegradedClock(VirtualPlayerData data)
	{
		super("Player Data Degraded Clock",(1000 * 60));
		this.parent = data;
	}

	@Override
	public void execute() throws Exception
	{
		if(!VirtualPlayerDataCtrl.isOnline(parent.getOwner())) { return; }
		Player p = parent.getActingPlayer();
		
		if(!parent.isSilenceDegradedAlarm())
		{
			PrintUtils.sendAlert(p,"Alert! Your PlayerData was found to be corrupted and could not be read correctly!"
					+ " Your data will not be saved!"
					+ " Please contact an administrator immediately!");
		}
		
		Logg.fatal("Player " + parent.getOwner() + " has degraded player data!");
		
		// Auto stop clock if no longer in degraded state
		if(!parent.isInDegradedState()) { this.stop(); }
	}
}