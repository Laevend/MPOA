package laeven.mpoa.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import laeven.mpoa.virtualplayerdata.data.VirtualPlayerData;

public class RPPGUISession
{
	private Player sessionOwner;
	private ResetStage resetStage = ResetStage.MAIN_MENU;
	private Material[] oldPattern = new Material[4];
	private Material[] newPattern = new Material[4];
	private Material[] confirmPattern = new Material[4];
	private VirtualPlayerData accountToLoginTo;
	
	private boolean completedOldPattern = false;
	private boolean completedNewPattern = false;
	private boolean completedConfirmPattern = false;

	public RPPGUISession(Player sessionOwner,VirtualPlayerData data)
	{
		this.sessionOwner = sessionOwner;
		this.accountToLoginTo = data;
	}
	
	public Player getSessionOwner()
	{
		return sessionOwner;
	}

	public void setSessionOwner(Player sessionOwner)
	{
		this.sessionOwner = sessionOwner;
	}
	
	public ResetStage getResetStage()
	{
		return resetStage;
	}

	public void setResetStage(ResetStage resetStage)
	{
		this.resetStage = resetStage;
	}

	public Material[] getOldPattern()
	{
		return oldPattern;
	}

	public void setOldPattern(Material[] oldPattern)
	{
		this.oldPattern = oldPattern;
	}

	public Material[] getNewPattern()
	{
		return newPattern;
	}

	public void setNewPattern(Material[] newPattern)
	{
		this.newPattern = newPattern;
	}

	public Material[] getConfirmPattern()
	{
		return confirmPattern;
	}

	public void setConfirmPattern(Material[] confirmPattern)
	{
		this.confirmPattern = confirmPattern;
	}

	public VirtualPlayerData getAccountToLoginTo()
	{
		return accountToLoginTo;
	}

	public void setAccountToLoginTo(VirtualPlayerData accountToLoginTo)
	{
		this.accountToLoginTo = accountToLoginTo;
	}

	public boolean hasCompletedOldPattern()
	{
		return completedOldPattern;
	}

	public void setCompletedOldPattern(boolean completedOldPattern)
	{
		this.completedOldPattern = completedOldPattern;
	}

	public boolean hasCompletedNewPattern()
	{
		return completedNewPattern;
	}

	public void setCompletedNewPattern(boolean completedNewPattern)
	{
		this.completedNewPattern = completedNewPattern;
	}

	public boolean hasCompletedConfirmPattern()
	{
		return completedConfirmPattern;
	}

	public void setCompletedConfirmPattern(boolean completedConfirmPattern)
	{
		this.completedConfirmPattern = completedConfirmPattern;
	}

	public enum ResetStage
	{
		MAIN_MENU,
		SELECTING_MATERIAL_A_FOR_OLD_PATTERN,
		SELECTING_MATERIAL_B_FOR_OLD_PATTERN,
		SELECTING_MATERIAL_C_FOR_OLD_PATTERN,
		SELECTING_MATERIAL_D_FOR_OLD_PATTERN,
		SELECTING_MATERIAL_A_FOR_NEW_PATTERN,
		SELECTING_MATERIAL_B_FOR_NEW_PATTERN,
		SELECTING_MATERIAL_C_FOR_NEW_PATTERN,
		SELECTING_MATERIAL_D_FOR_NEW_PATTERN,
		SELECTING_MATERIAL_A_FOR_CONFIRM_PATTERN,
		SELECTING_MATERIAL_B_FOR_CONFIRM_PATTERN,
		SELECTING_MATERIAL_C_FOR_CONFIRM_PATTERN,
		SELECTING_MATERIAL_D_FOR_CONFIRM_PATTERN
	}
}
