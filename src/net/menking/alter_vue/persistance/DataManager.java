package net.menking.alter_vue.persistance;

import net.menking.alter_vue.refund.Refund;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class DataManager {
	protected JavaPlugin plugin;

	public DataManager(JavaPlugin plugin) {
		this.plugin = plugin;
	}
	
	public abstract void close();
	
	public abstract boolean recordDeath(Player player, String deathMsg, Location loc, String items, int exp);
	
	public abstract boolean hasRefund(OfflinePlayer player);
	
	public abstract String[] getDeathInformation(OfflinePlayer player, int howMany);
	
	public abstract boolean setRefundable(OfflinePlayer player, int recordId);
	
	public abstract Refund getCurrentRefund(OfflinePlayer player, boolean flagReceived);
	
	public abstract Refund getCurrentRefund(int id);
}
