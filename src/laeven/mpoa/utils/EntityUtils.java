package laeven.mpoa.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public class EntityUtils
{
	public static Set<EntityType> LIVING_ENTITIES = new HashSet<>();
	
	public static void sortEntities()
	{
		for(EntityType type : EntityType.values())
		{
			Class<? extends Entity> entityClass = type.getEntityClass();
			
			if(getAllExtendedOrImplementedInterfacesRecursively(entityClass).contains(LivingEntity.class))
			{
				LIVING_ENTITIES.add(type);
			}
		}
	}
	
	private static Set<Class<?>> getAllExtendedOrImplementedInterfacesRecursively(Class<?> clazz)
	{
		if(clazz == null) { return Set.of(); }
		
	    Set<Class<?>> res = new HashSet<Class<?>>();
	    Class<?>[] interfaces = clazz.getInterfaces();

	    if(interfaces.length > 0)
	    {
	        res.addAll(Arrays.asList(interfaces));

	        for (Class<?> interfaze : interfaces)
	        {
	            res.addAll(getAllExtendedOrImplementedInterfacesRecursively(interfaze));
	        }
	    }
	    
	    return res;
	}
}
