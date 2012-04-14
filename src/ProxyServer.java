/*
Gautam Bajaj
200901018
*/
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ProxyServer
{
	public static void main(String[] args) throws Exception
	{
		//socket creation and getting request from client
		DatagramSocket ser_soc =new DatagramSocket(5000);

		while(true)
		{
		//variable declarations
		Socket proxysocket=null;
		String url=null;
		String request=null;
		String f_url=null;
		StringBuilder webpage=new StringBuilder();
		StringBuilder CheckHeader=new StringBuilder();
		String[] tokens=new String[3];
		String host=null;
		String key=null;
		String[] metadata=new String[3];
		String ETag=null;
		String[] fetchmeta=null;
		String CheckRequest=null;
		String LastModified=null;
		int endETag=0;
		int lastmodindex=0;
		int byteRead=0;
		int i=0;



		byte[] receive_data=new byte[100024];
		byte[] send_data=new byte[100024];
		DatagramPacket receive_packet=new DatagramPacket(receive_data,receive_data.length);
		ser_soc.receive(receive_packet);

		//processing request
		 request= new String(receive_packet.getData(),0,receive_packet.getLength());
		 InetAddress cli_IP=receive_packet.getAddress();
		 int cli_port=receive_packet.getPort();
		 System.out.println("GET RECEIVED: "+request);

		//Reading Cache from Hard disk
			Hashtable Cache=new Hashtable();
			File file_cache=new File("Cache");
			if(file_cache.exists())
			{
		       FileInputStream fileIn = new FileInputStream("Cache");
		       ObjectInputStream in = new ObjectInputStream(fileIn);
		       Cache = (Hashtable)in.readObject();
			}



		 if(request.equalsIgnoreCase("Print Cache"))
		 {
			 Enumeration e = Cache.elements();
			 Enumeration v = Cache.keys();
			 while(v.hasMoreElements())
			 {
			     String[] val=(String[])e.nextElement();
			     String kay=(String)v.nextElement();
			     webpage.delete(0, webpage.length());
					webpage.append(kay+'\t'+val[1]+'\t'+val[2]+"\n\n");
			 }
				send_data=webpage.toString().getBytes();
				DatagramPacket send_packet=new DatagramPacket(send_data,send_data.length,cli_IP,cli_port);
				ser_soc.send(send_packet);
		 }
		 else if(request.indexOf(".")==-1)
		 {
			 Enumeration v = Cache.keys();
			 while(v.hasMoreElements())
			 {
			     String kay=(String)v.nextElement();
			     if(kay.contains(request))
			     {
			    	 String[] val=(String[])Cache.get(kay);
			    	   webpage.delete(0, webpage.length());
						webpage.append(kay+'\t'+val[1]+'\t'+val[2]+"\n\n");
			     }
			 }
				send_data=webpage.toString().getBytes();
				DatagramPacket send_packet=new DatagramPacket(send_data,send_data.length,cli_IP,cli_port);
				ser_soc.send(send_packet);

		 }
		 else
		 {
		 //filtering host
		 tokens=request.split("\n");
		 host=tokens[1].substring(6, tokens[1].length());



		 //Finding our key or URL
		 String[] tokenedgets=tokens[0].split(" ");
		 /*if(tokenedgets[1]!="/")
			 url=host+tokenedgets[1];
		 else
			 url=host;*/
		 url=tokenedgets[1];


		 //Setting up connection to host
		 try{
		 InetAddress IP=InetAddress.getByName(host);
		 System.out.println(IP.toString());

		 if((host.indexOf("iiit.ac.in")==-1))
			 proxysocket=new Socket("hostelproxy.iiit.ac.in",8080);
		 else
			 proxysocket=new Socket(IP,80);
		 }
		 catch(Exception e)
		 {
			 webpage.append("INVALID URL");
			 send_data=webpage.toString().getBytes();
				DatagramPacket send_packet=new DatagramPacket(send_data,send_data.length,cli_IP,cli_port);
				ser_soc.send(send_packet);
			continue;
		 }
		 PrintWriter outToServer=new PrintWriter(proxysocket.getOutputStream(),true);
		 BufferedReader inFromServer=new BufferedReader(new InputStreamReader(proxysocket.getInputStream()));


		//Checking Cache
		if(Cache.containsKey(url))
		{
			//fetching from cache
			fetchmeta=(String[])Cache.get(url);
			CheckRequest=request+"If-Modified-Since: "+fetchmeta[1]+"\n";

			System.out.println("Stored Last Modified: "+fetchmeta[1]+'\n');


			//Checking if modified
			outToServer.println(CheckRequest);
			webpage.delete(0, webpage.length());
			while((byteRead=inFromServer.read())!=-1)
				webpage.append((char)byteRead);

			String[] checktokens=webpage.toString().split("\n");
			String[] tok=checktokens[0].split(" ");


			if(tok[1].equals("304"))
			{
				//Sending from Cache if not modified
				System.out.println("\n\nSending from CACHE as its not modified\n\n");
				webpage=new StringBuilder(fetchmeta[2]);
				send_data=webpage.toString().getBytes();
				DatagramPacket send_packet=new DatagramPacket(send_data,send_data.length,cli_IP,cli_port);
				ser_soc.send(send_packet);
			}
			else
			{
				//Deleting Previous Cache Value
				Cache.remove(url);

				//Sending the new page if not found latest in Cache
				System.out.println("\n\nSending Fresh as its modified\n\n");
				send_data=webpage.toString().getBytes();
				DatagramPacket send_packet=new DatagramPacket(send_data,send_data.length,cli_IP,cli_port);
				ser_soc.send(send_packet);


				//finding ETag
				i=webpage.toString().indexOf("ETag: ");
				do
				{
					endETag=i;
					if(i==-1)
						break;
					else
						i++;

				}while(webpage.toString().charAt(i-1)!='\n');

				if(i!=-1)
					ETag=webpage.toString().substring(webpage.toString().indexOf("ETag: ")+7, endETag);
				else
					ETag=null;



				System.out.println("URL :"+url);


				//Finding last modified
				i=webpage.toString().indexOf("Last-Modified: ");
				do
				{
						lastmodindex=i;
						if(i==-1)
							break;
						else
							i++;
				}while(webpage.toString().charAt(i-1)!='\n');
				if(i!=-1)
					LastModified=webpage.toString().substring(webpage.toString().indexOf("Last-Modified: ")+15, lastmodindex);
				else
					LastModified=null;

				System.out.println("LM :"+LastModified);

				key=url;

		    	if(LastModified!=null)
		    	{
				//Saving Cache to Hard disk
		    	metadata[0]=ETag;
				metadata[1]=LastModified;
				metadata[2]=webpage.toString();
				Cache.put(key,metadata);
				FileOutputStream fileOut = new FileOutputStream("Cache",true);
		        ObjectOutputStream out = new ObjectOutputStream(fileOut);
		        out.writeObject(Cache);
		        out.close();
		        fileOut.close();
		    	}
			}
		}
		else
		{
			//Fetching new webpage if not found in cache
			 outToServer.println(request);
			 webpage.delete(0, webpage.length());
			 while((byteRead=inFromServer.read())!=-1)
				webpage.append((char)byteRead);

			 //Sending to Client
			 System.out.println("\n\nSending Fresh as never Requested before\n\n");
			 send_data=webpage.toString().getBytes();
				DatagramPacket send_packet=new DatagramPacket(send_data,send_data.length,cli_IP,cli_port);
				ser_soc.send(send_packet);

				//finding ETag
				i=webpage.toString().indexOf("ETag: ");
				do
				{
					endETag=i;
					if(i==-1)
						break;
					else
						i++;

				}while(webpage.toString().charAt(i-1)!='\n');

				if(i!=-1)
					ETag=webpage.toString().substring(webpage.toString().indexOf("ETag: ")+7, endETag);
				else
					ETag=null;



				System.out.println("URL :"+url);



				//Finding last modified
				i=webpage.toString().indexOf("Last-Modified: ");
				do
				{
						lastmodindex=i;
						if(i==-1)
							break;
						else
							i++;
				}while(webpage.toString().charAt(i-1)!='\n');
				if(i!=-1)
					LastModified=webpage.toString().substring(webpage.toString().indexOf("Last-Modified: ")+15, lastmodindex);
				else
					LastModified=null;

				System.out.println("LM :"+LastModified);

				key=url;

				System.out.println("It worked till here");
		    	if(LastModified!=null)
		    	{
				//Saving Cache to Hard disk
		    	System.out.println("Saving this page to cache");
		    	metadata[0]=ETag;
				metadata[1]=LastModified;
				metadata[2]=webpage.toString();
				Cache.put(key,metadata);
				FileOutputStream fileOut = new FileOutputStream("Cache",true);
		        ObjectOutputStream out = new ObjectOutputStream(fileOut);
		        out.writeObject(Cache);
		        out.close();
		        fileOut.close();
		    	}
		    	else
		    		System.out.println("Caching is not allowed for this page");
		}

	}
}
}
}
