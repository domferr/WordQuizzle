package com.domenico.server;

import com.domenico.communication.ConnectionData;
import com.domenico.communication.UDPConnection;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Callable;

public class UDPRequest extends Multiplexer implements Callable<ConnectionData> {

    private static final int MAX_WAITING_TIME = 5000;
    private String from;
    private String to;
    private UDPConnection udpConnection;
    private ConnectionData response;

    public UDPRequest(DatagramChannel channel, SocketAddress remoteAddress, String from, String to) throws IOException {
        super(channel, SelectionKey.OP_WRITE, MAX_WAITING_TIME);
        udpConnection = new UDPConnection(channel, remoteAddress);
        this.from = from;
        this.to = to;
    }

    @Override
    public ConnectionData call() throws Exception {
        response = null;
        startProcessing();  //loops until time is out or connection is closed
        if (response == null)
            response = ConnectionData.Factory.newFailResponse("Tempo scaduto");

        return response;
    }

    @Override
    protected void onTimeout() throws IOException {
        stopProcessing();
    }

    @Override
    void onAcceptable(SelectionKey key) throws IOException {}

    @Override
    void onReadable(SelectionKey key) throws IOException {
        response = udpConnection.receiveData();
        stopProcessing();
    }

    @Override
    void onWritable(SelectionKey key) throws IOException {
        udpConnection.sendData(ConnectionData.Factory.newChallengeRequest(from, to));
        key.channel().register(selector, SelectionKey.OP_READ);
    }

    @Override
    void onEndConnection(SelectionKey key) throws IOException {
        stopProcessing();
    }
}