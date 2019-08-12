package co.lotc.pitautofill;

import co.lotc.core.bukkit.command.Commands;
import co.lotc.core.bukkit.util.Run;
import co.lotc.pitautofill.cmd.PitCommand;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.lordofthecraft.omniscience.api.OmniApi;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PitAutofill extends JavaPlugin {

    public static final String PREFIX = (ChatColor.DARK_GREEN + ""); // The same colouring prefix to be used throughout the plugin.
    public static final String ALT_COLOUR = (ChatColor.YELLOW + "");

    private static PitAutofill instance; // Keeps a static copy of our current instance and an accessor for it.
    public static PitAutofill get() {
        return instance;
    }

    int defaultCooldown;    // Default values for pit settings if unspecified.
    int defaultRefillValue;
    int defaultChanceValue;
    private ArrayList<ResourcePit> allPitsList = new ArrayList<>(); // List of our currently loaded pits.

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.defaultCooldown = getConfig().getInt("default-cooldown-value");
        this.defaultRefillValue = getConfig().getInt("default-refill-value");
        this.defaultChanceValue = getConfig().getInt("default-chance-value");

        init();

        Bukkit.getPluginManager().registerEvents(new FillSignListener(this), this);

        registerParameters();

        Commands.build(getCommand("pit"), PitCommand::new);

        registerOmniEvent();
    }

    @Override
    public void onDisable() { }

    // Register ResourcePit and Worlds as a tab-autocomplete.
    private void registerParameters() {

        Commands.defineArgumentType(ResourcePit.class)
                .defaultName("Pit")
                .defaultError("Failed to find a pit with that name.")
                .completer(() -> allPitsList.stream().map(ResourcePit::getName).collect(Collectors.toList()))
                .mapperWithSender((sender, name) -> getPit(name))
                .register();

        Commands.defineArgumentType(World.class)
                .defaultName("World")
                .defaultError("Failed to find a world by that name.")
                .completer(() -> Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()))
                .mapperWithSender((sender, name) -> Bukkit.getWorld(name))
                .register();
    }

    // Alert Omniscience to a logging type.
    private void registerOmniEvent() {
        if (Bukkit.getPluginManager().isPluginEnabled("Omniscience")) {
            OmniApi.registerEvent("refill", "refilled");
        }
    }

    //// STARTUP ////

    // Iterate through our list of pits, pre-loading all specified info from the config.
    public void init() {
        allPitsList = new ArrayList<>();

        File pitDir = new File(getDataFolder(), "pits");
        if (!pitDir.exists()) {
            pitDir.mkdirs();
        }

        File[] files = pitDir.listFiles(pathname -> pathname.getName().endsWith(".yml"));

        List<String> children = Lists.newArrayList();

        for (File file : files) {
            String pitName = file.getName().replaceAll("\\.yml", "");
            if (allPitsList.stream().anyMatch(pit -> pit.getName().equalsIgnoreCase(pitName))) {
                getLogger().severe("A duplicate pit with the name " + pitName + " was already found");
                continue;
            }
            ResourcePit pit =
                    ResourcePit.fromFile(pitName, file);
            allPitsList.add(pit);
        }

    }

    //// PUBLIC ////

    // Sets the defaultChanceValue.
    public void setDefaultChanceValue(int newValue) {
        defaultChanceValue = newValue;
        Run.as(this).async(() -> {
            this.getConfig().set("default-chance-value", newValue);
            this.saveConfig();
        });
    }

    public void addPit(ResourcePit pit) {
        allPitsList.add(pit);
    }

    public ArrayList<ResourcePit> getPits() {
        return allPitsList;
    }

    // Remove the pit with the same name from our list.
    public String deletePit(ResourcePit thisPit) {
        String output = "";
        if (thisPit != null) {
            allPitsList.remove(thisPit);

            Run.as(this).async(thisPit::delete);

            output = "Successfully deleted the pit '" + PitAutofill.ALT_COLOUR + thisPit.getName().toUpperCase() + PitAutofill.PREFIX + "'.";
        }
        return output;
    }

    // Returns the ResourcePit with the matching name.
    public ResourcePit getPit(String name) {
        if (name == null) {
            return null;
        }
        for (ResourcePit thisPit : allPitsList) {
            if (thisPit.getName().equalsIgnoreCase(name.toUpperCase()))
                return thisPit;
        }
        return null;
    }

    public ProtectedRegion getRegion(String id, World world) {
        if (world == null) {
            return null;
        }
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (manager == null) {
            return null;
        }
        return manager.getRegion(id);
    }

    public File getPitFile(String pitName) {
        return new File(new File(getDataFolder(), "pits"), pitName.toUpperCase() + ".yml");
    }

}