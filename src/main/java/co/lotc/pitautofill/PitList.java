package co.lotc.pitautofill;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class PitList {

	private static ArrayList<ResourcePit> allPitsList = new ArrayList<ResourcePit>();

	//// STARTUP ////

	public static void init() {
		// TODO: Load pits from config.
	}


	//// PUBLIC ////

	// Create a new pit with the given name and add to our list.
	public static String newPit(String name) {
		allPitsList.add(new ResourcePit(name));
		return "Successfully created the pit '" + name + "'.";
	}

	// Remove the pit with the same name from our list.
	public static String deletePit(String name) {

		String output = noPitFoundMsg(name);

		ResourcePit thisPit = getPit(name);
		if (thisPit != null) {
			allPitsList.remove(thisPit);
			output = "Successfully deleted the pit '" + name + "'.";
		}
		return output;
	}

	// Sets the given pit's region name.
	public static String setPitRegion(String name, String region, World world) {

		String output = noPitFoundMsg(name);

		ResourcePit thisPit = getPit(name);
		if (thisPit != null) {
			output = thisPit.setRegion(region, world);
		}
		return output;
	}

	// Sets the given pit's block types.
	public static String setPitBlocks(String name, String[] blockTypes) {

		String output = noPitFoundMsg(name);

		ResourcePit thisPit = getPit(name);
		if (thisPit != null) {
			output = thisPit.setBlockTypes(blockTypes);
		}
		return output;
	}

	// Refills the given pit.
	public static String fillPit(String name) {

		String output = noPitFoundMsg(name);

		ResourcePit thisPit = getPit(name);
		if (thisPit != null) {
			output = thisPit.fill();
		}
		return output;
	}


	//// PRIVATE ////

	// Returns the ResourcePit with the matching name.
	private static ResourcePit getPit(String name) {

		for (ResourcePit thisPit : allPitsList) {
			if (thisPit.getName().equalsIgnoreCase(name))
				return thisPit;
		}
		return null;
	}

	// Returns the pit not found error message.
	private static String noPitFoundMsg(String name) {
		return "No pit found with the name '" + name + "'.";
	}

}
