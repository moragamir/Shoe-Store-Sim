package bgu.spl.app;
import bgu.spl.mics.Broadcast;

public class TickBroadcast implements Broadcast {

	private int tick; // current tick
	private boolean isRunning; // if tick < duration we return true	
		
	public TickBroadcast(int tick, boolean terminate) {
		super();
		this.tick = tick;
		this.isRunning = terminate;
	}

	public int getTick(){ // get current tick
		return tick;
	}
	
	public boolean  isRunning(){ // checks if we reached duration time
		return isRunning;
	}
}

