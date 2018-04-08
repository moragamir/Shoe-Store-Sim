package bgu.spl.app;

public class PurchaseSchedule {

	private String shoeType;
	private int tick;
	
	public PurchaseSchedule(String shoeT, int ti){
		this.shoeType = shoeT;
		this.tick = ti;
	}
	
	public int getTick(){
		return tick;
	}
	
	public String getShoeType(){
		return shoeType;
	}
}
