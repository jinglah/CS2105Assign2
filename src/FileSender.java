import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.util.zip.*;

public class FileSender {

	public static void main(String[] args) throws Exception 
	{
		if (args.length != 4) {
			System.err.println("Usage: FileSender <host> <port> <src> <dest>");
			System.exit(-1);
		}

		InetSocketAddress addr = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
		//int num = Integer.parseInt(args[2]);
		DatagramSocket sk = new DatagramSocket();
		DatagramPacket pkt;
		byte[] data = new byte[20];
		ByteBuffer b = ByteBuffer.wrap(data);
		String src = args[2];
		CRC32 crc = new CRC32();

		File f = new File(src);
		byte[] buffer = new byte[60];
		int read = 0;
		BufferedInputStream is = new BufferedInputStream(new FileInputStream(f));
		int count = 1;
		
		while((read = is.read(buffer, 0, 60)) != -1)
		{
			b.clear();
			// reserve space for checksum
			b.putLong(0);
			b.putInt(count);
			crc.reset();
			crc.update(data, 8, data.length-8);
			long chksum = crc.getValue();
			b.rewind();
			b.putLong(chksum);
			byte[] packet = new byte[80];
			System.arraycopy(data, 0, packet, 0, data.length);
			System.arraycopy(buffer, 0, packet, data.length, buffer.length);
			pkt = new DatagramPacket(packet, packet.length, addr);
			// Debug output
			//System.out.println("Sent CRC:" + chksum + " Contents:" + bytesToHex(packet));
			sk.send(pkt);
			count += 1;
		}
		System.out.println("Sent " + count + " packets");
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}