package laeven.mpoa.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import laeven.mpoa.virtualplayerdata.data.VirtualPlayerData;

public class LoginGUISession
{
	private Player sessionOwner;
	private LoginStage loginStage = LoginStage.SELECTING_MATERIAL_A;
	private Material[] pattern = new Material[4];
	private VirtualPlayerData accountToLoginTo;

	public LoginGUISession(Player sessionOwner,VirtualPlayerData data)
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

	public LoginStage getLoginStage()
	{
		return loginStage;
	}

	public void setLoginStage(LoginStage loginStage)
	{
		this.loginStage = loginStage;
	}
	
	public Material[] getPattern()
	{
		return pattern;
	}

	public void setPattern(Material[] pattern)
	{
		this.pattern = pattern;
	}
	
	public VirtualPlayerData getAccountToLoginTo()
	{
		return accountToLoginTo;
	}

	public void setAccountToLoginTo(VirtualPlayerData accountToLoginTo)
	{
		this.accountToLoginTo = accountToLoginTo;
	}

	public enum LoginStage
	{
		SELECTING_MATERIAL_A,
		SELECTING_MATERIAL_B,
		SELECTING_MATERIAL_C,
		SELECTING_MATERIAL_D
	}
}
