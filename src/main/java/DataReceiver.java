import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.IOException;

import java.io.EOFException;
import java.net.SocketException;

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
	
	//stream for receiving audio
	private DataInputStream inputStream;
	

	public DataReceiver(DataInputStream stream) {
		inputStream = stream;
		callFlag = DEFAULT_FLAG;
		
		//define the audio format
		//sample rate, sample size, channels, signed, big-endian
		audioFormat = new AudioFormat(16000, 8, 2, true, true);
		
		try {
			//create dataline for playing audio
			sourceDataLine = (SourceDataLine) AudioSystem.getLine(
				new DataLine.Info(SourceDataLine.class, audioFormat));
		} catch (LineUnavailableException luex) {
			System.out.println("ERROR: AUDIO LINE NOT AVAILABLE");
		}
	}

	//set call flag to closed and close stream and dataline
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
			byte[] audioBuffer = new byte[1024];
			
			//open dataline to play audio
			sourceDataLine.open(audioFormat);
			sourceDataLine.start();
			
			//read first part of audio data into audioBuffer
			int bytesRead = inputStream.read(audioBuffer);

			//handle call, if closed or bytes read goes below zero, stop call
			while(!(callFlag == CLOSE_FLAG || bytesRead <= 0)) {
				//play audio through speakers
				sourceDataLine.write(audioBuffer, 0, audioBuffer.length);
				
				//read audio data into audioBuffer
				bytesRead = inputStream.read(audioBuffer);
				
				System.out.println("receiving audio:");
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