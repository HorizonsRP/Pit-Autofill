package co.lotc.pitautofill;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class FillSignListener implements Listener {

	@EventHandler
	public void OnSignRightclick(PlayerInteractEvent event) {

		Block block = event.getClickedBlock();

		// RClick && Either Sign Type
		if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) &&
			(block.getType().equals(Material.WALL_SIGN) ||
			 block.getType().equals(Material.SIGN))) {

			Sign sign = (Sign) block.getState();
			if (sign.getLine(1).equalsIgnoreCase("[Refill]")) {
				event.getPlayer().sendMessage(PitAutofill.PREFIX + PitList.fillPit(sign.getLine(2).replace(' ', '_')));
			}
		}
	}

}