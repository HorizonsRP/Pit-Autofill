package co.lotc.pitautofill;

import co.lotc.pitautofill.cmd.Pit;
import co.lotc.core.bukkit.command.Commands;

import com.griefcraft.lwc.LWCPlugin;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class PitAutofill extends JavaPlugin {

    public static final String PREFIX = (ChatColor.GOLD + "");      // The same colouring prefix to be used throughout the plugin.
    private static PitAutofill instance;
    public static PitAutofill get() { return instance; }            // Provides an accessor to our current instance.

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Bukkit.getPluginManager().registerEvents(new FillSignListener(), this);

        Commands.build(getCommand("pit"), Pit::new);
    }

    @Override
    public void onDisable() { }

    /*
     * Loads all existing pits from the config.
     */
    private void loadPits() {
        //TODO: Format and load pit info from configs.
    }

}// class