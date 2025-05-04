package laeven.mpoa.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import laeven.mpoa.utils.clocks.LimitedCyclesClock;
import laeven.mpoa.utils.structs.Pair;

/**
 * @author Laeven
 * A class to manage cooldowns.
 * <p>Some actions can be expensive to run. Cooldowns limit how often an action can be ran.
 */
public class Cooldown
{
	// Map<Player UUID,Map<CooldownAction UUID,Pair<cooldown delay in milliseconds,Milliseconds when cool down started>>>
	private static final Map<UUID,Map<UUID,Pair<Long,Long>>> cooldownMap = new HashMap<>();
	
	/**
	 * Checks if an action is on cooldown for a player
	 * @param p Player who is being checked for the action cooldown
	 * @param cooldownActionUUID The cooldown action UUID
	 * @return True if this action is on cooldown, false otherwise
	 */
	public static boolean isCooling(Player p,UUID cooldownActionUUID)
	{
		return isCooling(p,cooldownActionUUID,false);
	}
	
	/**
	 * Checks if an action is on cooldown for a player without sending them an action message of their time before cooldown expires
	 * @param p Player who is being checked for the action cooldown
	 * @param cooldownActionUUID The cooldown action UUID
	 * @param silentCheck If this check should be performed silently and not inform the player
	 * @return True if this action is on cooldown, false otherwise
	 */
	public static boolean isCooling(Player p,UUID cooldownActionUUID,boolean silentCheck)
	{
		if(!cooldownMap.containsKey(p.getUniqueId())) { return false; }
		if(!cooldownMap.get(p.getUniqueId()).containsKey(cooldownActionUUID)) { return false; }
		
		Pair<Long,Long> cooldown = cooldownMap.get(p.getUniqueId()).get(cooldownActionUUID);
		long delay = cooldown.getValueA();
		long timeStart = cooldown.getValueB();
		long timeDelta = System.currentTimeMillis() - timeStart;
		
		if((timeStart + delay) > System.currentTimeMillis())
		{
			if(silentCheck) { return true; }
			PrintUtils.actionBar(p,ColourUtils.transCol("&cThis action is on cooldown! " + String.format("%.2f",(double) (delay - timeDelta) / 1000) + " seconds remain."));
			return true;
		}
		
		// No longer needed
		cooldownMap.get(p.getUniqueId()).remove(cooldownActionUUID);
		return false;
	}
	
	/**
	 * Sets a new cooldown for a player of a specific cooldown action
	 * @param p Player to set cooldown for
	 * @param cooldownActionUUID The cooldown action UUID
	 * @param cooldownDelayInMili The number of milliseconds before this cooldown expires
	 */
	public static void setCooldown(Player p,UUID cooldownActionUUID,long cooldownDelayInMili)
	{
		if(!cooldownMap.containsKey(p.getUniqueId()))
		{
			cooldownMap.put(p.getUniqueId(),new HashMap<>());
		}
		
		if(cooldownMap.get(p.getUniqueId()).containsKey(cooldownActionUUID))
		{
			cooldownMap.get(p.getUniqueId()).remove(cooldownActionUUID);
		}
		
		cooldownMap.get(p.getUniqueId()).put(cooldownActionUUID,new Pair<>(cooldownDelayInMili,System.currentTimeMillis()));
	}
	
	/**
	 * Clears a cooldown for a player of a specific cooldown action
	 * @param p Player to clear cooldown for
	 * @param cooldownActionUUID The cooldown action UUID
	 */
	public static void clearCooldown(Player p,UUID cooldownActionUUID)
	{
		if(!cooldownMap.containsKey(p.getUniqueId())) { return; }
		if(!cooldownMap.get(p.getUniqueId()).containsKey(cooldownActionUUID)) { return; }
		cooldownMap.get(p.getUniqueId()).remove(cooldownActionUUID);
	}
	
	/**
	 * Clears all cooldowns for a player
	 * @param p Player to clear cooldown for
	 */
	public static void clearAllCooldowns(Player p)
	{
		if(!cooldownMap.containsKey(p.getUniqueId())) { return; }
		cooldownMap.remove(p.getUniqueId());
	}
	
	private static final Map<UUID,CountdownBeforeActionClock> countdownMap = new HashMap<>();
	
	/**
	 * Starts a visual cooldown countdown for a player and executes an action when the countdown finishes
	 * @param p Player to display cooldown countdown to
	 * @param title Title of the cooldown countdown
	 * @param countdownInSeconds Seconds until the countdown ends
	 * @param action Runnable action to execute once countdown ends
	 */
	public static void startCountdown(Player p,String title,int countdownInSeconds,Runnable action)
	{
		if(countdownMap.containsKey(p.getUniqueId()))
		{
			stopCountdown(p);
		}
		
		countdownMap.put(p.getUniqueId(),new CountdownBeforeActionClock(p,title,countdownInSeconds,action));
	}
	
	/**
	 * Stops and removes a countdown clock
	 * @param p Player to stop countdown on
	 */
	public static void stopCountdown(Player p)
	{
		if(!countdownMap.containsKey(p.getUniqueId())) { return; }
		
		CountdownBeforeActionClock oldCountdownClock = countdownMap.remove(p.getUniqueId());
		oldCountdownClock.stop();
		PrintUtils.clearTitle(p);
	}
	
	protected static class CountdownBeforeActionClock extends LimitedCyclesClock
	{
		private Player p;
		private Runnable action;
		private String subtitle = "";
		
		/**
		 * Creates a visible cooldown title count down sequence before an action is executed
		 * @param p Player who should be shown this countdown sequence
		 * @param title
		 * @param cooldownInSeconds
		 * @param action
		 */
		public CountdownBeforeActionClock(Player p,String title,int countdownInSeconds,Runnable action)
		{
			super("Countdown cooldown clock",1,TimeUtils.secondsToTicks(countdownInSeconds));
			this.p = p;
			this.action = action;
			
			if(title != null && !title.isEmpty() && !title.isBlank())
			{
				this.subtitle = ColourUtils.transCol("&7" + title);
			}
		}

		@Override
		public void execute() throws Exception
		{
			PrintUtils.clearTitle(p);
			PrintUtils.sendTitle(p,ColourUtils.transCol("&c" + String.valueOf(getCyclesLeft())),subtitle);
		}

		@Override
		public void finalExecute()
		{
			action.run();
			Cooldown.stopCountdown(p);
		}
	}
}
