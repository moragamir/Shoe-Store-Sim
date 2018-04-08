package bgu.spl.app;
import bgu.spl.mics.Request;

public class ManufacturingOrderRequest<Receipt> implements Request {
	private int amount;
	private String shoeType;
	private int curr_tick;
	
	public ManufacturingOrderRequest(int amount, String shoe,int tick){
		this.amount=(amount);
		shoeType=shoe;
		curr_tick=tick;
	}

	public ManufacturingOrderRequest(ManufacturingOrderRequest<Receipt> m){
		this.amount=m.getAmount();
		shoeType=m.getShoeType();
		curr_tick=m.getCurr_tick();
	}
	
	public int getAmount() {
		return amount;
	}

	public void setAmount(int a) {
		amount=a;
	}
	
	public String getShoeType() {
		return shoeType;
	}

	public int getCurr_tick() {
		return curr_tick;
	}

}
