import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.lang.System;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.math.BigInteger;

import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

	
public class VideoSender extends Thread {
	
	//videosender uses singleton design pattern
	private static VideoSender videoSender = null;
	
	//create single instance of class
	public static VideoSender createInstance(DatagramSocket socket, InetSocketAddress recipient) { 
		if (videoSender == null) {
			videoSender = new VideoSender(socket, recipient); 
		}

		return videoSender; 
	}
	
	public static VideoSender getInstance() {
		return videoSender;
	}
	
	//destroy the single instance of the class
	public static void destroyInstance() {
		if (videoSender != null) {
			if (!videoSender.isClosed()) {
				videoSender.closeThread();
			}
		}
		
		videoSender = null;
	}
	
	
	
	
	
	private VideoCapture videoCapture;
	private Mat matrix;
	
	//socket for sending udp packets
	private DatagramSocket udpSendSocket;
	//address of recipient of data
	private InetSocketAddress recipientAddress;
	
	private ScheduledExecutorService executor;
	
	private volatile int camFlag;
	
	//flag values for camera
	private final int DEFAULT_FLAG = 0;
	private final int CLOSE_FLAG = 1;
	private final int RUNNING_FLAG = 2;
	
	private VideoSender(DatagramSocket socket, InetSocketAddress recipient) {
		
		udpSendSocket = socket;
		recipientAddress = recipient;
		
		camFlag = DEFAULT_FLAG;
		
		//load opencv libary
		System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
		
		executor = null;
		videoCapture = null;
	}
	
	public void startCamera() {
		//open camera
		videoCapture = new VideoCapture(0);
		matrix = new Mat();
		
		//execute the run method every 35 milliseconds until stopped
		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(this, 0, 35, TimeUnit.MILLISECONDS);
		
		camFlag = RUNNING_FLAG;
	}
	
	public void stopCamera() {
		if (executor != null) {
			executor.shutdownNow();
		}
		if (videoCapture != null) {
			videoCapture.release();	
		}
		
		Platform.runLater(() -> {
			Main.showNoCameraMirror();
		});	
		
		camFlag = DEFAULT_FLAG;
	}
	
	public void closeThread() {
		camFlag = CLOSE_FLAG;
		this.stopCamera();
	}
	
	public boolean isClosed() {
		return camFlag == CLOSE_FLAG;
	}
	
	
	//first packet will have a 36 byte header:
	//6 byte 0x81's, then 30 byte length indicator
	public void sendPacketList(byte[] toSend) throws IOException, UnknownHostException {
		
		byte[] currentBuffer = new byte[DataReceiver.RECEIVE_BUFFER_SIZE];
		
		//add indicators of first packet to current packet
		for(int i = 0; i < 6; i++) {
			currentBuffer[i] = DataReceiver.IMAGE_PACKET_INDICATOR;
		}
		
		//convert size of image to byte array
		byte[] sizeAsInt = BigInteger.valueOf(toSend.length).toByteArray();
		
		int sizeIndex = 1;
		
		//add size to beginning packet in reverse from positions [6 to 35]
		for(int i = 35; i >= 6; i--) {
			if (sizeIndex <= sizeAsInt.length) {
				currentBuffer[i] = sizeAsInt[sizeAsInt.length - sizeIndex];
				sizeIndex ++;
			} else {
				currentBuffer[i] = 0;
			}
		}
		
		int currentBufferIndex = 36;
		int toSendIndex = 0;
		
		boolean sentFirst = false;
		
		//add data to packet buffer, first packet starts at index 36, others from 0
		//loop until all image bytes are loaded
		while(toSendIndex < toSend.length) {
			
			//when current packet is full, send and create new packet
			if (!(currentBufferIndex < currentBuffer.length)) {
				
				try {
					//encrypt packet data before sending
					byte[] encryptedBuffer = Encryptor.aesEncrypt(currentBuffer);
					
					DatagramPacket currentPacket =
						new DatagramPacket(encryptedBuffer, encryptedBuffer.length, recipientAddress);
						
					Timer.waitForOtherReceive();
						
					udpSendSocket.send(currentPacket);
					
					//wait for other user to load image or receive data
					if(!sentFirst) {
						Timer.waitForOtherLoad();
						
						sentFirst = true;
					} else {
						Timer.waitForOtherReceive();
					}
				
				} catch (Exception ex) {
					System.out.println("PACKET SEND ERROR: " + ex.getMessage());
				}
				
				//create next packet to load data into
				currentBuffer = new byte[DataReceiver.RECEIVE_BUFFER_SIZE];
				currentBuffer[0] = DataReceiver.IMAGE_PACKET_INDICATOR;
				currentBufferIndex = 1;
			}
			
			//when packet is not yet full, add bytes to packet buffer
			currentBuffer[currentBufferIndex] = toSend[toSendIndex];
			
			toSendIndex ++;
			currentBufferIndex ++;
		}
		
		try {
			//encrypt packet data before sending
			byte[] encryptedBuffer = Encryptor.aesEncrypt(currentBuffer);
			
			//send any remaining bytes in a final packet
			DatagramPacket finalPacket =
				new DatagramPacket(encryptedBuffer, encryptedBuffer.length, recipientAddress);
						
			udpSendSocket.send(finalPacket);
		} catch (Exception ex) {
			System.out.println("PACKET SEND ERROR: " + ex.getMessage());
		}
	}

	public void run() {
		// If camera is opened
		if(videoCapture.isOpened() && camFlag != CLOSE_FLAG) {

			// Reading the next video frame from the camera
			if (videoCapture.read(matrix)) {
			
				//encode image color
				Imgproc.cvtColor(matrix, matrix, Imgproc.COLOR_BGR2RGB);
				
				//convert image to jpg format and put in buffer
				MatOfByte buffer = new MatOfByte();
				Imgcodecs.imencode(".jpg", matrix, buffer);
				byte[] imageBytes = buffer.toArray();

				try {
					//send image in separate packets to other user
					sendPacketList(imageBytes);
				
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				
				//show mirror of the captured image to this user
				Platform.runLater(() -> {
					if (camFlag != CLOSE_FLAG) {
						Main.showMirrorImage(imageBytes);
					}
				});

			//if video read failed
			} else {
				
				//if camera was on, stop camera, show camera unavailable
				if (camFlag == RUNNING_FLAG) {
					Main.cameraOn = false;
					stopCamera();
					
					Platform.runLater(() -> {
						if (camFlag != CLOSE_FLAG) {
							Main.showUnavailableCam();
							Main.toggleCameraButton.setText("Start Camera");
						}
					});		
				}
			}
		}
	}				
}