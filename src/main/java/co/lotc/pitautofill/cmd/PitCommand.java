package co.lotc.pitautofill.cmd;

import co.lotc.core.command.annotate.Arg;
import co.lotc.core.command.annotate.Cmd;
import co.lotc.core.command.annotate.Flag;
import co.lotc.core.command.annotate.Range;
import co.lotc.core.util.TimeUtil;
import co.lotc.pitautofill.BaseCommand;
import co.lotc.pitautofill.PitAutofill;
import co.lotc.pitautofill.ResourcePit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.stream.Collectors;

public class PitCommand extends BaseCommand {

	@Cmd(value="Creates a new pit with given [Name] [Region] {Blocks}", permission="pit.create")
    @Flag(name = "world", description = "Specifies a specific world for this pit", type = World.class)
	public void create(CommandSender sender,
                       @Arg(value = "Pit_Name", description = "The name of the pit in question.") String name,
                       @Arg(value = "Region_Name", description = "The name of the region you wish to assign.") String regionName,
                       @Arg(value = "Block:##", description = "Any number of blocks and their chances to spawn, seperated by a ':'.") String[] blocks) {

		// Check that we have a world either by the player's location or provided with -world flag.
        validate(sender instanceof Player || hasFlag("world"), "If running from console provide the world via -world flag");
        World world = hasFlag("world") ? getFlag("world") : ((Player) sender).getWorld();
        validate(world != null, "World not found, please specify a world with -world");

        // We make region non-optional as it's important to set this up initially. A pit without a region might as well not exist
        ProtectedRegion region = plugin.getRegion(regionName, world);
        validate(region != null, "Region " + regionName + " not found, please specify another");

        // Warns the user that more than one pit are using this region. Does not throw an error as this may be a user choice.
        if (plugin.getPits().stream().anyMatch(pit -> pit.getRegion() == region && pit.getRegionWorld() == world)) {
        	msg(PitAutofill.PREFIX + "CAUTION:" + PitAutofill.ALT_COLOUR + " Multiple pits now share that region.");
		}

        // Build a pit with a region.
        ResourcePit pit = ResourcePit.builder(name)
									 .world(world)
									 .region(region)
									 .build();

        String output = pit.setBlockTypes(blocks);
        validate(output == null, output);

        // Register and save the pit
        plugin.addPit(pit);
        pit.save(pit.getName());
        msg(PitAutofill.PREFIX + "Successfully created the pit with the name " + name);
	}


	@Cmd(value="Sets the default block chance if unspecified.", permission="pit.create")
    public void setdefaultchance(@Arg(value = "Default Block Chance", description = "An number between 0 and 100 that represents the chance a block will have should it be unspecified.")
                                 @Range(min = 0, max = 100) int chance) {
        plugin.setDefaultChanceValue(chance);
        msg(PitAutofill.PREFIX + "Updated the default chance value for all pits.");

	}


	@Cmd(value="Set the given pit's WorldGuard region.", permission="pit.edit")
    @Flag(name = "world", description = "Specifies a specific world for this pit", type = World.class)
	public void setregion(CommandSender sender,
                          ResourcePit pit,
                          @Arg(value = "Region_Name", description = "The name of the region you wish to assign.") String regionName) {

		// Get our sender's world or the provided -world flag.
        validate(sender instanceof Player || hasFlag("world"), "If running from console provide the world via -world flag");
        World world = hasFlag("world") ? getFlag("world") : ((Player) sender).getWorld();

        // Check if the world has regions.
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        validate(manager != null, "Could not locate this world for valid regions");

        // Check if the world has that specific region.
        ProtectedRegion region = manager.getRegion(regionName);
        validate(region != null, "Could not locate a region with the name " + regionName);

        // Set the pit's region and let the sender know.
        pit.setRegion(region, world);
        msg(PitAutofill.PREFIX + "The pit '" + PitAutofill.ALT_COLOUR + pit.getName().toUpperCase() + PitAutofill.PREFIX + "' has been assigned the region '" +
                PitAutofill.ALT_COLOUR + regionName.toUpperCase() + PitAutofill.PREFIX + "'.");

	}


	@Cmd(value="Change the given pit's name.", permission="pit.edit")
    public void setname(ResourcePit pit,
                        @Arg(value = "New_Name", description = "The new name you wish to assign the pit.") String newName) {

        msg(PitAutofill.PREFIX + pit.setName(newName.toUpperCase()));
	}


	@Cmd(value="Premanently deletes a given pit.", permission="pit.edit")
    public void delete(ResourcePit pit) {

        msg(PitAutofill.PREFIX + plugin.deletePit(pit));
	}


	@Cmd(value="Set the given pit's block types by Block:##.", permission="pit.edit")
    public void setblocks(ResourcePit pit,
                          @Arg(value = "Block:##", description = "Any number of blocks and their chances to spawn, seperated by a ':'.") String[] blocks) {

        msg(PitAutofill.PREFIX + pit.setBlockTypes(blocks));
	}


	@Cmd(value="Set the max percentage a pit can be filled when refilling.", permission="pit.edit")
    public void setrefillvalue(ResourcePit pit,
                               @Arg(value = "Refill Value", description = "A number between 0 and 100 that represents the max saturation a pit can have when refilling.")
                               @Range(min = 0, max = 100) int value) {

        pit.setRefillValue(value);
        msg(PitAutofill.PREFIX + "Set the pit " + pit.getName() + "'s refill value to " + PitAutofill.ALT_COLOUR + value + PitAutofill.PREFIX + ".");
	}


	@Cmd(value="Sets the given pit's cooldown in seconds.", permission="pit.edit")
    public void setcooldown(ResourcePit pit,
                            @Arg(value = "Cooldown", description = "The number of seconds that must pass before one can refill a pit again.") int cooldown) {

        validate(cooldown > -1, "Cooldown cannot be negative");

        pit.setCooldown(cooldown);
        msg(PitAutofill.PREFIX + "Cooldown successfully set to " + PitAutofill.ALT_COLOUR + cooldown + " seconds" + PitAutofill.PREFIX + ".");
	}


	@Cmd(value="Sets the given pit's child which will be chain-filled.", permission="pit.edit")
	public void setchild(@Arg(value = "Parent", description = "The parent pit.") ResourcePit parent,
						 @Arg(value = "Child", description = "The child pit.") ResourcePit child) {
		msg(PitAutofill.PREFIX + parent.setChildPit(child));
	}


	// /pit fill -o overrides the cooldown & fill %
	@Cmd(value="Refills the given pit.", permission="pit.use")
    @Flag(name = "o", description = "Refills the pit regardless of cooldown or saturation.", permission = "pit.edit")
    public void fill(CommandSender sender, ResourcePit pit) {
        String result = pit.fill(sender, hasFlag("o"));
        msg(PitAutofill.PREFIX + result);
	}


	@Cmd(value="Provides a list of saved pits.", permission="pit.info")
    public void list() {
        msg(PitAutofill.PREFIX + "Pits: " + PitAutofill.ALT_COLOUR + plugin.getPits().stream().map(ResourcePit::getName).collect(Collectors.joining(", ")));
	}


	@Cmd(value="Provides info on the specified pit.", permission="pit.info")
    public void info(ResourcePit thisPit) {
        StringBuilder typeList = new StringBuilder("None");
        boolean first = true;

        // Comma seperated list of each block.
        for (Material mat : thisPit.getBlockChanceList()) {
            if (first) {
                typeList = new StringBuilder(thisPit.getBlockChance(mat) + "% " + mat.toString());
                first = false;
            } else {
                typeList.append(", ").append(thisPit.getBlockChance(mat)).append("% ").append(mat.toString());
            }
        }

        String message = (PitAutofill.ALT_COLOUR + ChatColor.BOLD + "Pit '" + thisPit.getName().toUpperCase() + "'" +
						  PitAutofill.PREFIX + "\nBlock Types: " + PitAutofill.ALT_COLOUR + typeList +
						  PitAutofill.PREFIX + "\nHas Region: " + PitAutofill.ALT_COLOUR + WordUtils.capitalize(thisPit.regionIsNotNull() + "") +
						  PitAutofill.PREFIX + "\nMin for Refill: " + PitAutofill.ALT_COLOUR + thisPit.getRefillValue() + "%");

        if (thisPit.getCooldown() <= 0) {
            message += PitAutofill.PREFIX + "\nCooldown: " + PitAutofill.ALT_COLOUR + "None";
        } else {
            message += PitAutofill.PREFIX + "\nCooldown: " + PitAutofill.ALT_COLOUR + thisPit.getCooldown() + " seconds";
        }

        if (thisPit.regionIsNotNull()) {
            message += (PitAutofill.PREFIX + "\nRegion Name: " + PitAutofill.ALT_COLOUR + thisPit.getRegion().getId() +
						PitAutofill.PREFIX + "\nWorld: " + PitAutofill.ALT_COLOUR + thisPit.getRegionWorld().getName());
        }

        msg(message);
	}

}