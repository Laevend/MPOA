package laeven.mpoa.utils.clocks;

import org.bukkit.scheduler.BukkitRunnable;

import laeven.mpoa.MPOA;
import laeven.mpoa.utils.Logg;

/**
 * 
 * @author Laeven
 * A clock that will call {@link #execute()} after a specified interval for a number of predetermined cycles.
 * <p>After which, {@link #finalExecute()} is called and the clock stops.
 */
public abstract class LimitedCyclesClock extends BukkitClock
{
	// The the max number of cycles before stopping
	private long cyclesLeft;
	
	/**
	 * Creates a repeating clock
	 * @param clockName Name of clock
	 * @param intervalInTicks Interval before each cycle when {@link #execute()} is called
	 * @param cycles Number of cycles before {@link #finalExecute()} is called
	 */
	public LimitedCyclesClock(String clockName,long durationInTicks,int cycles)
	{
		super(clockName,durationInTicks);
		this.cyclesLeft = cycles;
	}
	
	@Override
	protected void run()
	{
		clock = new BukkitRunnable()
		{
			@Override
			public void run() 
		    {
				if(clock.isCancelled()) { return; }
				
				if(attempts > continueAttempts)
		    	{
		    		cancel();
		    		Logg.fatal("Clock " + clockName + " was canceled due to failing > " + continueAttempts + " times");
		    		return;
		    	}
				
				if(cyclesLeft <= 0)
				{
					try
			    	{
						finalExecute();
			    	}
			    	catch(Exception e)
			    	{
			    		Logg.error(clockName + " tripped and threw an exception!",e);
			    	}
					
					cancel();
					return;
				}
				else
				{
					cyclesLeft--;
				}
				
				try
		    	{
		    		execute();
		    		attempts = 1;
		    	}
		    	catch(Exception e)
		    	{
		    		Logg.error(clockName + " tripped and threw an exception!",e);
		    		attempts++;
		    	}
		    }
		}.runTaskTimer(MPOA.instance(),0L,interval);
	}
	
	public long getCyclesLeft()
	{
		return cyclesLeft;
	}

	public abstract void execute() throws Exception;
	
	public abstract void finalExecute() throws Exception;
}