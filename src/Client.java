/*
Gautam bajaj
200901018
*/
import java.io.*;
import java.net.*;

class Client
{
	public static void main(String[] args) throws Exception
	{
		String url;
		String get;
		String host;
		String useragent;
		String f_url;
		String ifmodified;
		String nonematch;
		int brek;

		BufferedReader in_user=new BufferedReader(new InputStreamReader(System.in));

		DatagramSocket cli_soc=new DatagramSocket();
		InetAddress IPAddress=InetAddress.getByName("127.0.0.1");

		while(true)
		{
		System.out.print("Enter: ");
		String u_url=in_user.readLine();

		if(u_url.equalsIgnoreCase("Print Cache")|| u_url.indexOf('.')==-1)
		{
			f_url=u_url;
		}
		else
		{
			if(u_url.substring(0,7).equalsIgnoreCase("http://"))
			{
				url=u_url.substring(7, u_url.length());
			}
			else
			{
				url=u_url;
				u_url="http://"+url;
			}
			brek=url.indexOf('/');
			if(brek==-1)
			{
				get="GET "+u_url+" HTTP/1.1\n";
				host="Host: "+url+"\n";
			}
			else
			{
				get="GET "+u_url+" HTTP/1.1\n";
				host="Host: "+url.substring(0, brek)+"\n";
			}

			useragent="User-Agent: Gautam's Brower\n";
			f_url=get+host+useragent;
		}

		byte[] send_data=new byte[100024];
		byte[] receive_data=new byte[100024];

		//Sending HTTP request
		send_data=f_url.getBytes();
		DatagramPacket send_packet=new DatagramPacket(send_data,send_data.length,IPAddress,5000);
		cli_soc.send(send_packet);

		//Receiving Webpage
		DatagramPacket receive_packet=new DatagramPacket(receive_data,receive_data.length);
		cli_soc.receive(receive_packet);
		String webpage=new String(receive_packet.getData(),0,0,receive_packet.getLength());


		System.out.println(webpage);
	}
}
}
