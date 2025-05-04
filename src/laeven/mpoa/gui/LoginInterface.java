package laeven.mpoa.gui;

import java.util.Arrays;
import java.util.Collections;
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

import laeven.mpoa.gui.LoginGUISession.LoginStage;
import laeven.mpoa.utils.ColourUtils;
import laeven.mpoa.utils.ItemUtils;
import laeven.mpoa.utils.Logg;
import laeven.mpoa.utils.MaterialUtils;
import laeven.mpoa.utils.PrintUtils;
import laeven.mpoa.utils.SoundUtils;
import laeven.mpoa.utils.TimeUtils;
import laeven.mpoa.utils.security.HashingUtils;
import laeven.mpoa.utils.structs.UID4;
import laeven.mpoa.virtualplayerdata.VirtualPlayerDataCtrl;
import laeven.mpoa.virtualplayerdata.data.VirtualPlayerData;

public class LoginInterface extends GUIHandler implements Listener
{
	private static Map<UUID,LoginGUISession> sessions = new HashMap<>();
	
	private static final int[] materialSlots = new int[] {21,22,23,30,31,32,39,40,41};
	private static final int[] materialBorderSlots = new int[] {11,12,13,14,15,20,24,29,33,38,42,47,48,49,50,51};
	private static final int infoSlot = 4;
	private static final ItemStack infoIcon;
	public static final String GUI_NAME = "Login";
	
	static
	{
		infoIcon = new ItemStack(Material.VAULT);
		ItemUtils.setName("&aLogin Interface",infoIcon);
		ItemUtils.setLore(List.of
		(
			ColourUtils.transCol("&eTo log into your account, enter your material pattern password"),
			ColourUtils.transCol("&eby clicking on the blocks and items below.")
		),infoIcon);
	}
	
	public static void open(Player player,VirtualPlayerData data)
	{		
		if(sessions.containsKey(player.getUniqueId()))
		{
			PrintUtils.error(player,"Error! Could not open login interface! Please reconnect to the server and try again!");
			return;
		}
		
		try
		{
			sessions.put(player.getUniqueId(),new LoginGUISession(player,data));
			Inventory inv = Bukkit.createInventory(null,54,GUI_NAME);
			InventoryView view = player.openInventory(inv);
			SoundUtils.playSound(player,Sound.BLOCK_VAULT_OPEN_SHUTTER,1.0f);
			
			for(int i = 0; i < 54; i++)
			{
				ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemUtils.setName(" ",filler);
				view.setItem(i,filler);
			}
			
			for(int slot : materialBorderSlots)
			{
				ItemStack filler = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
				ItemUtils.setName(" ",filler);
				
				view.setItem(slot,filler);
			}
			
			view.setItem(infoSlot,infoIcon);
			
			shuffleA(view);
		}
		catch(Exception e1)
		{
			player.getOpenInventory().close();
			sessions.remove(player.getUniqueId());
			Logg.error("A problem occured when player " + player.getName() + " opened GUI 'Login'" ,e1);
			PrintUtils.error(player,"An error occured while opening this GUI! Please alert a member of staff!");
		}
	}
	
	private static void shuffleA(InventoryView view)
	{
		Collections.shuffle(MaterialUtils.getMatSelectionA());
		
		for(int i = 0; i < materialSlots.length; i++)
		{
			view.setItem(materialSlots[i],new ItemStack(MaterialUtils.getMatSelectionA().get(i)));
		}
	}
	
	private static void shuffleB(InventoryView view)
	{
		Collections.shuffle(MaterialUtils.getMatSelectionB());
		
		for(int i = 0; i < materialSlots.length; i++)
		{
			view.setItem(materialSlots[i],new ItemStack(MaterialUtils.getMatSelectionB().get(i)));
		}
	}
	
	private static void shuffleC(InventoryView view)
	{
		Collections.shuffle(MaterialUtils.getMatSelectionC());
		
		for(int i = 0; i < materialSlots.length; i++)
		{
			view.setItem(materialSlots[i],new ItemStack(MaterialUtils.getMatSelectionC().get(i)));
		}
	}
	
	private static void shuffleD(InventoryView view)
	{
		Collections.shuffle(MaterialUtils.getMatSelectionD());
		
		for(int i = 0; i < materialSlots.length; i++)
		{
			view.setItem(materialSlots[i],new ItemStack(MaterialUtils.getMatSelectionD().get(i)));
		}
	}
	
	@Override
	public void handleClick(InventoryClickEvent e)
	{
		Player p = (Player) e.getWhoClicked();
		
		if(Arrays.binarySearch(materialSlots,e.getRawSlot()) < 0)
		{
			SoundUtils.playSound(p,Sound.BLOCK_VAULT_BREAK,1.0f);
			return;
		}
		
		LoginGUISession session = sessions.get(p.getUniqueId());
		LoginStage stage = session.getLoginStage();
		Material[] pattern = session.getPattern();
		
		switch(stage)
		{
			case SELECTING_MATERIAL_A ->
			{
				pattern[0] = e.getView().getItem(e.getRawSlot()).getType();
				session.setLoginStage(LoginStage.SELECTING_MATERIAL_B);
				shuffleB(e.getView());
			}
			case SELECTING_MATERIAL_B ->
			{
				pattern[1] = e.getView().getItem(e.getRawSlot()).getType();
				session.setLoginStage(LoginStage.SELECTING_MATERIAL_C);
				shuffleC(e.getView());
			}
			case SELECTING_MATERIAL_C ->
			{
				pattern[2] = e.getView().getItem(e.getRawSlot()).getType();
				session.setLoginStage(LoginStage.SELECTING_MATERIAL_D);
				shuffleD(e.getView());
			}
			case SELECTING_MATERIAL_D ->
			{
				pattern[3] = e.getView().getItem(e.getRawSlot()).getType();
			}
		}
		
		SoundUtils.playSound(p,Sound.BLOCK_VAULT_INSERT_ITEM,1.0f);
		session.setPattern(pattern);
		
		if(stage != LoginStage.SELECTING_MATERIAL_D) { return; }
		
		checkLoginPattern(e);
	}
	
	private void checkLoginPattern(InventoryClickEvent e)
	{
		Player p = (Player) e.getWhoClicked();
		LoginGUISession session = sessions.get(p.getUniqueId());
		UID4 owner = session.getAccountToLoginTo().getOwner();
		Material[] pattern = session.getPattern();
		String password = 
				pattern[0].toString() + "," +
				pattern[1].toString() + "," + 
				pattern[2].toString() + "," + 
				pattern[3].toString();
		
		String hashedPassword = HashingUtils.hashToString(password,session.getAccountToLoginTo().getSalt());
		
		if(hashedPassword.equals(session.getAccountToLoginTo().getPassword()))
		{
			SoundUtils.playSound(p,Sound.BLOCK_VAULT_REJECT_REWARDED_PLAYER,1.0f);
			e.getWhoClicked().closeInventory();
			login(p,owner);
		}
		else
		{
			PrintUtils.error(p,"Material pattern password is incorrect!");
			SoundUtils.playSound(p,Sound.BLOCK_VAULT_BREAK,1.0f);
			e.getWhoClicked().closeInventory();
		}
	}
	
	private void login(Player player,UID4 account)
	{
		VirtualPlayerData pData = VirtualPlayerDataCtrl.getPlayerData(account);
		
		if(pData.isLocked())
		{
			PrintUtils.error(player,"Cannot login, this account is locked!");
			return;
		}
		
		// Check for cooldown
		if(pData.getTimelimit().isEnabled() && !pData.getTimelimit().hasCooldownExpired())
		{
			long timeRemaining = pData.getTimelimit().getMaxCooldownTime() - (System.currentTimeMillis() - pData.getTimelimit().getCooldownStartTime());
			PrintUtils.error(player,"Cannot login, Cooldown has not expired! Remaining time: " + TimeUtils.millisecondsToHoursMinutesSeconds(timeRemaining));
			return;
		}
		
		// Reset play time if it is 0 and cooldown has expired
		if(pData.getTimelimit().isEnabled() && pData.getTimelimit().getPlayTimeLeft() == 0)
		{
			pData.getTimelimit().resetPlayTime();
		}
		
		if(pData.getActingPlayer() != null)
		{
			PrintUtils.error(player,"This virtual account is already logged in using account " + pData.getActingPlayer().getName());
			return;
		}
		
		if(!VirtualPlayerDataCtrl.getActingAccounts().contains(player.getUniqueId()))
		{
			PrintUtils.error(player,"This player is not designated as an acting player! It cannot be used to login to a virtual account!");
			return;
		}
		
		if(!VirtualPlayerDataCtrl.login(account,player))
		{
			PrintUtils.error(player,"An error occured attempting to login to account " + account);
			return;
		}
		
		PrintUtils.success(player,"Login Successful!");
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
