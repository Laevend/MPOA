package laeven.mpoa.utils;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class SoundUtils
{
	public static void playSound(Player p,Sound s,float pitch)
	{
		p.playSound(p.getLocation(),s,SoundCategory.MASTER,1.0f,pitch);
	}
}
