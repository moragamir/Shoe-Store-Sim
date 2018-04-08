package bgu.spl.app;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import bgu.spl.app.Store.BuyResult;
import bgu.spl.mics.MicroService;

/**
 * This micro-service handles PurchaseOrderRequest. When the SellingService receives a Purchase-OrderRequest
 * it handles it by trying to take the required shoe from the storage. If it succeeded
 * it creates a receipt, file it in the store and pass it to the client (as the result of completing the
 * PurchaseOrderRequest). If there were no shoes on the requested type on stock, the selling service
 * will send RestockRequest, if the request completed with the value “false” (see ManagementService)
 * the SellingService will complete the PurchaseOrderRequest with the value of “null” (to indicate to
 * the client that the purchase was unsuccessful. If the client indicates in the order that he wish to
 * get this shoe only on discount and no more discounted shoes are left then it will complete the client
 * request with null result 
 * 
 * @param curr_tick : the current time in the clock
 * 
 */
public class SellingService extends MicroService{

	private int curr_tick;
	private ConcurrentHashMap<String,LinkedBlockingQueue<PurchaseOrderRequest>> restock_waiting_list;
	
	public SellingService(String name, CountDownLatch c, CountDownLatch f){ 
		super(name,c,f);
		curr_tick = 0;
		restock_waiting_list = new ConcurrentHashMap<String,LinkedBlockingQueue<PurchaseOrderRequest>>();
	}

	public void setTick(int t){
		curr_tick=t;
	}

	/**
	 * during initialization the seller subscribes to tick broadcasts and purchase order requests.
	 */
	protected void initialize() {
		
		
		subscribeBroadcast(TickBroadcast.class, tick_msg -> { //subscribe to timer
			if (!tick_msg.isRunning()){ //if reached duration time or minutes before termination=0
				terminate();
				Logger.getLogger("logger").log(Level.INFO, getName()+ " is terminating ");
			}
			setTick(tick_msg.getTick());
		});
		
		
		// SUBSCRIBE TO PURCHASE ORDER REQUEST
		subscribeRequest(PurchaseOrderRequest.class, (purMsg)-> {
			Logger.getLogger("logger").log(Level.INFO, getName()+" informed: "+ purMsg.getSenderName()+" wants to buy "+purMsg.getShoeType());
            Store myStore = Store.storeGetInstance();
            BuyResult purchase_result = myStore.take(purMsg.getShoeType(), purMsg.wantsDiscount()); // attempt to take
            
            //retreive data from the purchase message:	
            
            	String shoe = purMsg.getShoeType(); 
            	String sender=purMsg.getSenderName();
            	boolean discount=false;
            	int requestTick = purMsg.getTick(); 
            	int amountSold= purMsg.getAmount(); // when purchase message from a webclient = 1, when purchase from manager to factory can be more than 1
            	
            	if(purchase_result==BuyResult.DISCOUNTED_PRICE)
            		discount = true;
            	
           	//CREATE RECEIPT
        	Receipt receipt = new Receipt(getName(),sender,shoe, discount,curr_tick , requestTick ,amountSold); //need to get current tick from timer
			            
			switch(purchase_result){
            
            	case REGULAR_PRICE:{
            		myStore.file(receipt);
            		complete(purMsg, receipt);
            		break;
            	}
            	
            	
            	case NOT_ON_DISCOUNT:{
            		complete(purMsg,null);
            		break;
            	}
            	
            	case DISCOUNTED_PRICE:{
            		myStore.file(receipt);
            		complete(purMsg, receipt);
            		break;
            	}
            	
            	case NOT_IN_STOCK:{          		
            		if (!purMsg.wantsDiscount()){ // not a wish list request
	            		//send Restock Request to manager
	            		@SuppressWarnings("unchecked")
						boolean success = sendRequest(new RestockRequest<Boolean>(curr_tick, shoe ,1, getName()), (result) ->{ //manager will send (curr_tick%5)+1

		            		if (((Boolean) result).booleanValue()==false) //not succeeded
		            			Logger.getLogger("logger").log(Level.INFO, "Seller " +getName() +" notified that Restock request attempt for "+shoe+" had not succeeded");
	            			else {
	            				Logger.getLogger("logger").log(Level.INFO,getName()+" got informed that the ordered "+shoe+" for " +purMsg.getSenderName()+"  arrived!");
		            			int newTick = curr_tick;
		            			receipt.setIssuedTick(newTick); //put new finish time	            				
		            			Store.storeGetInstance().file(receipt);
		               			complete(purMsg,receipt);
		            		}			
	            		});
	            		
	            		
	            		// CHECKING IF THE REQUEST FOR PURCHASE HAS ARRIVED TO SOMEONE:
	            		if (success) {
	            			Logger.getLogger("logger").log(Level.INFO, "Sender "+getName()+" sent a restock request for "+shoe +" and waits for its completion");
	                           
	                           if (!restock_waiting_list.containsKey(purMsg.getShoeType())){
	                        	   LinkedBlockingQueue<PurchaseOrderRequest> newlist = new LinkedBlockingQueue<PurchaseOrderRequest>();
	                        	   newlist.add(purMsg);
	                          	   restock_waiting_list.put(purMsg.getShoeType(),newlist);
	                           }
	                           else
	                        	   restock_waiting_list.get(purMsg.getShoeType()).add(purMsg);
	            		}
	                    else{ //success=false
	                    	Logger.getLogger("logger").log(Level.INFO, "Sender " +getName()+ " tried to send a restock request for "+shoe+" but failed");
	                            complete(purMsg,null);             
	                            }
	            		break;
            		}
            		else
            			Logger.getLogger("logger").log(Level.INFO, sender+"'s wish: wanted to buy "+shoe+" but it's not in stock anymore");
            	}

            }
		});
		
		latchObject.countDown();
	}

}