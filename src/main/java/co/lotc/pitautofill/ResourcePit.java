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

	/*
	 * Sets the pit's region to a worldguard region with the given name, if found.
	 */
	public String setRegion(String regionName, World givenWorld) {
		String output = "Could not find a region with that name in that world.";

		RegionContainer container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager worldRegions = container.get(BukkitAdapter.adapt(givenWorld));

		// If our region set exists and has the specified region we set the pit's region to that.
		if (worldRegions != null && worldRegions.hasRegion(regionName)) {
			region = worldRegions.getRegion(regionName);
			world = givenWorld;
			output = "The pit '" + name + "' now uses the region '" + regionName + "'.";
		}

		return output;
	}

	/*
	 * Takes a set of strings as arguments to set the new block types.
	 * Assumes the format to be as block:chance where chance is chance%
	 * of that type being placed.
	 */
	public void setBlockTypes(String[] blockAndChance) {

		// Create a blank hashmap.
		HashMap<Material, Integer> newBlockTypes = new HashMap<>();

		for (String arg : blockAndChance) {

			// Get the index of our divider ":"
			int chanceIndex = arg.indexOf(":");
			// Initialize our block values.
			String type;
			int chance = 100;

			// If we have an index value we assign the first substring and try to parseInt the second.
			if (chanceIndex != -1) {
				type = arg.substring(0, chanceIndex);
				try {
					chance = Integer.parseInt(arg.substring(chanceIndex+1));
				} catch(NumberFormatException nfe) {
					chance = 100;
				}
				// Otherwise we assume the entire string is the block type and leave chance at -1.
			} else {
				type = arg;
			}
			// Grab the matching material and link it with the chance on our blockTypes hashmap.
			newBlockTypes.put(Material.matchMaterial(type), chance);
		}

		// If we got a blockType set of at least one we assign it now.
		if (newBlockTypes.keySet().size() > 0) {
			blockTypes = newBlockTypes;
		}

	}


	//// ACCESSORS ////

	/*
	 * Returns the pit's name string.
	 */
	public String getName() {
		return name;
	}

	/*
	 * Returns the pit's world.
	 */
	public World getRegionWorld() {
		return world;
	}

	/*
	 * Returns an ArrayList of the pit's materials ordered from highest to
	 * lowest chance.
	 */
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

	/*
	 * Returns the chance integer of the given material,
	 * if it exists in this pit.
	 */
	public double getBlockChance(Material material) {
		double output = -1;
		if (blockTypes.get(material) != null)
			output = blockTypes.get(material) / 100.0;
		return output;
	}

	/*
	 * Returns the ArrayList of all block locations in this pit.
	 */
	public ProtectedRegion getRegion() {
		return region;
	}


	//// OPERATORS ////

	/*
	 * So long as there're no players in the pit, fill it. If there are
	 * players, return a message to the sender as such.
	 */
	public String fill() {

		String output = "Pit Autofill Error. Please check with your administrator.";
		if (!playersAreInside()) {
			if (Bukkit.getPluginManager().isPluginEnabled("LWC"))
				removeLocks();
			if (changeBlocks()) {
				output = "The pit has been refilled.";
			} else {
				output = "No blocks or region specified for that pit.";
			}
		} else {
			output = "There are still players inside the pit.";
		}

		return output;
	}

	/*
	 * Returns true if there are any players inside the pit.
	 */
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

	/*
	 * Removes all block locks within the region.
	 */
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
	 * Changes all blocks within the region to the current blocklist. We use
	 * Math.random, going up through our list until we find a block chance that's
	 * higher than our random value. Once we do, set block location to the previous
	 * material we checked.
	 */
	private boolean changeBlocks() {
		boolean output = true;

		for (Location loc : getBlockLocationList()) {
			// Initialize our random value and the running total of this location.
			double randomChance = Math.random();
			double currentTotal = 0;

			// Run through our list of materials, checking their chances.
			Material finalMat = null;
			for(Material mat : getBlockList()) {
				double blockChance = getBlockChance(mat);

				// If we're still in this loop and our total chance is
				// greater than our random chance, then it has to be the
				// current material.
				if(blockChance + currentTotal >= randomChance) {
					finalMat = mat;
					break;
				}

				// If we didn't find our finalMat, we add our current chance to
				// the total so we can check the next section when we loop next.
				if (blockChance > 0)
					currentTotal += blockChance;
			}

			// Set this location's block to our chosen material. If there is none, we return false.
			if (finalMat != null) {
				loc.getBlock().setType(finalMat);
			} else {
				output = false;
				break;
			}
		}

		return output;
	}

	/*
	 * Returns a list of the block locations found in this region.
	 */
	private ArrayList<Location> getBlockLocationList() {
		ArrayList<Location> output = new ArrayList<>();

		// Grabs the min and max of our region.
		BlockVector3 min = region.getMinimumPoint();
		BlockVector3 max = region.getMaximumPoint();

		// Runs through the x y z coords between min and max
		for (int i = min.getBlockX(); i <= max.getBlockX(); i++) {
			for (int j = min.getBlockY(); j <= max.getBlockY(); j++) {
				for (int k = min.getBlockZ(); k <= max.getBlockZ(); k++) {
					// Add each location to our output.
					output.add(new Location(world, i, j, k));
				}
			}
		}// block for

		return output;
	}
}