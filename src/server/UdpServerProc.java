package server;

import schema.UdpPacket;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UdpServerProc implements Runnable{

    private Thread thread;
    private DatagramSocket udpsocket;
    private DatagramPacket packet;
    private ServerOperations serverOperations;

    public UdpServerProc(DatagramSocket socket, DatagramPacket packet, ServerOperations serverOperations) {
        this.packet = packet;
        this.udpsocket = socket;
        this.serverOperations = serverOperations;
    }

    @Override
    public void run() {

        try {
            UdpPacket udpPacket = (UdpPacket) deserialize(this.packet.getData());
            byte[] responseMessage;

            String message;

            System.out.println("In operations class");
            if (udpPacket.operationNumber != 3) {
                System.out.println("Course prefix: " + udpPacket.courseId.substring(0, 4) + " | Studentid: "
                        + udpPacket.studentId + " | term: " + udpPacket.term + " | courseId: " + udpPacket.courseId);
            }
            switch (udpPacket.operationNumber) {
                case 1:
                    message = serverOperations.enroll(udpPacket.courseId, udpPacket.studentId, udpPacket.term,
                            udpPacket.dept, udpPacket.swapOp);
                    responseMessage = serialize(message);
                    break;
                case 3:
                    message = serverOperations.listCourseAvailabilityUdp(udpPacket.studentId, udpPacket.term, udpPacket.dept);
                    responseMessage = serialize(message);
                    break;
                case 4:
                    message = serverOperations.dropCourse(udpPacket.studentId, udpPacket.courseId, udpPacket.term,
                            udpPacket.dept);
                    System.out.println("Message in udp server class: " + message);
                    responseMessage = serialize(message);
                    break;
                case 5:
                    message = serverOperations.removeCourseUdp(udpPacket.courseId, udpPacket.term);
                    responseMessage = serialize(message);
                    break;
                default:
                    responseMessage = serialize("Server Communication Error");
                    System.out.println("Operation not found!");
                    break;
            }
            DatagramPacket response = new DatagramPacket(responseMessage, responseMessage.length,
                    this.packet.getAddress(), packet.getPort());

            udpsocket.send(response);

        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IO: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
