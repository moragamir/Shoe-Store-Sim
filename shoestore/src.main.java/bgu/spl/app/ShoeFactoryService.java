package bgu.spl.app;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import bgu.spl.mics.MicroService;

/**
 * This micro-service describes a shoe factory that manufacture shoes for the store. This micro-service
 * handles the ManufacturingOrderRequest it takes it exactly 1 tick to manufacture a single shoe, this
 * means that completing a ManufacturingOrderRequest of 3 shoes will take it 3 ticks to complete
 * (starting from tick following the request). When done manufacturing, this micro-service completes
 * the request with a receipt (which has the value “store” in the customer field and “discount” = false).
 * The micro-service cannot manufacture more than one shoe per tick. For example, if on tick=5
 * the factory received ManufacturingOrderRequest for 3 purple-heel-shoes and in tick=7 it receive
 * another order for 2 orange-flip-flops it will complete the first request on tick 9 and the second on tick 11
 *
 *@param restock_orders : a list of ManufacturingOrderRequest that have been ordered by manager, will create them by queue
 *@param curr_tick: the current tick in the clock
 *@param curr_request : the current ManufacturingOrderRequest that the factory is working on
 *@param curr_amount : the current amount of shoes left to create in the curr_request order
 */
public class ShoeFactoryService extends MicroService{

	private LinkedBlockingQueue<ManufacturingOrderRequest<Receipt>> restock_orders;
	private int curr_tick;
	private ManufacturingOrderRequest<Receipt> curr_request;
	private int curr_amount;
	
	public ShoeFactoryService(String name, CountDownLatch c, CountDownLatch f){
		super(name,c,f);
		restock_orders = new LinkedBlockingQueue<ManufacturingOrderRequest<Receipt>>();
		curr_tick=1;
		curr_request=null;
	}

	public void setTick(int tick){
		curr_tick=tick;
	}
	
	public void setRequest(ManufacturingOrderRequest<Receipt> request){
		curr_request=request;
	}
	/**
	 * initialize subscribes the factory to tick broadcasts and to manufacturing order requests
	 * and will create 1 shoe per day for every order, when finishing he will send a receipt to the manager
	 */
	@Override
	protected void initialize() {
		
		subscribeBroadcast(TickBroadcast.class, tick_msg -> { //subscribe to timer
			if (!tick_msg.isRunning()){ //if reached duration time 
				Logger.getLogger("logger").log(Level.INFO,getName()+ " is terminating ");
				terminate();
			}
			setTick(tick_msg.getTick());
			
			if (curr_request!=null){
				if (curr_amount>0){
					Logger.getLogger("logger").log(Level.INFO,getName()+ " created a pair of " +curr_request.getShoeType());
					curr_amount--;
				}
				else if (curr_amount==0){
					Logger.getLogger("logger").log(Level.INFO,getName()+ " finished producing the restock request for "+curr_request.getAmount()+" "+curr_request.getShoeType());
					Receipt r= new Receipt(getName(),"store",curr_request.getShoeType(),false,curr_tick,curr_request.getCurr_tick(),curr_request.getAmount());
					complete(curr_request,r);
					if (!restock_orders.isEmpty()){
						curr_request = restock_orders.remove();
						curr_amount = curr_request.getAmount();
						if (curr_amount>0){
							Logger.getLogger("logger").log(Level.INFO,getName()+ " created a pair of " +curr_request.getShoeType());
							curr_amount--;
						}
					}
					else
						curr_request=null;
				}
			}
			else{
				if (!restock_orders.isEmpty()){
					setRequest(restock_orders.remove());
					Logger.getLogger("logger").log(Level.INFO,getName()+ " created 1 pair of "+curr_request.getShoeType());
					curr_request.setAmount(curr_request.getAmount()-1);
				}
			}
			
		});
		
		subscribeRequest(ManufacturingOrderRequest.class, man_req -> {
			Logger.getLogger("logger").log(Level.INFO,getName()+ " was asked from the store to produce "+man_req.getAmount()+" "+man_req.getShoeType());
			if (curr_request==null){
				setRequest(man_req);
				curr_amount=man_req.getAmount();
			}
			else
				try {
					restock_orders.put(man_req);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
		});
		
		latchObject.countDown();
	}
//	public Receipt(String seller, String customer, String shoeType, boolean discount, int issuedTick, int requestTick, int amountS){

	
	
}
