package laeven.mpoa.virtualplayerdata.clocks;

import laeven.mpoa.utils.Logg;
import laeven.mpoa.utils.clocks.HybridAsyncClock;
import laeven.mpoa.virtualplayerdata.VirtualPlayerDataCtrl;
import laeven.mpoa.virtualplayerdata.data.VirtualPlayerData;

/**
 * 
 * @author Laeven
 *
 */
public class PlayerDataSaveClock extends HybridAsyncClock
{
	private VirtualPlayerData parent;
	
	public PlayerDataSaveClock(VirtualPlayerData data)
	{
		super("Player Data Save Clock");
		this.parent = data;
	}

	@Override
	public void execute() throws Exception
	{
		Logg.verb("Saving PData from clock for -> " + parent.getUsername(),Logg.VerbGroup.PLAYER_DATA);
		this.parent.getInventory().updateInventory();
		this.parent.getEnderchest().updateEnderchest();
		this.parent.getEntityData().updatePlayerEntity();
		VirtualPlayerDataCtrl.savePlayerData(parent.getOwner(),false);
	}
}