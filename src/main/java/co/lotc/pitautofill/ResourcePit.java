package co.lotc.pitautofill;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Protection;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

public class ResourcePit {

	private String name;                           // The name/ID of the pit.
	private String regionName;                     // The name/ID of the pit WorldGuard region.
	private World world;                           // Stores the world the region resides in.
	private HashMap<Material, Integer> blockTypes; // Stored as block Material, chance Integer
	private ProtectedRegion region;                // Stores the WorldGuard region.


	//// CONSTRUCTORS ////

	// Constructor called on with given name. Initializes all other values.
	public ResourcePit(String givenName) {
		setName(givenName);
		region = null;
		blockTypes = new HashMap<>();
	}


	//// MODIFIERS ////

	// Sets the region's name to the new name.
	public void setName(String givenName) {
		name = givenName;
	}

	// Sets the pit's region to a worldguard region with the given name, if found.
	public String setRegion(String regionName, World givenWorld) {
		String output = "Could not find a region with that name in that world.";

		RegionContainer container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager worldRegions = container.get(BukkitAdapter.adapt(givenWorld));

		// If our region set exists and has the specified region we set the pit's region to that.
		if (worldRegions != null && worldRegions.hasRegion(regionName)) {
			region = worldRegions.getRegion(regionName);
			world = givenWorld;
			output = "The pit '" + name + "' has been assigned the region '" + regionName + "'.";
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

			// Default our parseInt to 100 if it doesn't work.
			if (chanceIndex != -1) {
				type = arg.substring(0, chanceIndex);
				try {
					chance = Integer.parseInt(arg.substring(chanceIndex+1));
				} catch (NumberFormatException nfe) {
					chance = 100;
				}
			} else {
				type = arg;
			}

			Material thisMaterial = Material.matchMaterial(type);
			if (thisMaterial != null)
				newBlockTypes.put(thisMaterial, chance);
		}

		if (newBlockTypes.keySet().size() > 0) {
			blockTypes = newBlockTypes;
		} else {
			output = "Please specify the blocks and their chances.";
		}

		return output;
	}


	//// ACCESSORS ////

	// Returns the pit's name string.
	public String getName() {
		return name;
	}

	// Returns the pit's world.
	public World getRegionWorld() {
		return world;
	}

	// Returns an ArrayList of the pit's materials ordered from highest to lowest chance.
	public ArrayList<Material> getBlockList() {

		// Create an output ArrayList and a dummy keySet so that we can edit it
		// without affecting our blockTypes HashMap.
		ArrayList<Material> output = new ArrayList<>();
		ArrayList<Material> keySet = new ArrayList<>();
		for (Material key : blockTypes.keySet()) {
			keySet.add(key);
		}

		// Run through adding as many entries as we have blockTypes, sorting from
		// highest to lowest chance.
		while (keySet.size() > 0) {

			Material highestChance = null;
			for (Material mat : keySet) {
				// Check to see if each mat's chance is higher than our highest chance.
				if (highestChance != null) {
					if (blockTypes.get(mat) > blockTypes.get(highestChance)) {
						highestChance = mat;
					}
					// If our highestChance is still null, just set it to the first value.
				} else {
					highestChance = mat;
				}
			}

			// Add our highestChance to our output and remove it from our temp list.
			output.add(highestChance);
			keySet.remove(highestChance);
		}

		//Send out our finalized list.
		return output;
	}

	// Returns the chance integer of the given material, if it exists in this pit.
	public double getBlockChance(Material material) {
		double output = -1;
		if (blockTypes.get(material) != null)
			output = blockTypes.get(material) / 100.0;
		return output;
	}

	// Returns the ArrayList of all block locations in this pit.
	public ProtectedRegion getRegion() {
		return region;
	}


	//// OPERATORS ////

	/*
	So long as there're no players in the pit, fill it. If there are
	players, return a message to the sender as such.
	 */
	public String fill() {

		String output = "Pit Autofill Error. Please check with your administrator.";
		if (!playersAreInside()) {
			if (Bukkit.getPluginManager().isPluginEnabled("LWC"))
				removeLocks();
			output = changeBlocks();
		} else {
			output = "There are still players inside the pit.";
		}

		return output;
	}

	// Returns true if there are any players inside the pit.
	private boolean playersAreInside() {
		boolean output = false;

		// For each player create a location stripped of pitch and yaw.
		for (Player player : Bukkit.getOnlinePlayers()) {
			// If our player is in the same world
			if (player.getWorld().equals(world)) {
				Location loc = player.getLocation();
				if (region.contains(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()))) {
					output = true;
					break;
				}
			}
		}// player for

		return output;
	}

	// Removes all block locks within the region.
	private void removeLocks() {
		// For each location in the region, if there's a lock, remove it.
		/*for (Location loc : getBlockLocationList()) {
			LWCPlugin lwcPlugin = Bukkit.getPluginManager().getPlugin("LWC");
			Protection prot = null;
			if (prot != null) {
				prot.remove();
			}
		}*/
	}

	/*
	Changes all blocks within the region to the current blocklist. We use
	Math.random, going up through our list until we find a block chance that's
	higher than our random value. Once we do, set block location to the previous
	material we checked.
	 */
	private String changeBlocks() {
		String output = "The pit has been refilled.";

		if (region != null) {
			for (Location loc : getBlockLocationList()) {

				double randomChance = Math.random();
				double currentTotal = 0;

				// Run through our list of materials and setting the block based on chance.
				Material finalMat = null;
				for (Material mat : getBlockList()) {
					double blockChance = getBlockChance(mat);

					if (blockChance + currentTotal >= randomChance) {
						finalMat = mat;
						break;
					}

					if (blockChance > 0)
						currentTotal += blockChance;
				}

				if (finalMat != null) {
					loc.getBlock().setType(finalMat);
				} else {
					output = "No blocks specified for that pit.";
					break;
				}
			}
		} else {
			output = "No region specified for that pit.";
		}

		return output;
	}

	// Returns a list of the block locations found in this region.
	private ArrayList<Location> getBlockLocationList() {
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
}