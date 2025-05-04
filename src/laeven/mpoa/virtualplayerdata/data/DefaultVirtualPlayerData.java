package laeven.mpoa.virtualplayerdata.data;

import java.util.Iterator;

import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import laeven.mpoa.utils.AttributeUtils;
import laeven.mpoa.utils.Logg;

public class DefaultVirtualPlayerData
{
	private static final int expLevel = 0;
	private static final float expProgress = 0.0f;
	private static final boolean allowFlight = false;
	private static final float flySpeed = 0.1f;
	private static final boolean isHealthScaled = false;
	private static final double healthScale = 20.0d;
	private static final double health = 20.0d;
	private static final float exhaustion = 0.0f;
	private static final int starvationRate = 80;
	private static final int foodLevel = 20;
	private static final float walkSpeed = 0.2f;
	private static final int fireTicks = -20;
	private static final float fallDistance = 0.0f;
	private static final boolean isGliding = false;
	private static final boolean isGlowing = false;
	private static final int freezeTicks = 0;
	private static final int maxNoDamageTicks = 20;
	private static final int noDamageTicks = 0;
	private static final boolean isSwimming = false;
	private static final Vector velocity = new Vector(0.0d,-0.0784000015258789d,0.0d);
	private static final boolean hasVisualFire = false;
	private static final int maxAir = 300;
	private static final int remainingAir = 300;
	
	@SuppressWarnings("deprecation")
	public static void resetPlayerData(Player p)
	{
		for(Iterator<Attribute> it = Registry.ATTRIBUTE.iterator(); it.hasNext();)
		{
			Attribute attribute = it.next();
			AttributeInstance attIns = p.getAttribute(attribute);
			
			// If attribute is not applicable to player then it will be null
			if(attIns == null) { continue; }
			
			for(AttributeModifier mod : attIns.getModifiers())
			{
				attIns.removeModifier(mod);
			}
			
			double base = AttributeUtils.getDefaultValue(attribute.getKey().getKey());
			
			if(base == -1d)
			{
				Logg.warn("Could not reset Attribute " + attribute.getKey().getKey() + "!");
			}
			
			attIns.setBaseValue(base);
		}
		
		p.setLevel(expLevel);
		p.setExp(expProgress);
		p.setAllowFlight(allowFlight);
		p.setFlySpeed(flySpeed);
		p.setHealthScaled(isHealthScaled);
		p.setHealthScale(healthScale);
		p.setHealth(health);
		p.setExhaustion(exhaustion);
		p.setStarvationRate(starvationRate);
		p.setFoodLevel(foodLevel);
		p.setRespawnLocation(null,true);
		p.setWalkSpeed(walkSpeed);
		p.setFireTicks(fireTicks);
		p.setFallDistance(fallDistance);
		p.setGliding(isGliding);
		p.setGlowing(isGlowing);
		p.setFreezeTicks(freezeTicks);
		p.setMaximumNoDamageTicks(maxNoDamageTicks);
		p.setNoDamageTicks(noDamageTicks);
		p.setSwimming(isSwimming);
		p.setVelocity(velocity);
		p.setVisualFire(hasVisualFire);
		p.setMaximumAir(maxAir);
		p.setRemainingAir(remainingAir);
		
		p.getInventory().clear();
		p.getEnderChest().clear();
		
		/**
		 * Not bothering to reset advancements or statistics here since they have no effect on an account waiting in the login world.
		 * They're changed when a player logs in anyway
		 */
	}
}
