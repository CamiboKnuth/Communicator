import javafx.application.Platform;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import java.lang.System;

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
	
	VideoSender videoSender;
	
	
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
		
		videoSender = VideoSender.createInstance(sendSocket, recipientAddress);
	}

	//set call flag to closed and close dataline
	public void closeThread() {
		callFlag = CLOSE_FLAG;
		
		videoSender.closeThread();
		VideoSender.destroyInstance();
		
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
			byte[] audioBuffer = new byte[DataReceiver.RECEIVE_BUFFER_SIZE - 4];
		
			//open data line to record audio
			targetDataLine.open(audioFormat);
			targetDataLine.start();
			
			int loopCount = 0;
			
			System.out.println("sending audio:");
			
			//handle call
			while(callFlag != CLOSE_FLAG) {
				
				if (loopCount > 200) {
					
					//wait 20 milliseconds before sending timer packet
					Timer.waitTwentyMillis();
					
					byte[] timerBytes = Timer.generateTimerPacketBytes();
					
					//create packet with timer data
					DatagramPacket timerSend =
						new DatagramPacket(timerBytes, timerBytes.length, recipientAddress);

					//send timer data
					udpSendSocket.send(timerSend);
				
					loopCount = 0;
				}

				//receive audio from dataLine (mic) in real time
				targetDataLine.read(audioBuffer, 0, audioBuffer.length);
				
				//append 4 bytes to beginning of buffer to indicate audio packet
				//audio buffers must be multiples of 4
				byte[] sendBuffer = new byte[DataReceiver.RECEIVE_BUFFER_SIZE];		
				System.arraycopy(audioBuffer, 0, sendBuffer, 4, audioBuffer.length);
				sendBuffer[0] = DataReceiver.AUDIO_PACKET_INDICATOR;
				
				//create packet with audio data
				DatagramPacket toSend =
					new DatagramPacket(sendBuffer, sendBuffer.length, recipientAddress);
					
				Timer.waitForOtherReceive();

				//send data
				udpSendSocket.send(toSend);
				
				//wait before sending next packet
				Timer.waitForOtherReceive();
				
				loopCount ++;
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