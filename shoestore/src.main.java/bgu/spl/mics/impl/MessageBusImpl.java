package bgu.spl.mics.impl;

import bgu.spl.mics.Broadcast;
import bgu.spl.mics.Message;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.Request;
import bgu.spl.mics.RequestCompleted;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;


public class MessageBusImpl implements bgu.spl.mics.MessageBus {
	private ConcurrentHashMap<MicroService,LinkedBlockingQueue<Message>> micServiceMap; 
	private ConcurrentHashMap<Class<? extends Request>,roundRobin> requestMap;
	private ConcurrentHashMap<Class<? extends Broadcast>,roundRobin> broadcastMap;
	private ConcurrentHashMap<Request,MicroService> askersMap;
	private ConcurrentHashMap<MicroService,Object> lockMap; 
	
	/**
	 * Constructor of the message bus implementation
	 * 
	 * @param micServiceMap : a hash that saves for each microService: the queue of awaiting tasks to work on
	 * @param requestMap :a hash that saves each request CLASS :list of the microServices that receive it
	 * @param broadcastMap : a hash the saves each broadcast TYPE :list of the microServices that receive it
	 * @param askersMap: a hash that saves for each message, who is the requester who asked for it
	 * @param lockMap : a hash of locks for each micro services used for synchronization
	 * 
	 */
	private MessageBusImpl(){	
		micServiceMap= new ConcurrentHashMap<MicroService,LinkedBlockingQueue<Message>>();
		requestMap = new ConcurrentHashMap<Class<? extends Request>,roundRobin>();
		broadcastMap = new ConcurrentHashMap<Class<? extends Broadcast>,roundRobin>();
		askersMap = new ConcurrentHashMap<Request,MicroService>();	
		lockMap = new ConcurrentHashMap<MicroService,Object>(); 
	}
	/**
	 * singleton for constructor
	 */
	private static class singletonHolder{
		private static MessageBusImpl msgBus = new MessageBusImpl();
	}
	
	/**
	 * singleton get instance method
	 */
	public static MessageBusImpl MsgBusGetInstance(){
		return singletonHolder.msgBus;
	}
	
	
    public void subscribeRequest(Class<? extends Request> type, MicroService m){
	    	// if this type of request doesn't exist, create it
	    	requestMap.putIfAbsent(type, new roundRobin());
	    	
	    	// if this microService is already in the list, don't add it. otherwise add it
	    	if (!requestMap.get(type).contains(m))
	    		requestMap.get(type).add(m);
    }


    public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m){

		// if this type of request doesn't exist, create it
		broadcastMap.putIfAbsent(type, new roundRobin());
		
		// if this microService is already in the list, don't add it. otherwise add it
		if (!broadcastMap.get(type).contains(m))
			broadcastMap.get(type).add(m);
    }

      
    public <T> void complete(Request<T> r, T result){
    	RequestCompleted<T> compReq = new RequestCompleted<T>(r,result);
    	MicroService asker = askersMap.get(r); 
    	LinkedBlockingQueue<Message> asker_list = micServiceMap.get(asker);
    	asker_list.offer(compReq);
    		//add to the requester queue the reqCompleted message
    	//removes the completed request from askersMap
    }


    public void sendBroadcast(Broadcast b){
    	if (broadcastMap.containsKey(b.getClass())){ 
    		roundRobin broadcastList = broadcastMap.get(b.getClass()); //list of interested MS in broadcast

    		for (MicroService m : broadcastList.getFirst()){ // send broadcast to all MS in the first list
    			synchronized(micServiceMap.get(m)){
    				micServiceMap.get(m).offer(b);
    			}
    		}
    		for (MicroService m : broadcastList.getSecond()){ // send broadcast to the other part of the list 
    			synchronized(micServiceMap.get(m)){
    				micServiceMap.get(m).offer(b);
    			}
    		}
    	}
    }


    public boolean sendRequest(Request<?> r, MicroService requester){
    	if (!requestMap.containsKey(r.getClass()))
    		return false;
    	if (!isRegistered(requester) || requestMap.get(r.getClass()).isEmpty()){ //the requester has unregistered, or none interested in this kind of message
    		return false;
    	}
    	else {
    		MicroService reqHandler = requestMap.get(r.getClass()).popFromQueue(); //remove from list until finds a registered user
    		synchronized(lockMap.get(reqHandler)){	    			 // add to the handlers message list
	        	if (isRegistered(reqHandler)){
	            	askersMap.put(r,requester); // add the requester (for oncomplete) 
	    			micServiceMap.get(reqHandler).offer(r);
	            	return true;
	    		}
    		}
    		while (!requestMap.get(r.getClass()).isEmpty() && !isRegistered(reqHandler)){ 
    	    		reqHandler = requestMap.get(r.getClass()).popFromQueue();
    	        	synchronized(lockMap.get(reqHandler)){
    	        		if (isRegistered(reqHandler)){ // add to the handlers message list
    	               		askersMap.put(r,requester); // add the requester (for oncomplete) 
    	        			micServiceMap.get(reqHandler).offer(r);
    	               		return true;
    	        		}
    				}	
    		}
    		return false;

    			// NOTE: code design allowing to send multiple messages to other people
    			// 		 multiple messages to the same MS are synchronized one by one
    			// edge case: 2 MS trying to add message to unregistered user, one must wait until the first MS to lock on the unregistered will find a new registered usr
    	} 	
    }


    public void register(MicroService m){
    	//add to microService [messages waiting for him]
       	if (!isRegistered(m)){
	    	micServiceMap.putIfAbsent(m, new LinkedBlockingQueue<Message>());
	    	lockMap.put(m, new Object()); //new lock
       	}
    }


    public void unregister(MicroService m){
    	synchronized(lockMap.get(m)){
    			micServiceMap.get(m).clear();   //removes all messages to the microservice from the list 
    			micServiceMap.remove(m); // remove the list
    		
    		//DELETE APPEARNCES IN REQUESTS LISTS 
    		for(Class<? extends Request> iter : requestMap.keySet() )
    			requestMap.get(iter).remove(m); // deletes if present in list
    		
    		//DELETE APPEARENCES IN BROADCAST LISTS
    		for(Class<? extends Broadcast> iter : broadcastMap.keySet())
    			broadcastMap.get(iter).remove(m); // deletes if present in list
    	}
    }
	
    

    public Message awaitMessage(MicroService m) throws InterruptedException{
    	if (isRegistered(m))
    		return micServiceMap.get(m).take();
    	else
    		return null; // MS has unregistered
    }
    
    
        
    private boolean isRegistered(MicroService m){
    		return micServiceMap.containsKey(m);
    }
    
    
    
    //************* THE FOLLOWING METHODS ARE USED ONLY! FOR THE J-UNIT TESTS!!! *******************************
    
    public ConcurrentHashMap<MicroService,LinkedBlockingQueue<Message>> getMicroMap(){
    	return micServiceMap;
    }
    
    public ConcurrentHashMap<Class<? extends Request>,roundRobin> getRequestMap(){
    	return requestMap;
    }
    
    public ConcurrentHashMap<Class<? extends Broadcast>,roundRobin> getBroadcastMap(){
    	return broadcastMap;
    }
    public ConcurrentHashMap<Request,MicroService> getAskerMap(){
    	return askersMap;
    }
     
    
    
}
