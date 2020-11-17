import javafx.application.Platform;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import javax.sound.sampled.*;


public class DataSender extends Thread {
	
	//flag for determining state of thread
	private volatile int callFlag;
	
	//flag values for thread state
	private final int DEFAULT_FLAG = 0;
	private final int CLOSE_FLAG = 1;
	
	//audio format for communicating
	private AudioFormat audioFormat;

	//dataline for recording audio
    private TargetDataLine targetDataLine;
	
	//socket for sending udp packets
	private DatagramSocket udpSendSocket;
	
	//address of recipient of data
	private InetSocketAddress recipientAddress;
	
	
	public DataSender(DatagramSocket sendSocket, String otherNum) {
		
		udpSendSocket = sendSocket;
		
		//will be sending to a port 16 higher than other instances's base port
		recipientAddress = IpTools.numberToInetSocketAddress(otherNum, 16);
		
		callFlag = DEFAULT_FLAG;
		
		//define the audio format
		//sample rate, sample size, channels, signed, big-endian
		audioFormat = new AudioFormat(16000, 16, 2, true, true);
		
		try {
			//create dataline for recording audio
			targetDataLine = (TargetDataLine) AudioSystem.getLine(
				new DataLine.Info(TargetDataLine.class, audioFormat));
		} catch (LineUnavailableException luex) {
			System.out.println("ERROR: AUDIO LINE NOT AVAILABLE");
		}
	}

	//set call flag to closed and close dataline
	public void closeThread() {
		callFlag = CLOSE_FLAG;
		
		try {
			targetDataLine.stop();
			targetDataLine.drain();
			targetDataLine.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}	
	}

	//run sending loop in original thread
	public void send() {
		try {
			//buffer to contain audio bytes
			byte[] audioBuffer = new byte[512];
		
			//open data line to record audio
			targetDataLine.open(audioFormat);
			targetDataLine.start();
			
			System.out.println("sending audio:");
			
			//handle call
			while(callFlag != CLOSE_FLAG) {

				//receive audio from dataLine (mic) in real time
				targetDataLine.read(audioBuffer, 0, audioBuffer.length);
				
				//create packet with audio data
				DatagramPacket toSend =
					new DatagramPacket(audioBuffer, audioBuffer.length, recipientAddress);

				//send data
				udpSendSocket.send(toSend);
			}
		
		} catch (Exception ex) {
			System.out.println("SEND EXCEPTION OCCURRED:\n" + ex.getMessage());
		}
		
		closeThread();
	}
	
	//run sending loop in separate thread
	public void run() {
		this.send();
	}
}