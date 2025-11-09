package test;

import org.zoolu.util.Bytes;
import org.zoolu.util.Flags;
import org.zoolu.util.Random;
import org.zoolu.util.SystemUtils;
import org.zoolu.util.log.DefaultLogger;
import org.zoolu.util.log.LoggerLevel;
import org.zoolu.util.log.WriterLogger;

import it.unipr.netsec.scoap.SecureCoapClient;
import io.ipstack.coap.client.CoapResponseHandler;
import io.ipstack.coap.message.CoapRequest;
import io.ipstack.coap.message.CoapRequestMethod;
import io.ipstack.coap.message.CoapResponse;
import io.ipstack.coap.provider.CoapProvider;
import io.ipstack.coap.provider.CoapURI;
import io.ipstack.coap.server.CoapResource;

import java.io.IOException;
import java.net.URISyntaxException;


/** Secure CoAP client.
 * It may send CoAP GET, PUT, and DELETE requests, or register for observing a remote resource.
 * <p>
 * It supports resource observation (RFC 7641) and blockwise transfer (RFC 7959). 
 */
public class SCoapClient {
	private SCoapClient() {}

	
	/** The main method.
	 * @param args command-line arguments 
	 * @throws URISyntaxException 
	 * @throws IOException */
	public static void main(String[] args) throws URISyntaxException, IOException {
		Flags flags=new Flags(args);
		boolean help=flags.getBoolean("-h","prints this help");
		int local_port=flags.getInteger("-p",CoapProvider.DYNAMIC_PORT,"<port>","local UDP port (default port is "+CoapProvider.DEFAUL_PORT+")");
		int max_block_size=flags.getInteger("-m",0,"<max-size>","maximum block size");
		int timeout=flags.getInteger("-t",-1,"<time>","request timeout");
		int verbose_level=flags.getInteger("-v",0,"<level>","verbose level");
		//boolean exit=flags.getBoolean("-x","stops observing when 'return' is pressed");
		String[] resource_tuple=flags.getStringTuple("-b",2,null,"<format> <value>","resource value in PUT or POST requests; format can be: NULL|TEXT|XML|JSON; value can be ASCII or HEX (0x..)");
		int resource_format=resource_tuple!=null? CoapResource.getContentFormatIdentifier(resource_tuple[0]) : -1;
		byte[] resource_value=resource_tuple!=null? (resource_tuple[1].startsWith("0x")? Bytes.fromHex(resource_tuple[1]) : resource_tuple[1].getBytes()): null;
		String method_name=flags.getString(Flags.PARAM,null,"<method>","method (e.g. GET, PUT, etc.)");
		String resource_uri=flags.getString(Flags.PARAM,null,"<uri>","resource URI");
				
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
		// CoAP client
		var client=new SecureCoapClient(local_port,clientId,key,broker,username,passwd);
		if (max_block_size>0) client.setMaximumBlockSize(max_block_size);		
		
		// handler for receiving the response
		CoapResponseHandler resp_handler=new CoapResponseHandler() {
			@Override
			public void onResponse(CoapRequest req, CoapResponse resp) {
				byte[] value=resp.getPayload();
				String format=CoapResource.getContentFormat(resp.getContentFormat());
				System.out.println("Response: "+resp.getResponseCode()+": "+(format!=null? format+": " : "")+(value!=null? new String(value) : "void"));
				if (req==null || !req.hasObserveRegister()) {
					client.halt();
					System.exit(0);
				}
			}
			@Override
			public void onRequestFailure(CoapRequest req) {
				if (req!=null && req.hasObserveRegister()) System.out.println("Observation finished");
				else System.out.println("Request failure");
				client.halt();
				System.exit(0);
			}
		};

		// request
		if (method_name.equalsIgnoreCase("OBSERVE")) {
			// resource observation
			CoapURI uri=new CoapURI(resource_uri);
			client.observe(uri,resp_handler);
			SystemUtils.readLine();
			client.observeCancel(uri);
		}
		else {
			// resource GET, PUT, POST, or DELETE
			if (timeout<=0) client.request(CoapRequestMethod.getMethodByName(method_name),new CoapURI(resource_uri),null,resource_format,resource_value,resp_handler);
			else {
				client.setTimeout(timeout);
				CoapResponse resp=client.request(CoapRequestMethod.getMethodByName(method_name),new CoapURI(resource_uri),resource_format,resource_value);
				if (resp!=null) resp_handler.onResponse(null,resp);
				else {
					System.out.println("Request timeout");
					System.exit(0);
				}
			}
		}
	}

}
