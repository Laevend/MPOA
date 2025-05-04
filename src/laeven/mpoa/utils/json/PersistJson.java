package laeven.mpoa.utils.json;

import com.google.gson.JsonObject;

import laeven.mpoa.exception.DeserialiseException;
import laeven.mpoa.exception.SerialiseException;

public interface PersistJson
{
	public JsonObject serialise() throws SerialiseException;
	
	public void deserialise(JsonObject obj) throws DeserialiseException;
}
