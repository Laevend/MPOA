package laeven.mpoa.gui;

import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import laeven.mpoa.MPOA;
import laeven.mpoa.utils.ItemUtils;
import laeven.mpoa.utils.Logg;
import laeven.mpoa.utils.PrintUtils;
import laeven.mpoa.utils.SoundUtils;
import laeven.mpoa.utils.data.DataUtils;

public abstract class GUIHandler implements Listener
{
	private static Set<String> guis = Set.of(LoginInterface.GUI_NAME,PatternSetInterface.GUI_NAME,ResetPatternPasswordInterface.GUI_NAME);
	
	public static final int[] materialSlots = new int[] {21,22,23,30,31,32,39,40,41};
	public static final int[] materialBorderSlots = new int[] {11,12,13,14,15,20,24,29,33,38,42,47,48,49,50,51};
	public static final int infoSlot = 4;
	
	@EventHandler(ignoreCancelled = false)
	public void onPlayerQuit(PlayerQuitEvent e)
	{
		removeSession(e.getPlayer().getUniqueId());
	}
	
	public static final String DT_GUI_ITEM = "gui_item";
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onClick(InventoryClickEvent e)
	{
		// The raw slot displays as -999 when clicking outside the inventory, don't ask me why
		// Raw slot displays -1 when clicking on the edge of an inventory...
		if(!guis.contains(e.getView().getTitle()) || e.getRawSlot() == -999 || e.getRawSlot() == -1 || e.getInventory().getHolder() != null) 
		{ 
			if(ItemUtils.isNullOrAir(e.getCursor())) { return; }
			if(DataUtils.has(DT_GUI_ITEM,e.getCursor()))
			{
				e.getWhoClicked().setItemOnCursor(null);
			}
			return;
		}
		
		if(!e.getView().getTitle().equals(getGUIName())) { return; }
		
		// W H A T   I S   T H I S???
		// Why is this here? Prevents weird laggy behaviour when clicking page buttons fast
		if(e.getAction().equals(InventoryAction.NOTHING)) { return; }
		
		// Prevents buggy GUI from having its items taken out of the handler fails
		try
		{
			e.setCancelled(true);
			handleClick(e);
		}
		catch(Exception e1)
		{
			e.setCancelled(true);
			e.getWhoClicked().closeInventory();
			Logg.error("A problem occured when player " + e.getWhoClicked().getName() +" interacted (Click) with GUI '" + getGUIName() + "' at slot " + e.getRawSlot(),e1);
			PrintUtils.error(e.getWhoClicked(),"An error occured while interacting with GUI " + getGUIName() + "! Please alert a member of staff!");
		}
	}
	
	public abstract void handleClick(InventoryClickEvent e);
	
	@EventHandler
	public void onDrag(InventoryDragEvent e)
	{
		if(!guis.contains(e.getView().getTitle()) || e.getInventory().getHolder() != null) { return; }
		if(!e.getView().getTitle().equals(getGUIName())) { return; }
		e.setCancelled(true);
	}
	
	@EventHandler
	public void onClose(InventoryCloseEvent e)
	{
		if(!guis.contains(e.getView().getTitle()) || e.getInventory().getHolder() != null) { return; }
		if(!e.getView().getTitle().equals(getGUIName())) { return; }
		
		Player p = (Player) e.getPlayer();
		removeSession(p.getUniqueId());
		SoundUtils.playSound(p,Sound.BLOCK_VAULT_CLOSE_SHUTTER,0.1f);
	}
	
	public abstract void removeSession(UUID uuid);
	
	public abstract String getGUIName();
	
	public static void registerGUI(String guiName)
	{
		guis.add(guiName);
	}
	
	/**
	 * Signs a GUI with random GUI uuid's
	 * @param view InventoryView
	 */
	public static void signGUI(InventoryView view)
	{
		Bukkit.getScheduler().runTaskAsynchronously(MPOA.instance(),() ->
		{
			ItemStack[] contents = view.getTopInventory().getContents();
			
			for(ItemStack s : contents)
			{
				if(s == null ) { continue; }
				signItem(s);
			}
		});
	}
	
	/**
	 * Signs a stack with a unique Id to prevent it being stolen from GUI's
	 * @param stack ItemStack
	 * @return Signed ItemStack
	 */
	public static synchronized ItemStack signItem(ItemStack stack)
	{
		if(DataUtils.has(DT_GUI_ITEM,stack)) { return stack; }
		DataUtils.set(DT_GUI_ITEM,UUID.randomUUID().toString(),stack);
		return stack;
	}
}
