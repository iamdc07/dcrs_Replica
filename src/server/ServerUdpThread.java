package server;

import schema.UdpPacket;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class ServerUdpThread implements Runnable {
    private String servername;
    private int port;
    private ServerOperations serverOperations;
    private Thread thread;
    private Logger logs;
    private FileHandler fileHandler;

    public ServerUdpThread(String servername, int port, ServerOperations serverOperations) {
        this.servername = servername;
        this.port = port;
        this.serverOperations = serverOperations;
    }

    @Override
    public void run() {
        logs = Logger.getLogger(servername + " UDP Server");
        try {
            fileHandler = new FileHandler(servername + " UDP Server" + ".log", true);
            logs.addHandler(fileHandler);
        } catch (IOException ioe) {
            logs.warning("Failed to create handler for log file.\n Message: " + ioe.getMessage());
        }
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(port);
            byte[] buffer = new byte[1000];
            logs.info("The UDP server for " + servername + " is up and running on port " + (port));

            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);

                UdpPacket udpPacket = (UdpPacket) deserialize(request.getData());
//                logs.info("COURSE: " + udpPacket.courseId);

                UdpServerProc udpServerProc = new UdpServerProc(socket, request, serverOperations);
                udpServerProc.start();
            }
        } catch (Exception e) {
            logs.warning("Exception thrown while server was running/trying to start.\\nMessage: " + e.getMessage());
        }
    }

    public void start() {
        // One in coming connection. Forking a thread.
        if (thread == null) {
            thread = new Thread(this, "Udp Process");
            thread.start();
        }
    }

    public static byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
            try (ObjectOutputStream o = new ObjectOutputStream(b)) {
                o.writeObject(obj);
            }
            return b.toByteArray();
        }
    }

    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream b = new ByteArrayInputStream(bytes)) {
            try (ObjectInputStream o = new ObjectInputStream(b)) {
                return o.readObject();
            }
        }
    }

}
