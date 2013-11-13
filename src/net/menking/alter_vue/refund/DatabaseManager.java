package net.menking.alter_vue.refund;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import net.menking.alter_vue.utils.ItemStackPackage;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class DatabaseManager {
	protected Connection db;
	private final JavaPlugin plugin;
	private boolean isReady = false;
	private String table;

	public DatabaseManager(JavaPlugin plugin, String host, String port, String user, 
			String pass, String database, String table) throws ClassNotFoundException, SQLException {
		this.plugin = plugin;
		Class.forName("com.mysql.jdbc.Driver");

		String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
		
		db = DriverManager.getConnection(url, user, pass);
		this.table = table;
	}
	
	public void verifySchema() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS " + this.table + " (id int not null auto_increment primary key, "
				+ "player varchar(40) not null, deathmsg varchar(255) not null, tod datetime not null, "
				+ "equipment text, exp int, refundable int not null default 0, refunded datetime, location varchar(255) not null)";
					
		Statement s = db.createStatement();
		s.executeUpdate(sql);

		isReady = true;
	}
	
	public boolean isReady() {
		return this.isReady;
	}
	
	public void close() {
		try {
			if( db != null ) db.close();
		} catch (SQLException e) {

		}
		
		this.isReady = false;
	}
	
	public void recordDeath(Player player, String deathMsg, Location loc, String items, int exp) throws SQLException {
		String sql = "INSERT INTO " + this.table + " VALUES (null, '" + player.getName() + "', '" + deathMsg + "', NOW(), "
				+ "'" + items + "', " + Integer.toString(exp) + ", 0, null, '" + loc.toString() + "')";
		Statement s;

		s = db.createStatement();
		s.executeUpdate(sql);
	}
	
	public boolean hasRefund(OfflinePlayer player) {
		String sql = "SELECT count(*) C FROM " + this.table + " WHERE player='" + player.getName() + "' AND "
				+ "refundable = 1 and refunded is NULL";
		
		Statement s;
		
		try {
			s = db.createStatement();
			ResultSet rs = s.executeQuery(sql);
			
			if( rs.next() ) {
				int count = rs.getInt("C");
				
				return (count > 0);
			}
		} catch(SQLException e) {
			e.printStackTrace();
		}
		
		return false;	
	}
	
	public String[] getDeathInformation(OfflinePlayer player, int howMany) {
		ArrayList<String> msg = new ArrayList<String>();
		
		String sql = "select * from (SELECT id, deathmsg, tod, exp, refundable FROM " + this.table
				+ " WHERE player='" + player.getName() + "' AND " + "refunded is NULL AND equipment <> '' "
				+ " ORDER BY tod DESC LIMIT " + Integer.toString(howMany) + ") k order by k.tod asc";
		
		Statement s;
		
		try {
			s = db.createStatement();
			ResultSet rs = s.executeQuery(sql);
			
			while( rs.next() ) {
				msg.add("[" + rs.getString("id") + "] " + (rs.getInt("refundable")==1?"* ":"") 
						+ rs.getString("deathmsg") + " @ " + rs.getDate("tod").toString() + " with " + rs.getString("exp") + " XP");
			}
		
			s.close();
			
			return msg.toArray(new String[msg.size()]);
			
		} catch(SQLException e) {
			e.printStackTrace();
		}
		
		return null;		
	}
	
	public boolean setRefundable(OfflinePlayer player, int recordId) {		
		try {
			Statement s;
			s = db.createStatement();
			
			String sql = "SELECT count(*) c FROM " + this.table + " WHERE player='" + player.getName() 
					+ "' AND refundable=1";
			ResultSet rs = s.executeQuery(sql);
			
			if( rs.next() ) {
				if( rs.getInt("c") > 0 ) {
					s.close();
					return false;
				}
			}
			
			sql = "UPDATE " + this.table + " SET refundable=1 WHERE id=" + Integer.toString(recordId);

			s.executeUpdate(sql);
			
			s.close();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			return false;
		}
		
	}
	
	public Refund getCurrentRefund(OfflinePlayer player, boolean flagReceived) {
		String sql = "SELECT * FROM " + this.table + " WHERE player='" + player.getName() + "' AND "
				+ "refundable = 1 and refunded is NULL";
		
		Statement s;
		
		try {
			
			s = db.createStatement();
			ResultSet rs = s.executeQuery(sql);
			
			if( rs.next() ) {
				String[] items = rs.getString("equipment").split("\\|");
				ArrayList<ItemStack> itemArray = new ArrayList<ItemStack>();
				
				for( String item : items ) {
					if( item == null ) continue;
					
					if( item.length() > 0 ) {
						ItemStack is = ItemStackPackage.unpack(item);
					
						if( is != null ) {
							itemArray.add(is);
						}
					}
				}
				
				int exp = rs.getInt("exp");
				Refund ref = new Refund(itemArray.toArray(new ItemStack[itemArray.size()]), exp);				
				
				rs.close();
				
				if( flagReceived ) {
					sql = "UPDATE " + this.table + " SET refundable = 0, refunded = now() WHERE player = '"
							+ player.getName() + "' and refundable = 1 and refunded is NULL";
					s.executeUpdate(sql);
				}
				
				return ref;
			}
			else {
				return null;
			}
		} catch(SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
}
