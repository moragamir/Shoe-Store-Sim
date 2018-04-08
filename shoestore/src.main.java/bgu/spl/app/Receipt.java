package bgu.spl.app;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Receipt {
	
	private String seller;
	private String customer;
	private String shoeType;
	private boolean discount;
	private int issuedTick;
	private int requestTick;
	private int amountSold;
	
	public Receipt(String seller, String customer, String shoeType, boolean discount, int issuedTick, int requestTick, int amountS){
		this.seller = seller;
		this.customer = customer;
		this.shoeType = shoeType;
		this.discount = discount;
		this.issuedTick=issuedTick;
		this.requestTick = requestTick;
		amountSold = amountS;
	}
	
	public void printReceipt(){
			if (discount)
				Logger.getLogger("logger").log(Level.INFO, "\n	 ================================   DISCOUNTED purchase receipt   ================================= \n		Customer: "+customer+", Amount: "+amountSold+ ", Shoe Type: "+shoeType+", Seller: "+seller+"\n		Order Tick: "+requestTick+", Issued Tick: "+issuedTick +" \n	 ================================================================================================== \n" );
			else
				Logger.getLogger("logger").log(Level.INFO, "\n	==================================   REGULAR purchase receipt   ================================== \n	 	Customer: "+customer+", Amount: "+amountSold+ ", Shoe Type: "+shoeType+", Seller: "+seller+"\n		Order Tick: "+requestTick+", Issued Tick: "+issuedTick +" \n	================================================================================================== \n");  			
	}

	public String getSeller() {
		return seller;
	}

	public void setSeller(String s){
		this.seller=s;
	}
	
	public String getSender() {
		return customer;
	}

	public String getShoeType() {
		return shoeType;
	}

	public boolean isDiscount() {
		return discount;
	}

	public int getIssuedTick() {
		return issuedTick;
	}
	
	public void setIssuedTick(int t){
		this.issuedTick=t;
	}

	public int getRequestTick() {
		return requestTick;
	}

	public int getAmountSold() {
		return amountSold;
	}

	
}
