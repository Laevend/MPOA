package laeven.mpoa.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import laeven.mpoa.gui.LoginInterface;
import laeven.mpoa.utils.PrintUtils;
import laeven.mpoa.utils.structs.TabTree;
import laeven.mpoa.utils.structs.TabTree.Node;
import laeven.mpoa.utils.structs.UID4;
import laeven.mpoa.virtualplayerdata.VirtualPlayerDataCtrl;
import laeven.mpoa.virtualplayerdata.data.VirtualPlayerData;

public class LoginCommand extends MPOACommand
{
	private static final String ACCOUNT_NAME = "<account-name>";
	
	// login <account-name>
	
	public void onCommand(CommandSender sender,String[] args)
	{
		if(args.length < 1)
		{
			PrintUtils.error(sender,"Not enough arguments!");
			return;
		}
		
		if(!(sender instanceof Player p))
		{
			PrintUtils.error(sender,"Console or CommandBlock cannot execute this command!");
			return;
		}
		
		if(VirtualPlayerDataCtrl.isPuppetingVirtualAccount(p))
		{
			PrintUtils.error(p,"This account is already puppeting a virtual account! You cannot login to another account!");
			return;
		}
		
		if(!VirtualPlayerDataCtrl.getActingAccounts().contains(p.getUniqueId()))
		{
			PrintUtils.error(sender,"This player is not designated as an acting player! It cannot be used to login to a virtual account!");
			return;
		}
		
		login(p,args[0]);
	}
	
	private void login(Player p,String accountName)
	{
		UID4 account = getAccountName(p,accountName);
		if(account == null) { return; }
		VirtualPlayerData pData = VirtualPlayerDataCtrl.getPlayerData(account);
		LoginInterface.open(p,pData);
	}
	
	private UID4 getAccountName(Player sender,String arg)
	{
		Set<String> accounts = VirtualPlayerDataCtrl.getVirtualAccountNames();
		
		if(!accounts.contains(arg))
		{
			PrintUtils.error(sender,arg + " is already loaded or is not a real virtual account!");
			return null;
		}
		
		UID4 uid = UID4.fromString(arg);
		VirtualPlayerDataCtrl.loadPlayerData(uid);
		return uid;
	}
	
	private static TabTree tree = new TabTree();
	
	public LoginCommand()
	{
		tree.getRoot().addBranch(ACCOUNT_NAME);
	}

	@Override
	public List<String> onTab(CommandSender sender, String[] args)
	{
		Node nextNode = tree.getRoot();
		
		if(args == null) { return new ArrayList<>(nextNode.branches.keySet()); }
		
		MainLoop:
		for(int i = 0; i < args.length; i++)
		{
			if(nextNode.branches.keySet().contains(args[i]))
			{
				nextNode = nextNode.branches.get(args[i]);
				continue;
			}
			
			for(String branch : nextNode.branches.keySet())
			{
				switch(branch)
				{
					case ACCOUNT_NAME -> 
					{
						if(UID4.UID4Pattern.matcher(args[i]).matches())
						{
							nextNode = nextNode.branches.get(branch);
							continue MainLoop;
						}
					}
				}
			}
			
			break MainLoop;
		}
		
		// Return suggestions
		List<String> suggestions = new ArrayList<>();
		
		for(String branch : nextNode.branches.keySet())
		{
			switch(branch)
			{
				case ACCOUNT_NAME -> 
				{
					suggestions.addAll(VirtualPlayerDataCtrl.getVirtualAccountNames());
				}
				default -> suggestions.add(branch);
			}
		}
		
		return suggestions;
	}
}
