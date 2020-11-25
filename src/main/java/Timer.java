import java.lang.System;
import java.math.BigInteger;


public class Timer {
	
	public static final byte TIMER_PACKET_INDICATOR = (byte) 0xc3;
	
	private static volatile boolean loadTimerRunning = false;
	private static volatile boolean receiveTimerRunning = false;
	private static volatile boolean hundredMilTimerRunning = false;
	
	private static volatile long loadTimerStart;
	private static volatile long receiveTimerStart;
	private static volatile long hundredMilTimerStart;
	
	private static volatile long otherLoadImageDelay = 15000000;
	private static volatile long otherReceiveDataDelay = 1000;
	
	private static volatile long myLoadImageDelay = 15000000;
	private static volatile long myReceiveDataDelay = 1000;
	

	public static void startLoadTimer() {
		loadTimerRunning = true;
		loadTimerStart = System.nanoTime();
		
	}
	
	public static void stopLoadTimer() {
		if (loadTimerRunning) {
			myLoadImageDelay = System.nanoTime() - loadTimerStart;
			loadTimerRunning = false;
		}
	}
	
	public static void cancelLoadTimer() {
		loadTimerRunning = false;
	}
	
	public static boolean loadTimerIsRunning() {
		return loadTimerRunning;
	}
	
	

	public static void startReceiveTimer() {
		receiveTimerRunning = true;
		receiveTimerStart = System.nanoTime();
		
	}
	
	public static void stopReceiveTimer() {
		if (receiveTimerRunning) {
			myReceiveDataDelay = System.nanoTime() - receiveTimerStart;
			receiveTimerRunning = false;
		}
	}
	
	public static void cancelReceiveTimer() {
		receiveTimerRunning = false;
	}
	
	public static boolean receiveTimerIsRunning() {
		return receiveTimerRunning;
	}
	
	

	public static void startTwoHundredMillisecondTimer() {
		hundredMilTimerRunning = true;
		hundredMilTimerStart = System.nanoTime();
	}
	
	public static boolean twoHundredMillisecondsPassed() {
		if(hundredMilTimerRunning) {
			if (System.nanoTime() - hundredMilTimerStart > 200000000) {
				hundredMilTimerRunning = false;
				
				return true;
			} 
		} 
		
		return false;
	}
	
	
	public static byte[] generateTimerPacketBytes() {
		byte[] timerBytes = new byte[DataReceiver.RECEIVE_BUFFER_SIZE];
		
		//add indicator of timer packet to byte array
		timerBytes[0] = TIMER_PACKET_INDICATOR;

		//convert myLoadImageDelay and myReceiveDataDelay to byte array
		byte[] loadTimeAsInt = BigInteger.valueOf(myLoadImageDelay).toByteArray();
		byte[] receiveTimeAsInt = BigInteger.valueOf(myReceiveDataDelay).toByteArray();
		
		int reverseIndex = 1;
		
		//add load time to packet in reverse from positions [1 to 30]
		for(int i = 30; i >= 1; i--) {
			if (reverseIndex <= loadTimeAsInt.length) {
				timerBytes[i] = loadTimeAsInt[loadTimeAsInt.length - reverseIndex];
				reverseIndex ++;
			} else {
				timerBytes[i] = 0;
			}
		}
		
		reverseIndex = 1;
		
		//add receive time to packet in reverse from positions [31 to 60]
		for(int i = 60; i >= 31; i--) {
			if (reverseIndex <= receiveTimeAsInt.length) {
				timerBytes[i] = receiveTimeAsInt[receiveTimeAsInt.length - reverseIndex];
				reverseIndex ++;
			} else {
				timerBytes[i] = 0;
			}
		}
		
		return timerBytes;
	}
	
	public static void loadOtherDelayBytes(byte[] toLoad) {

		byte[] loadDelayBytes = new byte[30];
		byte[] receiveDelayBytes = new byte[30];
		
		//get bytes at indexes: [1 to 30]
		for(int i = 0; i < loadDelayBytes.length; i++) {
			loadDelayBytes[i] = toLoad[i + 1];
		}
		
		//get bytes at indexes: [31 to 60]
		for(int i = 0; i < receiveDelayBytes.length; i++) {
			receiveDelayBytes[i] = toLoad[i + 31];
		}

		//convert byte array to integer
		otherLoadImageDelay = (new BigInteger(loadDelayBytes).intValueExact());
		otherReceiveDataDelay = (new BigInteger(receiveDelayBytes).intValueExact());
		
		System.out.println("Received Timer Packet, other delays are: load: "
			+ otherLoadImageDelay + ", receive: " + otherReceiveDataDelay);
	}
	
	public static synchronized void waitForOtherLoad() {
		long start = System.nanoTime();
		while(System.nanoTime() - start < otherLoadImageDelay * 2);		
	}
	
	public static synchronized void waitForOtherReceive() {
		long start = System.nanoTime();
		while(System.nanoTime() - start < otherReceiveDataDelay * 2);			
	}
	
	public static synchronized void waitTwentyMillis() {
		long start = System.nanoTime();
		long twentyMil = 20000000;
		while(System.nanoTime() - start < twentyMil);		
	}
	
}