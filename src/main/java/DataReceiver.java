import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import javax.sound.sampled.*;


public class DataReceiver extends Thread {
	
	//flag for determining state of thread
	private volatile int callFlag;
	
	//flag values for thread state
	private final int DEFAULT_FLAG = 0;
	private final int CLOSE_FLAG = 1;
	
	//audio format for communicating
	private AudioFormat audioFormat;

	//dataline for playing audio
    private SourceDataLine sourceDataLine;
	
	//socket for receiving udp packets
	private DatagramSocket udpReceiveSocket;
	

	public DataReceiver(DatagramSocket recSocket) {
		
		udpReceiveSocket = recSocket;

		callFlag = DEFAULT_FLAG;
		
		//define the audio format
		//sample rate, sample size, channels, signed, big-endian
		audioFormat = new AudioFormat(16000, 16, 2, true, true);
		
		try {
			//create dataline for playing audio
			sourceDataLine = (SourceDataLine) AudioSystem.getLine(
				new DataLine.Info(SourceDataLine.class, audioFormat));
		} catch (LineUnavailableException luex) {
			System.out.println("ERROR: AUDIO LINE NOT AVAILABLE");
		}
	}

	//set call flag to closed and close dataline
	public void closeThread() {
		callFlag = CLOSE_FLAG;
		
		try {
			sourceDataLine.stop();
			sourceDataLine.drain();
			sourceDataLine.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}	
	}

	//run reception loop in original thread
	public void receive() {
		try {	
			//buffer to contain audio bytes
			byte[] audioBuffer = new byte[512];
			
			//open dataline to play audio
			sourceDataLine.open(audioFormat);
			sourceDataLine.start();
			
			int timeoutStreak = 0;
			
			System.out.println("receiving audio:");

			//handle call, if closed or too many timeouts, stop receiver
			while(!(callFlag == CLOSE_FLAG || timeoutStreak > 10)) {
				try {
					//packet for receiving audio over internet
					DatagramPacket toReceive =
						new DatagramPacket(audioBuffer, audioBuffer.length);
					
					//wait to get audio packet
					udpReceiveSocket.receive(toReceive); 
					
					//play audio through speakers
					sourceDataLine.write(audioBuffer, 0, audioBuffer.length);

					timeoutStreak = 0;
				} catch (SocketTimeoutException stex) {
					timeoutStreak ++;
				}
			}
		} catch (Exception ex) {
			System.out.println("RECEIVE EXCEPTION OCCURRED:\n" + ex.getMessage());
		}
		
		closeThread();
		
		//end the call
		Platform.runLater(()->{
			Main.endCallAction();
		});
	}
	
	//run reception loop in separate thread
	public void run() {
		this.receive();
	}
}