package net.menking.alter_vue.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class ItemStackPackage {

	public static ItemStack unpack(String data) {
		JSONObject obj = (JSONObject)JSONValue.parse(data);
		
		if( obj == null ) return null;
		
		return ItemStackPackage.unpack(obj);
	}
	
	@SuppressWarnings("unchecked")
	public static String pack(ItemStack is) {
		JSONObject obj = new JSONObject();
		
		obj.put("amount",  new Integer(is.getAmount()));
		obj.put("durability",  new Short(is.getDurability()));
		
		MaterialData md = is.getData();

		obj.put("material",  md.getItemType().name());
		
		if( is.hasItemMeta() ) {
			ItemMeta im = is.getItemMeta();
			
			if( im.hasLore() ) {
				ArrayList<String> lores = (ArrayList<String>)im.getLore();
				
				JSONArray l1 = new JSONArray();
				
				for( String lore : lores ) {
					l1.add(lore);
				}
				
				obj.put("lores",  l1);
			}
			
			if( im.hasDisplayName()) {
				obj.put("displayName",  im.getDisplayName());
			}
			
			if( im.hasEnchants() ) {
				JSONArray l2 = new JSONArray();
				
				Map<Enchantment,Integer> enchants = im.getEnchants();
				
				for( Entry<Enchantment, Integer> enchant : enchants.entrySet() ) {
					JSONObject o2 = new JSONObject();
					
					o2.put("enchantName", enchant.getKey().getName());
					o2.put("enchantPower",  enchant.getValue());
					
					l2.add(o2);
				}
				
				obj.put("enchantments",  l2);
			}
			
			if( md.getItemType() == Material.ENCHANTED_BOOK ) {
				EnchantmentStorageMeta esm = (EnchantmentStorageMeta)is.getItemMeta();
				
				if( esm.hasStoredEnchants() ) {
					JSONArray l2 = new JSONArray();
					
					Map<Enchantment,Integer> enchants = esm.getStoredEnchants();
					
					for( Entry<Enchantment, Integer> enchant : enchants.entrySet() ) {
						JSONObject o2 = new JSONObject();
						
						o2.put("enchantName", enchant.getKey().getName());
						o2.put("enchantPower",  enchant.getValue());
						
						l2.add(o2);
					}
					
					obj.put("storedEnchants",  l2);
				}
				
			}			
		}
		
		return obj.toJSONString();
	}

	@SuppressWarnings("unchecked")
	private static ItemStack unpack(JSONObject obj) {
		int amount = ((Long)obj.get("amount")).intValue();
		short durability = ((Long)obj.get("durability")).shortValue();
		
		ItemStack is = new ItemStack(Material.getMaterial((String)obj.get("material")),
				amount, durability );

		ItemMeta im = is.getItemMeta();
		
		String displayName = (String)obj.get("displayName");
		
		if( displayName != null ) {
			im.setDisplayName(displayName);
		}
		
		JSONArray l5 = (JSONArray)obj.get("lores");
		
		if( l5 != null ) {
			im.setLore(l5);		
		}
		
		JSONArray l1 = (JSONArray)obj.get("enchantments");
		
		if( l1 != null ) {
			Iterator<JSONObject> i = l1.iterator();
			
			while(i.hasNext()) {
				JSONObject j1 = i.next();

				String enchantName = (String)j1.get("enchantName");
				int enchantPower = ((Long)j1.get("enchantPower")).intValue();
				
				Enchantment e = Enchantment.getByName(enchantName);
				
				im.addEnchant(e, enchantPower, true);
			}
		}

		if( is.getData().getItemType() == Material.ENCHANTED_BOOK ) {
			EnchantmentStorageMeta esm = (EnchantmentStorageMeta)im;
			
			JSONArray k1 = (JSONArray)obj.get("storedEnchants");
			
			if( k1 != null ) {
				Iterator<JSONObject> i = k1.iterator();
				
				while(i.hasNext()) {
					JSONObject j1 = i.next();

					String enchantName = (String) j1.get("enchantName");
					int enchantPower = ((Long)j1.get("enchantPower")).intValue();
					
					Enchantment e = Enchantment.getByName(enchantName);
					
					esm.addStoredEnchant(e,  enchantPower,  true);
				}
				
			}
			
			is.setItemMeta(esm);
		}
		else {
			is.setItemMeta(im);	
		}
		
		return is;
	}
	
	public static String[] toStringRaw(ItemStack is) {
		ArrayList<String> msg = new ArrayList<String>();

		MaterialData md = is.getData();

		msg.add("Item: " + md.getItemType().name() + "; Amt: " + Integer.toString(is.getAmount()) + "; Dur: " + Short.toString(is.getDurability()));
		
		if( is.hasItemMeta() ) {
			ItemMeta im = is.getItemMeta();
			
			if( im.hasEnchants() ) {
				Map<Enchantment,Integer> enchants = im.getEnchants();
				
				for( Entry<Enchantment, Integer> enchant : enchants.entrySet() ) {
					msg.add("    - " + enchant.getKey().getName() + " @ " + Integer.toString(enchant.getValue()));
				}
			}
			
			if( im.hasDisplayName()) {
				msg.add("Display Name: " + im.getDisplayName());
			}

			if( im.hasLore() ) {
				ArrayList<String> lores = (ArrayList<String>)im.getLore();
				
				for( String lore : lores ) {
					msg.add("Lore: " + lore);
				}
			}
			
			if( md.getItemType() == Material.ENCHANTED_BOOK ) {
				EnchantmentStorageMeta esm = (EnchantmentStorageMeta)is.getItemMeta();
				
				if( esm.hasStoredEnchants() ) {
					Map<Enchantment,Integer> enchants = esm.getStoredEnchants();
					
					for( Entry<Enchantment, Integer> enchant : enchants.entrySet() ) {
						msg.add("    - " + enchant.getKey().getName() + " @ " + Integer.toString(enchant.getValue()));
					}

				}
			}
		}

		return msg.toArray(new String[msg.size()]);
	}
	
	
	public static String[] toString(ItemStack is) {
		ArrayList<String> msg = new ArrayList<String>();
		
		MaterialData md = is.getData();

		msg.add(Integer.toString(is.getAmount()) + " of " + md.getItemType().name() + " ");
		
		if( is.hasItemMeta() ) {
			ItemMeta im = is.getItemMeta();
			
			if( im.hasEnchants() ) {
				Map<Enchantment,Integer> enchants = im.getEnchants();
				
				for( Entry<Enchantment, Integer> enchant : enchants.entrySet() ) {
					msg.add("    - " + enchant.getKey().getName() + " @ " + Integer.toString(enchant.getValue()));
				}
			}
			
			if( im.hasDisplayName()) {
				msg.add("Display Name: " + im.getDisplayName());
			}

			if( im.hasLore() ) {
				ArrayList<String> lores = (ArrayList<String>)im.getLore();
				
				for( String lore : lores ) {
					msg.add("Lore: " + lore);
				}
			}				
		}

		return msg.toArray(new String[msg.size()]);
	}
	
	/*
	@SuppressWarnings("unchecked")
	public static String[] jsonToString(String json) {
		ArrayList<String> msg = new ArrayList<String>();
		
		JSONObject obj = (JSONObject)JSONValue.parse(json);
		
		if( obj == null ) return null;
		
		msg.add("Amount: " + ((Long)obj.get("amount")).toString());
		msg.add("Durability: " + ((Long)obj.get("durability")).toString());
		msg.add("Material: " + (String)obj.get("material"));
		
		JSONArray l1 = (JSONArray)obj.get("enchantments");
		
		if( l1 != null ) {
			Iterator<JSONObject> i = l1.iterator();
			
			while(i.hasNext()) {
				JSONObject j1 = i.next();
				
				msg.add("Enchanted with " + (String)j1.get("enchantName") + " @ " + ((Long)j1.get("enchantPower")).toString());
			}
		}

		String displayName = (String)obj.get("displayName");
		
		if( displayName != null ) {
			msg.add("Display Name: " + displayName);
		}
		
		JSONArray l5 = (JSONArray)obj.get("lores");
		
		if( l5 != null ) {
			msg.add("Lore: " + l5.get(0));		
		}
		
		return msg.toArray(new String[msg.size()]);
	}
	*/
}
