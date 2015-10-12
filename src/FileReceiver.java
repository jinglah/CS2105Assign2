import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.util.zip.*;

public class FileReceiver {

	public static void main(String[] args) throws Exception 
	{
		if (args.length != 1) {
			System.err.println("Usage: FileReceiver <port>");
			System.exit(-1);
		}
		int port = Integer.parseInt(args[0]);
		DatagramSocket sk = new DatagramSocket(port);
		byte[] data = new byte[1500];
		DatagramPacket pkt = new DatagramPacket(data, data.length);
		ByteBuffer b = ByteBuffer.wrap(data);
		CRC32 crc = new CRC32();
		File destFile = null;
		FileOutputStream fos;
		String dest = "";
		
		byte[] dst = new byte[60];
		byte[] pktData = new byte[60];
		
		while(true)
		{
			pkt.setLength(data.length);
			sk.receive(pkt);
			if (pkt.getLength() < 8)
			{
				System.out.println("Pkt too short");
				continue;
			}
			b.rewind();
			long chksum = b.getLong();
			crc.reset();
			crc.update(data, 8, pkt.getLength()-8);
			// Debug output
			System.out.println("Received CRC:" + crc.getValue() + " Data:" + bytesToHex(data, pkt.getLength()));
			if (crc.getValue() != chksum)
			{
				System.out.println("Pkt corrupt");
				//send nak
				
			}
			else
			{
				int pktNum = b.getInt();
				System.out.println("Pkt " + pktNum);
				if(pktNum == 0)
				{
					//get destination
					dst = new byte[60];
					b.get(dst);
					//trim the destination byte array
					int i = dst.length - 1;
				    while (i >= 0 && dst[i] == 0)
				    {
				        i--;
				    }
				    dst = Arrays.copyOf(dst, i + 1);
				    dest = new String(dst);
					System.out.println("Destination: " + dest + dest.length());
				}
				else{
					//pack file back
					pktData = new byte[60];
					b.get(pktData);
//					int j = pktData.length - 1;
//					//trim the byte array
//				    while (j >= 0 && pktData[j] == 0)
//				    {
//				        j--;
//				    }
//				    pktData = Arrays.copyOf(pktData, j + 1);
					try{
						if(dest!= null)
						{
							destFile = new File(dest);
							fos = new FileOutputStream(destFile, true);
							fos.write(pktData);
							fos.close();
						}
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}
			
				DatagramPacket ack = new DatagramPacket(new byte[0], 0, 0,
						pkt.getSocketAddress());
				sk.send(ack);
			}	
		}
	}
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes, int len) {
	    char[] hexChars = new char[len * 2];
	    for ( int j = 0; j < len; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}
