package co.lotc.pitautofill;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class PitList {

	private static ArrayList<ResourcePit> allPitsList = new ArrayList<>();
	private static FileConfiguration config;

	//// STARTUP ////

	// Iterate through our list of pits for each world, plugging in the required info if specified in the config.
	public static void init() {

		config = PitAutofill.get().getConfig();

		if (config.getConfigurationSection("pits") == null)
			config.createSection("pits");

		for (String pitName : config.getConfigurationSection("pits").getKeys(false)) {

			String regionName = config.getString("pits." + pitName + ".regionName");
			String worldName = config.getString("pits." + pitName + ".worldName");
			ArrayList<String> blockTypes = new ArrayList<>();

			for (String block : config.getConfigurationSection("pits." + pitName + ".blockTypes").getKeys(false)) {
				blockTypes.add(block + ":" + config.getInt("pits." + pitName + ".blockTypes." + block));
			}


			PitAutofill.get().getServer().getLogger().info("[Pit-Autofill] " + ChatColor.stripColor(newPit(pitName)));

			if (regionName != null) {
				setPitRegion(pitName, regionName, worldName);
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
	public static String newPit(String name) {

		String output = "A pit with that name already exists.";

		if (getPit(name.toUpperCase()) == null) {
			allPitsList.add(new ResourcePit(name.toUpperCase()));

			config.createSection("pits." + name.toUpperCase());
			PitAutofill.get().saveConfig();

			output = "Successfully created the pit '" + PitAutofill.ALT_COLOUR + name.toUpperCase() + PitAutofill.PREFIX + "'.";
		}
		return output;
	}

	// Remove the pit with the same name from our list.
	public static String deletePit(String name) {

		String output = noPitFoundMsg(name.toUpperCase());

		ResourcePit thisPit = getPit(name.toUpperCase());
		if (thisPit != null) {
			allPitsList.remove(thisPit);

			config.set("pits." + name.toUpperCase(), null);
			PitAutofill.get().saveConfig();

			output = "Successfully deleted the pit '" + PitAutofill.ALT_COLOUR + name.toUpperCase() + PitAutofill.PREFIX + "'.";
		}
		return output;
	}

	// Sets the given pit's region name.
	public static String setPitRegion(String name, String region, String worldName) {

		String output = noPitFoundMsg(name.toUpperCase());

		ResourcePit thisPit = getPit(name.toUpperCase());
		if (thisPit != null) {
			// Saves to config inside setRegion.
			output = thisPit.setRegion(region, worldName);
		}
		return output;
	}

	// Sets the given pit's block types.
	public static String setPitBlocks(String name, String[] blockTypes) {

		String output = noPitFoundMsg(name.toUpperCase());

		ResourcePit thisPit = getPit(name.toUpperCase());
		if (thisPit != null) {
			// Saves to config inside setBlockTypes.
			output = thisPit.setBlockTypes(blockTypes);
		}
		return output;
	}

	// Changes the given pit's name.
	public static String setPitName(String name, String newName) {
		String output = noPitFoundMsg(name.toUpperCase());

		ResourcePit thisPit = getPit(name.toUpperCase());
		if (thisPit != null) {
			// Saves to config inside setName.
			output = thisPit.setName(newName.toUpperCase());
		}
		return output;
	}

	// Changes the given pit's refill value.
	public static String setPitRefillValue(String name, int value) {
		String output = noPitFoundMsg(name.toUpperCase());

		ResourcePit thisPit = getPit(name.toUpperCase());
		if (thisPit != null) {
			output = thisPit.setRefillValue(value);
		}
		return output;
	}

	// Refills the given pit.
	public static String fillPit(String name) {

		String output = noPitFoundMsg(name.toUpperCase());

		ResourcePit thisPit = getPit(name.toUpperCase());
		if (thisPit != null) {
			output = thisPit.fill();
		}
		return output;
	}

	// Retrieves a string list of stored pits.
	public static String getList() {
		String output = PitAutofill.ALT_COLOUR + "";
		boolean firstName = true;

		for (ResourcePit pit : allPitsList) {
			if (firstName) {
				output += pit.getName().toUpperCase();
				firstName = false;
			} else {
				output += ", " + pit.getName().toUpperCase();
			}
		}

		return output;
	}

	// Returns the ResourcePit with the matching name.
	public static ResourcePit getPit(String name) {

		for (ResourcePit thisPit : allPitsList) {
			if (thisPit.getName().equalsIgnoreCase(name.toUpperCase()))
				return thisPit;
		}
		return null;
	}


	//// PRIVATE ////

	// Returns the pit not found error message.
	private static String noPitFoundMsg(String name) {
		return "No pit found with the name '" + PitAutofill.ALT_COLOUR + name.toUpperCase() + PitAutofill.PREFIX + "'.";
	}

}
