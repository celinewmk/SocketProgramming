/**
 * PacketReceiver class representing the Server side
 */
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class PacketReceiver extends Thread {

    /**
     * Decode the data received from the client
     * @param receivedData data received from the client (header)
     * @return an integer representing the result of decoding the header
     * If 0 then correct, else data was corrupted
     */
    private static int decode(String receivedData) {
        List<String> headerFields = new ArrayList<>(); // initiliase array for header fields
        
        for (int i = 0; i < receivedData.length(); i = i + 4) {
            headerFields.add(receivedData.substring(i, i + 4));  // add the fields in the list
        }

        int sumInDecimal = 0;   // initialise the sum to verify the checksum

        for (String field : headerFields) {
            sumInDecimal += Integer.parseInt(field, 16); // add all the fields
        }

        String sumInHex = Integer.toHexString(sumInDecimal);

        // carry
        if (sumInHex.length() > 4) {
            String carry = sumInHex.substring(0, sumInHex.length() - 4); // save carry value
            String sumDiscardingCarry = sumInHex.substring(sumInHex.length() - 4);  // sum result without carry
            int carryInDecimal = Integer.parseInt(carry, 16);                 // convert values in décimal
            int sumDiscardingCarryInDecimal = Integer.parseInt(sumDiscardingCarry, 16);

            int sumWithCarry = Integer.sum(sumDiscardingCarryInDecimal, carryInDecimal); // add carry
            sumInHex = Integer.toHexString(sumWithCarry); 
        }

        // calculate complement
        int ffffInDecimal = Integer.parseInt("FFFF", 16);
        int sum = Integer.parseInt(sumInHex, 16);       // FFFF - sum
        int result = ffffInDecimal - sum;                     // return result
        return result;
    }
    
    /**
     * Convert the source IP address into text
     * @param ip client IP address (source)
     * @return a String representing the IPv4 in text (ex: 192.168.0.1)
     */
    private static String convertSrcIPtoText(String ip) {
        StringBuffer ipInText = new StringBuffer();  // initialise buffer to record the result
        int count = 0;
        for (int i = 0; i < ip.length(); i = i + 2) {
            String ipField = ip.substring(i, i + 2); // 2 characters in hex represent a part of the IP

            int ipFieldInDecimal = Integer.parseInt(ipField, 16); // convert into decimal
            if (count < 3) {                                            // constructs the result
                ipInText.append(ipFieldInDecimal + ".");
            } else {
                ipInText.append(ipFieldInDecimal);
            }
            count++;
        }
        return ipInText.toString();
    }
    public static void main(String[] args) throws Exception {
        System.out.println("Server Listening on 8888");
        ServerSocket serverSocket = new ServerSocket(8888);
        // server timeout 60 minutes
        serverSocket.setSoTimeout(1000 * 60 * 60);

        // Below method waits until client socket tries to connect
        Socket server = serverSocket.accept();

        // Read from client using input stream
        DataInputStream in = new DataInputStream(server.getInputStream());
        String receivedEncodedData = in.readUTF();

        // divide data received between the header and the payload
        // p.s: header is always 20 octets so 40 characters
        String header = receivedEncodedData.substring(0, 40);
        String payload = receivedEncodedData.substring(40);
        
        // verify checksum
        int checksumVerified = decode(header);

        if (checksumVerified == 0) {
            // case where the data was not corrupted
            StringBuffer buffer = new StringBuffer();  // initialise the buffer to record decoded payload
            for (int i = 0; i < payload.length(); i = i + 2) {
                String str = payload.substring(i, i + 2);
                buffer.append((char) Integer.parseInt(str, 16)); // decoding payload
            }
            String payloadDecoded = buffer.toString();

            // convert IP into decimal
            String ipSrc = convertSrcIPtoText(header.substring(header.length() - 16, header.length() - 8)); 
            // Message to display on the console
            String message = "Les donn\u00E9es reçues de " + ipSrc + " sont " + payloadDecoded + "\nLes donn\u00E9es ont "
                    + payload.length() * 4 +
                    " bits ou " + payload.length() / 2 + " octets. La longueur totale du paquet est de "
                    + receivedEncodedData.length() / 2
                    + " octets.\nLa v\u00E9rification de la somme de contr\u00F4le confirme que le paquet reçu est authentique.";
                    
            System.out.println(message);
        } else {
            // case where the data was corrupted
            System.out.println(
                    "La v\u00E9rification de la somme de contr\u00F4le montre que le paquet reçu est corrompu. Paquet jet\u00E9!");
        }

        serverSocket.close();
        // close the connection
        server.close();
    }
}
