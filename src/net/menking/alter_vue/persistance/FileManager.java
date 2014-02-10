package net.menking.alter_vue.persistance;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.menking.alter_vue.refund.Refund;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class FileManager extends DataManager {
	private File file;
	private YamlConfiguration config;

	public FileManager(JavaPlugin plugin, File f) throws FileNotFoundException, IOException, InvalidConfigurationException {
		super(plugin);
		
		this.file = f;
		this.config = new YamlConfiguration();
		
		this.config.load(f);

	}

	@Override
	public void close() {
		try {
			this.config.save(this.file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean recordDeath(Player player, String deathMsg, Location loc, String items, int exp) {
		String data = deathMsg + "|" + Long.toString(System.currentTimeMillis()) + "|" 
				+ items + "|" + Integer.toString(exp) + "|" + loc.toString();
		
		try {
			ArrayList<String> l = (ArrayList<String>) this.config.getList(player.getName());
		
			l.add(data);
		
			this.config.set(player.getName(), l);
			
			this.config.save(this.file);
			return true;
		} catch( Exception e ) {
			return false;
		}
	}

	@Override
	public boolean hasRefund(OfflinePlayer player) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String[] getDeathInformation(OfflinePlayer player, int howMany) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean setRefundable(OfflinePlayer player, int recordId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Refund getCurrentRefund(OfflinePlayer player, boolean flagReceived) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Refund getCurrentRefund(int id) {
		// TODO Auto-generated method stub
		return null;
	}

}
