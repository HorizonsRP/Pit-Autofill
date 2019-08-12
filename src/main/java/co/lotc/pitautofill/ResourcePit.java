package co.lotc.pitautofill;

import co.lotc.core.bukkit.util.Run;
import com.google.common.collect.Maps;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Protection;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.lordofthecraft.omniscience.api.data.DataKeys;
import net.lordofthecraft.omniscience.api.data.DataWrapper;
import net.lordofthecraft.omniscience.api.entry.OEntry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;

public class ResourcePit {

	private File file;
	private String child;                                        // The child pit that will be chain-filled after this one.

	private ResourcePit(File file, String name, World world, String[] blockTypes, ProtectedRegion region, Integer refillValue, Integer cooldown, Long lastUse) {
		this.file = file;
		this.name = name;
		this.world = world;
		this.blockTypes = blockTypes != null && blockTypes.length > 0 ? convertBlockTypes(blockTypes) : new HashMap<>();
		this.region = region;
		this.refillValue = refillValue;
		this.cooldown = cooldown;
		this.lastUse = lastUse;
	}

    private static final PitAutofill plugin = PitAutofill.get(); // Stores a reference to our main plugin instance.

	public static ResourcePit fromFile(String pitName, File file) {
		YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);

		String regionName = configuration.getString("regionName");
		String worldName = configuration.getString("worldName");
		int cooldown = configuration.getInt("cooldown");
		long lastRefill = configuration.getLong("lastRefill");
		int refillValue = configuration.getInt("refillValue");

		// Grab a string list of the 'block:chance'
		ArrayList<String> blockTypes = new ArrayList<>();
		ConfigurationSection blockTypeSection = configuration.getConfigurationSection("blockTypes");
		if (blockTypeSection != null) {
			for (String block : blockTypeSection.getKeys(false)) {
				blockTypes.add(block + ":" + blockTypeSection.getInt(block));
			}
		} else {
			PitAutofill.get().getLogger().warning("Block type was null for pit " + pitName);
		}

		// Validate we haven't loaded this pit yet
		if (PitAutofill.get().getPits().stream().anyMatch(pit -> pit.getName().equalsIgnoreCase(pitName))) {
			PitAutofill.get().getLogger().severe("A duplicate pit with the name " + pitName + " was already found");
			return null;
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

		return ResourcePit.builder(pitName, file)
				.cooldown(cooldown)
				.refillValue(refillValue)
				.lastUse(lastRefill)
				.blockTypes(stringedBlockTypes)
				.world(world)
				.region(region)
				.build();
	}
	private String name;                                         // The name/ID of the pit.
	private World world;                                         // Stores the world the region resides in.
	private HashMap<Material, Integer> blockTypes;               // Stored as block Material, chance Integer
	private ProtectedRegion region;                              // Stores the WorldGuard region.
    private Integer refillValue;                                 // Stores the % that needs to be remaining to reset this pit
    private Integer cooldown;                                    // Stores the cooldown value in seconds
    private Long lastUse;                                        // Stores the last use of this pit in date milliseconds

	private static ProtectedRegion getRegion(String id, World world) {
		if (world == null) {
			return null;
		}
		RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
		if (manager == null) {
			return null;
		}
		return manager.getRegion(id);
	}

	//// CONSTRUCTORS ////

	public static Builder builder(String name, File file) {
		return new Builder(name, file);
	}

	public String getName() {
		return name;
	}

	public long getCooldown() {
		return cooldown;
	}
	public int getRefillValue() {
		return refillValue;
	}
	public long getLastUse() {
		return lastUse;
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

	public ResourcePit getChild() {
		return plugin.getPit(child);
	}

	//// MODIFIERS ////

	// Sets the pit's refill value as a non-decimal percent value.
    public void setRefillValue(int newValue) {
        this.refillValue = newValue;
		save();
    }

	// Sets the pit's cooldown in milliseconds.
	public void setCooldown(int cooldownValue) {
		this.cooldown = cooldownValue;
		save();
	}

	// Sets the pit's last use time in date milliseconds.
	public void setLastUse(long lastUse) {
		this.lastUse = lastUse;
		save();
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
		return "Successfully updated the block types and chances.";
	}

	public String setChildPit(ResourcePit newChild) {
		String output = "Successfully updated child pit.";

		boolean childLoop = false;
		for(ResourcePit thisPit = newChild; thisPit != null; thisPit = thisPit.getChild()) {
			if (thisPit.equals(this)) {
				childLoop = true;
				break;
			}
		}

		if (!childLoop) {
			child = newChild != null ? newChild.name : null;
			save();
		} else {
			output = "Setting that pit as a child would cause an endless loop.";
		}

		return output;
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

	/*
	So long as there're no players in the pit, fill it. If there are
	players, return a message to the sender as such.
	 */
	public String fill(CommandSender sender, boolean override) {

		final String output;

		if (regionIsNotNull()) {
			if (!playersAreInside()) {

				float emptyCount = 0;
				float totalCount = 0;
				for (Location loc : getLocationList()) {

					Material existingMat = world.getBlockAt(loc).getType();
					// If our existing material matches ANY ignored material, we count it as empty.
					if (plugin.getConfig().getStringList("ignored-materials").stream().anyMatch(key -> Material.matchMaterial(key).equals(existingMat))) {
						emptyCount++;
					}
					totalCount++;
				}

				if (override || ((1f - (emptyCount / totalCount)) <= ((float) refillValue) / 100f)) {
					removeLocks();

					if (cooldown > 0 && lastUse != null && !override) {
						// Subtract the last use plus the cooldown from the current time to check the diff
						long remainingTime = (lastUse + (cooldown*1000)) - System.currentTimeMillis();
						if (remainingTime <= 0) {
							output = changeBlocks(sender, false);
						} else {
							output = "That pit is still on cooldown for " + PitAutofill.ALT_COLOUR + ((remainingTime/1000) + 1) + " seconds" + PitAutofill.PREFIX + ".";
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
			return output;
		} else {
			return "Successfully filled the pit '" + PitAutofill.ALT_COLOUR + name.toUpperCase() + PitAutofill.PREFIX + "'.";
		}
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
			if (child != null) {
				ResourcePit pit = plugin.getPit(child);
				if (pit != null) {
					pit.fill(sender, true);
				} else {
					plugin.getLogger().severe("Child pit " + child + " was unable to be located");
				}
			}

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
			YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);

			try {
				// Set our values, don't worry if they exist or not as long as we aren't throwing an NPE
				configuration.set("regionName", region == null ? null : region.getId());
				configuration.set("worldName", world == null ? null : world.getName());
				configuration.set("childPit", child);
				configuration.set("cooldown", cooldown);
				configuration.set("lastRefill", lastUse);
				configuration.set("refillValue", refillValue);
				configuration.set("blockTypes", Maps.newHashMap()); // Do this to prevent error
				blockTypes.forEach((type, chance) -> configuration.set("blockTypes." + type.name(), chance));

				configuration.save(file);
			} catch (IOException e) {
				PitAutofill.get().getLogger().log(Level.SEVERE, "Failed to save pit", e);
			}
		});
	}

	void delete() {
		file.delete();
	}

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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ResourcePit that = (ResourcePit) o;
		return (name.equals(that.name) &&
				Objects.equals(world, that.world) &&
				Objects.equals(blockTypes, that.blockTypes) &&
				Objects.equals(region, that.region) &&
				Objects.equals(refillValue, that.refillValue) &&
				Objects.equals(cooldown, that.cooldown) &&
				Objects.equals(lastUse, that.lastUse));
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, world, blockTypes, region, refillValue, cooldown, lastUse);
	}

	// ResroucePit Builder Class
    public static class Builder {
        private final String name;
		private final File file;
        private World world;
        private String[] blockTypes;
        private ProtectedRegion region;
        private Integer refillValue;
        private Integer cooldown;
        private Long lastUse;

		private Builder(String name, File file) {
            if (name == null) {
                throw new IllegalArgumentException("Name cannot be null");
            }
            this.name = name.toUpperCase();
			this.file = file;
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

        public Builder cooldown(Integer cooldown) {
            this.cooldown = cooldown;
            return this;
        }

        public Builder lastUse(Long lastUse) {
            this.lastUse = lastUse;
            return this;
        }

        public ResourcePit build() {
			return new ResourcePit(file, name, world, blockTypes, region, refillValue == null ? PitAutofill.get().defaultRefillValue : refillValue, cooldown == null ? PitAutofill.get().defaultCooldown : cooldown, lastUse);
        }
    }

}