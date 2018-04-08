package bgu.spl.app;
import bgu.spl.mics.Broadcast;

public class NewDiscountBroadcast implements Broadcast{

	private String shoeType;
	private int amount; //how many of them to give with discount
	
	public NewDiscountBroadcast(String shoe, int disAmount){
		shoeType=shoe;
		amount=disAmount;
	}
	
	public String getShoeType(){
		return shoeType;
	}
	
	public int getAmount(){
		return amount;
	}
}
