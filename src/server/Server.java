package server;

import data.Data;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class Server {
    private static Logger logs;
    private static FileHandler fileHandler;

    public static void main(String[] args) {

        int compPort = 1111, soenPort = 2222, insePort = 3333;

        int port = 0;
        String servername = "";

        // Check for the department and port number
        switch (args[0]) {
            case "1":
                servername = "COMP";
                port = compPort;
                break;
            case "2":
                servername = "SOEN";
                port = soenPort;
                break;
            case "3":
                servername = "INSE";
                port = insePort;
                break;
        }


        // set up the logging mechanism
        logs = Logger.getLogger(servername + " Server");
        try {
            fileHandler = new FileHandler(servername + ".log", true);
            logs.addHandler(fileHandler);
        } catch (IOException ioe) {
            logs.warning("Failed to create handler for log file.\n Message: " + ioe.getMessage());
        }

        ServerOperations serveroperations = new ServerOperations(servername, logs);

        ServerUdpThread serverUdpThread = new ServerUdpThread(servername, port + 1, serveroperations);
        serverUdpThread.start();

        try {
            byte[] buffer = new byte[1000];
            logs.info("The server for " + servername + " is up and running on port " + (port));

            DatagramSocket socket = null;
            MulticastSocket aSocket = null;
            aSocket = new MulticastSocket(port);
            aSocket.joinGroup(InetAddress.getByName("230.1.1.5"));

            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(request);

                Data data = (Data) deserialize(request.getData());
//                logs.info("COURSE: " + udpPacket.courseId);

                data.ack = true;
                byte[] ackReply = serialize(data);
                DatagramPacket ackPacket = new DatagramPacket(ackReply, ackReply.length,
                        request.getAddress(), request.getPort());

                aSocket.send(ackPacket);


                String message;
                byte[] responseMessage;
                logs.info("Operation ID: " + data.operationID);

                switch (data.operationID) {
                    case 1:
                        message = serveroperations.enroll(data.courseID, data.studentID, data.semester, data.department, false);
                        responseMessage = serialize(message);
                        break;
                    case 2:
                        message = serveroperations.dropCourse(data.studentID, data.courseID, data.semester, data.department);
                        responseMessage = serialize(message);
                        break;
                    case 3:
                        message = serveroperations.getClassSchedule(data.studentID);
                        responseMessage = serialize(message);
                        break;
                    case 4:
                        message = serveroperations.addCourse(data.userID, data.courseID, data.semester, data.department, data.capacity);
                        responseMessage = serialize(message);
                        break;
                    case 5:
                        message = serveroperations.removeCourse(data.courseID, data.courseID, data.semester, data.department);
                        responseMessage = serialize(message);
                        break;
                    case 6:
                        message = serveroperations.listCourseAvailability(data.userID, data.semester, data.department);
                        responseMessage = serialize(message);
                        break;
                    case 7:
                        message = serveroperations.swapCourse(data.studentID, data.oldCourseID, data.newCourseID, data.department, data.semester);
                        responseMessage = serialize(message);
                        break;
                    case 8:
                        ServerOperations.flag = 1;
                        message = "Fixed";
                        responseMessage = serialize(message);
                        break;
                    case 10:
                        Data records = serveroperations.sendData();
                        responseMessage = serialize(records);
                        break;
                    default:
                        message = "Wrong Operation ID";
                        responseMessage = serialize(message);
                        break;
                }
                DatagramPacket response = new DatagramPacket(responseMessage, responseMessage.length,
                        request.getAddress(), request.getPort());

                aSocket.send(response);
            }
        } catch (Exception e) {
            logs.warning("Exception thrown while server was running/trying to start.\\nMessage: " + e.getMessage());
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

// COMP : 1111
// COMPUDP : 1112
// SOEN : 2222
// SOENUDP : 2223
// INSE : 3333
// INSEUDP : 3334
