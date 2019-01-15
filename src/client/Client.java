package client;

import schema.UdpPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import static server.UdpServerProc.serialize;

public class Client {

    public static void main(String[] args) {
        UdpPacket udpPacket = new UdpPacket(1, "ABCD", "DEFG", "ABC", "adfsf", false);
        byte[] responseMessage = new byte[1000];


        try {
            DatagramSocket udpsocket = new DatagramSocket(8009);
            InetAddress IPAddress = InetAddress.getByName("localhost");
            responseMessage = serialize(udpPacket);
            DatagramPacket request = new DatagramPacket(responseMessage, responseMessage.length, IPAddress, 1112);

            udpsocket.send(request);
            System.out.println("REQUEST SENT: " + request);

        } catch (SocketException s) {
            s.printStackTrace();
        } catch (IOException s) {
            s.printStackTrace();
        }
    }

}
