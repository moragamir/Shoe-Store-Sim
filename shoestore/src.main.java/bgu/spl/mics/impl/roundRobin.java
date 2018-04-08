package bgu.spl.mics.impl;

import java.util.concurrent.LinkedBlockingQueue;

import bgu.spl.mics.MicroService;

/**
 * a round robin is a list of micro services that are handled in a round-robbin manner.
 * the Object that contains 2 lists - when pushing an object will always enter the 1st list, and when popping from the first it will always be pushed to the second list.
 * when the first list is empty, the second list will be moved to the first and the second list will be emptied
 */
public class roundRobin {
	
	private LinkedBlockingQueue<MicroService> first;
	private LinkedBlockingQueue<MicroService> second;
	
	/**
	 * Round robin constractor's, consist from 2 queues
	 * @param none
	 * @return new roundrobin object
	 */
	public roundRobin(){
		first = new LinkedBlockingQueue<MicroService>();
		second = new LinkedBlockingQueue<MicroService>();
	}

	/**
	 * field-getter : Roundrobin FirstQueue's
	 * @param none
	 * @return Roundrobin FirstQueue's
	 */
	public LinkedBlockingQueue<MicroService> getFirst(){
		return first;
	}
	
	/**
	 * field-getter : Roundrobin SecoundQueue's
	 * @param none
	 * @return Roundrobin SecoundQueue's
	 */
	public LinkedBlockingQueue<MicroService> getSecond(){
		return second;
	}

	/**
	 * 
	 */
	// if first list is empty move all from the second to the first
	private synchronized void resetQueues(){
		if (first.isEmpty()){
			MicroService temp;
			while (!second.isEmpty()){
				temp = second.poll();
				try {
					first.put(temp);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	//adds to first list
	public synchronized void add (MicroService m){
			try {
				first.put(m);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}
	
	// removes completely the micro service from the list
	public synchronized void remove(MicroService m){
		if (first.contains(m))
			first.remove(m);
		if (second.contains(m))
			second.remove(m);
		resetQueues();
	}
	
	//returns first in list and puts him back in the end of the list (round robin)
	public synchronized MicroService popFromQueue(){
		if (!first.isEmpty()){
			MicroService temp = first.remove();
			try {
				second.put(temp);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			resetQueues();
			return temp;
		}
		else
			throw new NullPointerException();
	}
	
	// both lists are empty
	public boolean isEmpty(){
		return (first.isEmpty()&&second.isEmpty());
	}
	
	//found in one of the lists
	public boolean contains(MicroService m){
		return (first.contains(m)||second.contains(m));
	}
	
	
	//***********THIS METHOD ONLY FOR JUNIT *******************
	
	public MicroService removeFirst(){
		return first.poll();
	}
}