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
	
	private VideoCapture videoCapture;
	private Mat matrix;
	
	//socket for sending udp packets
	private DatagramSocket udpSendSocket;
	//address of recipient of data
	private InetSocketAddress recipientAddress;
	
	private ScheduledExecutorService executor;
	
	private volatile boolean closeFlag;

	
	public VideoSender(DatagramSocket socket, InetSocketAddress recipient) {
		
		udpSendSocket = socket;
		recipientAddress = recipient;
		
		closeFlag = false;
		
		//load opencv libary
		System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
		
		//open camera
		videoCapture = new VideoCapture(0);
		matrix = new Mat();
	}
	
	public void execute() {
		//execute the run method every 35 milliseconds until stopped
		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(this, 0, 35, TimeUnit.MILLISECONDS);	
	}
	
	public void closeThread() {
		closeFlag = true;
		executor.shutdownNow();
		videoCapture.release();
	}
	
	//first packet will have	
	//36 byte header
	//6 byte 0x81's, 30 byte length indicator
	
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
				DatagramPacket currentPacket =
					new DatagramPacket(currentBuffer, currentBuffer.length, recipientAddress);
					
				Timer.waitForOtherReceive();
					
				udpSendSocket.send(currentPacket);
				
				//wait for other user to load image or receive data
				if(!sentFirst) {
					Timer.waitForOtherLoad();
					
					sentFirst = true;
				} else {
					Timer.waitForOtherReceive();
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
		
		//send any remaining bytes in a final packet
		DatagramPacket finalPacket =
			new DatagramPacket(currentBuffer, currentBufferIndex, recipientAddress);
					
		udpSendSocket.send(finalPacket);
	}

	public void run() {
		// If camera is opened
		if(videoCapture.isOpened() && !closeFlag) {

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
					if (!closeFlag) {
						Main.setMirrorImageSize(100, 100);
						Main.showMirrorImage(imageBytes);
					}
				});

			} else {
				//if video read failed, set mirror image size to 0
				Platform.runLater(() -> {
					if (!closeFlag) {
						Main.setMirrorImageSize(0, 0);
					}
				});				
			}
		}
	}				
}