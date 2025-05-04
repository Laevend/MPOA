package laeven.mpoa.loginworld;

import java.util.List;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

public class VoidChunkGenerator extends ChunkGenerator
{
	@Override
	public void generateNoise(WorldInfo info, Random random, int chunkX, int chunkZ, ChunkData chunkAccess) {}
	
	@Override
	public void generateSurface(WorldInfo info, Random random, int chunkX, int chunkZ, ChunkData chunkAccess) {}

	@Override
	public void generateBedrock(WorldInfo info, Random random, int chunkX, int chunkZ, ChunkData chunkAccess) {}

	@Override
	public void generateCaves(WorldInfo info, Random random, int chunkX, int chunkZ, ChunkData chunkAccess) {}
	
	@Override
    public List<BlockPopulator> getDefaultPopulators(World world)
	{
        return List.of(new BlankBlockPopulator());
    }
	
	public boolean shouldGenerateNoise() { return false; }
	public boolean shouldGenerateSurface() { return false; }
	public boolean shouldGenerateBedrock() { return false; }
	public boolean shouldGenerateCaves() { return false; }
	public boolean shouldGenerateDecorations() { return false; }
	public boolean shouldGenerateMobs() { return false; }
	public boolean shouldGenerateStructures() { return false; }
}
