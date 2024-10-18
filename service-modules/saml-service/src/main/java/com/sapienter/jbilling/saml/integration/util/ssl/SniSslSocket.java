package com.sapienter.jbilling.saml.integration.util.ssl;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

@RequiredArgsConstructor
public class SniSslSocket extends SSLSocket {
    private final SSLSocket delegate;

    public static Socket prepare(Socket socket) {
        if (!(socket instanceof SSLSocket)) {
            return socket;
        }
        SSLSocket sslSocket = (SSLSocket) socket;
        return new SniSslSocket(sslSocket);
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        prepareForSni(endpoint);
        delegate.connect(endpoint);
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        prepareForSni(endpoint);
        delegate.connect(endpoint, timeout);
    }

    private void prepareForSni(SocketAddress endpoint) {
        if (!(endpoint instanceof InetSocketAddress)) {
            return;
        }
        InetSocketAddress inetSocketAddress = (InetSocketAddress) endpoint;
        String host = inetSocketAddress.getHostString();
        if (StringUtils.isBlank(host)) {
            return;
        }
        SSLParameters sslParameters = getSSLParameters();
        if (!sslParameters.getServerNames().isEmpty()) {
            return;
        }
        sslParameters.setServerNames(ImmutableList.of(new SNIHostName(host)));
        setSSLParameters(sslParameters);
    }

    @Override
    public void addHandshakeCompletedListener(HandshakeCompletedListener listener) {
        delegate.addHandshakeCompletedListener(listener);
    }

    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
        delegate.bind(bindpoint);
    }

    @Override
    public InetAddress getInetAddress() {
        return delegate.getInetAddress();
    }

    @Override
    public InetAddress getLocalAddress() {
        return delegate.getLocalAddress();
    }

    @Override
    public int getLocalPort() {
        return delegate.getLocalPort();
    }

    @Override
    public SocketChannel getChannel() {
        return delegate.getChannel();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public boolean getEnableSessionCreation() {
        return delegate.getEnableSessionCreation();
    }

    @Override
    public void setEnableSessionCreation(boolean flag) {
        delegate.setEnableSessionCreation(flag);
    }

    @Override
    public String[] getEnabledCipherSuites() {
        return delegate.getEnabledCipherSuites();
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) {
        delegate.setEnabledCipherSuites(suites);
    }

    @Override
    public String[] getEnabledProtocols() {
        return delegate.getEnabledProtocols();
    }

    @Override
    public void setEnabledProtocols(String[] protocols) {
        delegate.setEnabledProtocols(protocols);
    }

    @Override
    public SSLSession getHandshakeSession() {
        return delegate.getHandshakeSession();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return delegate.getLocalSocketAddress();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return delegate.getInputStream();
    }

    @Override
    public boolean getNeedClientAuth() {
        return delegate.getNeedClientAuth();
    }

    @Override
    public void setNeedClientAuth(boolean need) {
        delegate.setNeedClientAuth(need);
    }

    @Override
    public int getPort() {
        return delegate.getPort();
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return delegate.getRemoteSocketAddress();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return delegate.getOutputStream();
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return delegate.getTcpNoDelay();
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
        delegate.setTcpNoDelay(on);
    }

    @Override
    public int getSoLinger() throws SocketException {
        return delegate.getSoLinger();
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        return delegate.getOOBInline();
    }

    @Override
    public void setOOBInline(boolean on) throws SocketException {
        delegate.setOOBInline(on);
    }

    @Override
    public int getSoTimeout() throws SocketException {
        return delegate.getSoTimeout();
    }

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
        delegate.setSoTimeout(timeout);
    }

    @Override
    public int getSendBufferSize() throws SocketException {
        return delegate.getSendBufferSize();
    }

    @Override
    public void setSendBufferSize(int size) throws SocketException {
        delegate.setSendBufferSize(size);
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        return delegate.getReceiveBufferSize();
    }

    @Override
    public void setReceiveBufferSize(int size) throws SocketException {
        delegate.setReceiveBufferSize(size);
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        return delegate.getKeepAlive();
    }

    @Override
    public void setKeepAlive(boolean on) throws SocketException {
        delegate.setKeepAlive(on);
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return delegate.getReuseAddress();
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        delegate.setReuseAddress(on);
    }

    @Override
    public SSLParameters getSSLParameters() {
        return delegate.getSSLParameters();
    }

    @Override
    public void setSSLParameters(SSLParameters params) {
        delegate.setSSLParameters(params);
    }

    @Override
    public SSLSession getSession() {
        return delegate.getSession();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public String[] getSupportedProtocols() {
        return delegate.getSupportedProtocols();
    }

    @Override
    public int getTrafficClass() throws SocketException {
        return delegate.getTrafficClass();
    }

    @Override
    public void setTrafficClass(int tc) throws SocketException {
        delegate.setTrafficClass(tc);
    }

    @Override
    public boolean getUseClientMode() {
        return delegate.getUseClientMode();
    }

    @Override
    public void setUseClientMode(boolean mode) {
        delegate.setUseClientMode(mode);
    }

    @Override
    public boolean getWantClientAuth() {
        return delegate.getWantClientAuth();
    }

    @Override
    public void setWantClientAuth(boolean want) {
        delegate.setWantClientAuth(want);
    }

    @Override
    public void sendUrgentData(int data) throws IOException {
        delegate.sendUrgentData(data);
    }

    @Override
    public boolean isConnected() {
        return delegate.isConnected();
    }

    @Override
    public boolean isBound() {
        return delegate.isBound();
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public boolean isInputShutdown() {
        return delegate.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return delegate.isOutputShutdown();
    }

    @Override
    public void removeHandshakeCompletedListener(HandshakeCompletedListener listener) {
        delegate.removeHandshakeCompletedListener(listener);
    }

    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
        delegate.setSoLinger(on, linger);
    }

    @Override
    public void shutdownInput() throws IOException {
        delegate.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        delegate.shutdownOutput();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        delegate.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    @Override
    public void startHandshake() throws IOException {
        delegate.startHandshake();
    }


}
