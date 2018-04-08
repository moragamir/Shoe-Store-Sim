package bgu.spl.app;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import bgu.spl.mics.MicroService;

/**
 * This micro-service can add discount to shoes in the store and send NewDiscountBroadcast to notify
 * clients about them. In order to do so, this service expects to get a list of DiscountSchedule as
 * argument to its constructor (the list does not guaranteed to be ordered).
 * In addition, the ManagementService handles RestockRequests that is being sent by the Sell-
 * ingService. Whenever a RestockRequest of a specific shoe type received the service first check that
 * this shoe type is not already on order (and if it does, it checks that there are enough ordered to
 * give one to the seller) if it doesn't (or the ordered amount was not enough) it will send a ManufacturingOrderRequest for (current-tick%5) + 
 * 1 shoes of this type, when this order completes - it
 * updates the store stock, file the receipt and only then complete the RestockRequest (and not before)
 * with the result of true. If there were no one that can handle the ManufacturingOrderRequest (i.e.,
 * no factories are available) it will complete the RestockRequest with the result false.
 * For example, assume that in tick=2 the ManagementService received a RestockRequest of brown-
 * flip-flops, it does not already ordered them so it send a ManufacturingOrderRequest for 3 brown-flip-
 * flops which take some time to complete. Now assume that in tick=3 the ManufacturingOrderRequest
 * is not yet completed but the ManagementService received another RestockRequest of brown-flip-flops
 * - since it knows that it ordered 3 brown-flip-flops (1 of which was already reserved) it will not make
 * a new order but also not complete the RestockRequest. once the ManufacturingOrderRequest is
 * completed with a receipt as its result, the ManagementService will file the receipt, complete the two
 * RestockRequest and add only 1 brown-flip-flops to the store (the one left).
 * 
 * @param curr_tick : current time in the clock
 * @param discountSchedule : a list of DiscountSChedule items to broadcast to interested microservices 
 * @param ordered_shoes : a hash map that keeps the amount of recent client orders to each shoe by Integer  
 * @param awaiting_requests : a hash map that keeps a list of awaiting restock requests keyed by the shoe,
 *  when the factory finishes the request the manager will take from the right shoe list and send a true message to the right amount of requests (calculated by the amount the factory created) 
 *
 */
public class ManagementService extends MicroService {

	private int curr_tick;
	private LinkedList<DiscountSchedule> discountSchedule; 
	private ConcurrentHashMap<String,AtomicInteger> ordered_shoes; 
	private ConcurrentHashMap<String,LinkedBlockingQueue<RestockRequest>> awaiting_requests;
	
	
	/**
	 * the constructor for the manager
	 * @param discountScheduleList is a list of DiscountSchedule that the manager sends as discount broadcasts
	 * @param c is the count down latch used in the initializing part of the program
	 * @param f is the count down latch used in the terminating part of the program
	 */
	public ManagementService(List<DiscountSchedule> discountScheduleList, CountDownLatch c,CountDownLatch f) { // list not guaranteed to be ordered 
		super("manager",c,f);
		ordered_shoes = new ConcurrentHashMap<String,AtomicInteger>();
		discountSchedule = new LinkedList<DiscountSchedule>();
		discountSchedule.addAll(discountScheduleList);
		awaiting_requests = new ConcurrentHashMap<String,LinkedBlockingQueue<RestockRequest>>();
	}

	/**
	 * this function is sort of a partial copy constructor used for the Json file to copy the necessary info for initializing
	 */
	public ManagementService(ManagementService m, CountDownLatch c, CountDownLatch f) { // list not guaranteed to be ordered 
		super("manager",c,f);
		this.curr_tick=m.getCurr_tick();
		this.discountSchedule = m.getDiscountList();
		this.ordered_shoes = m.getOrdered_shoes();
		this.awaiting_requests = new ConcurrentHashMap<String,LinkedBlockingQueue<RestockRequest>>();
	}
	
	/**
	 * the function subscribes the manager for tick broadcasts and re-stock requests,
	 * every tick he checks in his discount schedule for new discounts and broadcsts them.
	 * when receives a restock request he checks if he already ordered this shoe and if not he will send a
	 * ManufacturingOrderRequest to a factory
	 * @param tick_msg : is the broadcast message 
	 * @param restock_msg : is the re-stock request message
	 * 
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void initialize() {


		subscribeBroadcast(TickBroadcast.class, tick_msg -> { //subscribe to timer
			if (!tick_msg.isRunning()){ //if reached duration time 
				Logger.getLogger("logger").log(Level.INFO, " is terminating ");
				terminate();
			}
			setTick(tick_msg.getTick());
			Store myStore = Store.storeGetInstance();
			//DISCOUNT BROADCAST: EVERY TICK CHECK IF ONE EXISTS  
			for (DiscountSchedule disc_sched : discountSchedule){
				String shoe = disc_sched.getShoeType();
				int amount = disc_sched.getAmount();
				if (disc_sched.getTick()==curr_tick){
					myStore.addDiscount(shoe, amount);
					sendBroadcast(new NewDiscountBroadcast(shoe,amount));
				}
			}		
		});
		
		
		//SUBSCRIBE FOR RESTOCKS
		subscribeRequest(RestockRequest.class, restock_msg -> {
			String shoe=restock_msg.getShoeType();								// wanted shoe type
			if (!awaiting_requests.containsKey(shoe))
				awaiting_requests.put(shoe,new LinkedBlockingQueue<RestockRequest>());
			
			AtomicInteger newAmount = new AtomicInteger((curr_tick%5)+1);		//amount to order
			Store myStore = Store.storeGetInstance();
			int shoes_in_factory; 												//how much shoes we already ordered

			if (ordered_shoes==null) 		//create new ordered shoe list if it doesnt exist
				ordered_shoes = new ConcurrentHashMap<String,AtomicInteger>();
			
			if ( ordered_shoes.isEmpty())	//no shoes have been ordered
				shoes_in_factory=0;
			else{
				if (!ordered_shoes.containsKey(shoe)) // shoe of the specified type hasnt been ordered
					shoes_in_factory=0;
				else
					shoes_in_factory=ordered_shoes.get(shoe).intValue();  // number of ordered shoes of the shoe type requested
			}
			
			LinkedBlockingQueue<RestockRequest> count_orders = awaiting_requests.get(shoe);
			
			int	num_of_client_orders=0;
			if (count_orders!=null){
				num_of_client_orders = count_orders.size(); //how many web clients ordered the shoe
			}
			boolean	orderOrNot = (shoes_in_factory<num_of_client_orders+1); //ordered shoes are smaller than the amount of waiting orders (+1 for current restock request)
			if(!ordered_shoes.containsKey(shoe) || orderOrNot){ // MUST ORDER
				AtomicInteger in;
				if( !ordered_shoes.containsKey(shoe)){ // if the shoe hasn't been re-stocked at all
					in = new AtomicInteger(1);
					ordered_shoes.put(shoe, newAmount);
				}
				else{ //shoe has been re-stocked before
					in = new AtomicInteger(count_orders.size()+1); //new amount of client requests
					ordered_shoes.put(shoe,new AtomicInteger(newAmount.intValue() + ordered_shoes.get(shoe).intValue()));
				}
				count_orders.add(restock_msg);
				
				//IF NECESSARY SEND A MANUFACTURING ORDER REQUEST
				boolean success = sendRequest(new ManufacturingOrderRequest<Receipt>(newAmount.intValue(),shoe,curr_tick), facMsg->{
										
					if(facMsg==null){
						Logger.getLogger("logger").log(Level.INFO, "Restock attempt for :" +shoe+" failed - no factory available");
						complete(restock_msg,new Boolean(false));
					}
					else{
						int ordered = count_orders.size(); // how many requests from clients
						myStore.file((Receipt)facMsg);
						int amountToAddToStorage = ((Receipt)facMsg).getAmountSold()-ordered; //how much shoes add to storage
						if (amountToAddToStorage>0)
							myStore.add(shoe, amountToAddToStorage); 
						
						int oldNumOfOrders = ordered_shoes.get(shoe).intValue(); //old number of restock orders sent to the factory
						
						int newNumofOrders = oldNumOfOrders-((Receipt) facMsg).getAmountSold(); //reduce the amount that already completed
						if (newNumofOrders<0)
							newNumofOrders=0;
						
						ordered_shoes.get(shoe).set(newNumofOrders); // update data for this shoe (1 restock already completed)
						
						RestockRequest temp;
						for( int i=1; i<= ((Receipt)facMsg).getAmountSold() && !count_orders.isEmpty(); i++){
							temp = count_orders.remove();
							complete(temp,new Boolean(true));
						}
					}
						
				});
				
				if (success)
					Logger.getLogger("logger").log(Level.INFO, "the Manager sent a Restock request to factory for "+newAmount+" "+shoe+" and waits for arrival");
				else{
					Logger.getLogger("logger").log(Level.INFO, "the Manager tried to send a restock request for "+newAmount+" "+shoe+" but no one cares");
					complete(restock_msg,false);
				}	
			}
			else{ // Is not necessary to send a new order (already ordered) 
				count_orders.add(restock_msg);
				Logger.getLogger("logger").log(Level.INFO, "Manager respondes to "+restock_msg.getSeller()+": No need for Restock for "+shoe+", it's on it's way");
			}  
			
		});
		
		latchObject.countDown();
		
	}

	private void setTick(int tick) {
		this.curr_tick=tick;
	}

	public ConcurrentHashMap<String,AtomicInteger> getOrdered_shoes() {
		return ordered_shoes;
	}

	public void setOrdered_shoes(ConcurrentHashMap<String,AtomicInteger> ordered_shoes) {
		this.ordered_shoes = ordered_shoes;
	}

	public int getCurr_tick() {
		return curr_tick;
	}

	public void setCurr_tick(int curr_tick) {
		this.curr_tick = curr_tick;
	}
	
	private HashMap<String,AtomicInteger> getOrderedShoes(){
		HashMap<String,AtomicInteger> newhash = new HashMap<String,AtomicInteger>();
		if (ordered_shoes!=null){
			Set<String> keys = ordered_shoes.keySet();
			for (String s : keys){
				AtomicInteger temp = ordered_shoes.get(s);
				newhash.put(s,temp);
			}
		}
		return newhash;
	}

	
	private LinkedList<DiscountSchedule> getDiscountList(){
		LinkedList<DiscountSchedule> newlist = new LinkedList<DiscountSchedule>();
		if (discountSchedule!=null)
			for (DiscountSchedule d : discountSchedule){
				newlist.add(d);
			}
		return newlist;
	}

}