package laeven.mpoa.loginworld;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;

public class SingleBiomeProvider extends BiomeProvider
{
	private final Biome biome;
	
	public SingleBiomeProvider(Biome biome)
	{
		Objects.requireNonNull(biome,"Biome cannot be null!");
		this.biome = biome;
	}

	@Override
	public Biome getBiome(WorldInfo worldInfo, int x, int y, int z)
	{
		return biome;
	}

	@Override
	public List<Biome> getBiomes(WorldInfo worldInfo)
	{
		return Collections.singletonList(biome);
	}
}