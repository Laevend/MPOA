package laeven.mpoa.exception;

/**
 * @author Laeven
 * @since 1.0.0
 */
public class InvalidFileException extends Exception
{
	private static final long serialVersionUID = 940542356215937181L;

	public InvalidFileException(String errorMessage,Throwable err)
	{
        super(errorMessage,err);
    }
	
	public InvalidFileException(String errorMessage)
	{
        super(errorMessage);
    }
}
