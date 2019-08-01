package co.lotc.pitautofill;

import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Protection;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.lordofthecraft.omniscience.api.data.DataKeys;
import net.lordofthecraft.omniscience.api.data.DataWrapper;
import net.lordofthecraft.omniscience.api.entry.OEntry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class ResourcePit {

	private String name;                               // The name/ID of the pit.
	private World world;                               // Stores the world the region resides in.
	private HashMap<Material, Integer> blockTypes;     // Stored as block Material, chance Integer
	private ProtectedRegion region;                    // Stores the WorldGuard region.
	private PitAutofill plugin;                        // Stores a reference to our main plugin instance.

	//// CONSTRUCTORS ////

	// Constructor called on with given name. Initializes all other values.
	// Changed to package private as we dont need other projects trying to build their own pits - 501
	ResourcePit(String givenName) {
		name = givenName;
		world = null;
		blockTypes = new HashMap<>();
		region = null;
		plugin = PitAutofill.get();

		if (!plugin.getConfig().isSet("pits." + name.toUpperCase() + ".refillValue"))
			plugin.getConfig().set("pits." + name.toUpperCase() + ".refillValue", plugin.getConfig().getInt("default-refill-value"));

		if (!plugin.getConfig().isSet("pits." + name.toUpperCase() + ".cooldown"))
			plugin.getConfig().set("pits." + name.toUpperCase() + ".cooldown", plugin.getConfig().getInt("default-cooldown-value"));

		plugin.saveConfig();
	}

	public String getName() {
		return name;
	}
	public int getRefillValue() {
		return plugin.getConfig().getInt("pits." + name.toUpperCase() + ".refillValue");
	}
	public int getCooldown() {
		return plugin.getConfig().getInt("pits." + name.toUpperCase() + ".cooldown");
	}
	public ProtectedRegion getRegion() {
		return region;
	}
	public boolean regionIsNotNull() {
		return region != null;
	}
	public World getRegionWorld() {
		return world;
	}


	//// STATIC ////

	// Sets the chanceValue.
	public static String setDefaultChanceValue(int newValue) {

		String output = "Please enter a number between 0 and 100.";

		if (newValue >= 0 && newValue <= 100) {
			PitAutofill.get().getConfig().set("default-chance-value", newValue);
			PitAutofill.get().saveConfig();
			output = "Updated the default chance value for all pits.";
		}
		return output;
	}


	//// MODIFIERS ////

	// Sets the region's name to the new name.
	public String setName(String givenName) {

		String output = "Successfully changed the pit '" + PitAutofill.ALT_COLOUR + name + PitAutofill.PREFIX + "' to '" + PitAutofill.ALT_COLOUR + givenName + PitAutofill.PREFIX + "'.";

		if(!plugin.getConfig().isSet("pits." + givenName)) {
			plugin.getConfig().createSection("pits." + givenName);
			copyConfigSection(plugin.getConfig(), "pits." + name, "pits." + givenName);
			plugin.getConfig().set("pits." + name, null);
			plugin.saveConfig();

			name = givenName;
		} else {
			output = "A pit with that name already exists Please delete it before copying over it.";
		}
		return output;
	}

	// Sets the pit's region to a worldguard region with the given name, if found.
	public String setRegion(String regionName, World thisWorld) {
		String output = "Could not find a region with that name in that world.";

		RegionContainer container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();

		if (thisWorld != null) {
			RegionManager worldRegions = container.get(BukkitAdapter.adapt(thisWorld));

			// If our region set exists and has the specified region we set the pit's region to that.
			if (worldRegions != null && worldRegions.hasRegion(regionName)) {
				plugin.getConfig().set("pits." + name + ".regionName", regionName);
				plugin.getConfig().set("pits." + name + ".worldName", thisWorld.getName());
				plugin.saveConfig();

				region = worldRegions.getRegion(regionName);
				world = thisWorld;
				output = "The pit '" + PitAutofill.ALT_COLOUR + name.toUpperCase() + PitAutofill.PREFIX + "' has been assigned the region '" +
						 PitAutofill.ALT_COLOUR + regionName.toUpperCase() + PitAutofill.PREFIX + "'.";
			}
		} else {
			output = "There is no world with that name.";
		}
		return output;
	}

	/*
	Takes a set of strings as arguments to set the new block types.
	Assumes the format to be as block:chance where chance is chance%
	of that type being placed.
	 */
	public String setBlockTypes(String[] blockAndChance) {

		String output = "Successfully updated the pit block types.";
		HashMap<Material, Integer> newBlockTypes = new HashMap<>();

		for (String arg : blockAndChance) {

			int chanceIndex = arg.indexOf(":");

			String type;
			int chance = 100;

			// Default our parseInt if it doesn't work.
			if (chanceIndex != -1) {
				type = arg.substring(0, chanceIndex);
				try {
					chance = Integer.parseInt(arg.substring(chanceIndex+1));
				} catch (NumberFormatException nfe) {
					chance = plugin.getConfig().getInt("default-chance-value");
				}
			} else {
				type = arg;
			}

			Material thisMaterial = Material.matchMaterial(type);
			if (thisMaterial != null)
				newBlockTypes.put(thisMaterial, chance);
		}

		if (newBlockTypes.keySet().size() > 0) {
			blockTypes = checkChances(newBlockTypes);

			plugin.getConfig().set("pits." + name + ".blockTypes", null);
			for (Material mat : blockTypes.keySet()) {
				plugin.getConfig().set("pits." + name + ".blockTypes." + mat.toString(), getBlockChance(mat));
			}
			plugin.saveConfig();
		} else {
			output = "Please specify the blocks and their chances.";
		}
		return output;
	}

	// Sets the refillValue.
	public String setRefillValue(int newValue) {

		String output = "Please enter a number between 0 and 100.";

		if (newValue >= 0 && newValue <= 100) {
			plugin.getConfig().set("pits." + name.toUpperCase() + ".refillValue", newValue);
			plugin.saveConfig();
			output = "Updated the minimum refill value for the pit '" + PitAutofill.ALT_COLOUR + name + PitAutofill.PREFIX + "'.";
		}
		return output;
	}

	// Sets the pit's cooldown in seconds.
	public String setCooldown(int cooldownValue) {

		String output = "Please enter a positive integer.";

		if (cooldownValue >= 0) {
			plugin.getConfig().set("pits." + name.toUpperCase() + ".cooldown", cooldownValue);
			plugin.saveConfig();
			output = "Updated the usage cooldown for the pit '" + PitAutofill.ALT_COLOUR + name + PitAutofill.PREFIX + "'.";
		}
		return output;
	}


	//// ACCESSORS ////

	// Returns an ArrayList of the pit's materials ordered from highest to lowest chance.
	public ArrayList<Material> getBlockChanceList() {

		ArrayList<Material> output = new ArrayList<>();
		ArrayList<Material> keySet = new ArrayList<>(blockTypes.keySet());

		while (keySet.size() > 0) {

			Material highestChance = null;
			for (Material mat : keySet) {
				if (highestChance != null) {
					if (blockTypes.get(mat) > blockTypes.get(highestChance)) {
						highestChance = mat;
					}
				} else {
					highestChance = mat;
				}
			}

			output.add(highestChance);
			keySet.remove(highestChance);
		}

		return output;
	}

	// Returns the chance integer of the given material, if it exists in this pit.
	public int getBlockChance(Material material) {
		int output = -1;
		if (blockTypes.get(material) != null)
			output = blockTypes.get(material);
		return output;
	}


	//// OPERATORS ////

	// Recursively copies all data at the given configurationSection under the new name.
	private void copyConfigSection(FileConfiguration config, String oldPath, String newPath) {

		ConfigurationSection cs = config.getConfigurationSection(oldPath);
		if (cs != null) {
			for (String key : cs.getKeys(false)) {
				copyConfigSection(config, oldPath + "." + key, newPath + "." + key);
			}
		} else {
			config.set(newPath, config.get(oldPath));
		}
	}

	/*
	So long as there're no players in the pit, fill it. If there are
	players, return a message to the sender as such.
	 */
	public String fill(CommandSender sender) {

		final String output;

		if (regionIsNotNull()) {
			if (!playersAreInside()) {

				float airCount = 0;
				float totalCount = 0;
				for (Location loc : getLocationList()) {
					if (world.getBlockAt(loc).getType().equals(Material.AIR))
						airCount += 1f;
					totalCount += 1f;
				}

				if ((1f - (airCount / totalCount)) <= ((float) plugin.getConfig().getInt("pits." + name + ".refillValue")) / 100) {
					removeLocks();

					long fillCooldown = plugin.getConfig().getInt("pits." + name.toUpperCase() + ".cooldown");

					if (fillCooldown > 0) {
						long timeSinceRefill = (System.currentTimeMillis() - plugin.getConfig().getLong("pits." + name + ".lastFilled")) / 1000;

						if (timeSinceRefill > fillCooldown) {
							output = changeBlocks(sender);
						} else {
							output = "That pit is still on cooldown for " + PitAutofill.ALT_COLOUR + (fillCooldown - timeSinceRefill + 1) + " seconds" + PitAutofill.PREFIX + ".";
						}
					} else {
						output = changeBlocks(sender);
					}
				} else {
					output = "The pit is still too full. Please use what's currently there.";
				}
			} else {
				output = "There are still players inside the pit.";
			}
		} else {
			output = "No region specified for that pit.";
		}

		return output;
	}

	// Runs the fill command after bypassing the cooldown and refill values in the config.
	public String fillOverride(CommandSender sender) {
		int storedCooldown = plugin.getConfig().getInt("pits." + name.toUpperCase() + ".cooldown");
		int storedRefillValue = plugin.getConfig().getInt("pits." + name + ".refillValue");

		plugin.getConfig().set("pits." + name.toUpperCase() + ".cooldown", 0);
		plugin.getConfig().set("pits." + name.toUpperCase() + ".refillValue", 100);

		// Moved initialization down, it isn't needed so high up when this value is used once.
		final String output = fill(sender);

		plugin.getConfig().set("pits." + name.toUpperCase() + ".cooldown", storedCooldown);
		plugin.getConfig().set("pits." + name.toUpperCase() + ".refillValue", storedRefillValue);

		return output;
	}

	// Returns true if there are any players inside the pit.
	private boolean playersAreInside() {
		boolean output = false;

		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.getWorld().equals(world)) {
				Location loc = player.getLocation();
				if (region.contains(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()))) {
					output = true;
					break;
				}
			}
		}

		return output;
	}

	// Removes all block locks within the region.
	private void removeLocks() {
		// For each location in the region, if there's a lock, remove it.
		if (Bukkit.getPluginManager().isPluginEnabled("LWC")) {
			LWCPlugin lwcPlugin = (LWCPlugin) plugin.getServer().getPluginManager().getPlugin("LWC");
			if (lwcPlugin != null) { // Sanity check
				for (Location loc : getLocationList()) {
					Protection prot = lwcPlugin.getLWC().findProtection(loc);
					if (prot != null) {
						prot.remove();
					}
				}
			}
		}

	}

	/*
	Changes all blocks within the region to the current blocklist. We use
	Math.random, going up through our list until we find a block chance that's
	higher than our random value. Once we do, set block location to the previous
	material we checked.
	 */
	private String changeBlocks(CommandSender sender) {
		String output = "The pit has been refilled.";

		boolean fillSuccessful = false;

		for (Location loc : getLocationList()) {

			double randomChance = Math.random();
			double currentTotal = 0;

			// Run through our list of materials and setting the block based on chance.
			Material finalMat = null;
			for (Material mat : getBlockChanceList()) {
				double blockChance = ((double) getBlockChance(mat)) / 100;

				if (blockChance + currentTotal >= randomChance) {
					finalMat = mat;
					break;
				}

				if (blockChance > 0)
					currentTotal += blockChance;
			}

			if (finalMat != null) {
				loc.getBlock().setType(finalMat);
				fillSuccessful = true;
			} else {
				output = "No blocks specified for that pit.";
				break;
			}
		}

		// Only want to change the date and log the player once.
		if (fillSuccessful) {
			plugin.getConfig().set("pits." + name + ".lastFilled", System.currentTimeMillis());
			plugin.saveConfig();

			plugin.getLogger().info(name + " pit filled by " + sender.getName() + ".");

			// Omniscience Logging
			if (sender instanceof Player) {
				logPitRefill((Player) sender);
			}
		}

		return output;
	}

	/**
	 * A small logger method that will check to see if Omniscience is enabled and if so will build the data we need for logging this event
	 *
	 * @param sender The player who sent the fill request
	 */
	private void logPitRefill(Player sender) {
		if (Bukkit.getPluginManager().isPluginEnabled("Omniscience")) {
			DataWrapper wrapper = DataWrapper.createNew();
			wrapper.set(DataKeys.TARGET, name + " pit");
			OEntry.create().player(sender).customWithLocation("refill", wrapper, sender.getLocation()).save();
		}
	}

	// Returns a list of the block locations found in this region.
	private ArrayList<Location> getLocationList() {
		ArrayList<Location> output = new ArrayList<>();

		// Grabs the min and max of our region.
		BlockVector3 min = region.getMinimumPoint();
		BlockVector3 max = region.getMaximumPoint();

		// Runs through the x y z coords between min and max
		for (int i = min.getBlockX(); i <= max.getBlockX(); i++) {
			for (int j = min.getBlockY(); j <= max.getBlockY(); j++) {
				for (int k = min.getBlockZ(); k <= max.getBlockZ(); k++) {

					output.add(new Location(world, i, j, k));
				}
			}
		}

		return output;
	}

	// Makes sure the our chances total up to 100%.
	private HashMap<Material, Integer> checkChances(HashMap<Material, Integer> input) {
		HashMap<Material, Integer> output = new HashMap<>(input);

		int total = 0;
		for (int chance : output.values()) {
			total += chance;
		}

		if (total < 100) {
			output.put(Material.AIR, 100 - total);
		}

		return output;
	}
}