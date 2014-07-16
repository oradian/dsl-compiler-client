package com.dslplatform.compiler.client;

import com.dslplatform.compiler.client.json.JsonValue;
import com.dslplatform.compiler.client.parameters.Password;
import com.dslplatform.compiler.client.parameters.Prompt;
import com.dslplatform.compiler.client.parameters.Username;
import org.w3c.dom.Document;
import sun.misc.BASE64Encoder;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.SecureRandom;

public class DslServer {
	private static final String REMOTE_URL = "https://compiler.dsl-platform.com:8443/platform/";
	private static final SSLSocketFactory SSL_SOCKET_FACTORY;

	private static SSLSocketFactory createSSLSocketFactory() throws Exception {
		final KeyStore truststore = KeyStore.getInstance("jks");
		truststore.load(DslServer.class.getResourceAsStream("/startssl-ca.jks"), "startssl-ca".toCharArray());
		final TrustManagerFactory tMF = TrustManagerFactory.getInstance("PKIX");
		tMF.init(truststore);
		final SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, tMF.getTrustManagers(), new SecureRandom());
		return sslContext.getSocketFactory();
	}

	static {
		try {
			SSL_SOCKET_FACTORY = createSSLSocketFactory();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String readResponseError(final HttpsURLConnection conn) throws IOException {
		if (conn.getContentType() != null && conn.getContentType().startsWith("application/xml")) {
			final Either<Document> xml = Utils.readXml(conn.getErrorStream());
			if (!xml.isSuccess()) {
				return "INTERNAL ERROR: Error reading xml response\n" + xml.whyNot();
			}
			final String error = xml.get().getDocumentElement().getTextContent();
			return error != null ? error : "UNKNOWN ERROR";
		}
		final String result = Utils.read(conn.getErrorStream());
		if ("application/json".equals(conn.getContentType()) && result.length() > 0) {
			return JsonValue.readFrom(result).asString();
		}
		return result;
	}

	private static Either<HttpsURLConnection> setupConnection(
			final String address,
			final Context context,
			final boolean sendJson,
			final boolean getJson) {
		final HttpsURLConnection conn;
		try {
			final URL url = new URL(REMOTE_URL + address);
			conn = (HttpsURLConnection) url.openConnection();
		} catch (Exception ex) {
			return Either.fail(ex.getMessage());
		}
		conn.setSSLSocketFactory(SSL_SOCKET_FACTORY);
		final Either<String> username = Username.getOrLoad(context);
		if (!username.isSuccess()) {
			return Either.fail(username.whyNot());
		}
		final String password = Password.getOrLoad(context);
		final BASE64Encoder encoder = new BASE64Encoder();
		conn.setConnectTimeout(10000);
		conn.setReadTimeout(60000);
		try {
			final String base64Login = encoder.encode((username.get() + ":" + password).getBytes("UTF-8"));
			conn.addRequestProperty("Authorization", "Basic " + base64Login);
		} catch (UnsupportedEncodingException ex) {
			return Either.fail(ex.getMessage());
		}
		if (sendJson) {
			conn.addRequestProperty("Content-type", "application/json");
		}
		if (getJson) {
			conn.addRequestProperty("Accept", "application/json");
		}
		return Either.success(conn);
	}

	private static boolean tryRestart(
			final HttpsURLConnection conn,
			final Context context) throws IOException {
		context.error("Authorization failed.");
		context.error(readResponseError(conn));
		if (!context.canInteract()) {
			System.exit(0);
		}
		final String value = context.ask("Retry (y/N):");
		if (!"y".equalsIgnoreCase(value)) {
			System.exit(0);
		}
		context.log("Retrying...");
		Username.retryInput(context);
		Password.retryInput(context);
		return true;
	}

	public static Either<String> get(final String address, final Context context) {
		Either<HttpsURLConnection> tryConn = setupConnection(address, context, false, true);
		if (!tryConn.isSuccess()) {
			return Either.fail(tryConn.whyNot());
		}
		HttpsURLConnection conn = tryConn.get();
		try {
			return Either.success(Utils.read(conn.getInputStream()));
		} catch (UnknownHostException ex) {
			return Either.fail("Error connecting to compiler.dsl-platform.com\nCheck if Internet connection is down: " + ex.getMessage());
		} catch (Exception ex) {
			try {
				if (conn.getResponseCode() == 403 && tryRestart(conn, context)) {
					return get(address, context);
				}
				if (conn.getErrorStream() != null) {
					return Either.fail(readResponseError(conn));
				}
			} catch (Exception e) {
				return Either.fail(e.getMessage());
			}
			return Either.fail(ex.getMessage());
		}
	}

	public static Either<String> put(final String address, final Context context, JsonValue json) {
		return send(address, "PUT", context, json.toString());
	}

	/*public static Either<String> post(final String address, final Map<InputParameter, String> parameters, JsonValue json) {
		return send(address, "POST", parameters, json.toString());
	}*/

	private static Either<String> send(
			final String address,
			final String method,
			final Context context,
			final String argument) {
		Either<HttpsURLConnection> tryConn = setupConnection(address, context, true, true);
		if (!tryConn.isSuccess()) {
			return Either.fail(tryConn.whyNot());
		}
		HttpsURLConnection conn = tryConn.get();
		try {
			conn.setDoOutput(true);
			conn.setRequestMethod(method);
			final OutputStream os = conn.getOutputStream();
			os.write(argument.getBytes("UTF-8"));
			os.close();
			return Either.success(Utils.read(conn.getInputStream()));
		} catch (UnknownHostException ex) {
			return Either.fail("Error connecting to compiler.dsl-platform.com\nCheck if Internet connection is down: " + ex.getMessage());
		} catch (Exception ex) {
			try {
				if (conn.getResponseCode() == 403 && tryRestart(conn, context)) {
					return send(address, method, context, argument);
				}
				if (conn.getErrorStream() != null) {
					return Either.fail(readResponseError(conn));
				}
			} catch (Exception e) {
				return Either.fail(e.getMessage());
			}
			return Either.fail(ex.getMessage());
		}
	}

	public static void downloadAndUnpack(final String file, final File path) throws IOException {
		final URL server = new URL(REMOTE_URL + "download/" + file + ".zip");
		final HttpsURLConnection conn = (HttpsURLConnection)server.openConnection();
		conn.setSSLSocketFactory(DslServer.SSL_SOCKET_FACTORY);
		Utils.unpackZip(path, conn.getInputStream());
	}
}
