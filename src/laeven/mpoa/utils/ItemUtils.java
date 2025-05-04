package laeven.mpoa.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import laeven.mpoa.utils.data.DataUtils;

/**
 * @author Laeven
 */
public class ItemUtils
{
	private static final ItemStack ERROR_ITEMSTACK = new ItemStack(Material.GREEN_CONCRETE);
	
	static
	{
		ItemMeta meta = ERROR_ITEMSTACK.getItemMeta();
		meta.setDisplayName(ColourUtils.transCol("&aGeneric Object"));
		ERROR_ITEMSTACK.setItemMeta(meta);
	}
	
	public static void setName(String name,ItemStack stack)
	{
		ItemMeta meta = stack.getItemMeta();
		meta.setDisplayName(ColourUtils.transCol(name));
		stack.setItemMeta(meta);
	}
	
	public static void setLore(List<String> lore,ItemStack stack)
	{
		ItemMeta meta = stack.getItemMeta();
		meta.setLore(lore);
		stack.setItemMeta(meta);
	}
	
	/**
	 * Checks if this item stack is null or is air
	 * @param stack The item stack
	 * @return Is this item stack empty?
	 */
	public static boolean isNullOrAir(ItemStack stack)
	{
		return stack == null || stack.getType().equals(Material.AIR) ? true : false;
	}
	
	/**
	 * Serialise an ItemStack
	 * @param item The itemstack
	 * @return BASE64 Encoded string
	 * @throws IllegalStateException
	 */
	public static String toBase64(ItemStack item)
	{
		try(ByteArrayOutputStream baos = new ByteArrayOutputStream(); BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos))
		{			
	        boos.writeObject(item);
	        boos.close();
	        baos.close();
	        return Base64.getEncoder().encodeToString(baos.toByteArray());
	    } 
		catch (Exception e)
		{
			Logg.error("Vertex could not encode base64 itemstack!",e);
			return null;
	    }
	}
	
	/**
	 * deserialise an ItemStack
	 * @param encodedItem BASE64 String
	 * @return ItemStack
	 * @throws IllegalStateException
	 */
	public static ItemStack fromBase64(String encodedItem)
	{
		byte [] data = Base64.getDecoder().decode(encodedItem);
		
		try(ByteArrayInputStream bais = new ByteArrayInputStream(data); BukkitObjectInputStream bois = new BukkitObjectInputStream(bais))
		{			
			ItemStack stack = (ItemStack) bois.readObject();
	        bois.close();
	        bais.close();
	        return stack;
		}
		catch(Exception e)
		{
			Logg.error("Could not decode base64 itemstack!",e);
			return ERROR_ITEMSTACK.clone();
		}
    }
	
	/**
	 * Checks if this ItemStack was the result of an error.
	 * <p>
	 * When an operation goes wrong that involves setting an ItemStack, a fallback generic item is used instead
	 * @param stack ItemStack to check
	 * @return True if this ItemStack is an error item, false otherwise
	 */
	public static boolean isErrorItemStack(ItemStack stack)
	{
		return DataUtils.has("error_item",stack);
	}
}
