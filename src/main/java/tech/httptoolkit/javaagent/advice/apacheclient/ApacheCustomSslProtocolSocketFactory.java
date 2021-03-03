package tech.httptoolkit.javaagent.advice.apacheclient;

import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import tech.httptoolkit.javaagent.HttpProxyAgent;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.*;

public class ApacheCustomSslProtocolSocketFactory implements SecureProtocolSocketFactory {

    private final SSLSocketFactory interceptedSocketFactory = HttpProxyAgent
            .getInterceptedSslContext()
            .getSocketFactory();

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return interceptedSocketFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
        return interceptedSocketFactory.createSocket(host, port, localAddress, localPort);
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return interceptedSocketFactory.createSocket(socket, host, port, autoClose);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort, HttpConnectionParams params) throws IOException {
        // Marginally more complicated logic here unfortunately, since timeout isn't natively
        // supported. Minimal implementation taken from the existing lib implementations:
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        int timeout = params.getConnectionTimeout();
        Socket socket;

        SocketFactory socketfactory = SSLSocketFactory.getDefault();
        if (timeout == 0) {
            socket = socketfactory.createSocket(host, port, localAddress, localPort);
        } else {
            socket = socketfactory.createSocket();
            SocketAddress localAddr = new InetSocketAddress(localAddress, localPort);
            SocketAddress remoteAddr = new InetSocketAddress(host, port);
            socket.bind(localAddr);
            socket.connect(remoteAddr, timeout);
        }

        return socket;
    }
}
