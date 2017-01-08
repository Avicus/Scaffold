package net.avicus.scaffold;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@ToString(exclude = {"folder", "configFile"})
public class ScaffoldWorld {
    @Getter private final String name;
    @Getter private final String worldName;
    @Getter private final File folder;
    private final File configFile;

    public ScaffoldWorld(String name) {
        this.name = name;
        this.worldName = "scaffold/" + this.name;
        this.folder = new File(this.worldName);
        this.configFile = new File(this.folder, "scaffold.yml");
    }

    public Optional<World> getWorld() {
        return Optional.ofNullable(Bukkit.getWorld(this.worldName));
    }

    public Optional<Configuration> getConfig() {
        try {
            if (this.configFile.exists())
                return Optional.of(YamlConfiguration.loadConfiguration(this.configFile));
            return Optional.empty();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public boolean isOpen() {
        return getWorld().isPresent();
    }

    public boolean isCreated() {
        return this.folder.exists() && new File(this.folder, "level.dat").exists();
    }

    public World create(WorldType type, Environment env, long seed) throws IOException {
        Preconditions.checkArgument(!isOpen(), "World already loaded.");
        Preconditions.checkArgument(!isCreated(), "World already created.");

        YamlConfiguration config = new YamlConfiguration();
        config.set("type", type.name());
        config.set("environment", env.name());
        config.set("seed", seed);

        WorldCreator creator = worldCreator(Optional.of(config));
        World world = creator.createWorld();
        world.setSpawnLocation(0, 3, 0);
        world.setAutoSave(true);
        world.save();

        Vector min = new Vector(-1, 0, -1);
        Vector max = new Vector(1, 0, 1);

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++)
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++)
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++)
                    world.getBlockAt(x, y, z).setType(Material.GLASS);

        config.save(this.configFile);

        return world;
    }

    public World load() {
        Preconditions.checkArgument(!isOpen(), "World already loaded.");
        Preconditions.checkArgument(isCreated(), "World is not created.");
        deleteUid();
        Optional<Configuration> config = getConfig();
        WorldCreator creator = worldCreator(config);
        World world = creator.createWorld();
        world.setAutoSave(true);
        return world;
    }

    private void deleteUid() {
        try {
            FileUtils.forceDelete(new File(this.folder, "uid.dat"));
        } catch (Exception e) {
            // meh...
        }
    }

    public boolean unload() {
        Preconditions.checkArgument(isOpen(), "World is not loaded.");
        Preconditions.checkArgument(isCreated(), "World is not created.");

        World world = getWorld().get();
        world.save();
        return Bukkit.unloadWorld(world, true);
    }

    private WorldCreator worldCreator(Optional<Configuration> config) {
        WorldCreator creator = new WorldCreator(this.worldName);
        creator.generator(new NullChunkGenerator());
        if (config.isPresent()) {
            WorldType type = WorldType.valueOf(config.get().getString("type").toUpperCase());
            Environment environment =  Environment.valueOf(config.get().getString("environment").toUpperCase());
            long seed = config.get().getLong("seed");

            creator.type(type);
            creator.environment(environment);
            creator.seed(seed);
        }
        return creator;
    }

    public static Optional<ScaffoldWorld> ofWorld(World world) {
        if (!world.getName().startsWith("scaffold/"))
            return Optional.empty();

        return Optional.of(new ScaffoldWorld(world.getName().replace("scaffold/", "")));
    }

    public static ScaffoldWorld ofSearch(String query) {
        File[] files = new File("scaffold").listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().equalsIgnoreCase(query)) {
                    return new ScaffoldWorld(file.getName());
                }
            }
        }
        return new ScaffoldWorld(query);
    }
}
