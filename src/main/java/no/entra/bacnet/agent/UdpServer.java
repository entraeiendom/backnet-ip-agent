package no.entra.bacnet.agent;

import no.entra.bacnet.agent.rec.ProcessRecordedFile;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import static org.slf4j.LoggerFactory.getLogger;

public class UdpServer extends Thread {
    private static final Logger log = getLogger(UdpServer.class);
    private DatagramSocket socket;
    private boolean listening;
    private boolean recording;
    private byte[] buf = new byte[256]; //TODO 2048
    public static final int BACNET_DEFAULT_PORT = 47808;

    private long messageCount = 0;
    ProcessRecordedFile processRecordedFile = null;
    File recordingFile = null;

    public UdpServer() throws SocketException {
        socket = new DatagramSocket(BACNET_DEFAULT_PORT);
        String path = "bacnet-hexstring-recording";
        recordingFile = new File(path);
        processRecordedFile = new ProcessRecordedFile(recordingFile);
    }

    public void run() {
        listening = true;

        while (listening) {
            DatagramPacket packet
                    = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            packet = new DatagramPacket(buf, buf.length, address, port);
            String received = new String(packet.getData(), 0, packet.getLength());
            addMessageCount();
            convertAndForward(received);
            sendReply(packet, received);
        }
        socket.close();
    }

    void convertAndForward(String hexString) {
        log.trace("Received message: {}", hexString);
        if(recording) {
            processRecordedFile.writeToFile(hexString);
        }
    }

    void sendReply(DatagramPacket packet, String received) {
        boolean sendReply = expectingReply(received);
        if (sendReply) {
            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean expectingReply(String received) {
        boolean expectingReply = false;
        if (received != null && received.startsWith("hello")) {
            expectingReply = true;
        }
        return expectingReply;
    }

    void addMessageCount() {
        if (messageCount < Long.MAX_VALUE) {
            messageCount ++;
        } else {
            messageCount = 1;
        }
    }

    public long getMessageCount() {
        return messageCount;
    }

    public void setListening(boolean listening) {
        this.listening = listening;
    }

    public boolean isRecording() {
        return recording;
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
    }

    public File getRecordingFile() {
        return recordingFile;
    }

    public void setRecordingFile(File recordingFile) {
        this.recordingFile = recordingFile;
    }
}
