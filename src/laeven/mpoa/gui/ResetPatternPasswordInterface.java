package laeven.mpoa.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import laeven.mpoa.gui.RPPGUISession.ResetStage;
import laeven.mpoa.utils.ColourUtils;
import laeven.mpoa.utils.ItemUtils;
import laeven.mpoa.utils.Logg;
import laeven.mpoa.utils.PrintUtils;
import laeven.mpoa.utils.SoundUtils;
import laeven.mpoa.virtualplayerdata.data.VirtualPlayerData;

public class ResetPatternPasswordInterface extends GUIHandler implements Listener
{
	private static Map<UUID,RPPGUISession> sessions = new HashMap<>();
	public static final String GUI_NAME = "Reset Pattern";
	
	public static void open(Player player,VirtualPlayerData data)
	{
		try
		{
			if(!sessions.containsKey(player.getUniqueId()))
			{
				sessions.put(player.getUniqueId(),new RPPGUISession(player,data));
			}
			
			Inventory inv = Bukkit.createInventory(null,27,GUI_NAME);
			InventoryView view = player.openInventory(inv);
			SoundUtils.playSound(player,Sound.BLOCK_VAULT_OPEN_SHUTTER,1.0f);
			
			ItemStack oldPatternIcon = new ItemStack(Material.OMINOUS_TRIAL_KEY);
			ItemStack newPatternIcon = new ItemStack(Material.TRIAL_KEY);
			ItemStack confirmPatternIcon = new ItemStack(Material.TRIAL_KEY);
			
			RPPGUISession sess = sessions.get(player.getUniqueId());
			
			if(!sess.hasCompletedOldPattern() && !sess.hasCompletedNewPattern() && !sess.hasCompletedConfirmPattern())
			{
				ItemUtils.setName("&eEnter Old Pattern",oldPatternIcon);
				ItemUtils.setLore(List.of(ColourUtils.transCol("&eClick me to start")), oldPatternIcon);
				ItemUtils.setName("&7Enter New Pattern",newPatternIcon);
				ItemUtils.setLore(List.of(ColourUtils.transCol("&7...")), newPatternIcon);
				ItemUtils.setName("&7Confirm New Pattern",confirmPatternIcon);
				ItemUtils.setLore(List.of(ColourUtils.transCol("&7...")),confirmPatternIcon);	
			}
			else if(sess.hasCompletedOldPattern() && !sess.hasCompletedNewPattern() && !sess.hasCompletedConfirmPattern())
			{
				ItemUtils.setName("&aEnter Old Pattern",oldPatternIcon);
				ItemUtils.setLore(List.of(ColourUtils.transCol("&aCompleted")), oldPatternIcon);
				ItemUtils.setName("&eEnter New Pattern",newPatternIcon);
				ItemUtils.setLore(List.of(ColourUtils.transCol("&eClick me to start")), newPatternIcon);
				ItemUtils.setName("&7Confirm New Pattern",confirmPatternIcon);
				ItemUtils.setLore(List.of(ColourUtils.transCol("&7...")),confirmPatternIcon);
			}
			else if(sess.hasCompletedOldPattern() && sess.hasCompletedNewPattern() && !sess.hasCompletedConfirmPattern())
			{
				ItemUtils.setName("&aEnter Old Pattern",oldPatternIcon);
				ItemUtils.setLore(List.of(ColourUtils.transCol("&aCompleted")), oldPatternIcon);
				ItemUtils.setName("&aEnter New Pattern",newPatternIcon);
				ItemUtils.setLore(List.of(ColourUtils.transCol("&aCompleted")), newPatternIcon);
				ItemUtils.setName("&eConfirm New Pattern",confirmPatternIcon);
				ItemUtils.setLore(List.of(ColourUtils.transCol("&eClick me to start")),confirmPatternIcon);
			}
			else
			{
				ItemUtils.setName("&aEnter Old Pattern",oldPatternIcon);
				ItemUtils.setLore(List.of(ColourUtils.transCol("&aCompleted")), oldPatternIcon);
				ItemUtils.setName("&aEnter New Pattern",newPatternIcon);
				ItemUtils.setLore(List.of(ColourUtils.transCol("&aCompleted")), newPatternIcon);
				ItemUtils.setName("&aConfirm New Pattern",confirmPatternIcon);
				ItemUtils.setLore(List.of(ColourUtils.transCol("&aCompleted")),confirmPatternIcon);
			}
			
			for(int i = 0; i < 27; i++)
			{
				ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemUtils.setName(" ",filler);
				view.setItem(i,filler);
			}
			
			view.setItem(10,oldPatternIcon);
			view.setItem(13,newPatternIcon);
			view.setItem(16,confirmPatternIcon);
		}
		catch(Exception e1)
		{
			player.getOpenInventory().close();
			sessions.remove(player.getUniqueId());
			Logg.error("A problem occured when player " + player.getName() + " opened GUI 'Reset Pattern'" ,e1);
			PrintUtils.error(player,"An error occured while opening this GUI! Please alert a member of staff!");
		}
	}
	
	@Override
	public void handleClick(InventoryClickEvent e)
	{
		Player p = (Player) e.getWhoClicked();		
		RPPGUISession sess = sessions.get(p.getUniqueId());
		
		switch(e.getRawSlot())
		{
			case 10 ->
			{
				if(sess.hasCompletedOldPattern())
				{
					SoundUtils.playSound(p,Sound.BLOCK_VAULT_BREAK,1.0f);
					return;
				}
				
				sess.setResetStage(ResetStage.SELECTING_MATERIAL_A_FOR_OLD_PATTERN);
				SoundUtils.playSound(p,Sound.BLOCK_VAULT_EJECT_ITEM,1.0f);
				e.getWhoClicked().closeInventory();
				PatternSetInterface.open(p,sess);
			}
			case 13 ->
			{
				if(sess.hasCompletedNewPattern() || !sess.hasCompletedOldPattern())
				{
					SoundUtils.playSound(p,Sound.BLOCK_VAULT_BREAK,1.0f);
					return;
				}
				
				sess.setResetStage(ResetStage.SELECTING_MATERIAL_A_FOR_NEW_PATTERN);
				SoundUtils.playSound(p,Sound.BLOCK_VAULT_EJECT_ITEM,1.0f);
				e.getWhoClicked().closeInventory();
				PatternSetInterface.open(p,sess);
			}
			case 16 ->
			{
				if(sess.hasCompletedConfirmPattern() || !sess.hasCompletedOldPattern() || !sess.hasCompletedNewPattern())
				{
					SoundUtils.playSound(p,Sound.BLOCK_VAULT_BREAK,1.0f);
					return;
				}
				
				sess.setResetStage(ResetStage.SELECTING_MATERIAL_A_FOR_CONFIRM_PATTERN);
				SoundUtils.playSound(p,Sound.BLOCK_VAULT_EJECT_ITEM,1.0f);
				e.getWhoClicked().closeInventory();
				PatternSetInterface.open(p,sess);
			}
			default ->
			{
				SoundUtils.playSound(p,Sound.BLOCK_VAULT_BREAK,1.0f);
				return;
			}
		}
	}
	
	public static Map<UUID,RPPGUISession> getSessions()
	{
		return sessions;
	}

	@Override
	public void removeSession(UUID uuid)
	{
		sessions.remove(uuid);
	}

	@Override
	public String getGUIName()
	{
		return GUI_NAME;
	}
}
