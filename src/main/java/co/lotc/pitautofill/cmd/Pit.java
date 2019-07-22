package co.lotc.pitautofill.cmd;

import co.lotc.core.command.annotate.Cmd;
import co.lotc.pitautofill.PitAutofill;
import co.lotc.pitautofill.PitList;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import co.lotc.pitautofill.BaseCommand;

public class Pit extends BaseCommand {

	/*
	TODO LIST:
	Save & Load existing pits.
	Maybe add a cooldown?
	/pit info > Show info about the bit to sender.
	 */

	// PitAutofill Info
	public void invoke(CommandSender sender) {
		sender.sendMessage(PitAutofill.PREFIX + "Pit Autofill refills the resource pits on command. Use /help Pit-Autofill for more information.");
	}


	// Create new pit
	@Cmd(value="Creates a new pit with given [name] {WorldGuard Region}", permission="pit.create")
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

	@Cmd(value="Creates a new pit with given [name] {WorldGuard Region}", permission="pit.create")
	public void create(CommandSender sender, String name, String regionName, String worldName) {
		sender.sendMessage(PitAutofill.PREFIX + PitList.newPit(name));
		sender.sendMessage(PitAutofill.PREFIX + PitList.setPitRegion(name, regionName, Bukkit.getWorld(worldName)) );
	}


	// Deletion of a pit
	@Cmd(value="Premanently deletes a given pit.", permission="pit.edit")
	public void delete(CommandSender sender, String name) {
		sender.sendMessage(PitAutofill.PREFIX + PitList.deletePit(name));
	}


	// Sets the pit's worldguard region.
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


	// Sets the pit's block types.
	@Cmd(value="Set the given pit's block types by ##%Block.", permission="pit.edit")
	public void setblocks(CommandSender sender, String name, String[] blocks) {
		sender.sendMessage(PitAutofill.PREFIX + PitList.setPitBlocks(name, blocks));
	}


	// Refills the given pit.
	@Cmd(value="Refill the given pit.", permission="pit.edit")
	public void fill(CommandSender sender, String name) {
		sender.sendMessage(PitAutofill.PREFIX + PitList.fillPit(name));
	}


	// Provides a list of saved pits.
	@Cmd(value="List of existing pits.", permission="pit.info")
	public void list(CommandSender sender) {
		sender.sendMessage(PitAutofill.PREFIX + "Pits: " + PitList.getList());
	}

}