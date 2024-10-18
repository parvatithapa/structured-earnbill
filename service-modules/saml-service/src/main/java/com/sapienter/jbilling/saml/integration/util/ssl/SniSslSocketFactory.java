package com.sapienter.jbilling.saml.integration.util.ssl;

import lombok.RequiredArgsConstructor;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

@RequiredArgsConstructor
public class SniSslSocketFactory extends SSLSocketFactory {
    private final SSLSocketFactory delegate;

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return SniSslSocket.prepare(delegate.createSocket(socket, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return SniSslSocket.prepare(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return SniSslSocket.prepare(delegate.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return SniSslSocket.prepare(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return SniSslSocket.prepare(delegate.createSocket(address, port, localAddress, localPort));
    }

    @Override
    public Socket createSocket() throws IOException {
        return SniSslSocket.prepare(delegate.createSocket());
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }
}
