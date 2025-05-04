package laeven.mpoa.utils;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import laeven.mpoa.MPOA;

/**
 * @author Laeven
 * @since 1.0.0
 */
public class DelayUtils
{
	/**
	 * Executes a delayed task
	 * @param runn The runnable object to execute
	 */
	public static void executeDelayedTask(Runnable runn)
	{
		executeDelayedTask(runn,1L);
	}
	
	/**
	 * Executes a delayed task
	 * @param runn The runnable object to execute
	 * @param ticksToWait Number of ticks to wait before executing this runnable object
	 * @return task id
	 */
	public static int executeDelayedTask(Runnable runn,long ticksToWait)
	{
        return Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(MPOA.instance(),runn,ticksToWait);
	}
	
	/**
	 * Executes a delayed bukkit task
	 * @param runn The runnable object to execute
	 * @param ticksToWait Number of ticks to wait before executing this runnable object
	 * @return BukkitTask
	 */
	public static BukkitTask executeDelayedBukkitTask(Runnable runn,long ticksToWait)
	{
        return Bukkit.getServer().getScheduler().runTaskLater(MPOA.instance(),runn,ticksToWait);
	}
}