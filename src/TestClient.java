import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.lekebilen.quasseldroid.qtcomm.QDataInputStream;
import com.lekebilen.quasseldroid.qtcomm.QDataOutputStream;
import com.lekebilen.quasseldroid.qtcomm.QMetaType;
import com.lekebilen.quasseldroid.qtcomm.QMetaTypeRegistry;
import com.lekebilen.quasseldroid.qtcomm.QVariant;


public class TestClient {
	public static void main(String[] args) {
		try {
			SocketFactory factory = (SocketFactory)SocketFactory.getDefault();
			Socket socket = (Socket)factory.createSocket("localhost", 4242);
			QDataOutputStream ss = new QDataOutputStream(socket.getOutputStream());
			
			Map<String, QVariant<?>> initial = new HashMap<String, QVariant<?>>();
			
			DateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy HH:mm:ss");
			Date date = new Date();
			initial.put("ClientDate", new QVariant<String>(dateFormat.format(date), QVariant.Type.String));
			initial.put("UseSsl", new QVariant<Boolean>(true, QVariant.Type.Bool));
			initial.put("ClientVersion", new QVariant<String>("v0.6.1 (dist-<a href='http://git.quassel-irc.org/?p=quassel.git;a=commit;h=611ebccdb6a2a4a89cf1f565bee7e72bcad13ffb'>611ebcc</a>)", QVariant.Type.String));
			initial.put("UseCompression", new QVariant<Boolean>(false, QVariant.Type.Bool));
			initial.put("MsgType", new QVariant<String>("ClientInit", QVariant.Type.String));
			initial.put("ProtocolVersion", new QVariant<Integer>(10, QVariant.Type.Int));
			
			QDataOutputStream bos = new QDataOutputStream(new ByteArrayOutputStream());
			QVariant<Map<String, QVariant<?>>> bufstruct = new QVariant<Map<String, QVariant<?>>>(initial, QVariant.Type.Map);
			QMetaTypeRegistry.serialize(QMetaType.Type.QVariant, bos, bufstruct);
			
			// Tell the other end how much data to expect
			ss.writeUInt(bos.size(), 32);
			// Send data 
			QMetaTypeRegistry.serialize(QMetaType.Type.QVariant, ss, bufstruct);
			
			// Time to read from the core
			QDataInputStream is = new QDataInputStream(socket.getInputStream());
			//QDataInputStream is = new QDataInputStream(new FileInputStream("/home/sandsmark/tmp/qvariant/file2.dat"));
			int len = is.readInt();
			System.out.println("We're getting this many bytesies from the core: " + len);
            
/*			QDataOutputStream outstream = new QDataOutputStream(new FileOutputStream("c:\\users\\sandsmark\\kek"));
			byte [] buffer = new byte[len];
			is.read(buffer);
			outstream.write(buffer);
			return;*/
				
			Map<String, QVariant<?>> init;
			QVariant <Map<String, QVariant<?>>> v = (QVariant <Map<String, QVariant<?>>>)QMetaTypeRegistry.unserialize(QMetaType.Type.QVariant, is);

			init = (Map<String, QVariant<?>>)v.getData();
			System.out.println("Got answer from server: ");
			for (String key : init.keySet()) {
				System.out.println("\t" + key + " : " + init.get(key));
			}
			
			 // We should check that the core is new and dandy here. 
			 
			// Now SMACK DAB ENCRYPTION YO
			SSLContext sslContext = SSLContext.getInstance("SSL");
			TrustManager[] myTMs = new TrustManager [] {
                    new CustomTrustManager() };
			//sslContext.init(null, new CustomTrustManager[] {}, null);
			sslContext.init(null, myTMs, null);

			
			SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
			
			SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(socket, "localhost", 4242, true);
			sslSocket.setEnabledProtocols(new String[] {"SSLv3"});
			for (String protocol: sslSocket.getEnabledProtocols()) {
				System.out.println(protocol);
			}
			sslSocket.setUseClientMode(true);
			sslSocket.startHandshake();
			
			
			// Start login
			Map<String, QVariant<?>> login = new HashMap<String, QVariant<?>>();
			login.put("MsgType", new QVariant<String>("ClientLogin", QVariant.Type.String));
			login.put("User", new QVariant<String>("test", QVariant.Type.String));
			login.put("Password", new QVariant<String>("test", QVariant.Type.String));
			
			bos = new QDataOutputStream(new ByteArrayOutputStream());
			bufstruct = new QVariant<Map<String, QVariant<?>>>(login, QVariant.Type.Map);
			QMetaTypeRegistry.serialize(QMetaType.Type.QVariant, bos, bufstruct);
			ss = new QDataOutputStream(sslSocket.getOutputStream());
			ss.writeUInt(bos.size(), 32);
			// Send data 
			QMetaTypeRegistry.serialize(QMetaType.Type.QVariant, ss, bufstruct);

			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static class CustomTrustManager implements javax.net.ssl.X509TrustManager {
	     /*
	      * The default X509TrustManager returned by SunX509.  We'll delegate
	      * decisions to it, and fall back to the logic in this class if the
	      * default X509TrustManager doesn't trust it.
	      */
	     X509TrustManager sunJSSEX509TrustManager;
	 
	     CustomTrustManager() throws Exception {
	         // create a "default" JSSE X509TrustManager.
	 
	         KeyStore ks = KeyStore.getInstance("JKS");
	         //ks.load(new FileInputStream("trustedCerts"),
	         //    "passphrase".toCharArray());
	 
	         TrustManagerFactory tmf =
	TrustManagerFactory.getInstance("SunX509", "SunJSSE");
	         tmf.init(ks);
	 
	         TrustManager tms [] = tmf.getTrustManagers();
	 
	         /*
	          * Iterate over the returned trustmanagers, look
	          * for an instance of X509TrustManager.  If found,
	          * use that as our "default" trust manager.
	          */
	         for (int i = 0; i < tms.length; i++) {
	             if (tms[i] instanceof X509TrustManager) {
	                 sunJSSEX509TrustManager = (X509TrustManager) tms[i];
	                 return;
	             }
	         }
	 
	         /*
	          * Find some other way to initialize, or else we have to fail the
	          * constructor.
	          */
	         throw new Exception("Couldn't initialize");
	     }
	 
	     /*
	      * Delegate to the default trust manager.
	      */
	     public void checkClientTrusted(X509Certificate[] chain, String authType)
	                 throws CertificateException {
	         try {
	             sunJSSEX509TrustManager.checkClientTrusted(chain, authType);
	         } catch (CertificateException excep) {

	         }
	     }
	 
	     /*
	      * Delegate to the default trust manager.
	      */
	     public void checkServerTrusted(X509Certificate[] chain, String authType)
	                 throws CertificateException {
	         try {
	             sunJSSEX509TrustManager.checkServerTrusted(chain, authType);
	         } catch (CertificateException excep) {
	             for (X509Certificate cert : chain) {
	            	 System.out.println(cert.getEncoded());
	             }
	         }
	     }
	 
	     /*
	      * Merely pass this through.
	      */
	     public X509Certificate[] getAcceptedIssuers() {
	         return sunJSSEX509TrustManager.getAcceptedIssuers();
	     }
		
	}

}