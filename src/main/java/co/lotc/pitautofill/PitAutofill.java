package co.lotc.pitautofill;

import co.lotc.core.bukkit.command.Commands;
import co.lotc.core.bukkit.util.Run;
import co.lotc.pitautofill.cmd.PitCommand;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.lordofthecraft.omniscience.api.OmniApi;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
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
    boolean ignoreWater;
    boolean ignoreLava;
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
        this.ignoreWater = getConfig().getBoolean("ignore-water");
        this.ignoreLava = getConfig().getBoolean("ignore-laval");

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
    private void init() {

        FileConfiguration config = getConfig();

        if (config.getConfigurationSection("pits") == null)
            config.createSection("pits");

        for (String pitName : config.getConfigurationSection("pits").getKeys(false)) {

            // Grab the config section that represents this pit. If null break this iteration.
            ConfigurationSection pitNameSection = config.getConfigurationSection("pits." + pitName);
            if (pitNameSection == null) {
                getLogger().warning("Section was null for the pit " + pitName);
                continue;
            }

            // Load basic values
            String regionName = pitNameSection.getString("regionName");
            String worldName = pitNameSection.getString("worldName");
            int cooldown = pitNameSection.getInt("cooldown");
            long lastRefill = pitNameSection.getLong("lastRefill");
            int refillValue = pitNameSection.getInt("refillValue");

            // Grab a string list of the 'block:chance'
            ArrayList<String> blockTypes = new ArrayList<>();
            ConfigurationSection blockTypeSection = pitNameSection.getConfigurationSection("blockTypes");
            if (blockTypeSection != null) {
                for (String block : blockTypeSection.getKeys(false)) {
                    blockTypes.add(block + ":" + blockTypeSection.getInt(block));
                }
            } else {
                getLogger().warning("Block type was null for pit " + pitName);
            }

            // Validate we haven't loaded this pit yet
            if (allPitsList.stream().anyMatch(pit -> pit.getName().equalsIgnoreCase(pitName))) {
                getLogger().severe("A duplicate pit with the name " + pitName + " was already found");
                continue;
            }

            // Convert ArrayList to String[]
            String[] stringedBlockTypes = new String[blockTypes.size()];
            if (blockTypes.size() > 0) {
                for (int i = 0; i < stringedBlockTypes.length; i++) {
                    stringedBlockTypes[i] = blockTypes.get(i);
                }
            }

            // Go ahead and parse world/region
            World world = worldName != null ? Bukkit.getWorld(worldName) : null;
            ProtectedRegion region = getRegion(regionName, world);

            // Create a new pit using our parsed values
            ResourcePit pit = ResourcePit.builder(pitName)
                                         .cooldown(cooldown)
                                         .refillValue(refillValue)
                                         .lastUse(lastRefill)
                                         .blockTypes(stringedBlockTypes)
                                         .world(world)
                                         .region(region)
                                         .build();

            // Register the pit
            this.addPit(pit);
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

            Run.as(this).async(() -> {
                this.getConfig().set("pits." + thisPit.getName().toUpperCase(), null);
                PitAutofill.get().saveConfig();
            });

            output = "Successfully deleted the pit '" + PitAutofill.ALT_COLOUR + thisPit.getName().toUpperCase() + PitAutofill.PREFIX + "'.";
        }
        return output;
    }

    // Returns the ResourcePit with the matching name.
    public ResourcePit getPit(String name) {

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

    //// PRIVATE ////

    // Returns the pit not found error message.
    private String noPitFoundMsg(String name) {
        return "No pit found with the name '" + PitAutofill.ALT_COLOUR + name.toUpperCase() + PitAutofill.PREFIX + "'.";
    }

}