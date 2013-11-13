package net.menking.alter_vue.refund;

import org.bukkit.inventory.ItemStack;

public class Refund {
	private ItemStack[] itemStack = null;
	private int exp = 0;
	
	public Refund(ItemStack[] is, int exp) {
		this.itemStack = is;
		this.exp = exp;
	}

	public ItemStack[] getItemStack() {
		return itemStack;
	}

	public void setItemStack(ItemStack[] itemStack) {
		this.itemStack = itemStack;
	}

	public int getExp() {
		return exp;
	}

	public void setExp(int exp) {
		this.exp = exp;
	}
	

}
