import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.util.zip.*;

public class FileSender {
	private final static int DATA_SIZE = 60;
	private final static int HEADER_SIZE = 20;
	final static Timer timer = new Timer();

	public static void main(String[] args) throws Exception 
	{
		long start = System.currentTimeMillis();
		if (args.length != 4) {
			System.err.println("Usage: FileSender <host> <port> <src> <dest>");
			System.exit(-1);
		}

		InetSocketAddress addr = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
		//int num = Integer.parseInt(args[2]);
		DatagramSocket sk = new DatagramSocket();
		sk.setSoTimeout(10);
		DatagramPacket pkt;
		byte[] rcv = new byte[60];
		DatagramPacket rcvPkt; 
		
		String dest = args[3];
	
		byte[] data = new byte[80];
		ByteBuffer b = ByteBuffer.wrap(data);
		String src = args[2];
		CRC32 crc = new CRC32();
		
		File f = new File(src);
		byte[] buffer = new byte[60];
		int read = 0;
		BufferedInputStream is = new BufferedInputStream(new FileInputStream(f));
		int count = 1;
		
		//send destination
		b.clear();
		b.putLong(0);
		b.putInt(0);
		b.put(dest.getBytes());
		crc.reset();
		crc.update(data, 8, data.length-8);
		long chksum = crc.getValue();
		b.rewind();
		b.putLong(chksum);
		pkt = new DatagramPacket(data, data.length, addr);
		rcvPkt = new DatagramPacket(rcv, rcv.length);
		ByteBuffer r = ByteBuffer.wrap(rcv);
		sk.send(pkt);
		int ackNum;
		while(true)
		{
			r.clear();
			rcvPkt.setLength(rcv.length);
			try{
				sk.receive(rcvPkt);
				System.out.println("Received!");
			}
			catch (SocketTimeoutException e) {
                //resending
				System.out.println("Resending 0");
				sk.send(pkt);
                continue;
            }
			
			if(rcvPkt.getLength() >= 4)
			{
				ackNum = r.getInt();
				System.out.println("received" + ackNum);
				if(ackNum == 0)
				{
					System.out.println(ackNum);
					break;
				}
			}
		}
		
		while((read = is.read(buffer, 0, 60)) != -1)
		{
			b.clear();
			// reserve space for checksum
			b.putLong(0);
			b.putInt(count);
			b.put(buffer);
			crc.reset();
			crc.update(data, 8, (read + HEADER_SIZE)-8);
			chksum = crc.getValue();
			b.rewind();
			b.putLong(chksum);
			System.out.println("buffer length: "+ (read + 20) + "data: " + data.length);
			pkt = new DatagramPacket(data, (read + HEADER_SIZE), addr);
			sk.send(pkt);
			
			while(true)
			{
				r.clear();
				rcvPkt.setLength(rcv.length);
				try{
					sk.receive(rcvPkt);
				}
				catch (SocketTimeoutException e) {
	                //resending
					System.out.println("Resending" + count);
					sk.send(pkt);
	                continue;
	            }
				
				if(rcvPkt.getLength() >= 4)
				{
					ackNum = r.getInt();
					if(ackNum == count)
					{
						System.out.println(ackNum);
						break;
					}
				}
			}
			r.clear();
			
			count += 1;
		}
		System.out.println("Sent " + (count-1) + " packets");
		sk.close();
		is.close();
		System.exit(0);
	
		//long end = System.currentTimeMillis();
		//System.out.println((end - start)/1000);
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
