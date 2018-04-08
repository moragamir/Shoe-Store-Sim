package bgu.spl.app;

import bgu.spl.mics.MicroService;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *This micro-service is our global system timer (handles the clock ticks in the system). It is responsible
 * for counting how much clock ticks passed since the beginning of its execution and notifying every
 * other micro service (thats interested) about it using the TickBroadcast.
 * The TimeService receives the number of milliseconds each clock tick takes (speed:int) together
 * with the number of ticks before termination (duration:int) as a constructor arguments.
 * Be careful that you are not blocking the event loop of the timer micro-service. You can use the
 * Timer class in java to help you with that.
 * The current time always start from 1.
 *
 *@param speed : the time between each interval of the timer task (which sends a tick broadcast)
 *@param duration : tick when program will terminate
 *@param current_tick : the current tick in the clock
 */

public class TimeService extends MicroService {
	private int speed;
	private int duration;
	private int current_tick;
	
	public TimeService(int duration, int speed, CountDownLatch c, CountDownLatch f){
		super("timer",c,f);
		this.duration = duration;
		this.speed=speed;
		current_tick=1;
	}

	
	public int getSpeed() {
		return speed;
	}

	public int getDuration() {
		return duration;
	}

	public void setCurrTick(int t){
		current_tick=t;
	}
	
	/**
	 * initializing he will subscribe to its own ticks and when receives a tick thats bigger than the program duration he will terminate himself
	 * only after all the micro services finish their tasks (using a count down latch) 
	 * he will use a timer task to send the tick broadcasts every "speed" seconds
	 * @speed every how much time to run the time task
	 * the timer task 
	 * @method run send the tick broadcast to all the micro services interested and then increases the tick by 1
	 */
	protected void initialize(){
		subscribeBroadcast(TickBroadcast.class,(tick_msg)->{
			if (tick_msg.getTick()>duration){
				try {
					finish.await();
				} catch (Exception e) {
					e.printStackTrace();
				}
				terminate();
				Store.storeGetInstance().print();
			}
		});
		Logger.getLogger("logger").log(Level.INFO,"Timer started");
		try {
			latchObject.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Timer time = new Timer();
		time.scheduleAtFixedRate(new TimerTask(){  //NOTICE: scheduler receives 3 variables: timetask,time to begin, and interval time to perform the timetask
			
			//overriding the run for timetask to 
			public void run(){
				if (current_tick>duration){
					Logger.getLogger("logger").log(Level.INFO,"The program's running time has come to end");
					sendBroadcast(new TickBroadcast(current_tick, false));
					time.cancel();
				}
				else{
					Logger.getLogger("logger").log(Level.INFO,"\n	################################## Tick "+current_tick+" ################################## \n");
					sendBroadcast(new TickBroadcast(current_tick, current_tick<=duration ));
					setCurrTick(current_tick+1);
				}
			}
			
		},0,speed);  //END OF SCHEULER FUNCTION (0 = delay befor start, speed= interval time to perform the timetask
	}
}
