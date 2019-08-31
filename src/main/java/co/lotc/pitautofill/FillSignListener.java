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

		// Only fire for one hand.
		if (event.getHand() == EquipmentSlot.HAND) {

			Block block = event.getClickedBlock();
			if (block != null) {

				// RClick && Either Sign Type
				if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) &&
					(block.getBlockData().getMaterial().equals(Material.SPRUCE_SIGN) ||
					 block.getBlockData().getMaterial().equals(Material.SPRUCE_WALL_SIGN) ||
					 block.getBlockData().getMaterial().equals(Material.ACACIA_SIGN) ||
					 block.getBlockData().getMaterial().equals(Material.ACACIA_WALL_SIGN) ||
					 block.getBlockData().getMaterial().equals(Material.BIRCH_SIGN) ||
					 block.getBlockData().getMaterial().equals(Material.BIRCH_WALL_SIGN) ||
					 block.getBlockData().getMaterial().equals(Material.OAK_SIGN) ||
					 block.getBlockData().getMaterial().equals(Material.OAK_WALL_SIGN) ||
					 block.getBlockData().getMaterial().equals(Material.DARK_OAK_SIGN) ||
					 block.getBlockData().getMaterial().equals(Material.DARK_OAK_WALL_SIGN) ||
					 block.getBlockData().getMaterial().equals(Material.JUNGLE_SIGN) ||
					 block.getBlockData().getMaterial().equals(Material.JUNGLE_WALL_SIGN) )) {

					Sign sign = (Sign) block.getState();
					if (sign.getLine(1).equalsIgnoreCase(SIGN_IDENTIFIER)) {

						if (event.getPlayer().hasPermission("pit.use")) {
							String pitName = sign.getLine(2).replace(' ', '_').toUpperCase();
							ResourcePit pit = plugin.getPit(pitName);
							if (pit != null) {
								String result = pit.fill(event.getPlayer(), false);
								event.getPlayer().sendMessage(PitAutofill.PREFIX + result);
							} else {
								event.getPlayer().sendMessage(PitAutofill.PREFIX + "The pit '" + PitAutofill.ALT_COLOUR + pitName + PitAutofill.PREFIX + "' does not exist.");
							}
						} else {
							event.getPlayer().sendMessage(PitAutofill.PREFIX + "You cannot refill the pits.");
						}

					}
				}

			}
		}
	}

	// Updates a sign as needed if it matches upon update.
	@EventHandler(ignoreCancelled = true)
	public void onSignPlaced(SignChangeEvent event) {
		if (SIGN_IDENTIFIER.equalsIgnoreCase(event.getLine(1))) {
			if (event.getPlayer().hasPermission("pit.edit")) {
				event.setLine(1, SIGN_IDENTIFIER);
				event.setLine(2, WordUtils.capitalizeFully(event.getLine(2).replace('_', ' ')));
				plugin.getLogger().info("Refill sign placed by " + event.getPlayer().getName() + " at " + event.getBlock().getLocation().toString() + ".");
			} else {
				event.setCancelled(true);
				event.getPlayer().sendMessage(PitAutofill.PREFIX + "You cannot create a refill sign.");
			}
		}
	}

}