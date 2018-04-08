package bgu.spl.tests;

import org.junit.Assert.*;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

import org.junit.After;
import org.junit.BeforeClass;

import bgu.spl.app.DiscountSchedule;
import bgu.spl.app.ManagementService;
import bgu.spl.app.PurchaseOrderRequest;
import bgu.spl.app.RestockRequest;
import bgu.spl.app.SellingService;
import bgu.spl.app.TickBroadcast;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.Request;
import bgu.spl.mics.RequestCompleted;
import bgu.spl.mics.impl.MessageBusImpl;


public class MessageBusImplTest {	
	private static MessageBusImpl bus;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		bus = MessageBusImpl.MsgBusGetInstance();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSubscribeRequest() {
		MicroService m = new SellingService("sason", null,null);
		bus.subscribeRequest(PurchaseOrderRequest.class, m);
		assertEquals(bus.getRequestMap().get(PurchaseOrderRequest.class).removeFirst(),m);
	}
	@Test
	public void testSubscribeBroadcast() {
		MicroService m = new SellingService("nitzan", null,null);
		bus.subscribeBroadcast(TickBroadcast.class, m);
		assertEquals(bus.getBroadcastMap().get(TickBroadcast.class).removeFirst(),m);
	}
	@Test
	public void testSendBroadcast() {
		MicroService m = new SellingService("nitzanizzer", null,null);
		bus.register(m);
		TickBroadcast t = new TickBroadcast(2,true);
		bus.subscribeBroadcast(TickBroadcast.class, m);
		bus.sendBroadcast(t);
		assertEquals(bus.getMicroMap().get(m).peek(),t);
	}
	@Test
	public void testSendRequest() {
		MicroService m = new SellingService("sasonizer", null,null);
		bus.register(m);
		MicroService manager = new ManagementService(new LinkedList<DiscountSchedule>(), null,null);
		bus.register(manager);
		RestockRequest r = new RestockRequest(50,"nike", 1,"sasonizer");
		bus.subscribeRequest(r.getClass(), manager);
		boolean flag = bus.sendRequest(r, m);
		assertTrue(flag);
		assertEquals(bus.getMicroMap().get(manager).remove(),r);
	}
	@Test
	public void testComplete() {
		Request req = new RestockRequest(50,"nike", 1,"seller1");
		Boolean result = true;
		MicroService m = new SellingService("sasonnn", null,null);
		bus.register(m);
		ConcurrentHashMap<Request,MicroService> map = bus.getAskerMap();
		map.put(req, m); //add to round robin
		bus.complete(req, result);
		assertTrue(bus.getMicroMap().get(m).peek().getClass()==RequestCompleted.class);
		// Checks that the stock exists
	}
	@Test
	public void testRegister() {
		MicroService m = new SellingService("bob",null, null);
		bus.register(m);
		assertTrue(bus.getMicroMap().containsKey(m));
	}
	@Test
	public void testUnregister() {
		MicroService m = new SellingService("dude", null, null);
		bus.register(m);
		bus.unregister(m);
		assertFalse(bus.getMicroMap().containsKey(m));
	}
	@Test
	public void testAwaitMessage() {
		MicroService m = new SellingService("sason", null,null);
		bus.register(m);
		MicroService manager = new ManagementService(new LinkedList<DiscountSchedule>(),null, null);
		bus.register(manager);
		RestockRequest r = new RestockRequest(40,"diadora",2,"seller2");
		bus.subscribeRequest(RestockRequest.class, manager);
		bus.sendRequest(r, m);
		try {
			assertEquals(bus.awaitMessage(manager),r);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


}
