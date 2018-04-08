package bgu.spl.app;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ShoeStorageInfo {
		
	private String shoeType;
	private int amount;
	private int discountedAmount;
	
	public ShoeStorageInfo(String shoeT, int amount, int discount){
		this.shoeType = shoeT;
		this.amount = amount;
		this.discountedAmount = discount;
	}
	
	public String getName(){
		return this.shoeType;
	}
	
	public int getAmount(){
		return this.amount;
	}
	
	public int getDiscountAmount(){
		return this.discountedAmount;
	}
	
	public void setAmount(int amount2){
		this.amount = amount2;
	}
	
	public void setDiscounts(int dis){
		this.discountedAmount = dis;
	}
	
	
	public void printInfo(){
		Logger.getLogger("logger").log(Level.INFO,"Shoe Type: "+shoeType+", Amount on storage: "+amount+", Discounted amount: "+discountedAmount);
	}
}
