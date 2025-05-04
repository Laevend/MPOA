package laeven.mpoa.virtualplayerdata.data;

import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

import com.google.gson.JsonObject;

import laeven.mpoa.exception.DeserialiseException;
import laeven.mpoa.exception.SerialiseException;
import laeven.mpoa.utils.ColourUtils;
import laeven.mpoa.utils.DelayUtils;
import laeven.mpoa.utils.Logg;
import laeven.mpoa.utils.MathUtils;
import laeven.mpoa.utils.TimeUtils;
import laeven.mpoa.utils.clocks.LimitedCyclesClock;
import laeven.mpoa.utils.json.PersistJson;
import laeven.mpoa.utils.tools.Deserialise;
import laeven.mpoa.virtualplayerdata.VirtualPlayerDataCtrl;

/**
 * An adapted variant of the PlayerData module from Dape
 */
public class VirtualPlayerTimeLimit implements PersistJson
{
	private VirtualPlayerData parent;
	
	private boolean enabled = true;
	
	// Total duration that will elapse before this account can be played on again
	private long maxCooldownTime = 28800000L; // 8 hours
	
	// Total duration that this account can be played for before being logged out
	private long maxPlayTime = 1800000L; // 30 minutes
	
	// Total duration remaining until auto logout
	private long playTimeLeft = maxPlayTime;
	
	// A point in time when the cooldown starts
	private long cooldownStartTime = 0;
	
	// The boss bar used to display the cooldown
	private BossBar displayBar = null;
	private TimeLimitClock clock = null;
	
	public VirtualPlayerTimeLimit(VirtualPlayerData data)
	{
		super();
		this.parent = data;
	}
	
	public VirtualPlayerData getParent()
	{
		return parent;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public long getMaxCooldownTime()
	{
		return maxCooldownTime;
	}

	public void setMaxCooldownTime(long maxCooldownTime)
	{
		this.maxCooldownTime = maxCooldownTime;
	}

	public long getMaxPlayTime()
	{
		return maxPlayTime;
	}

	public void setMaxPlayTime(long maxPlayTime)
	{		
		this.maxPlayTime = maxPlayTime;
		
		if(this.maxPlayTime < playTimeLeft)
		{
			playTimeLeft = this.maxPlayTime;
		}
	}
	
	public void resetPlayTime()
	{
		playTimeLeft = maxPlayTime;
	}

	public long getPlayTimeLeft()
	{
		return playTimeLeft;
	}

	public void setPlayTimeLeft(long playTimeLeft)
	{
		this.playTimeLeft = MathUtils.clamp(0,maxPlayTime,playTimeLeft);
	}

	public long getCooldownStartTime()
	{
		return cooldownStartTime;
	}

	public void setCooldownStartTime(long cooldownStartTime)
	{
		this.cooldownStartTime = cooldownStartTime;
	}

	public BossBar getDisplayBar()
	{
		if(displayBar == null)
		{
			displayBar = Bukkit.createBossBar("Play Time Left [??:??:??]",BarColor.WHITE,BarStyle.SOLID);
			displayBar.setVisible(true);
		}
		
		return displayBar;
	}
	
	public void resetDisplayBar()
	{
		displayBar = null;
	}
	
	public boolean hasCooldownExpired()
	{
		return System.currentTimeMillis() - cooldownStartTime > maxCooldownTime;
	}
	
	public void resetCooldown()
	{
		cooldownStartTime = 0;
	}
	
	public void restartTimer()
	{
		stopTimer();
		
		DelayUtils.executeDelayedBukkitTask(() ->
		{
			startTimer();
		},2);
	}
	
	public void startTimer()
	{
		if(parent == null || parent.getActingPlayer() == null) { return; }
		
		long seconds = TimeUnit.MILLISECONDS.toSeconds(playTimeLeft);
		
		if(seconds > Integer.MAX_VALUE)
		{
			Logg.error("Play time remaining is too large!");
			return;
		}
		
		getDisplayBar().removeAll();
		getDisplayBar().addPlayer(parent.getActingPlayer());
		
		if(clock != null)
		{
			if(clock.isEnabled())
			{
				clock.stop();
			}
			
			clock = null;
		}
		
		clock = new TimeLimitClock((int) seconds);
		clock.start();
	}
	
	public void stopTimer()
	{
		if(parent == null || parent.getActingPlayer() == null) { return; }
		
		getDisplayBar().removeAll();
		
		if(clock != null)
		{
			if(clock.isEnabled())
			{
				clock.stop();
			}
			
			clock = null;
		}
	}
	
	public static final String ENABLED = "enabled";
	public static final String MAX_COOLDOWN_TIME = "max_cooldown_time";
	public static final String MAX_PLAY_TIME = "max_play_time";
	public static final String PLAY_TIME_LEFT = "play_time_left";
	public static final String COOLDOWN_START_TIME = "cooldown_start_time";

	@Override
	public JsonObject serialise() throws SerialiseException
	{
		JsonObject obj = new JsonObject();
		
		obj.addProperty(ENABLED,enabled);
		obj.addProperty(MAX_COOLDOWN_TIME,maxCooldownTime);
		obj.addProperty(MAX_PLAY_TIME,maxPlayTime);
		obj.addProperty(PLAY_TIME_LEFT,playTimeLeft);
		obj.addProperty(COOLDOWN_START_TIME,cooldownStartTime);
		return obj;
	}

	@Override
	public void deserialise(JsonObject obj) throws DeserialiseException
	{
		enabled = Deserialise.assertAndGetProperty(ENABLED,Deserialise.Type.BOOLEAN,obj).getAsBoolean();
		maxCooldownTime = Deserialise.assertAndGetProperty(MAX_COOLDOWN_TIME,Deserialise.Type.NUMBER,obj).getAsLong();
		maxPlayTime = Deserialise.assertAndGetProperty(MAX_PLAY_TIME,Deserialise.Type.NUMBER,obj).getAsLong();
		playTimeLeft = Deserialise.assertAndGetProperty(PLAY_TIME_LEFT,Deserialise.Type.NUMBER,obj).getAsLong();
		cooldownStartTime = Deserialise.assertAndGetProperty(COOLDOWN_START_TIME,Deserialise.Type.NUMBER,obj).getAsLong();
		
		maxPlayTime = MathUtils.clamp(1000,Integer.MAX_VALUE,maxPlayTime);
		playTimeLeft = MathUtils.clamp(0,maxPlayTime,playTimeLeft);
		maxCooldownTime = MathUtils.clamp(1000,Integer.MAX_VALUE,maxPlayTime);
	}
	
	private class TimeLimitClock extends LimitedCyclesClock
	{
		public TimeLimitClock(int cycles)
		{
			super("TimeLimitClock",20,cycles);
		}

		@Override
		public void execute() throws Exception
		{
			BossBar bar = getDisplayBar();
			playTimeLeft -= 1000;
			double progress = ((double) playTimeLeft) / ((double) maxPlayTime);
			progress = MathUtils.clamp(0.0d,1.0d,progress);
			bar.setProgress(progress);
			
			if(progress > 0.4d)
			{
				bar.setColor(BarColor.GREEN);
				bar.setTitle(ColourUtils.transCol("&bPlay Time Left &8[&a" + TimeUtils.millisecondsToHoursMinutesSeconds(playTimeLeft) + "&8]"));
				return;
			}
			
			if(progress < 0.2d)
			{
				bar.setColor(BarColor.RED);
				bar.setTitle(ColourUtils.transCol("&bPlay Time Left &8[&4" + TimeUtils.millisecondsToHoursMinutesSeconds(playTimeLeft) + "&8]"));
				return;
			}
			
			if(progress < 0.4d)
			{
				bar.setColor(BarColor.YELLOW);
				bar.setTitle(ColourUtils.transCol("&bPlay Time Left &8[&e" + TimeUtils.millisecondsToHoursMinutesSeconds(playTimeLeft) + "&8]"));
				return;
			}
		}

		@Override
		public void finalExecute() throws Exception
		{
			cooldownStartTime = System.currentTimeMillis();
			
			VirtualPlayerDataCtrl.logout(parent.getActingPlayer());
		}
		
	}
}