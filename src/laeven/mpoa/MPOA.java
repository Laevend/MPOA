package laeven.mpoa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import laeven.mpoa.commands.AccountsCommand;
import laeven.mpoa.commands.ChangePasswordCommand;
import laeven.mpoa.commands.LoginCommand;
import laeven.mpoa.commands.LogoutCommand;
import laeven.mpoa.config.Configurable;
import laeven.mpoa.config.Configure;
import laeven.mpoa.config.YamlConfig;
import laeven.mpoa.extras.EmulatedPearls;
import laeven.mpoa.extras.EmulatedPetOwnership;
import laeven.mpoa.gui.LoginInterface;
import laeven.mpoa.gui.PatternSetInterface;
import laeven.mpoa.gui.ResetPatternPasswordInterface;
import laeven.mpoa.loginworld.LoginWorldCtrl;
import laeven.mpoa.utils.AttributeUtils;
import laeven.mpoa.utils.ColourUtils;
import laeven.mpoa.utils.EntityUtils;
import laeven.mpoa.utils.Logg;
import laeven.mpoa.utils.MaterialUtils;
import laeven.mpoa.utils.WorldUtils;
import laeven.mpoa.utils.data.DataUtils;
import laeven.mpoa.utils.structs.Namespace;
import laeven.mpoa.utils.structs.UID4;
import laeven.mpoa.utils.tools.ClasspathCollector;
import laeven.mpoa.virtualplayerdata.VirtualPlayerDataCtrl;
import laeven.mpoa.virtualplayerdata.data.VirtualPlayerData;

/**
 * A subset of Dape infrastructure to create a plugin that allows one physical account
 * to be used as many virtual accounts
 */
public class MPOA extends JavaPlugin implements Listener
{
	private static MPOA INSTANCE;
	private static Path MPOA_PLUGIN_PATH;
	private static final String NAMESPACE_NAME = "MPOA";
	private static final String DT_PLUGIN_MANAGED_ENTITY = "plugin_managed_entity";
	private static YamlConfig config = null;
	
	private static long initStartTime = 0;
	private static long initEndTime = 0;
	
	/* =================== *
	 *   Pre-Bukkit Load
	 * =================== */
	
	@Override
	public void onEnable()
	{
		initStartTime = System.currentTimeMillis();
		INSTANCE = this;
		MPOA_PLUGIN_PATH = Path.of(getFile().getAbsolutePath());
		createConfig();
		printHeader();
		
		configureLogger();
		
		initEndTime = System.currentTimeMillis();
		
		Logg.info("&f&lMPOA Pre-Bukkit load initialised in " + ((initEndTime - initStartTime) / 1000F) + " seconds");
		
		AccountsCommand accCmd = new AccountsCommand();
		
		this.getCommand("accounts").setExecutor(accCmd);
		this.getCommand("accounts").setTabCompleter(accCmd);
		
		LoginCommand loginCmd = new LoginCommand();
		
		this.getCommand("login").setExecutor(loginCmd);
		this.getCommand("login").setTabCompleter(loginCmd);
		
		LogoutCommand logoutCmd = new LogoutCommand();
		
		this.getCommand("logout").setExecutor(logoutCmd);
		this.getCommand("logout").setTabCompleter(logoutCmd);
		
		ChangePasswordCommand changePasswordCmd = new ChangePasswordCommand();
		
		this.getCommand("change-password").setExecutor(changePasswordCmd);
		this.getCommand("change-password").setTabCompleter(changePasswordCmd);
		
		MaterialUtils.sortItems();
		MaterialUtils.loadPatternCollections();
		EntityUtils.sortEntities();
		VirtualPlayerDataCtrl.loadActingAccounts();
		VirtualPlayerDataCtrl.loadLinkedAccountPlayers();
		VirtualPlayerDataCtrl.loadResetPassword();
		AttributeUtils.collectDefaults();
		
		getServer().getPluginManager().registerEvents(new VirtualPlayerDataCtrl(),this);
		getServer().getPluginManager().registerEvents(new LoginWorldCtrl(),this);
		getServer().getPluginManager().registerEvents(this,this);
		getServer().getPluginManager().registerEvents(new LoginInterface(),this);
		getServer().getPluginManager().registerEvents(new ResetPatternPasswordInterface(),this);
		getServer().getPluginManager().registerEvents(new PatternSetInterface(),this);
		
		getServer().getPluginManager().registerEvents(new EmulatedPearls(),this);
		getServer().getPluginManager().registerEvents(new EmulatedPetOwnership(),this);
	}
	
	@EventHandler
	public void onServerLoad(ServerLoadEvent e)
	{
		LoginWorldCtrl.load();
		
		if(WorldUtils.getDefaultWorldName() == "login_world")
		{
			Logg.fatal("Your level-name in server.properties cannot be 'login_world'!");
			Bukkit.shutdown();
		}
	}
	
	@Override
	public void onDisable()
	{
		for(UID4 loggedInUID : VirtualPlayerDataCtrl.getLoggedInVirtualAccounts().values())
		{
			VirtualPlayerData data = VirtualPlayerDataCtrl.getPlayerData(loggedInUID);
			VirtualPlayerDataCtrl.backup(data);
			VirtualPlayerDataCtrl.savePlayerData(loggedInUID,false);
		}
		
		VirtualPlayerDataCtrl.saveLinkedAccountPlayers();
	}
	
	private void createConfig()
	{
		Map<String,Object> defaults = new HashMap<>();
		
		Logg.title("Collecting Default Configuration Values...");
		
		try
		{
			ClasspathCollector collector = new ClasspathCollector(MPOA_PLUGIN_PATH,MPOA.class.getClassLoader());
			Set<String> configurableClasses = collector.getClasspathsAssignableFrom(Configurable.class);
			
			for(String clazz : configurableClasses)
			{			
				Class<?> configurableClass = Class.forName(clazz,false,MPOA.class.getClassLoader());
				
				for(Method method : configurableClass.getDeclaredMethods())
				{
					// Lambda and switch cases used in command logic is registered as a method
					if(method.getName().contains("$")) { continue; }
					
					// Ignore methods with no path annotation
					if(!method.isAnnotationPresent(Configure.class)) { continue; }
					
					// Static check
					if(!Modifier.isStatic(method.getModifiers()))
					{
						Logg.error("Configurable class " + configurableClass.getSimpleName() + " has a defaults collection method (" + method.getName() + ") that is not static!");
						Logg.Common.printFail(Logg.Common.Component.CONFIG,"Collecting",configurableClass.getSimpleName());
						continue;
					}
					
					// Invalid parameters check
					if(method.getParameters().length != 0)
					{
						Logg.error("Configurable class " + configurableClass.getSimpleName() + " has a defaults collection method (" + method.getName() + ") with too many parameters!");
						Logg.Common.printFail(Logg.Common.Component.CONFIG,"Collecting",configurableClass.getSimpleName());
						continue;
					}
					
					Class<?> methodParam = method.getReturnType();
					
					// Return void type check
					if(methodParam.getName().equals("void"))
					{
						Logg.error("Configurable class " + configurableClass.getSimpleName() + " has a defaults collection method (" + method.getName() + ") with a void return type! Expected Map<String,Object>!");
						Logg.Common.printFail(Logg.Common.Component.CONFIG,"Collecting",configurableClass.getSimpleName());
						continue;
					}
					
					// Return Map type check
					if(!methodParam.getCanonicalName().equals(Map.class.getCanonicalName()))
					{
						Logg.error("Configurable class " + configurableClass.getSimpleName() + " has a defaults collection method (" + method.getName() + ") with an invalid return type! Expected Map<String,Object>, Got " + methodParam.getCanonicalName());
						Logg.Common.printFail(Logg.Common.Component.CONFIG,"Collecting",configurableClass.getSimpleName());
						continue;
					}
					
					// Parameterised check
					try
					{
						@SuppressWarnings("unchecked")
						Map<String,Object> retrievedDefaults = (Map<String, Object>) method.invoke(null);
						defaults.putAll(retrievedDefaults);
						Logg.Common.printOk(Logg.Common.Component.CONFIG,"Collecting",configurableClass.getSimpleName());
					}
					catch(Exception e)
					{
						Logg.error("Configurable class " + configurableClass.getSimpleName() + " has a defaults collection method (" + method.getName() + ") with an invalid return parameterised types! Expected Map<String,Object>");
						Logg.Common.printFail(Logg.Common.Component.CONFIG,"Collecting",configurableClass.getSimpleName());
					}
				}
			}
		}
		catch (Exception e)
		{
			Logg.fatal("Configurables could not be initialised!",e);
		}
		
		config = new YamlConfig(internalFilePath("config.yml"),defaults,"MPOA config file");
	}
	
	private void configureLogger()
	{
		try
		{
			// Register all verbose groups
			for(Field f : Logg.VerbGroup.class.getDeclaredFields())
			{
				Namespace verboseGroup = (Namespace) f.get(null);
				Logg.registerVerboseLogGroup(verboseGroup);
			}
		}
		catch(Exception e)
		{
			Logg.error("Error occured attempting to register logger verbose groups!",e);
		}
		
		Logg.setHideVerbose(config.getBoolean("logger.hide_verbose"));
		Logg.setHideWarnings(config.getBoolean("logger.hide_warnings"));
		Logg.setHideErrors(config.getBoolean("logger.hide_errors"));
		Logg.setHideFatals(config.getBoolean("logger.hide_fatals"));
		Logg.setSilenceExceptions(config.getBoolean("logger.hide_exceptions"));
		
		List<String> enabledVerboseGroups = (List<String>) config.getStringList("logger.verbose.enabled_groups");
		if(enabledVerboseGroups == null) { return; }
		
		for(String groupName : enabledVerboseGroups)
		{
			Logg.setVerboseGroupEnabled(Namespace.fromString(groupName),true);
		}
	}
	
	/**
	 * Used to shutdown the server in times when the server is left in a state that cannot be recovered
	 * Shutting down the server prevents further data degradation and unpredictable server states
	 */
	public static void forceShutdown()
	{
		Logg.error("A fatal error has occured. The server will be forcefully shutdown to prevent further damage.");
		Bukkit.getServer().shutdown();
	}
	
	/**
	 * Used to shutdown the server in times when the server is left in a state that cannot be recovered
	 * Shutting down the server prevents further data degradation and unpredictable server states
	 */
	public static void forceShutdown(String reason)
	{
		Logg.error("The server is being forcefully shutdown. Reason: " + reason);
		Bukkit.getServer().shutdown();
	}
	
	/**
	 * Prints header for MPOA
	 */
	private final void printHeader()
	{
		// String builder necessary to create a single string otherwise the logger prints the time for each line
		StringBuilder sb = new StringBuilder();
		
		sb.append("\r" + String.format("%" + 400 + "s", "") + "\n\n\n");
		
		for(String s : logo)
		{
			sb.append(s);
		}
		
		sb.append("\n\n\n");
		
		Logg.raw(ColourUtils.transCol(sb.toString()));
	}
	
	public static final MPOA instance()
	{
		return INSTANCE;
	}
	
	public static Path featureFilePath(String path)
	{
		Path p = Paths.get(MPOA.instance().getDataFolder().getPath() + File.separator + "feature" + File.separator + path);		
		return p; 
	}
	
	/**
	 * Returns an internal file path for mpoas plugin data folder with an appended directory path
	 * @param path Appended directory path starting from ./plugins//
	 * @return Path of directory or file internal to mpoas plugin data directory
	 */
	public static Path internalFilePath(String path)
	{
		Path p = Paths.get(MPOA.instance().getDataFolder().getPath() + File.separator + path);		
		return p; 
	}
	
	public static final Path getPluginPath()
	{
		return MPOA_PLUGIN_PATH;
	}
	
	public static String getNamespaceName()
	{
		return NAMESPACE_NAME;
	}
	
	public static NamespacedKey getNamespacedKey()
	{
		return new NamespacedKey(MPOA.instance(),getNamespaceName());
	}

	public static int getMajorVersion()
	{
		String major = INSTANCE.getDescription().getVersion().split("[.]")[0];
		return major.length() == 0 ? 0 : Integer.parseInt(major);
	}

	public static int getMinorVersion()
	{
		String minor = INSTANCE.getDescription().getVersion().split("[.]")[1];
		return minor.length() == 0 ? 0 : Integer.parseInt(minor);
	}

	public static int getRevision()
	{
		String patch = INSTANCE.getDescription().getVersion().split("[.]")[2];
		return patch.length() == 0 ? 0 : Integer.parseInt(patch);
	}
	
	public static int getHotfix()
	{
		String hotfix = INSTANCE.getDescription().getVersion().split("[.]")[3];
		return hotfix.length() == 0 ? 0 : Integer.parseInt(hotfix);
	}

	public static String getVersion()
	{
		return INSTANCE.getDescription().getVersion();
	}
	
	public static YamlConfig getConfigFile()
	{
		return config;
	}
	
	@Override
	public void saveConfig()
	{
		config.saveConfig();
	}
	
	public void reloadConfig()
	{
		config.reloadConfig();
	}
	
	@Override
	public void saveDefaultConfig()
	{
		throw new UnsupportedOperationException("The default configuration is not supported!");
	}
	
	@Override
	public FileConfiguration getConfig()
	{
		throw new UnsupportedOperationException("The default configuration is not supported! Please use '.instance().getConfigFile()");
	}
	
	/**
	 * Signs an entity making it identify as plugin managed
	 * @param e Entity
	 */
	public static void setAsPluginManaged(Entity e)
	{
		DataUtils.set(DT_PLUGIN_MANAGED_ENTITY,1,e);
	}
	
	/**
	 * Checks if this entity is managed by this plugin by looking for a data tag
	 * @param e Entity to check
	 * @return True if this a plugin managed entity, false otherwise
	 */
	public static boolean isPluginManaged(Entity e)
	{
		return DataUtils.has(NAMESPACE_NAME + ":" + DT_PLUGIN_MANAGED_ENTITY,e);
	}

//	private final String[] logo = new String[]
//	{
//		"",
//		"",
//		"",
//		"",
//		"",
//		"    &8[ &7M P O A &8]&r\r\n",
//		"",
//		"    &9Version &8> &e" + getDescription().getVersion() + "&r\r\n",
//		"    &9Message Prefix &8> " + Logg.MPOA_PREFIX + "&r\r\n",
//		"    &9Contributors &8> &8[&e" + String.join(",",getDescription().getAuthors()) + "&8]&r\r\n",
//		"    &9Bukkit Ver &8> &e" + Bukkit.getBukkitVersion() + "&r\r\n",
//		"",
//		"",
//		"",
//		"",
//		"",
//	};
	
	private final String[] logo = new String[]
	{
		"",
		"",
		"",
		"",
		"",
		"    &8[ &7M P O A &8]&r\r\n",
		"",
		"    &9Version &8> &e" + getDescription().getVersion() + "&r\r\n",
		"    &9Message Prefix &8> " + Logg.MPOA_PREFIX + "&r\r\n",
		"    &9Contributors &8> &8[&e" + String.join(",",getDescription().getAuthors()) + "&8]&r\r\n",
		"    &9Bukkit Ver &8> &e" + Bukkit.getBukkitVersion() + "&r\r\n",
		"    &c+================================================+\r\n",
		"    &4     P R E - R E L E A S E       B U I L D        \r\n",
		"    &c+================================================+\r\n",
		"",
		"",
	};	
}
