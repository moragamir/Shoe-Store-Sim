package bgu.spl.app;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import bgu.spl.mics.MicroService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *This micro-service describes one client connected to the web-site. The WebsiteClientService expects
 * to get two lists as arguments to its constructor: purchaseSchedule: List<PurchaseSchedule>
 * - contains purchases that the client needs to make (every purchase has a corresponding time tick to send the PurchaseRequest
 * ). The list does not guaranteed to be sorted. Important: The WebsiteClientService will make the purchase on
 * the tick specified on the schedule irrelevant of the discount on that item. wishList: Set<String>
 * - The client wish list contains name of shoe types that the client will buy only when there is a discount on them (and immidiatly when he found out of such
 * discount). Once the client bought a shoe from its wish list - he removes it from the list.
 * In order to get notified when new discount is available, the client should subscribe to the NewDiscountBroadcast
 * message.  If the client finish receiving all its purchases and have nothing in its
 * wishList it must immediately terminate.
 * 
 * @param purchaseSchedule is the list of PurchaseSchedule objects that he sends
 * @param wishList is a set of shoes that the client wishes to buy on discount
 * @param waiting_for_receipts is the amount of requests the client sent and is waiting to receive (from re stocks)
 */
public class WebsiteClientService extends MicroService {
	
	private int curr_tick;
	private LinkedBlockingQueue<PurchaseSchedule> purchaseSchedule;
	private Set<String> wishList;
	AtomicInteger waiting_for_receipts; // count of orders requested (when =0 we terminate if no other future requests exist) 
	
	public WebsiteClientService(String name, List<PurchaseSchedule> pur_list, Set<String> wish_list, CountDownLatch c,CountDownLatch f) {
		super(name,c,f);
		purchaseSchedule = new LinkedBlockingQueue<PurchaseSchedule>();
		purchaseSchedule.addAll(pur_list);
		this.wishList = new HashSet<String>();
		this.wishList.addAll(wish_list);
		curr_tick=1;
		waiting_for_receipts = new AtomicInteger(0);
	}
	
	public WebsiteClientService(WebsiteClientService w, CountDownLatch c, CountDownLatch f){
		super(w.getName(),c,f);
		this.curr_tick=w.getCurr_tick();
		this.purchaseSchedule=w.getPurchase_list();
		this.wishList=w.getWish_list();
		this.waiting_for_receipts = new AtomicInteger(0);
	}

	public int getCurr_tick() {
		return curr_tick;
	}

	public LinkedBlockingQueue<PurchaseSchedule> getPurchase_list() {
		LinkedBlockingQueue<PurchaseSchedule> newlist = new LinkedBlockingQueue<PurchaseSchedule>();
		if (purchaseSchedule!=null){
			for (PurchaseSchedule p : purchaseSchedule){
				newlist.add(p);
			}
		}
		return newlist;
	}

	public Set<String> getWish_list() {
		HashSet<String> newset = new HashSet<String>();
		if (wishList!=null)
			newset.addAll(wishList);
		return newset;
	}

	public void setTick(int t){
		curr_tick=t;
	}
	/**
	 * initializing will subscribe the web client to tick broadcasts and to new discount broadcasts
	 * every tick he will check if he has a Purchase schedule for that tick and will make it if positive
	 * if a discount broadcast for an item on his wish list will be sent by the manager he will immediately send a purchase request for it
	 */
	@Override
	protected void initialize() {
		
		subscribeBroadcast(TickBroadcast.class, tick_msg -> { //subscribe to timer
			if (!tick_msg.isRunning()){ //if reached duration time or minutes before termination=0
				Logger.getLogger("logger").log(Level.INFO,getName()+ " is terminating ");
				terminate();
			}
			setTick(tick_msg.getTick());
			
			for (PurchaseSchedule curr_p : purchaseSchedule){ //RUN OVER LIST AND CHECK IF TICK = CURR TICK
				
				if (curr_p.getTick()==curr_tick){
										
					String shoeT = curr_p.getShoeType();
					boolean success = sendRequest(new PurchaseOrderRequest(getName(),curr_tick,shoeT,1,false), receipt -> { //false= dont care about discount
						if (receipt==null){
							Logger.getLogger("logger").log(Level.INFO,getName()+ " tried to purchase "+shoeT+" but failed");
							waiting_for_receipts.decrementAndGet();
						}
						else{
							if (receipt.isDiscount())
								wishList.remove(shoeT);
							waiting_for_receipts.decrementAndGet();
							receipt.printReceipt();
						}
					} );
					if (success){ // someone received and handling his purchase request 
						Logger.getLogger("logger").log(Level.INFO,getName()+ " sent a Purchase request for "+shoeT+ " and waiting for its completion" );	
						waiting_for_receipts.incrementAndGet();
					}
					else{   // no one cares
						Logger.getLogger("logger").log(Level.INFO,getName()+ "tried to send a Purchase request for "+shoeT+" but no one cares");
						waiting_for_receipts.decrementAndGet();	
					}
					
					boolean found=false;
					if (purchaseSchedule!=null){
						for (PurchaseSchedule pur_curr : purchaseSchedule){ //removes the item from the list
							if (!found && pur_curr.getShoeType()==shoeT && pur_curr.getTick()==curr_tick){
								purchaseSchedule.remove(pur_curr);
								found=true;
							}
						}
					}
				}
			}
			if (wishList==null || wishList.isEmpty())
				if(purchaseSchedule==null || purchaseSchedule.isEmpty())
					if (waiting_for_receipts.get()==0)
						terminate();
			
		});
		
		
		// subscribe to discounts
		subscribeBroadcast(NewDiscountBroadcast.class, disc_msg -> { //SUBSCRIBE TO DISCOUNTS			
			String shoe = disc_msg.getShoeType();
			if (wishList.contains(shoe)){
				wishList.remove(shoe);
				Logger.getLogger("logger").log(Level.INFO,getName()+"'s wish has come true! "+ disc_msg.getShoeType()+" is on discount! she will try to buy it");
				PurchaseOrderRequest p = new PurchaseOrderRequest(getName(),curr_tick,shoe,1,true);
				
				boolean success = sendRequest(p, receipt -> {
					if (receipt==null){
						wishList.add(shoe);
						Logger.getLogger("logger").log(Level.INFO,getName()+ " tried to purchase with discounted price "+shoe+", but failed");
						waiting_for_receipts.decrementAndGet();
					}
					else{ 
							waiting_for_receipts.decrementAndGet();
							receipt.printReceipt();
					}
				});
				if (success){ // someone received and handling his purchase request 
					waiting_for_receipts.incrementAndGet();
					Logger.getLogger("logger").log(Level.INFO,getName()+ " sent a Purchase request for "+shoe+ " and waiting for its completion" );
				}
				else{ // no one cares
					Logger.getLogger("logger").log(Level.INFO,getName()+ "tried to send a Purchase request for "+shoe+" but no one cares");
					waiting_for_receipts.decrementAndGet();
				}
				boolean found=false;
				for (PurchaseSchedule curr_p : purchaseSchedule){ //removes the item from the list
					if (!found && curr_p.getShoeType()==shoe && curr_p.getTick()==curr_tick){
						purchaseSchedule.remove(curr_p);
						found=true;
					}
				}
			}
		});
		latchObject.countDown();
	}
	
}
