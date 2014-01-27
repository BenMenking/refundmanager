package net.menking.alter_vue.refund;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import net.menking.alter_vue.utils.ItemStackPackage;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class RefundPlugin extends JavaPlugin implements Listener {
	private DatabaseManager db;
	private static String header = "[RefundPlugin] ";
	private HashMap<String, BukkitTask> control = null;
	private ArrayList<String> ignoredWorlds = null;

	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		
		getServer().getPluginManager().registerEvents(this,  this);
		
		reload();
	}
	
	private void reload() {
		try {
			db = new DatabaseManager(this, getConfig().getString("database.host"), getConfig().getString("database.port"),
				getConfig().getString("database.username"), getConfig().getString("database.password"), 
				getConfig().getString("database.database"), getConfig().getString("database.table"));
		} catch (ClassNotFoundException e) {
			getServer().getLogger().severe(RefundPlugin.header + "Could not find the database driver needed!  Disabling plugin");
			getServer().getPluginManager().disablePlugin(this);
		} catch (SQLException e) {
			getServer().getLogger().severe(RefundPlugin.header + "SQL exception!  Disabling plugin and trace to follow");
			getServer().getLogger().fine(e.toString());
			getServer().getPluginManager().disablePlugin(this);			
		}
		
		try {
			db.verifySchema();
			
			if( control == null ) {
				control = new HashMap<String, BukkitTask>();
			}
			
			if( ignoredWorlds == null ) {
				ignoredWorlds = new ArrayList<String>();
			}
			else {
				ignoredWorlds.clear();
			}
			
			ignoredWorlds = (ArrayList<String>)getConfig().getStringList("ignored-worlds");
		} catch (SQLException e) {
			getServer().getLogger().severe(RefundPlugin.header + "SQL exception  Disabling plugin, trace to follow:");
			getServer().getLogger().fine(e.toString());
			getServer().getPluginManager().disablePlugin(this);
		}
	}
	
	@Override
	public void onDisable() {
		// let's cleanup our toys, even though other ppl won't!
		//
		if( db != null ) {
			db.close();
			
			db = null;
		}
		
		if( control != null ) {
			for(String key : control.keySet() ) {
				control.get(key).cancel();
				control.remove(key);
			}
			
			control.clear();
			control = null;
		}
	}
	
	@EventHandler
	public void OnPlayerDeathEvent(PlayerDeathEvent event) {
		// first off, check to see if this player is located in an ignored world
		//
		if( ignoredWorlds.contains(event.getEntity().getWorld().getName()) ) {
			return;
		}
		
		// now let's get a record of all the inventory they dropped
		//
		String items = "";
		
		for(ItemStack item : event.getDrops()) {
			String json = ItemStackPackage.pack(item);
			
			items += json + "|";
		}
		
		try {
			// and insert the database record
			//
			db.recordDeath(event.getEntity(), event.getDeathMessage(), event.getEntity().getLocation(), items, event.getDroppedExp());
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		// if we don't allow drop, clear them
		//
		if( !getConfig().getBoolean("allow-drops") ) {
			event.setDroppedExp(0);
			event.getDrops().clear();	
		}
	}
	
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		Player player = event.getPlayer();
		
		try {
			if( db.hasRefund(player ) ) {
				nagPlayer(player);
			}
		} catch (SQLException e) {
			this.getServer().getLogger().warning(RefundPlugin.header + " onPlayerLoginEvent(): " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event ) {
		if( control.containsKey(event.getPlayer().getName())) {
			// cancel the nag message task and remove player from list
			//
			control.get(event.getPlayer().getName()).cancel();
			
			control.remove(event.getPlayer().getName());
		}
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if( !(sender instanceof Player) ) {
			sender.sendMessage("This command can only be run by an in-game player");
			return true;
		}
		
		Player player = (Player)sender;
		
		if( args.length == 0 && sender.hasPermission("refund.help") ) {
			sender.sendMessage(refundHelpMsg(sender));
			return true;
		}
		
		if( args.length > 0 ) {
			/** ACCEPT **/
			if( args[0].equalsIgnoreCase("accept") && sender.hasPermission("refund.user.accept")) {
				if( control.containsKey(player.getName() )) {
					control.get(player.getName()).cancel();
					control.remove(player.getName());
				}
				
				try {
					if( !db.hasRefund(player ) ) {
						player.sendMessage(ChatColor.YELLOW + "Sorry, you don't have any refunds available." + ChatColor.RESET);
						return true;
					}
				} catch (SQLException e) {
					this.getServer().getLogger().warning(RefundPlugin.header + " onCommand('accept')#hasRefund(): " + e.getMessage());
					e.printStackTrace();
				}				
				
				Refund ref = null;
				
				try {
					ref = db.getCurrentRefund(player, true);
				} catch (SQLException e) {
					this.getServer().getLogger().warning(RefundPlugin.header + " onCommand('accept')#getCurrentRefund(): " + e.getMessage());
					e.printStackTrace();
				}
				
				if( ref == null ) {
					player.sendMessage(ChatColor.YELLOW + "An error occurred and unable to retrieve your refund." + ChatColor.RESET);
					return true;					
				}
				
				player.getInventory().clear();
				
				for(ItemStack is : ref.getItemStack() ) {
					player.getInventory().addItem(is);
				}
				
				//player.setTotalExperience(ref.getExp());
				
				player.sendMessage(ChatColor.GREEN + "Your inventory and experience have been refunded!" + ChatColor.RESET);
			}
			/** LIST **/
			else if( args[0].equalsIgnoreCase("list") && sender.hasPermission("refund.user.list")) {
				try {
					if( !db.hasRefund(player) ) {
						player.sendMessage(ChatColor.YELLOW + "Sorry, you have no refunds available." + ChatColor.RESET);
						return true;
					}
				} catch (SQLException e) {
					this.getServer().getLogger().warning(RefundPlugin.header + " onCommand('list')#hasRefund(): " + e.getMessage());
					e.printStackTrace();
				}

				ArrayList<String> msg = new ArrayList<String>();
				
				Refund ref = null;
				try {
					ref = db.getCurrentRefund(player, false);
				} catch (SQLException e) {
					this.getServer().getLogger().warning(RefundPlugin.header + " onCommand('list')#getCurrentRefund(): " + e.getMessage());
					e.printStackTrace();
				}
				
				if( ref == null ) {
					player.sendMessage(ChatColor.GREEN + "That's odd.  Could not retrieve the refund information." + ChatColor.RESET);
					return true;					
				}
				
				for(ItemStack is : ref.getItemStack() ) {
					if( is == null ) continue;
					
					String[] frag = ItemStackPackage.toString(is);
					
					for( String s : frag ) {
						if( s != null ) {
							//getServer().getLogger().info(RefundPlugin.header + "LIST: " + s);
							msg.add(s);
						}
					}
				}
				
				// for a later date and time
				//msg.add("Experience: " + Integer.toString(ref.getExp()));
				
				player.sendMessage(msg.toArray(new String[msg.size()]));				
			}
			/** DECLINE **/
			else if( args[0].equalsIgnoreCase("decline") && sender.hasPermission("refund.user.decline")) {
				if( control.containsKey(player.getName() )) {
					control.get(player.getName()).cancel();
					control.remove(player.getName());
				}

				try {
					if( !db.hasRefund(player) ) {
						player.sendMessage(ChatColor.YELLOW + "Sorry, you have no refunds available." + ChatColor.RESET);
						return true;					
					}
				} catch (SQLException e) {
					this.getServer().getLogger().warning(RefundPlugin.header + " onCommand('decline')#hasRefund(): " + e.getMessage());
					e.printStackTrace();
				}
				// throw away the result

				try {
					db.getCurrentRefund(player, true);
				} catch (SQLException e) {
					this.getServer().getLogger().warning(RefundPlugin.header + " onCommand('decline')#getCurrentRefund(): " + e.getMessage());
					e.printStackTrace();
				}
				
				player.sendMessage(ChatColor.GREEN + "You declined the refund.  Thank you!" + ChatColor.RESET);
			}
			/** DETECT **/
			else if( args[0].equalsIgnoreCase("detect") && sender.hasPermission("refund.admin.detect")) {
				String[] msg = ItemStackPackage.toStringRaw(player.getItemInHand());
				
				player.sendMessage(msg);
			}
			/** SHOW **/
			else if( args[0].equalsIgnoreCase("show") && sender.hasPermission("refund.admin.show")) {
				if( args.length < 2 ) {
					sender.sendMessage(refundHelpMsg(sender));
					return true;
				}
				
				OfflinePlayer target = getServer().getOfflinePlayer(args[1]);
				
				if( target == null ) {
					sender.sendMessage(ChatColor.RED + "Sorry, player '" + args[1] + "' does not exist on this server");
					return true;
				}
				
				String[] msg = null;
				
				try {
					msg = db.getDeathInformation(target, (args.length==3?Integer.parseInt(args[2]):2));
				} catch (NumberFormatException | SQLException e) {
					this.getServer().getLogger().warning(RefundPlugin.header + " onCommand('show')#getDeathInformation(): " + e.getMessage());
					e.printStackTrace();
				}
				
				if( msg.length == 0 ) {
					player.sendMessage(ChatColor.YELLOW + "Player '" + args[1] + "' does not have any records available." + ChatColor.RESET);
				}
				else {				
					player.sendMessage(msg);
				}
			}
			/** REFUND **/
			else if( args[0].equalsIgnoreCase("refund") && sender.hasPermission("refund.admin.refund")) {
				if( args.length != 3 ) {
					sender.sendMessage(refundHelpMsg(sender));
					return true;					
				}
				
				OfflinePlayer target = getServer().getOfflinePlayer(args[1]);
				
				if( target == null ) {
					player.sendMessage(ChatColor.RED + "The player '" + args[1] + "' does not exist on this server" + ChatColor.RESET);
					return true;
				}
				
				try {
					if( db.setRefundable(target, Integer.parseInt(args[2]) ) ) {
						player.sendMessage(ChatColor.GREEN + "Player will be given notice that refund is available" + ChatColor.RESET);
						
						if( target.isOnline() ) {
							nagPlayer((Player)target);
						}
					}
					else {
						player.sendMessage(ChatColor.YELLOW + "Player may already have a refund waiting.  Could not mark refund" + ChatColor.RESET);
					}
				} catch (NumberFormatException | SQLException e) {
					this.getServer().getLogger().warning(RefundPlugin.header + " onCommand('refund')#setRefundable(): " + e.getMessage());
					e.printStackTrace();
				}
			}
			/** RELOAD **/
			else if( args[0].equalsIgnoreCase("reload") && sender.hasPermission("refund.admin.reload")) {
				reload();
				
				sender.sendMessage(ChatColor.GOLD + "RefundManager configuration has been reloaded" + ChatColor.RESET);
			}
			/** SHOWITEMS **/
			else if( args[0].equalsIgnoreCase("showitems") && sender.hasPermission("refund.admin.list")) {
				ArrayList<String> msg = new ArrayList<String>();
				
				Refund ref = null;
				try {
					ref = db.getCurrentRefund(Integer.parseInt(args[1]));
				} catch (SQLException e) {
					this.getServer().getLogger().warning(RefundPlugin.header + " onCommand('showitems')#getCurrentRefund(): " + e.getMessage());
					e.printStackTrace();
				}
				
				if( ref == null ) {
					player.sendMessage(ChatColor.GREEN + "That's odd.  Could not retrieve the refund information." + ChatColor.RESET);
					return true;					
				}
				
				for(ItemStack is : ref.getItemStack() ) {
					if( is == null ) continue;
					
					String[] frag = ItemStackPackage.toString(is);
					
					for( String s : frag ) {
						if( s != null ) {
							//getServer().getLogger().info(RefundPlugin.header + "LIST: " + s);
							msg.add(s);
						}
					}
				}
				
				// for a later date and time
				//msg.add("Experience: " + Integer.toString(ref.getExp()));
				
				player.sendMessage(msg.toArray(new String[msg.size()]));				
				
			}
			else {
				sender.sendMessage(refundHelpMsg(sender));
			}
		}

		return true;
	}
	
	public boolean keepNagging(Player player) {
		return control.containsKey(player.getName());
	}
	
	public void removeNag(Player player) {
		if( control.containsKey(player.getName())) {
			BukkitTask task = control.get(player.getName());
			task.cancel();
			control.remove(player.getName());
		}
	}
	
	public void nagPlayer(Player player) {
		// wait for 5 seconds, send msg, then wait 3 minutes and message again
		BukkitTask task = new YouHaveRefundNag(this, player).runTaskTimer(this, 20 * 5, 20 * 3 * 60);
		control.put(player.getName(),  task);
	}
	
	private String[] refundHelpMsg(CommandSender sender) {
		ArrayList<String> bucket = new ArrayList<String>();
				
		bucket.add(ChatColor.GREEN + "Refund Manager" + ChatColor.RESET);
		bucket.add(ChatColor.GREEN + "----------------------------------" + ChatColor.RESET);
		if( sender.hasPermission("refund.help") )
			bucket.add(ChatColor.GREEN + "/refund help " + ChatColor.BLUE + "Show this help message" + ChatColor.RESET);
		if( sender.hasPermission("refund.user.accept") )
			bucket.add(ChatColor.GREEN + "/refund accept " + ChatColor.BLUE + "Accept the refund (will clear current inventory!)" + ChatColor.RESET);
		if( sender.hasPermission("refund.user.list") )
			bucket.add(ChatColor.GREEN + "/refund list " + ChatColor.BLUE + "Show items to be refunded" + ChatColor.RESET);
		if( sender.hasPermission("refund.user.decline") )
			bucket.add(ChatColor.GREEN + "/refund decline " + ChatColor.BLUE + "Decline the refund and stop nag message" + ChatColor.RESET);
		if( sender.hasPermission("refund.admin.detect") )
			bucket.add(ChatColor.GREEN + "/refund detect " + ChatColor.BLUE + "Show extra item info of item in hand" + ChatColor.RESET);
		if( sender.hasPermission("refund.admin.show") )
			bucket.add(ChatColor.GREEN + "/refund show <player> [2]" + ChatColor.BLUE + "Show possible deaths to be refunded. Asterick denote current refund." + ChatColor.RESET);
		if( sender.hasPermission("refund.admin.refund") )
			bucket.add(ChatColor.GREEN + "/refund refund <player> <id> " + ChatColor.BLUE + "Mark a refund and notify player" + ChatColor.RESET);
		if( sender.hasPermission("refund.admin.list") )
			bucket.add(ChatColor.GREEN + "/refund showitems <id> " + ChatColor.BLUE + "Show items to be refunded for <id>" + ChatColor.RESET);		
		
		String[] message = bucket.toArray(new String[bucket.size()]);
		
		return message;
	}
}
