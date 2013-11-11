package net.menking.alter_vue.refund;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class YouHaveRefundNag extends BukkitRunnable {
	private final Player player;
	private final RefundPlugin plugin;
	
	public YouHaveRefundNag(RefundPlugin plugin, Player player) {
		this.player = player;
		this.plugin = plugin;
	}
	
	@Override
	public void run() {
		if( player.isOnline() ) {
			if( plugin.keepNagging(player)) {
				player.sendMessage(ChatColor.AQUA + "**** You have a refund waiting! ****");
				player.sendMessage(ChatColor.AQUA + "Type " + ChatColor.GOLD + "/refund help" + ChatColor.AQUA
					+ " for more information" + ChatColor.RESET);
				player.sendMessage(ChatColor.AQUA + "Accepting a refund " + ChatColor.BOLD 
					+ "WILL CLEAR" + ChatColor.RESET + ChatColor.AQUA 
					+ " your current inventory, ");
				player.sendMessage(ChatColor.AQUA + "so go put it somewhere safe!" + ChatColor.RESET);				
			}
		}
		else {
			plugin.removeNag(player);
		}
	}

}
