package bgu.spl.app;
import bgu.spl.app.ShoeStorageInfo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * the Store contains all information about the shoes in the store and the receipts of purchasing and ordering shoes
 * @param storage : a list of ShoeStorageInfo objects- each shoe has an amount and a discounted amount
 * @param receipt_list : a list of receipts of clients that purchased shoes and shoe orders of the manager from the factory 
 *
 */

public class Store {
	
	private ConcurrentHashMap<String, ShoeStorageInfo> storage; 
	private LinkedBlockingQueue<Receipt> receipt_list;	
	
	private Store(){
		storage = new ConcurrentHashMap<String, ShoeStorageInfo>();
		receipt_list = new LinkedBlockingQueue<Receipt>();
	}
	
	//singleton for constructor
	private static class storeSingletonHolder{
		private static Store store = new Store();
	}
	//singleton getInstance method
	public static Store storeGetInstance(){
			return storeSingletonHolder.store;
	}
		

	// ENUM
	public enum BuyResult{
		NOT_IN_STOCK, NOT_ON_DISCOUNT, REGULAR_PRICE, DISCOUNTED_PRICE
	}
	
	/**
	 * This method should be called in order to initialize the store storage before starting an execution
		(by the ShoeStoreRunner class defined later). The method will add the items in the given array to
		the store.
	 * @param array contain storage of shoes
	 * @return none 
	 */
	//adds all items to the hash map for initialization 
	public void load(ShoeStorageInfo[] storage){
		for(int i=0; i<storage.length; i++)
			this.storage.put(storage[i].getName(), storage[i]);
	}
	
	/**
	 *This method will attempt to take a single showType from the store. It receives the shoeType to
	take and a boolean - onlyDiscount which indicates that the caller wish to take the item only if it is
	in discount. Its result is an enum which have the following values:
	• NOT_IN_STOCK: which indicates that there were no shoe of this type in stock (the store storage should not be changed in this case)
	• NOT_ON_DISCOUNT: which indicates that the "onlyDiscount" was true and there are no discounted shoes with the requested type.
	• REGULAR_PRICE: which means that the item was successfully taken (the amount of items of this type was reduced by one)
	• DISCOUNTED_PRICE: which means that was successfully taken in a discounted price (theamount of items of this type was reduced by one and the amount of discounted items reducedby one)
	Important: If there is discount available for the requested shoe type on stock it should be taken,
	even if the onlyDiscount parameter is false.
	*@param shoeType- shoe name
	*@param onlyDiscount- boolean if in wishList
	*@return BuyResult: NOT_IN_STOCK/ NOT_ON_DISCOUNT/ REGULAR_PRICE/ DISCOUNTED_PRICE
	 */
	// attempts to take a shoe from storage, returns an ENUM result
	public synchronized BuyResult take(String shoeType, boolean onlyDiscount){
		if(!storage.containsKey(shoeType)) // shoe doesnt exist in hash map
			return BuyResult.NOT_IN_STOCK;
		
		ShoeStorageInfo shoe = storage.get(shoeType); 
		
		if (shoe.getAmount()==0) // shoe amount = 0 
			return BuyResult.NOT_IN_STOCK;
		
		if(shoe.getDiscountAmount()==0 && onlyDiscount) // client wants discount but not possible 
			return BuyResult.NOT_ON_DISCOUNT;
		
		if(shoe.getDiscountAmount()>0){  // client gets discounted shoe 
			int new_amount = storage.get(shoeType).getAmount()-1;
			int new_discountAmount = storage.get(shoeType).getDiscountAmount()-1;
			storage.get(shoeType).setAmount(new_amount);
			storage.get(shoeType).setDiscounts(new_discountAmount);
			if (storage.get(shoeType).getAmount()==0)
				storage.remove(shoeType);
			return BuyResult.DISCOUNTED_PRICE;
		}
		
		else {				// client gets regular price shoe
			int new_amount = storage.get(shoeType).getAmount()-1;
			storage.get(shoeType).setAmount(new_amount);
			if (storage.get(shoeType).getAmount()==0)
				storage.remove(shoeType);
			return BuyResult.REGULAR_PRICE;
		}
		
	}
	
	/**
	 * This method adds the given amount to the ShoeStorageInfo of the given shoeType.
	 * @param shoeType- shoe name
	 * @param amount- how much to add
	 */
	// adds an amount of a single shoe type to storage
	public void add(String shoeType, int amount){
		if(!storage.containsKey(shoeType)){
			ShoeStorageInfo new_shoe = new ShoeStorageInfo(shoeType, amount,0); 
			storage.putIfAbsent(shoeType, new_shoe);
		}
		else{
			int newamount = storage.get(shoeType).getAmount()+amount;
			storage.get(shoeType).setAmount(newamount);
		}
			
	}
	
	/**
	 * Adds the given amount to the corresponding ShoeStorageInfo’s discountedAmount field.
	 * @param shoeType : shoe name
	 * @param amount : how much to add
	 */
	public void addDiscount(String shoeType, int amount){
		if (storage.containsKey(shoeType)){
			int currAmount = storage.get(shoeType).getAmount();
			int oldDiscount = storage.get(shoeType).getDiscountAmount();
			int amountnotDiscounted = currAmount-oldDiscount;
			if (amountnotDiscounted>=amount){
				int finalAmount = oldDiscount+amount;
				Logger.getLogger("logger").log(Level.INFO,"the manager announced of a discount for "+amount+" "+shoeType+"!!");
				storage.get(shoeType).setDiscounts(finalAmount);
			}
			else if (amountnotDiscounted==0)
				Logger.getLogger("logger").log(Level.INFO,"Manager announces: all "+shoeType+" are discounted");
			else if (amountnotDiscounted!=0){ // the amount of shoes to discount is the rest of the shoes left that are not on discount
				Logger.getLogger("logger").log(Level.INFO,"the manager announced of a discount for "+amountnotDiscounted+" "+shoeType+"!!");
				storage.get(shoeType).setDiscounts(currAmount);
			}
			else if (currAmount==0){
				Logger.getLogger("logger").log(Level.INFO,"Manager wanted to discount "+ shoeType+ " but it doesn't exist in storage");
			}
		}
		else 
			Logger.getLogger("logger").log(Level.INFO,"Manager sent a discount broadcast for "+ shoeType+ " but it doesn't exist in storage, sneaky guy");
	}
	
	/**
	 * Save the given receipt in the store.
	 * @param receipt : the receipt to add to the receipt_list
	 */
	public void file(Receipt receipt){
		receipt_list.add(receipt);
	}
	
	/**
	 * This method prints to the standard output the following information:
	• For each item on stock - its name, amount and discountedAmount
	• For each receipt filed in the store - all its fields Please print this information in a comperhansive and easy to follow format.
	 */
	public void print(){
		synchronized(System.out){
			Logger.getLogger("logger").log(Level.INFO,"\n 			################################## Store storage ################################## \n");
			for ( String shoeType : storage.keySet()){
				ShoeStorageInfo shoe = storage.get(shoeType);
				shoe.printInfo();
			}
			Logger.getLogger("logger").log(Level.INFO,"\n 			################################## Store Receipts ################################## \n");
					Logger.getLogger("logger").log(Level.INFO,"			#######################       NUMBER OF RECEIPTS = "+receipt_list.size()+"       ########################");
			for (Receipt curr_receipt : receipt_list){
				 curr_receipt.printReceipt();
			}
		}
	}
}