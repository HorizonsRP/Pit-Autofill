package co.lotc.pitautofill;

import co.lotc.core.bukkit.command.Commands;
import co.lotc.pitautofill.cmd.Pit;
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

    public static final String PREFIX = (ChatColor.DARK_GREEN + "");      // The same colouring prefix to be used throughout the plugin.
    public static final String ALT_COLOUR = (ChatColor.YELLOW + "");

    private static PitAutofill instance;
    // Provides an accessor to our current instance.
    public static PitAutofill get() {
        return instance;
    }

    private static ArrayList<ResourcePit> allPitsList = new ArrayList<>();

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        init();

        Bukkit.getPluginManager().registerEvents(new FillSignListener(this), this);

        registerParameters();
        Commands.build(getCommand("pit"), Pit::new);

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

    // Iterate through our list of pits for each world, plugging in the required info if specified in the config.
    private void init() {

        FileConfiguration config = getConfig();

        if (config.getConfigurationSection("pits") == null)
            config.createSection("pits");

        for (String pitName : config.getConfigurationSection("pits").getKeys(false)) {

            String regionName = config.getString("pits." + pitName + ".regionName");
            String worldName = config.getString("pits." + pitName + ".worldName");
            ArrayList<String> blockTypes = new ArrayList<>();

            ConfigurationSection configSection = config.getConfigurationSection("pits." + pitName + ".blockTypes");
            if (configSection != null) {
                for (String block : configSection.getKeys(false)) {
                    blockTypes.add(block + ":" + config.getInt("pits." + pitName + ".blockTypes." + block));
                }
            }

            PitAutofill.get().getServer().getLogger().info("[Pit-Autofill] " + ChatColor.stripColor(newPit(pitName)));

            if (regionName != null) {
                setPitRegion(pitName, regionName, worldName != null ? Bukkit.getWorld(worldName) : null);
            }

            if (blockTypes.size() > 0) {
                String[] stringedBlockTypes = new String[blockTypes.size()];
                for (int i = 0; i < stringedBlockTypes.length; i++) {
                    stringedBlockTypes[i] = blockTypes.get(i);
                }

                setPitBlocks(pitName, stringedBlockTypes);
            }
        }

    }


    //// PUBLIC ////

    // Create a new pit with the given name and add to our list.
    public String newPit(String name) {

        String output = "A pit with that name already exists.";

        if (getPit(name.toUpperCase()) == null) {
            if (!this.getConfig().isSet("pits." + name.toUpperCase())) {
                this.getConfig().createSection("pits." + name.toUpperCase());
                PitAutofill.get().saveConfig();
            }

            allPitsList.add(new ResourcePit(name.toUpperCase()));

            output = "Successfully created the pit '" + PitAutofill.ALT_COLOUR + name.toUpperCase() + PitAutofill.PREFIX + "'.";
        }
        return output;
    }

    // Remove the pit with the same name from our list.
    public String deletePit(ResourcePit thisPit) {
        String output = "";
        if (thisPit != null) {
            allPitsList.remove(thisPit);

            this.getConfig().set("pits." + thisPit.getName().toUpperCase(), null);
            PitAutofill.get().saveConfig();

            output = "Successfully deleted the pit '" + PitAutofill.ALT_COLOUR + thisPit.getName().toUpperCase() + PitAutofill.PREFIX + "'.";
        }
        return output;
    }

    // Sets the given pit's region name.
    public String setPitRegion(String name, String region, World world) {

        String output = noPitFoundMsg(name.toUpperCase());

        ResourcePit thisPit = getPit(name.toUpperCase());
        if (thisPit != null) {
            // Saves to config inside setRegion.
            output = thisPit.setRegion(region, world);
        }
        return output;
    }

    // Retrieves a string list of stored pits.
    public String getList() {
        StringBuilder output = new StringBuilder(PitAutofill.ALT_COLOUR + "");
        boolean firstName = true;

        for (ResourcePit pit : allPitsList) {
            if (firstName) {
                output.append(pit.getName().toUpperCase());
                firstName = false;
            } else {
                output.append(", ").append(pit.getName().toUpperCase());
            }
        }

        return output.toString();
    }


    //// PRIVATE ////

    // Refills the given pit.
    String fillPit(CommandSender sender, String name) {

        String output = noPitFoundMsg(name.toUpperCase());

        ResourcePit thisPit = getPit(name.toUpperCase());
        if (thisPit != null) {
            output = thisPit.fill(sender);
        }
        return output;
    }

    // Returns the pit not found error message.
    private String noPitFoundMsg(String name) {
        return "No pit found with the name '" + PitAutofill.ALT_COLOUR + name.toUpperCase() + PitAutofill.PREFIX + "'.";
    }

    // Returns the ResourcePit with the matching name.
    private ResourcePit getPit(String name) {

        for (ResourcePit thisPit : allPitsList) {
            if (thisPit.getName().equalsIgnoreCase(name.toUpperCase()))
                return thisPit;
        }
        return null;
    }

    // Sets the given pit's block types.
    private void setPitBlocks(String name, String[] blockTypes) {
        ResourcePit thisPit = getPit(name.toUpperCase());
        if (thisPit != null) {
            // Saves to config inside setBlockTypes.
            thisPit.setBlockTypes(blockTypes);
        }
    }

}