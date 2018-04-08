package bgu.spl.app;
import bgu.spl.mics.Request;

public class RestockRequest<Boolean> implements Request{

	private int currentTick;
	private String shoeType;
	private int amount;
	private String seller;
	
	public RestockRequest(int tick, String shoe, int amount, String seller){
		currentTick=tick;
		shoeType=shoe;
		this.amount = amount;
		this.seller=seller;
	}
	
	public RestockRequest(RestockRequest<Receipt> request){
		currentTick=request.getTick();
		shoeType=request.getShoeType();
		this.amount = request.getAmount();
		this.seller=request.getSeller();
	}
	
	
	public String getShoeType(){
		return shoeType;
	}

	
	public int getAmount(){
		return amount;
	}
	
	public void setAmount(int amountN){
		amount=amountN;
	}
	
	public int getTick(){
		return currentTick;
	}
	
	public String getSeller(){
		return this.seller;
	}
}
