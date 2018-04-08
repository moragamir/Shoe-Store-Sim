package bgu.spl.tests;

import static org.junit.Assert.*;

import bgu.spl.*;
import org.junit.Test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bgu.spl.app.Store.BuyResult;
import bgu.spl.app.Receipt;
import bgu.spl.app.ShoeStorageInfo;
import bgu.spl.app.Store;

public class StoreTest {
	
	private static Store store;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		store = Store.storeGetInstance();
	}

/*	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		store.print();
	}
*/
	@Before
	public void setUp() throws Exception {
		ShoeStorageInfo[] stock = {new ShoeStorageInfo("red-sandals", 7, 0),
				new ShoeStorageInfo("green-boots", 7, 0),
				new ShoeStorageInfo("black-sneakers", 3, 0),
				new ShoeStorageInfo("pink-flip-flops", 9, 0)};
		store.load(stock);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetInstance() {
		assertTrue(store.storeGetInstance()!=null);
	}
	@Test
	public void testTake() {
		assertEquals(store.take("nike-air", false), BuyResult.NOT_IN_STOCK);
		assertEquals(store.take("green-boots", false), BuyResult.REGULAR_PRICE);
	}
		
	@Test
	public void testLoad() {
		ShoeStorageInfo[] stock = {new ShoeStorageInfo("red-sandals", 7, 0),
				new ShoeStorageInfo("green-boots", 2, 0),
				new ShoeStorageInfo("black-sneakers", 1, 0),
				new ShoeStorageInfo("pink-flip-flops", 0, 0)};
		store.load(stock);
		assertEquals(store.take("black-sneakers",false), BuyResult.REGULAR_PRICE);
		// Checks that the stock exists
	}



}
