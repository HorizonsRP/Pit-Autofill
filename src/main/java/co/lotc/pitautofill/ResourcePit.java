package co.lotc.pitautofill;

import co.lotc.core.bukkit.util.Run;
import co.lotc.core.util.TimeUtil;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Protection;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.lordofthecraft.omniscience.api.data.DataKeys;
import net.lordofthecraft.omniscience.api.data.DataWrapper;
import net.lordofthecraft.omniscience.api.entry.OEntry;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ResourcePit {

    private static final PitAutofill plugin = PitAutofill.get(); // Stores a reference to our main plugin instance.
	private String name;                                  // The name/ID of the pit.
	private World world;                                  // Stores the world the region resides in.
	private HashMap<Material, Integer> blockTypes;        // Stored as block Material, chance Integer
	private ProtectedRegion region;                       // Stores the WorldGuard region.
    // These values are not using int or long primitives because primitives cannot be null. These can be!
    // Always something to keep in mind when working with these
    private Integer refillValue;                          // Stores the % that needs to be remaining to reset this pit
    private Long cooldown;                                  // Stores the cooldown in milliseconds that'll be applied when it's refilled
    private Long lastUse;                                  // Stores the last use of this pit

    private ResourcePit(String name, World world, String[] blockTypes, ProtectedRegion region, Integer refillValue, Long cooldown, Long lastUse) {
		this.name = name;
		this.world = world;
		this.blockTypes = blockTypes != null && blockTypes.length > 0 ? convertBlockTypes(blockTypes) : new HashMap<>();
		this.region = region;
		this.refillValue = refillValue;
		this.cooldown = cooldown;
		this.lastUse = lastUse;
	}

	//// CONSTRUCTORS ////

	public static Builder builder(String name) {
		return new Builder(name);
	}

	public String getName() {
		return name;
	}

	public int getRefillValue() {
		return refillValue;
	}

	// Sets the refillValue.
	public ProtectedRegion getRegion() {
		return region;
	}
	public boolean regionIsNotNull() {
		return region != null;
	}
	public World getRegionWorld() {
		return world;
	}

	public long getCooldown() {
		return cooldown;
	}

	// Sets the pit's cooldown in milliseconds.
	public void setCooldown(long cooldownValue) {
		this.cooldown = cooldownValue;
		save();
	}

	//// MODIFIERS ////

	public long getLastUse() {
		return lastUse;
	}

	public void setLastUse(long lastUse) {
		this.lastUse = lastUse;
		save();
	}

    public void setRefillValue(int newValue) {
        this.refillValue = newValue;
        save();
    }

	// Sets the region's name to the new name.
	public String setName(String givenName) {

		String output = "Successfully changed the pit '" + PitAutofill.ALT_COLOUR + name + PitAutofill.PREFIX + "' to '" + PitAutofill.ALT_COLOUR + givenName + PitAutofill.PREFIX + "'.";

		if(!plugin.getConfig().isSet("pits." + givenName)) {
			// See save() for info
			Run.as(plugin).async(() -> {
				plugin.getConfig().createSection("pits." + givenName);
				copyConfigSection(plugin.getConfig(), "pits." + name, "pits." + givenName);
				plugin.getConfig().set("pits." + name, null);
				plugin.saveConfig();
			});

			name = givenName;
		} else {
			output = "A pit with that name already exists Please delete it before copying over it.";
		}
		return output;
	}

	// Sets the pit's region to a worldguard region with the given name, if found.
	public void setRegion(ProtectedRegion region, World world) {
		this.region = region;
		this.world = world;
		save();
	}

	/*
	Takes a set of strings as arguments to set the new block types.
	Assumes the format to be as block:chance where chance is chance%
	of that type being placed.
	 */
	public String setBlockTypes(String[] blockAndChance) {
		HashMap<Material, Integer> newBlockTypes = convertBlockTypes(blockAndChance);

		if (newBlockTypes.keySet().size() > 0) {
			blockTypes = checkChances(newBlockTypes);

			save();
		} else {
			return "Please specify the blocks and their chances.";
		}
		return null;
	}

	private HashMap<Material, Integer> convertBlockTypes(String[] blockAndChance) {
		HashMap<Material, Integer> newBlockTypes = new HashMap<>();

		if (blockAndChance == null) {
			plugin.getLogger().severe("Block and chance was null for the pit " + name);
			return newBlockTypes;
		}

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
		return newBlockTypes;
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
	public boolean fill(CommandSender sender, boolean override) {

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

				if (override || ((1f - (airCount / totalCount)) <= ((float) refillValue) / 100)) {
					removeLocks();

					if (cooldown > 0 && lastUse != null && !override) {
						// Subtract the last use plus the cooldown from the current time to check the diff
						long remainingTime = (lastUse + cooldown) - System.currentTimeMillis();
						// If diff doesnt exist then lets change blocks
						if (remainingTime <= 0) {
							output = changeBlocks(sender, false);
						} else {
							// Print that things are still on CD using tythan time util
							output = "That pit is still on cooldown for " + PitAutofill.ALT_COLOUR + TimeUtil.printMillis(remainingTime).toPlainText() + ChatColor.RED + ".";
						}
					} else {
						output = changeBlocks(sender, override);
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

		if (output != null) {
			sender.sendMessage(ChatColor.DARK_RED + "Error: " + ChatColor.RED + output);
			return false;
		}
		return true;
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
	private String changeBlocks(CommandSender sender, boolean override) {
		String output = null;

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
			if (!override) {
				this.lastUse = System.currentTimeMillis();
				save();
			}

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

	/**
	 * Runs a save task for this specific resource pit
	 */
	public void save() {
		// Queue a task using a Tythan Util
		Run.as(plugin).async(() -> {
			// This is an anonymous inner class. All of this code will be run on an entirely new thread!
			// This is fine as follows because we only GET objects.

			// cooldown = 10; Avoid this
			// Avoid setting variables in other threads!
			// This can create an odd situation where someone might set the cooldown to 0 from another thread but this one took a while so even though the latest value is 0 we might overwrite it with 10!

			// Get the config of the plugin
			FileConfiguration config = plugin.getConfig();
			// Prep our configuration section, creating a new one if needed as a sanity check. the ternary operator (?) is a fancy "if then" statement on one line.
			ConfigurationSection section = config.getConfigurationSection("pits." + name);
			if (section == null) {
				section = config.createSection("pits." + name);
			}
			// Lambdas can't access non-final variables
			final ConfigurationSection localSection = section;
			// Sanity null check since java is bad at this
			// Set our values, don't worry if they exist or not as long as we aren't throwing an NPE
			localSection.set("regionName", region == null ? null : region.getId());
			localSection.set("worldName", world == null ? null : world.getName());
			localSection.set("cooldown", cooldown);
			localSection.set("lastRefill", lastUse);
			localSection.set("refillValue", refillValue);
			localSection.set("blockTypes", null);
			blockTypes.forEach((type, chance) -> localSection.set("blockTypes." + type.name(), chance));
				/*
				The above is the same as this
				for (Material mat : blockTypes.keySet()) {
					plugin.getConfig().set("pits." + name + ".blockTypes." + mat.toString(), getBlockChance(mat));
				}
				 */
			config.set("pits." + name, localSection);
			// Save
			plugin.saveConfig();
		});
		// The above code is the same as this!
		/* Run.as(plugin).async(new Runnable() {
			@Override
			public void run() {
				// Code
			}
		}); */
	}

    // Since this is a data holding object that might be stored in collections it's
    // good practice to both build a toString (for debugging) and a hashcode/equals

	@Override
	public String toString() {
		return "ResourcePit{" +
				"name='" + name + '\'' +
				", world=" + world +
				", blockTypes=" + blockTypes +
				", region=" + region +
				", refillValue=" + refillValue +
				", cooldown=" + cooldown +
				", lastUse=" + lastUse +
				'}';
	}

    // Equals is important for when you have a collection of objects

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ResourcePit that = (ResourcePit) o;
		return name.equals(that.name) &&
				Objects.equals(world, that.world) &&
				Objects.equals(blockTypes, that.blockTypes) &&
				Objects.equals(region, that.region) &&
				Objects.equals(refillValue, that.refillValue) &&
				Objects.equals(cooldown, that.cooldown) &&
				Objects.equals(lastUse, that.lastUse);
	}

    // Hashcode is what powers HashMaps and HashSets. It compares the hashCodes to existing keys and if they're the same it replaces
    // Very important if you're doing collections to implement this! This one was generated by intellij

	@Override
	public int hashCode() {
		return Objects.hash(name, world, blockTypes, region, refillValue, cooldown, lastUse);
	}

    /**
     * Meet Bob the Builder.
     * <p>
     * This is a builder pattern, basically an alternative constructor. It allows for optional variables.
     * See the various invocations of it for more information
     */
    public static class Builder {
        private final String name;
        private World world;
        private String[] blockTypes;
        private ProtectedRegion region;
        private Integer refillValue;
        private Long cooldown;
        private Long lastUse;

        private Builder(String name) {
            if (name == null) {
                throw new IllegalArgumentException("Name cannot be null");
            }
            this.name = name.toUpperCase();
        }

        public Builder world(World world) {
            this.world = world;
            return this;
        }

        public Builder blockTypes(String[] blockTypes) {
            this.blockTypes = blockTypes;
            return this;
        }

        public Builder region(ProtectedRegion region) {
            this.region = region;
            return this;
        }

        public Builder refillValue(Integer refillValue) {
            this.refillValue = refillValue;
            return this;
        }

        public Builder cooldown(Long cooldown) {
            this.cooldown = cooldown;
            return this;
        }

        public Builder lastUse(Long lastUse) {
            this.lastUse = lastUse;
            return this;
        }

        public ResourcePit build() {
            return new ResourcePit(name, world, blockTypes, region, refillValue == null ? PitAutofill.get().defaultRefillValue : refillValue, cooldown == null ? PitAutofill.get().defaultCooldown : cooldown, lastUse);
        }
    }
}