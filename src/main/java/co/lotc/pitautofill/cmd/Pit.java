package co.lotc.pitautofill.cmd;

import co.lotc.core.command.annotate.Arg;
import co.lotc.core.command.annotate.Cmd;
import co.lotc.pitautofill.PitAutofill;
import co.lotc.pitautofill.PitList;
import co.lotc.pitautofill.ResourcePit;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import co.lotc.pitautofill.BaseCommand;

public class Pit extends BaseCommand {

	@Cmd(value="Creates a new pit with given [name]", permission="pit.create")
	public void create(CommandSender sender, @Arg(value = "pit_name", description = "The name of the pit you wish to create.")String name) {
		sender.sendMessage(PitAutofill.PREFIX + PitList.newPit(name));
	}
	@Cmd(value="Creates a new pit with given [name] {WorldGuard Region}", permission="pit.create")
	public void create(CommandSender sender,
					   @Arg(value = "pit_name", description = "The name of the pit in question.")String name,
					   @Arg(value = "region_name", description = "The name of the region you wish to assign.")String regionName) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			player.sendMessage(PitAutofill.PREFIX + PitList.newPit(name));
			player.sendMessage(PitAutofill.PREFIX + PitList.setPitRegion(name, regionName, player.getWorld().getName()) );
		} else {
			sender.sendMessage(PitAutofill.PREFIX + "If you're creating from console, please specify a world name after the region name.");
		}
	}
	@Cmd(value="Creates a new pit with given [name] {WorldGuard Region} {World Name}", permission="pit.create")
	public void create(CommandSender sender,
					   @Arg(value = "pit_name", description = "The name of the pit in question.")String name,
					   @Arg(value = "region_name", description = "The name of the region you wish to assign.")String regionName,
					   @Arg(value = "world_name", description = "The name of the world the region resides in.")String worldName) {
		sender.sendMessage(PitAutofill.PREFIX + PitList.newPit(name));
		sender.sendMessage(PitAutofill.PREFIX + PitList.setPitRegion(name, regionName, worldName));
	}


	@Cmd(value="Sets the default block chance if unspecified.", permission="pit.create")
	public void setdefaultchance(CommandSender sender, @Arg(value = "default block chance", description = "An number between 0 and 100 that represents the chance a block will have should it be unspecified.")int chance) {
		sender.sendMessage(PitAutofill.PREFIX + ResourcePit.setDefaultChanceValue(chance));
	}


	@Cmd(value="Set the given pit's WorldGuard region.", permission="pit.edit")
	public void setregion(CommandSender sender,
						  @Arg(value = "pit_name", description = "The name of the pit in question.")String name,
						  @Arg(value = "region_name", description = "The name of the region you wish to assign.")String regionName) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			player.sendMessage(PitAutofill.PREFIX + PitList.setPitRegion(name, regionName, player.getWorld().getName()) );
		} else {
			sender.sendMessage(PitAutofill.PREFIX + "If you're setting the region from console, please specify a world name after the region name.");
		}
	}
	@Cmd(value="Set the given pit's WorldGuard region.", permission="pit.edit")
	public void setregion(CommandSender sender,
						  @Arg(value = "pit_name", description = "The name of the pit in question.")String name,
						  @Arg(value = "region_name", description = "The name of the region you wish to assign.")String regionName,
						  @Arg(value = "world_name", description = "The name of the world the region resides in.")String worldName) {
		sender.sendMessage(PitAutofill.PREFIX + PitList.setPitRegion(name, regionName, worldName));
	}


	@Cmd(value="Change the given pit's name.", permission="pit.edit")
	public void setname(CommandSender sender,
						@Arg(value = "old_name", description = "The name of the pit in question.")String name,
						@Arg(value = "new_name", description = "The new name you wish to assign the pit.")String newName) {
		sender.sendMessage(PitAutofill.PREFIX + PitList.setPitName(name, newName));
	}


	@Cmd(value="Premanently deletes a given pit.", permission="pit.edit")
	public void delete(CommandSender sender, @Arg(value = "pit_name", description = "The name of the pit in question.")String name) {
		sender.sendMessage(PitAutofill.PREFIX + PitList.deletePit(name));
	}


	@Cmd(value="Set the given pit's block types by Block:##.", permission="pit.edit")
	public void setblocks(CommandSender sender,
						  @Arg(value = "pit_name", description = "The name of the pit in question.")String name,
						  @Arg(value = "block:##", description = "Any number of blocks and their chances to spawn, seperated by a ':'.")String[] blocks) {
		sender.sendMessage(PitAutofill.PREFIX + PitList.setPitBlocks(name, blocks));
	}


	@Cmd(value="Set the max percentage a pit can be filled when refilling.", permission="pit.edit")
	public void setrefillvalue(CommandSender sender,
							   @Arg(value = "pit_name", description = "The name of the pit in question.")String name,
							   @Arg(value = "refill value", description = "An number between 0 and 100 that represents the max saturation a pit can have when refilling.")int value) {
		sender.sendMessage(PitAutofill.PREFIX + PitList.setPitRefillValue(name, value));
	}


	@Cmd(value="Sets the given pit's cooldown in seconds.", permission="pit.edit")
	public void setcooldown(CommandSender sender,
							@Arg(value = "pit_name", description = "The name of the pit in question.")String name,
							@Arg(value = "cooldown", description = "The number of seconds that must pass before one can refill a pit again.")int value) {
		sender.sendMessage(PitAutofill.PREFIX + PitList.setCooldown(name, value));
	}


	@Cmd(value="Refills the given pit.", permission="pit.use")
	public void fill(CommandSender sender, @Arg(value = "pit_name", description = "The name of the pit in question.")String name) {
		sender.sendMessage(PitAutofill.PREFIX + PitList.fillPit(name));
	}


	@Cmd(value="Provides a list of saved pits.", permission="pit.info")
	public void list(CommandSender sender) {
		sender.sendMessage(PitAutofill.PREFIX + "Pits: " + PitList.getList());
	}


	@Cmd(value="Provides info on the specified pit.", permission="pit.info")
	public void info(CommandSender sender, @Arg(value = "pit_name", description = "The name of the pit in question.")String name) {

		ResourcePit thisPit = PitList.getPit(name);

		if (thisPit != null) {
			String typeList = "None";
			boolean first = true;

			// Comma seperated list of each block.
			for (Material mat : thisPit.getBlockChanceList()) {
				if (first) {
					typeList = thisPit.getBlockChance(mat) + "% " + mat.toString();
					first = false;
				} else {
					typeList += ", " + thisPit.getBlockChance(mat) + "% " + mat.toString();
				}
			}

			String message = (PitAutofill.ALT_COLOUR + ChatColor.BOLD + "Pit '" + name.toUpperCase() + "'" +
							  PitAutofill.PREFIX + "\nBlock Types: " + PitAutofill.ALT_COLOUR + typeList +
							  PitAutofill.PREFIX + "\nHas Region: " + PitAutofill.ALT_COLOUR + WordUtils.capitalize(thisPit.regionIsNotNull() + "") +
							  PitAutofill.PREFIX + "\nMin for Refill: " + PitAutofill.ALT_COLOUR + thisPit.getRefillValue() + "%");

			if (thisPit.getCooldown() <= 0) {
				message +=  PitAutofill.PREFIX + "\nCooldown: " + PitAutofill.ALT_COLOUR + "None";
			} else {
				message +=  PitAutofill.PREFIX + "\nCooldown: " + PitAutofill.ALT_COLOUR + thisPit.getCooldown() + " seconds";
			}

			if (thisPit.regionIsNotNull()) {
				message += (PitAutofill.PREFIX + "\nRegion Name: " + PitAutofill.ALT_COLOUR + thisPit.getRegion().getId() +
							PitAutofill.PREFIX + "\nWorld: " + PitAutofill.ALT_COLOUR + thisPit.getRegionWorld().getName());
			}

			sender.sendMessage(message);
		} else {
			sender.sendMessage(PitAutofill.PREFIX + "No pit found with the name '" + PitAutofill.ALT_COLOUR + name + PitAutofill.PREFIX + "'.");
		}
	}

}