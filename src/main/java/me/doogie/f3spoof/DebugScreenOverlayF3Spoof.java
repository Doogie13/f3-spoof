package me.doogie.f3spoof;

import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixUtils;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.Connection;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.feature.module.IModule;
import org.rusherhack.client.api.feature.module.ToggleableModule;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Doogie13
 * @since 15/07/2024
 */
public class DebugScreenOverlayF3Spoof extends DebugScreenOverlay {
    
    public DebugScreenOverlayF3Spoof(Minecraft minecraft) {
        super(minecraft);
        this.minecraft = minecraft;
    }
    
    private final Minecraft minecraft;

    protected @NotNull List<String> getGameInformation() {

        // don't use any other classes so this continues to work without the rest of the plugin
        Optional<IModule> f3Spoof1 = RusherHackAPI.getModuleManager().getFeature("F3Spoof");
        if (f3Spoof1.isEmpty() || !((ToggleableModule) f3Spoof1.get()).isToggled())
            return super.getGameInformation();

        PostChain postChain;
        Level level;
        String string3;
        IntegratedServer integratedServer = this.minecraft.getSingleplayerServer();
        ClientPacketListener clientPacketListener = this.minecraft.getConnection();
        Connection connection = clientPacketListener.getConnection();
        float f = connection.getAverageSentPackets();
        float g = connection.getAverageReceivedPackets();
        TickRateManager tickRateManager = this.getLevel().tickRateManager();
        String string = tickRateManager.isSteppingForward() ? " (frozen - stepping)" : (tickRateManager.isFrozen() ? " (frozen)" : "");
        if (integratedServer != null) {
            ServerTickRateManager serverTickRateManager = integratedServer.tickRateManager();
            boolean bl = serverTickRateManager.isSprinting();
            if (bl) {
                string = " (sprinting)";
            }
            String string2 = bl ? "-" : String.format(Locale.ROOT, "%.1f", Float.valueOf(tickRateManager.millisecondsPerTick()));
            string3 = String.format(Locale.ROOT, "Integrated server @ %.1f/%s ms%s, %.0f tx, %.0f rx", Float.valueOf(integratedServer.getCurrentSmoothedTickTime()), string2, string, Float.valueOf(f), Float.valueOf(g));
        } else {
            string3 = String.format(Locale.ROOT, "\"%s\" server%s, %.0f tx, %.0f rx", clientPacketListener.serverBrand(), string, Float.valueOf(f), Float.valueOf(g));
        }
        BlockPos blockPos = this.minecraft.getCameraEntity().blockPosition();
        if (this.minecraft.showOnlyReducedInfo()) {
            return Lists.newArrayList("Minecraft " + SharedConstants.getCurrentVersion().getName() + " (" + this.minecraft.getLaunchedVersion() + "/" + ClientBrandRetriever.getClientModName() + ")", this.minecraft.fpsString, string3, this.minecraft.levelRenderer.getSectionStatistics(), this.minecraft.levelRenderer.getEntityStatistics(), "P: " + this.minecraft.particleEngine.countParticles() + ". T: " + this.minecraft.level.getEntityCount(), this.minecraft.level.gatherChunkSourceStats(), "", String.format(Locale.ROOT, "Chunk-relative: %d %d %d", blockPos.getX() & 0xF, blockPos.getY() & 0xF, blockPos.getZ() & 0xF));
        }
        Entity entity = this.minecraft.getCameraEntity();
        Direction direction = entity.getDirection();
        String string4 = "Towards [Hidden]";
        ChunkPos chunkPos = new ChunkPos(blockPos);
        LongSets.EmptySet longSet = (level = this.getLevel()) instanceof ServerLevel ? (LongSets.EmptySet) ((ServerLevel)level).getForcedChunks() : LongSets.EMPTY_SET;
        ArrayList<String> list = Lists.newArrayList("Minecraft " + SharedConstants.getCurrentVersion().getName() + " (" + this.minecraft.getLaunchedVersion() + "/" + ClientBrandRetriever.getClientModName() + (String)("release".equalsIgnoreCase(this.minecraft.getVersionType()) ? "" : "/" + this.minecraft.getVersionType()) + ")", this.minecraft.fpsString, string3, this.minecraft.levelRenderer.getSectionStatistics(), this.minecraft.levelRenderer.getEntityStatistics(), "P: " + this.minecraft.particleEngine.countParticles() + ". T: " + this.minecraft.level.getEntityCount(), this.minecraft.level.gatherChunkSourceStats());
        String string5 = this.getServerChunkStats();
        if (string5 != null) {
            list.add(string5);
        }
        list.add(this.minecraft.level.dimension().location() + " FC: " + longSet.size());
        list.add("");
        list.add("XYZ: [Hidden]");
        list.add("Block: [Hidden]");
        list.add("Chunk: [Hidden]");
        list.add("Facing: [Hidden]");
        LevelChunk levelChunk = this.getClientChunk();
        if (levelChunk == null || levelChunk.isEmpty()) {
            list.add("Waiting for chunk...");
        } else {
            int i = this.minecraft.level.getChunkSource().getLightEngine().getRawBrightness(blockPos, 0);
            int j = this.minecraft.level.getBrightness(LightLayer.SKY, blockPos);
            int k = this.minecraft.level.getBrightness(LightLayer.BLOCK, blockPos);
            list.add("Client Light: " + i + " (" + j + " sky, " + k + " block)");
            LevelChunk levelChunk2 = this.getServerChunk();
            StringBuilder stringBuilder = new StringBuilder("CH");
            for (Heightmap.Types types : Heightmap.Types.values()) {
                if (!types.sendToClient()) continue;
                stringBuilder.append(" ").append(HEIGHTMAP_NAMES.get(types)).append(": ").append(levelChunk.getHeight(types, blockPos.getX(), blockPos.getZ()));
            }
            list.add(stringBuilder.toString());
            stringBuilder.setLength(0);
            stringBuilder.append("SH");
            for (Heightmap.Types types : Heightmap.Types.values()) {
                if (!types.keepAfterWorldgen()) continue;
                stringBuilder.append(" ").append(HEIGHTMAP_NAMES.get(types)).append(": ");
                if (levelChunk2 != null) {
                    stringBuilder.append(levelChunk2.getHeight(types, blockPos.getX(), blockPos.getZ()));
                    continue;
                }
                stringBuilder.append("??");
            }
            list.add(stringBuilder.toString());
            if (blockPos.getY() >= this.minecraft.level.getMinBuildHeight() && blockPos.getY() < this.minecraft.level.getMaxBuildHeight()) {
                list.add("Biome: " + printBiome(this.minecraft.level.getBiome(blockPos)));
                if (levelChunk2 != null) {
                    float h = level.getMoonBrightness();
                    long l = levelChunk2.getInhabitedTime();
                    DifficultyInstance difficultyInstance = new DifficultyInstance(level.getDifficulty(), level.getDayTime(), l, h);
                    list.add(String.format(Locale.ROOT, "Local Difficulty: %.2f // %.2f (Day %d)", Float.valueOf(difficultyInstance.getEffectiveDifficulty()), Float.valueOf(difficultyInstance.getSpecialMultiplier()), this.minecraft.level.getDayTime() / 24000L));
                } else {
                    list.add("Local Difficulty: ??");
                }
            }
            if (levelChunk2 != null && levelChunk2.isOldNoiseGeneration()) {
                list.add("Blending: Old");
            }
        }
        ServerLevel serverLevel = this.getServerLevel();
        if (serverLevel != null) {
            ServerChunkCache serverChunkCache = serverLevel.getChunkSource();
            ChunkGenerator chunkGenerator = serverChunkCache.getGenerator();
            RandomState randomState = serverChunkCache.randomState();
            chunkGenerator.addDebugScreenInfo(list, randomState, blockPos);
            Climate.Sampler sampler = randomState.sampler();
            BiomeSource biomeSource = chunkGenerator.getBiomeSource();
            biomeSource.addDebugInfo(list, blockPos, sampler);
            NaturalSpawner.SpawnState spawnState = serverChunkCache.getLastSpawnState();
            if (spawnState != null) {
                Object2IntMap<MobCategory> object2IntMap = spawnState.getMobCategoryCounts();
                int m = spawnState.getSpawnableChunkCount();
                list.add("SC: " + m + ", " + Stream.of(MobCategory.values()).map(mobCategory -> Character.toUpperCase(mobCategory.getName().charAt(0)) + ": " + object2IntMap.getInt(mobCategory)).collect(Collectors.joining(", ")));
            } else {
                list.add("SC: N/A");
            }
        }
        if ((postChain = this.minecraft.gameRenderer.currentEffect()) != null) {
            list.add("Shader: " + postChain.getName());
        }
        list.add(this.minecraft.getSoundManager().getDebugString() + String.format(Locale.ROOT, " (Mood %d%%)", Math.round(this.minecraft.player.getCurrentMood() * 100.0f)));
        return list;
    }

    private static String printBiome(Holder<Biome> biomeHolder) {
        return biomeHolder.unwrap().map(resourceKey -> resourceKey.location().toString(), biome -> "[unregistered " + biome + "]");
    }

    @Nullable
    private ServerLevel getServerLevel() {
        IntegratedServer integratedServer = this.minecraft.getSingleplayerServer();
        if (integratedServer != null) {
            return integratedServer.getLevel(this.minecraft.level.dimension());
        }
        return null;
    }

    @Nullable
    private String getServerChunkStats() {
        ServerLevel serverLevel = this.getServerLevel();
        if (serverLevel != null) {
            return serverLevel.gatherChunkSourceStats();
        }
        return null;
    }

    private Level getLevel() {
        return DataFixUtils.orElse(Optional.ofNullable(this.minecraft.getSingleplayerServer()).flatMap(integratedServer -> Optional.ofNullable(integratedServer.getLevel(this.minecraft.level.dimension()))), this.minecraft.level);
    }

    @Nullable
    private LevelChunk getServerChunk() {
        CompletableFuture<LevelChunk> serverChunk = null;

        Optional<Field> any = Arrays.stream(DebugScreenOverlay.class.getDeclaredFields())
                .filter(it -> {
                    try {
                        it.setAccessible(true);
                        return CompletableFuture.class.isInstance(it.get(this));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }).findAny();
        if (any.isEmpty())
            return null;
        Field field = any
                .get();

        try {
            field.setAccessible(true);
            serverChunk = (CompletableFuture<LevelChunk>) field.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        ChunkPos lastPos = null;

        Optional<Field> any1 = Arrays.stream(DebugScreenOverlay.class.getDeclaredFields())
                .filter(it -> {
                    try {
                        it.setAccessible(true);
                        return ChunkPos.class.isInstance(it.get(this));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }).findAny();
        if (any1.isEmpty())
            return null;
        field = any1
                .get();

        try {
            field.setAccessible(true);
            lastPos = (ChunkPos) field.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        if (serverChunk == null) {
            ServerLevel serverLevel = this.getServerLevel();
            if (serverLevel == null) {
                return null;
            }
            serverChunk = serverLevel.getChunkSource().getChunkFuture(lastPos.x, lastPos.z, ChunkStatus.FULL, false).thenApply(either -> either.map(chunkAccess -> (LevelChunk)chunkAccess, chunkLoadingFailure -> null));
        }
        return serverChunk.getNow(null);
    }

    private LevelChunk getClientChunk() {

        LevelChunk clientChunk = null;

        Optional<Field> any = Arrays.stream(DebugScreenOverlay.class.getDeclaredFields())
                .filter(it -> {
                    try {
                        it.setAccessible(true);
                        return LevelChunk.class.isInstance(it.get(this));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }).findAny();
        if (any.isEmpty())
            return null;
        Field field = any
                .get();

        try {
            field.setAccessible(true);
            clientChunk = (LevelChunk) field.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        ChunkPos lastPos = null;

        Optional<Field> any1 = Arrays.stream(DebugScreenOverlay.class.getDeclaredFields())
                .filter(it -> {
                    try {
                        it.setAccessible(true);
                        return ChunkPos.class.isInstance(it.get(this));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }).findAny();
        if (any1.isEmpty())
            return null;
        field = any1
                .get();

        try {
            field.setAccessible(true);
            lastPos = (ChunkPos) field.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        if (clientChunk == null) {
            clientChunk = this.minecraft.level.getChunk(lastPos.x, lastPos.z);
        }
        return clientChunk;
    }

    private static final Map<Heightmap.Types, String> HEIGHTMAP_NAMES = Util.make(new EnumMap(Heightmap.Types.class), enumMap -> {
        enumMap.put(Heightmap.Types.WORLD_SURFACE_WG, "SW");
        enumMap.put(Heightmap.Types.WORLD_SURFACE, "S");
        enumMap.put(Heightmap.Types.OCEAN_FLOOR_WG, "OW");
        enumMap.put(Heightmap.Types.OCEAN_FLOOR, "O");
        enumMap.put(Heightmap.Types.MOTION_BLOCKING, "M");
        enumMap.put(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, "ML");
    });
    
}
