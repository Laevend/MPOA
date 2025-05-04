package laeven.mpoa.extras;

import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import laeven.mpoa.utils.data.DataUtils;
import laeven.mpoa.utils.structs.UID4;
import laeven.mpoa.virtualplayerdata.VirtualPlayerDataCtrl;

public class EmulatedPearls implements Listener
{
	private String UID4_ID = "uid4_id";
	
	@EventHandler
	public void onPearlSpawn(ProjectileLaunchEvent e)
	{
		if(e.getEntityType() != EntityType.ENDER_PEARL) { return; }
		if(!(e.getEntity().getShooter() instanceof Player p)) { return; }
		if(!VirtualPlayerDataCtrl.isPuppetingVirtualAccount(p)) { return; }
		
		EnderPearl pearl = (EnderPearl) e.getEntity();
		DataUtils.set(UID4_ID,VirtualPlayerDataCtrl.getPlayerData(p).getOwner().toString(),pearl);
	}
	
	@EventHandler
	public void onPearlHit(ProjectileHitEvent e)
	{
		if(e.getEntityType() != EntityType.ENDER_PEARL) { return; }
		if(!(e.getEntity().getShooter() instanceof Player p)) { return; }
		if(!VirtualPlayerDataCtrl.isPuppetingVirtualAccount(p)) { return; }
		
		EnderPearl pearl = (EnderPearl) e.getEntity();
		
		if(!DataUtils.has(UID4_ID,pearl)) { return; }
		UID4 uid4 = UID4.fromString(DataUtils.get(UID4_ID,pearl).asString());
		
		// Remove a pearl if it is attempting to teleport a player that is logged into a different virtual account than the virtual account who fired the pearl
		if(!VirtualPlayerDataCtrl.getPlayerData(p).getOwner().equals(uid4))
		{
			e.getEntity().remove();
		}
	}
}
