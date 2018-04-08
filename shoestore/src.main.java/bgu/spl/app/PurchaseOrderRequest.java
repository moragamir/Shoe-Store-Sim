package bgu.spl.app;
import bgu.spl.mics.Request;

public class PurchaseOrderRequest implements Request<Receipt>{
	
	private String senderName;
	private int requestTick;
	private String shoeType;
	private int amountRequested;
	private boolean discount;
	
	public PurchaseOrderRequest(String name, int tick, String shoe, int amount, boolean discount){
		senderName = name;
		requestTick = tick;
		shoeType=shoe;
		amountRequested = amount;
		this.discount=discount;
	}
	
	public String getSenderName(){
		return senderName;
	}
	
	public String getShoeType(){
		return shoeType;
	}

	public boolean wantsDiscount(){
		return discount;
	}
	
	public int getAmount(){
		return amountRequested;
	}
	
	public int getTick(){
		return requestTick;
	}
	
}
