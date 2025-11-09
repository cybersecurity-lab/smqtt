/*
 * Copyright (c) 2018 NetSec Lab - University of Parma (Italy)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT
 * SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 * Author(s):
 * Luca Veltri (luca.veltri@unipr.it)
 */

package test;

import org.zoolu.util.Bytes;
import org.zoolu.util.Flags;
import org.zoolu.util.Random;
import org.zoolu.util.SystemUtils;
import org.zoolu.util.log.DefaultLogger;
import org.zoolu.util.log.LoggerLevel;
import org.zoolu.util.log.WriterLogger;

import it.unipr.netsec.scoap.SecureCoapServer;
import io.ipstack.coap.provider.CoapProvider;
import io.ipstack.coap.server.CoapResource;
import io.ipstack.coap.server.CoapServer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;


/** Ready-to-use simple stateful SecureCoAP server.
 * It handles CoAP GET, PUT, and DELETE requests statefully, automatically handling request and response retransmissions.
 * <p>
 * It supports resource observation (RFC 7641) and blockwise transfer (RFC 7959). 
 */
public class SCoapServer {
	private SCoapServer() {}

	
	/** The main method. 
	 * @throws IOException */
	public static void main(String[] args) throws IOException {
		Flags flags=new Flags(args);
		boolean help=flags.getBoolean("-h","prints this help");
		int local_port=flags.getInteger("-p",CoapProvider.DEFAUL_PORT,"<port>","server UDP port (default port is "+CoapProvider.DEFAUL_PORT+")");
		int max_block_size=flags.getInteger("-m",0,"<max-size>","maximum block size");
		int verbose_level=flags.getInteger("-v",0,"<level>","verbose level");
		boolean write_mode=flags.getBoolean("-w","server in write-enabled mode");
		boolean exit=flags.getBoolean("-x","exits if 'return' is pressed");
		HashSet<CoapResource> resources=new HashSet<CoapResource>();
		String[] resource_tuple=flags.getStringTuple("-a",3,null,"<name> <format> <value>","add a resource; format can be: NULL|TEXT|XML|JSON; value can be ASCII or HEX (0x..)");
		while (resource_tuple!=null) {
			String resource_name=resource_tuple[0];
			int resource_format=CoapResource.getContentFormatIdentifier(resource_tuple[1]);
			String str=resource_tuple[2];
			byte[] resource_value=str.startsWith("0x")? Bytes.fromHex(str) : str.getBytes();
			CoapResource res=new CoapResource(resource_name,resource_format,resource_value);
			resources.add(res);
			System.out.println("Adding server resource: "+res);
			resource_tuple=flags.getStringTuple("-a",3,null,null,null);
		}
		
		if (help) {
			System.out.println(flags.toUsageString(SCoapServer.class.getSimpleName()));
			System.exit(0);
		}
		if (verbose_level==1) DefaultLogger.setLogger(new WriterLogger(System.out,LoggerLevel.INFO));
		else
		if (verbose_level==2) DefaultLogger.setLogger(new WriterLogger(System.out,LoggerLevel.DEBUG));
		else
		if (verbose_level>=3) DefaultLogger.setLogger(new WriterLogger(System.out,LoggerLevel.TRACE));

		// GKD client
		var clientId= "client-"+Random.nextInt();
		var key= Bytes.fromHex("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
		var broker= "127.0.0.1:1883";
		var username= "test";
		var passwd= "test";
		// CoAP server
		var server=new SecureCoapServer(local_port,clientId,key,broker,username,passwd);
		if (write_mode) server.setWriteMode(true);
		if (max_block_size>0) server.setMaximumBlockSize(max_block_size);
		System.out.println("CoAP server running on port: "+local_port);
		System.out.println("Write mode: "+(write_mode? "enabled":"disabled"));
		if (max_block_size>0) System.out.println("Maximum block size: "+max_block_size);
		
		for (Iterator<CoapResource> i=resources.iterator(); i.hasNext(); ) {
			CoapResource res=i.next();
			server.setResource(res.getName(),res.getFormat(),res.getValue());
		}
		
		if (exit) {
			//System.out.println("Press 'Return' to exit..");
			SystemUtils.readLine();
			server.halt();
			System.exit(0);
		}
	}

}
