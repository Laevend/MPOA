package laeven.mpoa.utils.data;

import java.util.UUID;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import laeven.mpoa.MPOA;

public class DataContainerValue
{
	private NamespacedKey key;
	private PersistentDataContainer container;
	
	public DataContainerValue(String key,PersistentDataContainer container)
	{
		this.key = new NamespacedKey(MPOA.instance(),key);
		this.container = container;
	}
	
	public boolean asBoolean()
	{
		return (boolean) container.get(key,PersistentDataType.BOOLEAN);
	}
	
	public byte asByte()
	{
		return (byte) container.get(key,PersistentDataType.BYTE);
	}
	
	public byte[] asByteArray()
	{
		return (byte[]) container.get(key,PersistentDataType.BYTE_ARRAY);
	}
	
	public double asDouble()
	{
		return (double) container.get(key,PersistentDataType.DOUBLE);
	}
	
	public float asFloat()
	{
		return (float) container.get(key,PersistentDataType.FLOAT);
	}
	
	public int asInt()
	{
		return (int) container.get(key,PersistentDataType.INTEGER);
	}
	
	public int[] asIntArray()
	{
		return (int[]) container.get(key,PersistentDataType.INTEGER_ARRAY);
	}
	
	public long asLong()
	{
		return (long) container.get(key,PersistentDataType.LONG);
	}
	
	public long[] asLongArray()
	{
		return (long[]) container.get(key,PersistentDataType.LONG_ARRAY);
	}
	
	public short asShort()
	{
		return (short) container.get(key,PersistentDataType.SHORT);
	}
	
	public String asString()
	{
		return (String) container.get(key,PersistentDataType.STRING);
	}
	
	public UUID asUUID()
	{
		return UUID.fromString(asString());
	}
	
	public JsonObject asJson()
	{
		return JsonParser.parseString(asString()).getAsJsonObject();
	}
}
