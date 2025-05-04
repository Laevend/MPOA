package laeven.mpoa.commands;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import laeven.mpoa.utils.PrintUtils;
import laeven.mpoa.virtualplayerdata.VirtualPlayerDataCtrl;

public class LogoutCommand extends MPOACommand
{
	// logout
	
	public void onCommand(CommandSender sender,String[] args)
	{
		if(!(sender instanceof Player p))
		{
			PrintUtils.error(sender,"Console or CommandBlock cannot execute this command!");
			return;
		}
		
		logout(p);
	}
	
	private void logout(Player p)
	{
		if(!VirtualPlayerDataCtrl.isPuppetingVirtualAccount(p))
		{
			PrintUtils.error(p,"This account is not puppeting a virtual account! You cannot logout!");
			return;
		}
		
		VirtualPlayerDataCtrl.logout(p);
	}

	@Override
	public List<String> onTab(CommandSender sender, String[] args)
	{
		return Collections.emptyList();
	}
}
