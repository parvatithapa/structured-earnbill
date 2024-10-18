package com.sapienter.jbilling.saml.integration.util.ssl;

import lombok.RequiredArgsConstructor;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.SchemeLayeredSocketFactory;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

@SuppressWarnings("deprecation")
@RequiredArgsConstructor
public class SniSslSchemeLayeredSocketFactory implements SchemeLayeredSocketFactory {
    private final SchemeLayeredSocketFactory delegate;

    @Override
    public Socket createSocket(HttpParams params) throws IOException {
        return SniSslSocket.prepare(delegate.createSocket(params));
    }

    @Override
    public Socket createLayeredSocket(Socket socket, String host, int port, HttpParams params) throws IOException, UnknownHostException {
        return SniSslSocket.prepare(delegate.createLayeredSocket(socket, host, port, params));
    }

    @Override
    public boolean isSecure(Socket sock) throws IllegalArgumentException {
        return delegate.isSecure(sock);
    }

    @Override
    public Socket connectSocket(Socket socket, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
        return delegate.connectSocket(socket, remoteAddress, localAddress, params);
    }

}
