package test;

import java.io.IOException;
import java.net.URISyntaxException;

import org.zoolu.util.Bytes;
import org.zoolu.util.Random;
import org.zoolu.util.SystemUtils;
import org.zoolu.util.log.DefaultLogger;
import org.zoolu.util.log.LoggerLevel;
import org.zoolu.util.log.WriterLogger;

import it.unipr.netsec.scoap.SecureCoapClient;
import it.unipr.netsec.scoap.SecureCoapServer;
import it.unipr.netsec.smqtt.SecureMqttClient;
import it.unipr.netsec.smqtt.gkd.GKDServer;
import it.unipr.netsec.smqtt.gkd.method.SlottedGKDClient;
import it.unipr.netsec.smqtt.gkd.method.SlottedGKDService;
import io.ipstack.coap.client.CoapResponseHandler;
import io.ipstack.coap.message.CoapRequest;
import io.ipstack.coap.message.CoapRequestMethod;
import io.ipstack.coap.message.CoapResponse;
import io.ipstack.coap.provider.CoapURI;
import io.ipstack.coap.server.CoapResource;
import io.ipstack.coap.server.CoapServer;

public class GKDCoapTest {
	private GKDCoapTest() {}

	static final int COAP_PORT= 5683;
	
	public static void main(String[] args) throws IOException, URISyntaxException {
				
		DefaultLogger.setLogger(new WriterLogger(System.out,LoggerLevel.DEBUG));

		// MQTT
		var broker= "127.0.0.1:1883";
		var username= "test";
		var passwd= "test";
		
		// verbose mode
		DefaultLogger.setLogger(new WriterLogger(System.out));
		//PahoClient.VERBOSE= true;
		GKDServer.VERBOSE= true;
		SecureCoapClient.VERBOSE= true;
		//SecureCoapServer.VERBOSE= true;
		
		// GKD server
		SystemUtils.run(() -> new GKDServer("server",broker));
		
		// CoAP server
		var serverId= "server-"+Random.nextInt();
		var serverKey= Bytes.fromHex("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
		var server=new SecureCoapServer(COAP_PORT,serverId,serverKey,broker,username,passwd);
		server.setResource("/test",CoapResource.getContentFormatIdentifier("TEXT"),"It's working!".getBytes());
		System.out.println("CoAP server running at port "+COAP_PORT);

		// CoAP client
		var clientId= "client-"+Random.nextInt();
		var clientKey= Bytes.fromHex("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
		var client=new SecureCoapClient(-1,clientId,clientKey,broker,username,passwd);
		client.request(CoapRequestMethod.GET,new CoapURI("coap://127.0.0.1/test"),new CoapResponseHandler() {
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
				System.out.println("Request failure");
				client.halt();
				System.exit(0);
			}
		});
	}

}
