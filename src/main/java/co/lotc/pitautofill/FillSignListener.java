package co.lotc.pitautofill;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class FillSignListener implements Listener {

	private final static String SIGN_IDENTIFIER = "[Refill]";
	private final PitAutofill plugin;

	FillSignListener(PitAutofill plugin) {
		this.plugin = plugin;
	}

	@EventHandler // We don't ignore cancelled here so that players can interact with locked signs.
	public void OnSignRightclick(PlayerInteractEvent event) {
		// This event fires twice, once for each hand. This makes it so that unless the hand is our MAIN HAND then we don't operate. Yes, this event is a bit dumb for that reason. - 501
		if (event.getHand() != EquipmentSlot.HAND) {
			return;
		}
		Block block = event.getClickedBlock();
		// Block can be null ESPECIALLY if we dont ignore cancelled events.
		if (block == null) {
			return;
		}

		// RClick && Either Sign Type
		if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) &&
			(block.getType().equals(Material.WALL_SIGN) ||
			 block.getType().equals(Material.SIGN))) {

			Sign sign = (Sign) block.getState();
			if (sign.getLine(1).equalsIgnoreCase(SIGN_IDENTIFIER)) {
				if (event.getPlayer().hasPermission("pit.use")) {
					// Passing around a players name is a bad practice, either use the object OR the players UUID.
					event.getPlayer().sendMessage(PitAutofill.PREFIX + plugin.fillPit(event.getPlayer(), sign.getLine(2).replace(' ', '_')));
				} else {
					event.getPlayer().sendMessage(PitAutofill.PREFIX + "You cannot refill the pits.");
				}
			}
		}
	}

	// Updates a sign as needed if it matches upon update.
	@EventHandler(ignoreCancelled = true)
	// Ignore any event that was cancelled before it reached our plugin. We don't want to start registering stuff that
	public void onSignPlaced(SignChangeEvent event) {
		if (SIGN_IDENTIFIER.equalsIgnoreCase(event.getLine(1))) {
			if (event.getPlayer().hasPermission("pit.edit")) {
				event.setLine(1, SIGN_IDENTIFIER);
				event.setLine(2, WordUtils.capitalizeFully(event.getLine(2).replace('_', ' ')));
				PitAutofill.get().getLogger().info("Refill sign placed by " + event.getPlayer().getName() + " at " + event.getBlock().getLocation().toString() + ".");
			} else {
				event.setCancelled(true);
				event.getPlayer().sendMessage(PitAutofill.PREFIX + "You cannot create a refill sign.");
			}
		}
	}

}