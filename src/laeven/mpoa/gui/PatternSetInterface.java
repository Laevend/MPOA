package laeven.mpoa.gui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import laeven.mpoa.utils.MaterialUtils;
import laeven.mpoa.utils.PrintUtils;
import laeven.mpoa.utils.SoundUtils;
import laeven.mpoa.utils.security.HashingUtils;
import laeven.mpoa.virtualplayerdata.data.VirtualPlayerData;

public class PatternSetInterface extends GUIHandler implements Listener
{
	public static final String GUI_NAME = "Enter Pattern";
	
	public static void open(Player p,RPPGUISession sess)
	{
		try
		{
			Inventory inv = Bukkit.createInventory(null,54,GUI_NAME);
			InventoryView view = p.openInventory(inv);
			SoundUtils.playSound(p,Sound.BLOCK_VAULT_OPEN_SHUTTER,1.0f);
			ItemStack infoIcon = new ItemStack(Material.VAULT);
			ResetPatternPasswordInterface.getSessions().put(p.getUniqueId(),sess);
			
			for(int i = 0; i < 54; i++)
			{
				ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemUtils.setName(" ",filler);
				view.setItem(i,filler);
			}
			
			switch(sess.getResetStage())
			{
				case SELECTING_MATERIAL_A_FOR_OLD_PATTERN:
				case SELECTING_MATERIAL_B_FOR_OLD_PATTERN:
				case SELECTING_MATERIAL_C_FOR_OLD_PATTERN:
				case SELECTING_MATERIAL_D_FOR_OLD_PATTERN:
				{
					ItemUtils.setName("&aEnter your &eCURRENT &amaterial password",infoIcon);
					ItemUtils.setLore(List.of
					(
							ColourUtils.transCol("&eClick on the blocks and items below to enter material pattern password"),
							ColourUtils.transCol("&eto enter your old material pattern password.")
					),infoIcon);
					break;
				}
				case SELECTING_MATERIAL_A_FOR_NEW_PATTERN:
				case SELECTING_MATERIAL_B_FOR_NEW_PATTERN:
				case SELECTING_MATERIAL_C_FOR_NEW_PATTERN:
				case SELECTING_MATERIAL_D_FOR_NEW_PATTERN:
				{
					ItemUtils.setName("&aEnter your &bNEW &amaterial password",infoIcon);
					ItemUtils.setLore(List.of
					(
							ColourUtils.transCol("&eClick on the blocks and items below to enter material pattern password"),
							ColourUtils.transCol("&eto enter your new material pattern password.")
					),infoIcon);
					break;
				}
				case SELECTING_MATERIAL_A_FOR_CONFIRM_PATTERN:
				case SELECTING_MATERIAL_B_FOR_CONFIRM_PATTERN:
				case SELECTING_MATERIAL_C_FOR_CONFIRM_PATTERN:
				case SELECTING_MATERIAL_D_FOR_CONFIRM_PATTERN:
				{
					ItemUtils.setName("&aReenter your &bNEW &amaterial password",infoIcon);
					ItemUtils.setLore(List.of
					(
						ColourUtils.transCol("&eClick on the blocks and items below to enter material pattern password"),
						ColourUtils.transCol("&eto enter your new material pattern password again.")
					),infoIcon);
					break;
				}
				default: { return; }
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
			p.getOpenInventory().close();
			ResetPatternPasswordInterface.getSessions().remove(p.getUniqueId());
			Logg.error("A problem occured when player " + p.getName() + " opened GUI 'Login'" ,e1);
			PrintUtils.error(p,"An error occured while opening this GUI! Please alert a member of staff!");
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
		
		RPPGUISession sess = ResetPatternPasswordInterface.getSessions().get(p.getUniqueId());
		ResetStage stage = sess.getResetStage();
		Material[] pattern;
		
		switch(stage)
		{
			case SELECTING_MATERIAL_A_FOR_OLD_PATTERN:
			case SELECTING_MATERIAL_B_FOR_OLD_PATTERN:
			case SELECTING_MATERIAL_C_FOR_OLD_PATTERN:
			case SELECTING_MATERIAL_D_FOR_OLD_PATTERN:
			{
				pattern = sess.getOldPattern();
				break;
			}
			case SELECTING_MATERIAL_A_FOR_NEW_PATTERN:
			case SELECTING_MATERIAL_B_FOR_NEW_PATTERN:
			case SELECTING_MATERIAL_C_FOR_NEW_PATTERN:
			case SELECTING_MATERIAL_D_FOR_NEW_PATTERN:
			{
				pattern = sess.getNewPattern();
				break;
			}
			case SELECTING_MATERIAL_A_FOR_CONFIRM_PATTERN:
			case SELECTING_MATERIAL_B_FOR_CONFIRM_PATTERN:
			case SELECTING_MATERIAL_C_FOR_CONFIRM_PATTERN:
			case SELECTING_MATERIAL_D_FOR_CONFIRM_PATTERN:
			{
				pattern = sess.getConfirmPattern();
				break;
			}
			default: { return; }
		}
		
		switch(stage)
		{
			case SELECTING_MATERIAL_A_FOR_OLD_PATTERN:
			{
				sess.setResetStage(ResetStage.SELECTING_MATERIAL_B_FOR_OLD_PATTERN);
				pattern[0] = e.getView().getItem(e.getRawSlot()).getType();
				shuffleB(e.getView());
				break;
			}
			case SELECTING_MATERIAL_A_FOR_NEW_PATTERN:
			{
				sess.setResetStage(ResetStage.SELECTING_MATERIAL_B_FOR_NEW_PATTERN);
				pattern[0] = e.getView().getItem(e.getRawSlot()).getType();
				shuffleB(e.getView());
				break;
			}
			case SELECTING_MATERIAL_A_FOR_CONFIRM_PATTERN:
			{
				sess.setResetStage(ResetStage.SELECTING_MATERIAL_B_FOR_CONFIRM_PATTERN);
				pattern[0] = e.getView().getItem(e.getRawSlot()).getType();
				shuffleB(e.getView());
				break;
			}
			
			case SELECTING_MATERIAL_B_FOR_OLD_PATTERN:
			{
				sess.setResetStage(ResetStage.SELECTING_MATERIAL_C_FOR_OLD_PATTERN);
				pattern[1] = e.getView().getItem(e.getRawSlot()).getType();
				shuffleC(e.getView());
				break;
			}
			case SELECTING_MATERIAL_B_FOR_NEW_PATTERN:
			{
				sess.setResetStage(ResetStage.SELECTING_MATERIAL_C_FOR_NEW_PATTERN);
				pattern[1] = e.getView().getItem(e.getRawSlot()).getType();
				shuffleC(e.getView());
				break;
			}
			case SELECTING_MATERIAL_B_FOR_CONFIRM_PATTERN:
			{
				sess.setResetStage(ResetStage.SELECTING_MATERIAL_C_FOR_CONFIRM_PATTERN);
				pattern[1] = e.getView().getItem(e.getRawSlot()).getType();
				shuffleC(e.getView());
				break;
			}
			
			case SELECTING_MATERIAL_C_FOR_OLD_PATTERN:
			{
				sess.setResetStage(ResetStage.SELECTING_MATERIAL_D_FOR_OLD_PATTERN);
				pattern[2] = e.getView().getItem(e.getRawSlot()).getType();
				shuffleD(e.getView());
				break;
			}
			case SELECTING_MATERIAL_C_FOR_NEW_PATTERN:
			{
				sess.setResetStage(ResetStage.SELECTING_MATERIAL_D_FOR_NEW_PATTERN);
				pattern[2] = e.getView().getItem(e.getRawSlot()).getType();
				shuffleD(e.getView());
				break;
			}
			case SELECTING_MATERIAL_C_FOR_CONFIRM_PATTERN:
			{
				sess.setResetStage(ResetStage.SELECTING_MATERIAL_D_FOR_CONFIRM_PATTERN);
				pattern[2] = e.getView().getItem(e.getRawSlot()).getType();
				shuffleD(e.getView());
				break;
			}
			
			case SELECTING_MATERIAL_D_FOR_OLD_PATTERN:
			case SELECTING_MATERIAL_D_FOR_NEW_PATTERN:
			case SELECTING_MATERIAL_D_FOR_CONFIRM_PATTERN:
			{
				pattern[3] = e.getView().getItem(e.getRawSlot()).getType();
				break;
			}
			default: { return; }
		}
		
		SoundUtils.playSound(p,Sound.BLOCK_VAULT_INSERT_ITEM,1.0f);
		
		if(stage == ResetStage.SELECTING_MATERIAL_D_FOR_OLD_PATTERN)
		{
			checkAgainstOldPatternOnAccount(e);
			return;
		}
		
		if(stage == ResetStage.SELECTING_MATERIAL_D_FOR_NEW_PATTERN)
		{
			e.getWhoClicked().closeInventory();
			sess.setCompletedNewPattern(true);
			ResetPatternPasswordInterface.getSessions().put(p.getUniqueId(),sess);
			ResetPatternPasswordInterface.open(p,sess.getAccountToLoginTo());
			return;
		}
		
		if(stage == ResetStage.SELECTING_MATERIAL_D_FOR_CONFIRM_PATTERN)
		{
			changePattern(e);
			return;
		}
	}
	
	private void checkAgainstOldPatternOnAccount(InventoryClickEvent e)
	{
		Player p = (Player) e.getWhoClicked();
		RPPGUISession sess = ResetPatternPasswordInterface.getSessions().get(p.getUniqueId());
		
		if(!hashPattern(sess.getOldPattern(),sess).equals(sess.getAccountToLoginTo().getPassword()))
		{
			PrintUtils.error(p,"Material old pattern does not match your existing pattern!");
			SoundUtils.playSound(p,Sound.BLOCK_VAULT_BREAK,1.0f);
			e.getWhoClicked().closeInventory();
		}
		else
		{
			e.getWhoClicked().closeInventory();
			sess.setCompletedOldPattern(true);
			ResetPatternPasswordInterface.getSessions().put(p.getUniqueId(),sess);
			ResetPatternPasswordInterface.open(p,sess.getAccountToLoginTo());
		}
	}
	
	private void changePattern(InventoryClickEvent e)
	{
		Player p = (Player) e.getWhoClicked();
		RPPGUISession sess = ResetPatternPasswordInterface.getSessions().get(p.getUniqueId());
		VirtualPlayerData data = sess.getAccountToLoginTo();
		
		String hashedNewPassword = hashPattern(sess.getNewPattern(),sess);
		String hashedNewConfirmPassword = hashPattern(sess.getConfirmPattern(),sess);
		
		if(hashedNewPassword.equals(hashedNewConfirmPassword))
		{
			SoundUtils.playSound(p,Sound.BLOCK_VAULT_REJECT_REWARDED_PLAYER,1.0f);
			SoundUtils.playSound(p,Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB,1.0f);
			e.getWhoClicked().closeInventory();
			data.setNewPassword(sess.getNewPattern());
			PrintUtils.success(p,"New material pattern password set!");
			sess.setCompletedConfirmPattern(true);
		}
		else
		{
			PrintUtils.error(p,"Both new material pattern passwords does not match!");
			SoundUtils.playSound(p,Sound.BLOCK_VAULT_BREAK,1.0f);
			e.getWhoClicked().closeInventory();
		}
	}
	
	private String hashPattern(Material[] mat,RPPGUISession sess)
	{
		String password = 
				mat[0].toString() + "," +
				mat[1].toString() + "," + 
				mat[2].toString() + "," + 
				mat[3].toString();
		
		String hashedPassword = HashingUtils.hashToString(password,sess.getAccountToLoginTo().getSalt());
		return hashedPassword;
	}

	@Override
	public void removeSession(UUID uuid)
	{
		ResetPatternPasswordInterface.getSessions().remove(uuid);
	}

	@Override
	public String getGUIName()
	{
		return GUI_NAME;
	}
}
