package laeven.mpoa.extras;

import java.util.UUID;

import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import laeven.mpoa.utils.DelayUtils;
import laeven.mpoa.utils.PrintUtils;
import laeven.mpoa.utils.data.DataUtils;
import laeven.mpoa.utils.structs.UID4;
import laeven.mpoa.virtualplayerdata.VirtualPlayerDataCtrl;

public class EmulatedPetOwnership implements Listener
{
	private String UID4_ID = "uid4_id";
	
	@EventHandler
	public void onTame(EntityTameEvent e)
	{
		if(!(e.getOwner() instanceof Player p)) { return; }
		if(!VirtualPlayerDataCtrl.isPuppetingVirtualAccount(p)) { return; }
		
		Tameable tameable = (Tameable) e.getEntity();
		DataUtils.set(UID4_ID,VirtualPlayerDataCtrl.getPlayerData(p).getOwner().toString(),tameable);
		
		DelayUtils.executeDelayedBukkitTask(() ->
		{
			// Override owner with blank owner after tame event finished
			tameable.setOwner(new BlankOwner());
		},1);
	}
	
	@EventHandler
	public void onInteractTameable(PlayerInteractEntityEvent e)
	{
		if(e.getHand() != EquipmentSlot.HAND) { return; }
		if(!(e.getRightClicked() instanceof Tameable)) { return; }
		if(!(e.getRightClicked() instanceof Sittable)) { return; }
		if(!VirtualPlayerDataCtrl.isPuppetingVirtualAccount(e.getPlayer())) { return; }
		
		Tameable tameable = (Tameable) e.getRightClicked();
		
		if(!DataUtils.has(UID4_ID,tameable)) { return; }
		UID4 uid4 = UID4.fromString(DataUtils.get(UID4_ID,tameable).asString());
		
		// Prevent interaction with a tamed entity from a player that is logged into a different virtual account than the virtual account who tamed the animal
		if(!VirtualPlayerDataCtrl.getPlayerData(e.getPlayer()).getOwner().equals(uid4))
		{
			tameable.setOwner(new BlankOwner());
			e.setCancelled(true);
			PrintUtils.error(e.getPlayer(),"You cannot interact with this! It is owned by " + uid4.toString());
			return;
		}
		else
		{
			// Allows the virtual account to interact with pets by setting its owner the the player being used to puppet the virtual account
			tameable.setOwner(e.getPlayer());
		}
	}
	
	@EventHandler
	public void onMountTameable(EntityMountEvent e)
	{
		if(!(e.getEntity()instanceof Player p)) { return; }
		if(!(e.getMount() instanceof Tameable)) { return; }
		if(!VirtualPlayerDataCtrl.isPuppetingVirtualAccount(p)) { return; }
		
		Tameable tameable = (Tameable) e.getMount();
		
		if(!DataUtils.has(UID4_ID,tameable)) { return; }
		UID4 uid4 = UID4.fromString(DataUtils.get(UID4_ID,tameable).asString());
		
		// Prevent interaction with a tamed entity from a player that is logged into a different virtual account than the virtual account who tamed the animal
		if(!VirtualPlayerDataCtrl.getPlayerData(p).getOwner().equals(uid4))
		{
			e.setCancelled(true);
			PrintUtils.error(p,"You cannot mount this! It is owned by " + uid4.toString());
		}
	}
	
	private static class BlankOwner implements AnimalTamer
	{
		private UUID blankUUID = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
		
		@Override
		public String getName()
		{
			return "None";
		}
	
		@Override
		public UUID getUniqueId()
		{
			return blankUUID;
		}		
	}
}
