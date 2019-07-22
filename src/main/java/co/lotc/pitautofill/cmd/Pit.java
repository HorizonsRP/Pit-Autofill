package co.lotc.pitautofill.cmd;

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

	/*
	TODO LIST:
	Save & Load existing pits.
	Maybe add a cooldown?
	 */

	// PitAutofill Info
	public void invoke(CommandSender sender) {
		sender.sendMessage(PitAutofill.PREFIX + "Pit Autofill refills the resource pits on command. Use /help Pit-Autofill for more information.");
	}


	@Cmd(value="Creates a new pit with given [name]", permission="pit.create")
	public void create(CommandSender sender, String name) {
		sender.sendMessage(PitAutofill.PREFIX + PitList.newPit(name));
	}
	@Cmd(value="Creates a new pit with given [name] {WorldGuard Region}", permission="pit.create")
	public void create(CommandSender sender, String name, String regionName) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			player.sendMessage(PitAutofill.PREFIX + PitList.newPit(name));
			player.sendMessage(PitAutofill.PREFIX + PitList.setPitRegion(name, regionName, player.getWorld()) );
		} else {
			sender.sendMessage(PitAutofill.PREFIX + "If you're creating from console, please specify a world name after the region name.");
		}
	}
	@Cmd(value="Creates a new pit with given [name] {WorldGuard Region} {World Name}", permission="pit.create")
	public void create(CommandSender sender, String name, String regionName, String worldName) {
		sender.sendMessage(PitAutofill.PREFIX + PitList.newPit(name));
		sender.sendMessage(PitAutofill.PREFIX + PitList.setPitRegion(name, regionName, Bukkit.getWorld(worldName)) );
	}


	@Cmd(value="Set the given pit's WorldGuard region.", permission="pit.edit")
	public void setregion(CommandSender sender, String name, String regionName) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			player.sendMessage(PitAutofill.PREFIX + PitList.setPitRegion(name, regionName, player.getWorld()) );
		} else {
			sender.sendMessage(PitAutofill.PREFIX + "If you're setting the region from console, please specify a world name after the region name.");
		}
	}
	@Cmd(value="Set the given pit's WorldGuard region.", permission="pit.edit")
	public void setregion(CommandSender sender, String name, String regionName, String worldName) {
		sender.sendMessage(PitAutofill.PREFIX + PitList.setPitRegion(name, regionName, Bukkit.getWorld(worldName)) );
	}


	@Cmd(value="Premanently deletes a given pit.", permission="pit.edit")
	public void delete(CommandSender sender, String name) {
		sender.sendMessage(PitAutofill.PREFIX + PitList.deletePit(name));
	}


	@Cmd(value="Set the given pit's block types by Block:##.", permission="pit.edit")
	public void setblocks(CommandSender sender, String name, String[] blocks) {
		sender.sendMessage(PitAutofill.PREFIX + PitList.setPitBlocks(name, blocks));
	}


	@Cmd(value="Refills the given pit.", permission="pit.edit")
	public void fill(CommandSender sender, String name) {
		sender.sendMessage(PitAutofill.PREFIX + PitList.fillPit(name));
	}


	@Cmd(value="Provides a list of saved pits.", permission="pit.info")
	public void list(CommandSender sender) {
		sender.sendMessage(PitAutofill.PREFIX + "Pits: " + PitList.getList());
	}


	@Cmd(value="Provides info on the specified pit.", permission="pit.info")
	public void info(CommandSender sender, String name) {

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
							  PitAutofill.PREFIX + "\nHas Region: " + PitAutofill.ALT_COLOUR + WordUtils.capitalize(thisPit.regionIsNotNull() + ""));

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