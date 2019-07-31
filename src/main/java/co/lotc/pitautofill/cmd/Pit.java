package co.lotc.pitautofill.cmd;

import co.lotc.core.command.annotate.Arg;
import co.lotc.core.command.annotate.Cmd;
import co.lotc.core.command.annotate.Flag;
import co.lotc.pitautofill.BaseCommand;
import co.lotc.pitautofill.PitAutofill;
import co.lotc.pitautofill.ResourcePit;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Pit extends BaseCommand {

	/*
	501 notes

	I made it so that World.class is a valid parameter. I registered it in PitAutofill.java, and now /pit will allow tab completion of the world.
	I also did this for ResourcePit, and now /pit will allow tab completion of all available ResourcePits

	After some refactoring the CommandSender parameter wasn't needed, so it can be safely removed

	 */

	@Cmd(value="Creates a new pit with given [name] {WorldGuard Region}", permission="pit.create")
    // We make -world an optional parameter that can be provided
    @Flag(name = "world", description = "Specifies a specific world for this pit", type = World.class)
	public void create(CommandSender sender,
					   @Arg(value = "pit_name", description = "The name of the pit in question.") String name,
					   @Arg(value = "region_name", description = "The name of the region you wish to assign.") String regionName) {
        // the validate method is a fun utility method sporadic provides. If the condition on the left is false then an exception is thrown (aka rest of the code doesnt run) and we send the message on the right to the players.
        validate(sender instanceof Player || hasFlag("world"), "If running from console provide the world via -world flag");
        World world = hasFlag("world") ? getFlag("world") : ((Player) sender).getWorld();
        // MSG is much shorter than sender.sendMessage and accomplishes the same goals
        // Under the hood it gets whatever sent this command and relays the provided text to them, provided by your BaseCommand implementation
        msg(PitAutofill.PREFIX + plugin.newPit(name));
        msg(PitAutofill.PREFIX + plugin.setPitRegion(name, regionName, world));
	}

	@Cmd(value="Sets the default block chance if unspecified.", permission="pit.create")
    public void setdefaultchance(@Arg(value = "default block chance", description = "An number between 0 and 100 that represents the chance a block will have should it be unspecified.") int chance) {
        msg(PitAutofill.PREFIX + ResourcePit.setDefaultChanceValue(chance));
	}

	@Cmd(value="Set the given pit's WorldGuard region.", permission="pit.edit")
    @Flag(name = "world", description = "Specifies a specific world for this pit", type = World.class)
	public void setregion(CommandSender sender,
                          ResourcePit pit,
                          @Arg(value = "region_name", description = "The name of the region you wish to assign.") String regionName) {
        validate(sender instanceof Player || hasFlag("world"), "If running from console provide the world via -world flag");
        World world = hasFlag("world") ? getFlag("world") : ((Player) sender).getWorld();
        msg(PitAutofill.PREFIX + pit.setRegion(regionName, world));
	}

	@Cmd(value="Change the given pit's name.", permission="pit.edit")
    public void setname(ResourcePit pit,
                        @Arg(value = "new_name", description = "The new name you wish to assign the pit.") String newName) {
        msg(PitAutofill.PREFIX + pit.setName(newName));
	}

	@Cmd(value="Premanently deletes a given pit.", permission="pit.edit")
    public void delete(ResourcePit pit) {
        msg(PitAutofill.PREFIX + plugin.deletePit(pit));
	}

	@Cmd(value="Set the given pit's block types by Block:##.", permission="pit.edit")
    public void setblocks(ResourcePit pit,
                          @Arg(value = "block:##", description = "Any number of blocks and their chances to spawn, seperated by a ':'.") String[] blocks) {
        msg(PitAutofill.PREFIX + pit.setBlockTypes(blocks));
	}

	@Cmd(value="Set the max percentage a pit can be filled when refilling.", permission="pit.edit")
    public void setrefillvalue(ResourcePit pit,
                               @Arg(value = "refill value", description = "An number between 0 and 100 that represents the max saturation a pit can have when refilling.") int value) {
        msg(PitAutofill.PREFIX + pit.setRefillValue(value));
	}

	@Cmd(value="Sets the given pit's cooldown in seconds.", permission="pit.edit")
    public void setcooldown(ResourcePit pit,
                            @Arg(value = "cooldown", description = "The number of seconds that must pass before one can refill a pit again.") int value) {
        msg(PitAutofill.PREFIX + pit.setCooldown(value));
	}

	@Cmd(value="Refills the given pit.", permission="pit.use")
    // /pit fill -o overrides the cooldown
    @Flag(name = "o", description = "Overrides the cooldown on this pit", permission = "pit.edit")
    public void fill(CommandSender sender, ResourcePit pit) {
        if (hasFlag("o")) {
            msg(PitAutofill.PREFIX + pit.fillOverride(sender));
        } else {
            msg(PitAutofill.PREFIX + pit.fill(sender));
        }
	}

	@Cmd(value="Provides a list of saved pits.", permission="pit.info")
    public void list() {
        msg(PitAutofill.PREFIX + "Pits: " + plugin.getList());
	}

	@Cmd(value="Provides info on the specified pit.", permission="pit.info")
    public void info(ResourcePit thisPit) {
        // String concatenation in a for loop is a dangerous practice. Use a String Builder when doing this in the future - 501
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