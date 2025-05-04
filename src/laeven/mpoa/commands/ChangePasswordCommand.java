package laeven.mpoa.commands;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import laeven.mpoa.gui.ResetPatternPasswordInterface;
import laeven.mpoa.utils.PrintUtils;
import laeven.mpoa.virtualplayerdata.VirtualPlayerDataCtrl;

public class ChangePasswordCommand extends MPOACommand
{
	// change-password
	
	public void onCommand(CommandSender sender,String[] args)
	{
		if(!(sender instanceof Player p))
		{
			PrintUtils.error(sender,"Console or CommandBlock cannot execute this command!");
			return;
		}
		
		changePassword(p);
	}
	
	private void changePassword(Player p)
	{
		if(!VirtualPlayerDataCtrl.isPuppetingVirtualAccount(p))
		{
			PrintUtils.error(p,"This account is not puppeting a virtual account! You cannot change its password!");
			return;
		}
		
		ResetPatternPasswordInterface.open(p,VirtualPlayerDataCtrl.getPlayerData(p));
	}

	@Override
	public List<String> onTab(CommandSender sender, String[] args)
	{
		return Collections.emptyList();
	}
}
