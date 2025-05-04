package laeven.mpoa.exception;

/**
 * @author Laeven
 * @since 1.0.0
 */
public class MissingJsonAttributeException extends Exception
{
	private static final long serialVersionUID = 2473482744940397751L;
	
	public MissingJsonAttributeException(String reader,String jsonName,String attribute)
	{
        super("Json Reader '" + reader + "' attempting to parse '" + jsonName + "' is missing mandatory attribute: " + attribute);
    }
}
