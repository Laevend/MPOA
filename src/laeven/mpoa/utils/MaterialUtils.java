package laeven.mpoa.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;

import laeven.mpoa.MPOA;
import laeven.mpoa.config.Configurable;
import laeven.mpoa.config.Configure;

/**
 * @author Laeven
 * @since 1.0.0
 */
public class MaterialUtils implements Configurable
{
	public static Set<Material> BLOCKS = new HashSet<>();
	public static Set<Material> ITEMS = new HashSet<>();
	
	private static List<Material> matSelectionA = Arrays.asList
	(
		Material.NOTE_BLOCK,
		Material.CHEST,
		Material.ANVIL,
		Material.DISPENSER,
		Material.BREWING_STAND,
		Material.FURNACE,
		Material.ENCHANTING_TABLE,
		Material.CRAFTING_TABLE,
		Material.BEACON
	);
	
	private static List<Material> matSelectionB = Arrays.asList
	(
		Material.OBSIDIAN,
		Material.SPONGE,
		Material.GRASS_BLOCK,
		Material.BEDROCK,
		Material.COBBLESTONE,
		Material.OAK_LOG,
		Material.GRAVEL,
		Material.SAND,
		Material.NETHERRACK
	);
	
	private static List<Material> matSelectionC = Arrays.asList
	(
		Material.CROSSBOW,
		Material.SHIELD,
		Material.IRON_AXE,
		Material.DIAMOND_SWORD,
		Material.BOW,
		Material.STONE_SHOVEL,
		Material.NETHERITE_HOE,
		Material.ARROW,
		Material.GOLDEN_PICKAXE
	);
	
	private static List<Material> matSelectionD = Arrays.asList
	(
		Material.REDSTONE_BLOCK,
		Material.LAPIS_BLOCK,
		Material.AMETHYST_BLOCK,
		Material.IRON_BLOCK,
		Material.GOLD_BLOCK,
		Material.DIAMOND_BLOCK,
		Material.COPPER_BLOCK,
		Material.NETHERITE_BLOCK,
		Material.EMERALD_BLOCK
	);
	
	/**
	 * Sorts materials into blocks and items for quicker saving of statistics
	 */
	public static void sortItems()
	{
		for(Material mat : Material.values())
		{
			if(mat.isBlock())
			{
				BLOCKS.add(mat);
				continue;
			}
			
			if(mat.isItem())
			{
				ITEMS.add(mat);
				continue;
			}
		}
	}
	
	public static void loadPatternCollections()
	{
		List<Material> patternCollection;
		
		Logg.info("Loading login interface material pattern collection A");
		patternCollection = loadPattern("login_interface.material_pattern_collection_a");
		if(patternCollection != null) { matSelectionA = patternCollection; }
		
		Logg.info("Loading login interface material pattern collection B");
		patternCollection = loadPattern("login_interface.material_pattern_collection_b");
		if(patternCollection != null) { matSelectionB = patternCollection; }
		
		Logg.info("Loading login interface material pattern collection C");
		patternCollection = loadPattern("login_interface.material_pattern_collection_c");
		if(patternCollection != null) { matSelectionC = patternCollection; }
		
		Logg.info("Loading login interface material pattern collection D");
		patternCollection = loadPattern("login_interface.material_pattern_collection_d");
		if(patternCollection != null) { matSelectionD = patternCollection; }
	}
	
	private static List<Material> loadPattern(String configKey)
	{
		List<Material> patternCollection = new ArrayList<>();
		
		for(String sMaterial : MPOA.getConfigFile().getStringList(configKey))
		{
			Material mat = null;
			
			try { mat = Material.valueOf(sMaterial); } catch(Exception e)
			{
				Logg.error("Material " + mat.toString() + " is not a valid material!");
				return null;
			}
			
			patternCollection.add(mat);
		}
		
		return patternCollection;
	}
	
	public static List<Material> getMatSelectionA()
	{
		return matSelectionA;
	}

	public static List<Material> getMatSelectionB()
	{
		return matSelectionB;
	}

	public static List<Material> getMatSelectionC()
	{
		return matSelectionC;
	}

	public static List<Material> getMatSelectionD()
	{
		return matSelectionD;
	}

	@Configure
	public static Map<String,Object> getDefaults()
	{
		return Map.of(
				"login_interface.material_pattern_collection_a",
				List.of
				(
					Material.NOTE_BLOCK.toString(),
					Material.CHEST.toString(),
					Material.ANVIL.toString(),
					Material.DISPENSER.toString(),
					Material.BREWING_STAND.toString(),
					Material.FURNACE.toString(),
					Material.ENCHANTING_TABLE.toString(),
					Material.CRAFTING_TABLE.toString(),
					Material.BEACON.toString()
				),
				"login_interface.material_pattern_collection_b",
				List.of
				(
					Material.OBSIDIAN.toString(),
					Material.SPONGE.toString(),
					Material.GRASS_BLOCK.toString(),
					Material.BEDROCK.toString(),
					Material.COBBLESTONE.toString(),
					Material.OAK_LOG.toString(),
					Material.GRAVEL.toString(),
					Material.SAND.toString(),
					Material.NETHERRACK.toString()
				),
				"login_interface.material_pattern_collection_c",
				List.of
				(
					Material.CROSSBOW.toString(),
					Material.SHIELD.toString(),
					Material.IRON_AXE.toString(),
					Material.DIAMOND_SWORD.toString(),
					Material.BOW.toString(),
					Material.STONE_SHOVEL.toString(),
					Material.NETHERITE_HOE.toString(),
					Material.ARROW.toString(),
					Material.GOLDEN_PICKAXE.toString()
				),
				"login_interface.material_pattern_collection_d",
				List.of
				(
					Material.REDSTONE_BLOCK.toString(),
					Material.LAPIS_BLOCK.toString(),
					Material.AMETHYST_BLOCK.toString(),
					Material.IRON_BLOCK.toString(),
					Material.GOLD_BLOCK.toString(),
					Material.DIAMOND_BLOCK.toString(),
					Material.COPPER_BLOCK.toString(),
					Material.NETHERITE_BLOCK.toString(),
					Material.EMERALD_BLOCK.toString()
				));
	}
}
