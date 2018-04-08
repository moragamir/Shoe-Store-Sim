package bgu.spl.app;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.logging.*;

import com.google.gson.Gson;

/**
 * When started, it should accept as argument (command line argument) the name of the json input file to read - there
	are some example input files in the files attched to this work and you can create more yourself).
	The ShoeStoreRunner should read the input file (using Gson), it then should add the initial storage to
	the store and create and start the micro-services. When the current tick number is larger than the
	duration given to the TimeService in the input file all the micro-services should gracefully terminate
	themselves. You must make sure that micro-services will not miss the first TickBroadcast (because they was not started yet).
	After all the micro-services terminate themselves the ShoeStoreRunner should call the Store’s
	print function and exit. Important: all the threads in the system should be terminated gracefully.
 *
 *@param logger : prints all methods to the console with relevant info
 *
 */

public class ShoeStoreRunner {
	
	public static Logger logger=Logger.getLogger("logger");
	public static void main(String[] args) {
	
		Scanner sc = new Scanner(System.in);
		String input = sc.nextLine(); // enter the name of the json file
		CountDownLatch readyForTimer;
		CountDownLatch finishProgram;
		int numOfThreads;
		List<SellingService> sellers=new LinkedList<SellingService>();
		List<ShoeFactoryService> factorys=new LinkedList<ShoeFactoryService>();
		Gson gson=new Gson();
		try {
			//"/users/studs/bsc/2016/sdanie/workspace/assignment2/src.main.java/"+input+".json"
			BufferedReader reader = new BufferedReader(new FileReader(args[0]));
			JsonData data = gson.fromJson(reader,JsonData.class);
			Store.storeGetInstance().load(data.initialStorage);
				
			numOfThreads= data.services.factories+data.services.sellers+ data.services.customers.length+1; //1= manager
			readyForTimer = new CountDownLatch(numOfThreads); 
			finishProgram = new CountDownLatch(numOfThreads-1); // only the timer
			for(int i=1;i<= data.services.factories;i++){
				factorys.add(new ShoeFactoryService("factory "+i,readyForTimer,finishProgram));
		
			}
				
			
			for(int i=1;i<= data.services.sellers;i++){
				sellers.add(new SellingService("seller "+i,readyForTimer,finishProgram));
			}
			ManagementService manager=new ManagementService( data.services.manager,readyForTimer, finishProgram);
			
			Thread t = new Thread(manager);
			t.start();
			
			WebsiteClientService webclient;
			for(WebsiteClientService client : data.services.customers){
				webclient = new WebsiteClientService(client,readyForTimer, finishProgram);
				new Thread(webclient).start();
			
			}
			
			for(SellingService s : sellers)
				new Thread(s).start();
			
			for(ShoeFactoryService s : factorys)
				new Thread(s).start();
				
			new Thread(new TimeService( data.services.time.getDuration(), data.services.time.getSpeed(),readyForTimer,finishProgram)).start();
			
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		
	}