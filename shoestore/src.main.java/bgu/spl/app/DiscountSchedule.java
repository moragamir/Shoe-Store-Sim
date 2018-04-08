package bgu.spl.app;

public class DiscountSchedule {
	
	private String shoeType;
	private int tick;
	private int amount;
	
	
	public DiscountSchedule(String shoeT, int ti, int amount){
		this.shoeType = shoeT;
		this.tick = ti;
		this.amount = amount;
	}
	

	public int getTick(){
		return tick;
	}
	
	public String getShoeType(){
		return shoeType;
	}
	
	public int getAmount(){
		return amount;
	}
	
	public void addToAmount(int a){
		amount=amount+a;
	}
}
