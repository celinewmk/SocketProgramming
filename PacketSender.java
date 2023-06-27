/**
 * PacketSender class representing the Client side
 */
import java.net.*;
import java.io.*;

public class PacketSender {

    // fields of the header
    private static String totalLength = "0000";
    private static String checksum = "0000";
    private static String ipSrcInHex;
    private static String ipDstInHex;

    private static final String lengthAndTOS = "4500";
    private static final String identification = "1C46";
    private static final String flagAndOffset = "4000";
    private static final String TTLAndProtocol = "4006";
    private static final int NUM_OF_DIGITS_IN_HEADER = 40;

    /**
     * Convert the text to hexadecimal
     * @param text the payload
     * @return a String representing the payload converted into hex
     */
    private static String textToHEX(String text) {
        byte[] textByte = text.getBytes();            // convert text to bytes
        StringBuffer textinHex = new StringBuffer();  // initialise the buffer to record the result
        for (int i = 0; i < textByte.length; i++) {
            String hexString = String.format("%02X", textByte[i]);  // convert bytes in hexadecimal with 2 digits
            textinHex.append(hexString);              // add to buffer
        }
        return textinHex.toString();
    }

    /**
     * Convert IPv4 address in hexadecimal
     * @param ip IP address to convert
     * @return a String representing IP address converted into hexadecimal
     */
    private static String convertIPtoHex(String ip) {
        String[] ipStr = ip.split("\\.");      // split IP into 4 parts (split at the dot)
        StringBuffer ipInHex = new StringBuffer();   // initialise the buffer to record the result
        for (int i = 0; i < ipStr.length; i++) {
            String hexString = String.format("%02X", Integer.parseInt(ipStr[i]));  // convert number to hexadecimal with 2 digits
            ipInHex.append(hexString);               //add to buffer
        }
        return ipInHex.toString();
    }

    /**
     * Calculate checksum
     */
    private static void calculateChecksum() {
        // convert all the header fields into decimal to calculate checksum
        int lengthAndTOSInDecimal = Integer.parseInt(lengthAndTOS, 16);
        int totalLengthInDecimal = Integer.parseInt(totalLength, 16);
        int identificationInDecimal = Integer.parseInt(identification, 16);
        int indicateursEtDecalageInDecimal = Integer.parseInt(flagAndOffset, 16);
        int TTLAndProtocolInDecimal = Integer.parseInt(TTLAndProtocol, 16);
        int checksumInDecimal = Integer.parseInt(checksum, 16);
        
        String ipSrcFirst = ipSrcInHex.substring(0, 4);
        String ipSrcSecond = ipSrcInHex.substring(4);
        String ipDstFirst = ipDstInHex.substring(0, 4);
        String ipDstSecond = ipDstInHex.substring(4);

        int ipSrcFirstInDecimal = Integer.parseInt(ipSrcFirst, 16);
        int ipSrcSecondInDecimal = Integer.parseInt(ipSrcSecond, 16);
        int ipDstFirstInDecimal = Integer.parseInt(ipDstFirst, 16);
        int ipDstSecondInDecimal = Integer.parseInt(ipDstSecond, 16);
        
        // calculate sum
        int sumOfAllTheField = lengthAndTOSInDecimal + totalLengthInDecimal + identificationInDecimal + indicateursEtDecalageInDecimal +
                TTLAndProtocolInDecimal + checksumInDecimal + ipSrcFirstInDecimal + ipSrcSecondInDecimal + ipDstFirstInDecimal + ipDstSecondInDecimal;

        // convert to hexadecimal
        String fullSumInHex = Integer.toHexString(sumOfAllTheField);

        // carry
        if (fullSumInHex.length() > 4) {
            String carry = fullSumInHex.substring(0, fullSumInHex.length() - 4); // save carry value
            String sumDiscardingCarry = fullSumInHex.substring(fullSumInHex.length() - 4);  // sum result without carry
            int carryInDecimal = Integer.parseInt(carry, 16);                         // convert values in décimal
            int sumDiscardingCarryInDecimal = Integer.parseInt(sumDiscardingCarry, 16); 
            
            int sumWithCarry = Integer.sum(sumDiscardingCarryInDecimal, carryInDecimal); // add carry
            fullSumInHex = Integer.toHexString(sumWithCarry);
        }

        // calculate complement
        int ffffInDecimal = Integer.parseInt("FFFF", 16);
        int sum = Integer.parseInt(fullSumInHex, 16);          // FFFF - sum
        int checksumComplement = ffffInDecimal - sum;

        checksum = String.format("%04X", checksumComplement); // update variable for checksum
    }
    
    /**
     * Encapsulate the data into IP datagram 
     * @param ipSrc client IP address
     * @param ipDst server IP address
     * @param payload message to send to the server
     * @return a String representing IP datagram to send to the server
     */
    private static String encapsulate(String ipSrc, String ipDst, String payload) {
        
        ipSrcInHex = convertIPtoHex(ipSrc);  // convert client IP address in hexadecimal
        ipDstInHex = convertIPtoHex(ipDst);  // convert server IP address in hexadecimal

        // add padding when header lenght + payload length is not divisble by 8
        while (((NUM_OF_DIGITS_IN_HEADER + payload.length()) % 8) != 0) { 
            payload += "0";
        }

        // calculate total lenght in hexadecimal
        totalLength = String.format("%04X", (NUM_OF_DIGITS_IN_HEADER + payload.length()) / 2);

        // calculate checksum in hexadecimal
        calculateChecksum();

        // encapsule the data in IP datagram
        String datagram = lengthAndTOS + totalLength + identification + flagAndOffset + TTLAndProtocol +
                checksum + ipSrcInHex + ipDstInHex + payload;

        return datagram;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Erreur. Vous devez donner l'adresse IP et le payload sur la ligne de commande. Comme par exemple: java PacketSender 192.168.0.1 \"COLOMBIA 2 - MESSI 0\"");
            return;
        }
        String ipDst = args[0];       // should be in ipv4 format, ex: 192.168.0.1
        String payload = args[1];     // should be in between quotes
        
        // Connection to server - localhost @ port 8888
        Socket client = new Socket("localhost", 8888);
        // if the server is not listening, we get an Exception
        // java.net.ConnectException: Connection refused: connect

        // get the ip of machine's local host
        InetAddress ipSource = InetAddress.getLocalHost();
        String ipSrc = ipSource.getHostAddress();

        // encapsulate the data
        String datagramIP = encapsulate(ipSrc, ipDst, textToHEX(payload));
        // String datagramIP = encapsulate("192.168.0.3", ipDst, textToHEX(payload)); // you can change ipSrc to a hardcoded IP to test

        String dataToSend = datagramIP;
        // String dataToSend = "050000281C46400040069D35C0A80003C0A80001434F4C4F4D4249412032202D204D455353492030"; // uncomment this to test corrupted data
        
        // write to server using output stream
        DataOutputStream out = new DataOutputStream(client.getOutputStream());
        out.writeUTF(dataToSend); 
        System.out.println("Datagramme envoy\u00E9 \u00E0 " + args[0] +": \n" + dataToSend);

        // close the connection
        client.close();
    }
}
