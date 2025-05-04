package laeven.mpoa.loginworld;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import laeven.mpoa.MPOA;
import laeven.mpoa.config.Configurable;
import laeven.mpoa.config.Configure;
import laeven.mpoa.utils.Cooldown;
import laeven.mpoa.utils.Logg;
import laeven.mpoa.utils.MathUtils;
import laeven.mpoa.utils.PrintUtils;
import laeven.mpoa.utils.WorldUtils;
import laeven.mpoa.virtualplayerdata.VirtualPlayerDataCtrl;
import laeven.mpoa.virtualplayerdata.data.DefaultVirtualPlayerData;

public class LoginWorldCtrl implements Listener, Configurable
{	
	private static final UUID movementCooldown = UUID.randomUUID();
	
	private static String worldName = "login_world";
	private static World world;
	private static double spawnX = 0d;
	private static double spawnY = 0d;
	private static double spawnZ = 0d;
	
	private static Location loginWorldSpawn;
	
	private static Set<UUID> waitingAccounts = new HashSet<>();
	
	public static void load()
	{
		String worldNameFromConfig = MPOA.getConfigFile().getString("login_world.name");
		spawnX = MathUtils.clamp(-25_000_000,25_000_000,MPOA.getConfigFile().getDouble("login_world.spawn_x"));
		spawnY = MathUtils.clamp(-25_000_000,25_000_000,MPOA.getConfigFile().getDouble("login_world.spawn_y"));
		spawnZ = MathUtils.clamp(-25_000_000,25_000_000,MPOA.getConfigFile().getDouble("login_world.spawn_z"));
		
		if(worldNameFromConfig == null || worldNameFromConfig.isEmpty() || worldNameFromConfig.isBlank())
		{
			Logg.error("Login world name from config is null, empty, or blank!");
		}
		else
		{
			worldName = worldNameFromConfig;
		}
		
		Path worldPath = Paths.get(WorldUtils.getWorldPath(worldName));
		
		if(!Files.exists(worldPath))
		{
			if(MPOA.getConfigFile().getBoolean("login_world.generate_void_world"))
			{
				generateVoidWorld();
			}
			else
			{
				Logg.error("You have elected to not generate a void login world but have not substituted one either!");
				return;
			}
		}
		else
		{
			world = WorldUtils.createNewWorld(worldName,Environment.THE_END);
		}
		
		if(world == null)
		{
			Logg.error("Error occured attempting to create login world!");
			return;
		}
		
		loginWorldSpawn = new Location(world,spawnX,spawnY,spawnZ,0f,0f);
	}
	
	public static void setWaiting(Player p)
	{
		waitingAccounts.add(p.getUniqueId());
		p.teleport(loginWorldSpawn);
		p.setRespawnLocation(loginWorldSpawn);
		
		// You have to allow someone to fly in spectator? Why? This is bad design.
		p.setAllowFlight(true);
		p.setGameMode(GameMode.SPECTATOR);
		
		DefaultVirtualPlayerData.resetPlayerData(p);
	}
	
	public static void removeWaiting(Player p)
	{
		waitingAccounts.remove(p.getUniqueId());
		p.setRespawnLocation(null);
		p.setAllowFlight(false);
		p.setGameMode(GameMode.SURVIVAL);
	}
	
	@EventHandler
	public void onMove(PlayerMoveEvent e)
	{
		if(Cooldown.isCooling(e.getPlayer(),movementCooldown,true)) { return; }
		if(!waitingAccounts.contains(e.getPlayer().getUniqueId())) { return; }
		
		Player p = e.getPlayer();
		p.teleport(loginWorldSpawn);
		
		// You have to allow someone to fly in spectator? Why? This is bad design.
		p.setAllowFlight(true);
		p.setGameMode(GameMode.SPECTATOR);
		PrintUtils.sendTitle(p,"","&eLogin to a virtual account to continue.");
		
		Cooldown.setCooldown(e.getPlayer(),movementCooldown,500);
	}
	
	@EventHandler(priority = EventPriority.LOWEST,ignoreCancelled = false)
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		if(!VirtualPlayerDataCtrl.getActingAccounts().contains(e.getPlayer().getUniqueId())) { return; }
		setWaiting(e.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST,ignoreCancelled = false)
	public void onPlayerQuit(PlayerQuitEvent e)
	{
		if(!VirtualPlayerDataCtrl.getActingAccounts().contains(e.getPlayer().getUniqueId())) { return; }
		removeWaiting(e.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST,ignoreCancelled = false)
	public void onSpawn(CreatureSpawnEvent e)
	{
		if(!e.getLocation().getWorld().getName().equals(worldName)) { return; }
		e.getEntity().remove();
	}
	
	private static void generateVoidWorld()
	{
		Path worldPath = Paths.get(WorldUtils.getWorldPath(worldName));
		
		if(Files.exists(worldPath))
		{
			Logg.error("Cannot generate new void world for login word because it already exists!");
			return;
		}
		
		world = new WorldCreator(worldName).environment(Environment.NORMAL).type(WorldType.FLAT).generateStructures(false).generator(new VoidChunkGenerator()).biomeProvider(new SingleBiomeProvider(Biome.THE_END)).createWorld();
		world.setAutoSave(false);
		world.getWorldBorder().setCenter(spawnX,spawnY);
		world.getWorldBorder().setSize(1024);
		world.setSpawnLocation(new Location(world,spawnX,spawnY,spawnZ));
		world.setTime(6000);
		world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN,true);
		world.setGameRule(GameRule.KEEP_INVENTORY,false);
		world.setGameRule(GameRule.FALL_DAMAGE,false);
		world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS,false);
		world.setGameRule(GameRule.DO_FIRE_TICK,false);
		world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);
		world.setGameRule(GameRule.DO_WEATHER_CYCLE,false);
		world.setGameRule(GameRule.DO_TILE_DROPS,false);
		world.setGameRule(GameRule.DO_TRADER_SPAWNING,false);
		world.setGameRule(GameRule.RANDOM_TICK_SPEED,0);
		world.setGameRule(GameRule.DISABLE_RAIDS,true);
		world.setGameRule(GameRule.FIRE_DAMAGE,false);
		world.setGameRule(GameRule.DROWNING_DAMAGE,false);
		world.setGameRule(GameRule.DISABLE_PLAYER_MOVEMENT_CHECK,true);
		world.setGameRule(GameRule.DO_INSOMNIA,false);
		world.setGameRule(GameRule.DO_MOB_SPAWNING,false);
		world.setGameRule(GameRule.DO_ENTITY_DROPS,false);
		world.setGameRule(GameRule.DO_MOB_LOOT,false);
		world.setGameRule(GameRule.DO_PATROL_SPAWNING,false);
		world.setGameRule(GameRule.DO_VINES_SPREAD,false);
		world.setGameRule(GameRule.DO_WARDEN_SPAWNING,false);
		world.setGameRule(GameRule.FREEZE_DAMAGE,false);
		world.setGameRule(GameRule.SPAWN_CHUNK_RADIUS,1);
		world.setGameRule(GameRule.TNT_EXPLODES,false);
		world.setGameRule(GameRule.SHOW_DEATH_MESSAGES,false);
		world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS,true);
	}

	public static String getWorldName()
	{
		return worldName;
	}

	public static World getWorld()
	{
		return world;
	}

	public static double getSpawnX()
	{
		return spawnX;
	}

	public static double getSpawnY()
	{
		return spawnY;
	}

	public static double getSpawnZ()
	{
		return spawnZ;
	}

	public static Location getLoginWorldSpawn()
	{
		return loginWorldSpawn;
	}

	public static Set<UUID> getWaitingAccounts()
	{
		return waitingAccounts;
	}

	@Configure
	public static Map<String,Object> getDefaults()
	{
		return Map.of("login_world.name","login_world",
				  "login_world.generate_void_world",true,
				  "login_world.spawn_x",0.5d,
				  "login_world.spawn_y",-70d,
				  "login_world.spawn_z",0.5d);
	}
}
