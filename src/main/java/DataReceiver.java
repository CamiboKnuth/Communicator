import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import java.math.BigInteger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import javax.sound.sampled.*;


public class DataReceiver extends Thread {
	
	//indicators for video and audio packets
	public static final byte IMAGE_PACKET_INDICATOR = (byte) 0x81;
	public static final byte AUDIO_PACKET_INDICATOR = (byte) 0xe7;
	
	//number of bytes received per packet
	public static final int RECEIVE_BUFFER_SIZE = 500;

	
	//flag for determining state of thread
	private volatile int callFlag;
	
	//flag values for thread state
	private final int DEFAULT_FLAG = 0;
	private final int CLOSE_FLAG = 1;
	
	private byte[] imageBytesBuffer;
	private int imageIndex;
	
	//audio format for communicating
	private AudioFormat audioFormat;
	//dataline for playing audio
    private SourceDataLine sourceDataLine;

	//socket for receiving udp packets
	private DatagramSocket udpReceiveSocket;
	

	public DataReceiver(DatagramSocket recSocket) {
		
		udpReceiveSocket = recSocket;

		callFlag = DEFAULT_FLAG;
		
		imageBytesBuffer = new byte[0];
		
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
	
	public void loadBytes(byte[] toLoad, int startPos) {
		
		//starting at startPos, add bytes in toLoad to the image bytes buffer
		for(int i = startPos; i < toLoad.length; i++) {

			//do not continue if image bytes buffer is full
			if (imageIndex < imageBytesBuffer.length) {
				imageBytesBuffer[imageIndex] = toLoad[i];
				imageIndex ++;
			} else {
				break;
			}
		}			
	}
	
	public boolean isTimerPacket(byte[] toTest) {
		//packet is timer packet if it starts with timer packet indicator byte
		return (toTest[0] == Timer.TIMER_PACKET_INDICATOR);
	}
	
	public boolean isAudioPacket(byte[] toTest) {
		//packet is audio data if it starts with a audio packet indicator byte
		return (toTest[0] == AUDIO_PACKET_INDICATOR);
	}
	
	public boolean isImagePacket(byte[] toTest) {
		//packet is jpg image if it starts with a video packet indicator byte
		return (toTest[0] == IMAGE_PACKET_INDICATOR);
	}
	
	public boolean isBeginningImagePacket(byte[] toTest) {
		//packet is the start of a jpg image if it starts with six
		//video packet indicator bytes
		for(int i = 0; i < 6; i++) {
			if(toTest[i] != IMAGE_PACKET_INDICATOR) {
				return false;
			}
		}
		
		return true;
	}
	
	public int getImageBufferLength(byte[] firstBuffer) {
		//length is 30 bits long, max length is 1.073 GigaBytes
		byte[] lengthInBits = new byte[30];
		
		//get bytes at indexes: [6 to 35]
		for(int i = 0; i < lengthInBits.length; i++) {
			lengthInBits[i] = firstBuffer[i + 6];
		}

		//convert byte array to integer
		return (new BigInteger(lengthInBits).intValueExact());
	}

	//run reception loop in original thread
	public void receive() {
		try {	
			//buffer to contain bytes from other user
			byte[] receiveBuffer = new byte[RECEIVE_BUFFER_SIZE];
			
			//open dataline to play audio
			sourceDataLine.open(audioFormat);
			sourceDataLine.start();
			
			int timeoutStreak = 0;
			boolean receivingVideo = false;
			
			System.out.println("receiving data:");

			//handle call, if closed or too many timeouts, stop receiver
			while(!(callFlag == CLOSE_FLAG || timeoutStreak > 12)) {
				
				try {
					//packet for receiving audio and images over internet
					DatagramPacket receivedPacket =
						new DatagramPacket(receiveBuffer, receiveBuffer.length);
					
					//wait to get packet
					udpReceiveSocket.receive(receivedPacket);
					
					//begin timing receive in nanoseconds
					Timer.startReceiveTimer();
					
					byte[] receivedData = receivedPacket.getData();
					
					if (isImagePacket(receivedData)) {
						
						//start 200 milli timer after every image packet
						Timer.startTwoHundredMillisecondTimer();
						
						//beginning packets indicate start of new jpg image
						if (isBeginningImagePacket(receivedData)) {
							
							//start load timer
							Timer.startLoadTimer();
							//turn off receive timer if timing load time
							Timer.cancelReceiveTimer();
							
							//if just starting to get video packets...
							if(!receivingVideo) {								
								receivingVideo = true;
							}
							
							//store in new array to prevent changes during image showing
							byte[] toShow = imageBytesBuffer;
							
							//show image
							Platform.runLater(() -> {
								if (callFlag != CLOSE_FLAG) {
									Main.showReceivedImage(toShow);
								}
							});
							
							//reset image index
							imageIndex = 0;
							//set next image buffer length to size indicated by beginning packet
							imageBytesBuffer = new byte[getImageBufferLength(receivedData)];
							
							//load first part into imageBytes
							loadBytes(receivedData, 36);
							
							//measure time taken to load image
							Timer.stopLoadTimer();
						
						} else {
							//load image data into image bytes buffer
							loadBytes(receivedData, 1);
							
							//measure time taken to receive packet			
							Timer.stopReceiveTimer();
							
						}
					} else if (isAudioPacket(receivedData)) {
						//play audio through speakers, starting at offset 4
						sourceDataLine.write(receivedData, 4, receivedData.length - 4);
						
					} else if (isTimerPacket(receivedData)) {
						Timer.loadOtherDelayBytes(receivedData);
					}
					
					//if other user has not sent video for 200 millis
					if(receivingVideo) {
						if (Timer.twoHundredMillisecondsPassed()) {

							receivingVideo = false;
							
							//collapse video view
							Platform.runLater(() -> {
								if (callFlag != CLOSE_FLAG) {
									//Main.fitReceivedImageSize();//(350, 235);
									Main.showNoVideoImage();
								}
							});	
						}
					}
					
					//resize image based on size of window
					Platform.runLater(() -> {
						if (callFlag != CLOSE_FLAG) {
							Main.fitReceivedImageSize();
						}
					});				
					
					
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