package com.hase.competition.tcp.socket;

import com.hase.competition.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class DataSourceServerSocket {
    final Logger LOGGER = LoggerFactory.getLogger(DataSourceServerSocket.class.getName());
    private ServerSocket serverSocket;
    public DataSourceServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public int getDataSourcePort(){
        int dataSourcePort = 0;
        try {
            Socket dataSourceSocket = serverSocket.accept();
            responseOKResult(dataSourceSocket);
            dataSourceSocket = serverSocket.accept();
            dataSourcePort = getDataSourcePort(dataSourceSocket);
            if (dataSourcePort == -1) {
                LOGGER.error("get data source port failed");
            } else {
                LOGGER.error("get data source port: " + dataSourcePort);
            }
        } catch (IOException e) {
            LOGGER.error("socket error! exception info as below ", e);
        } finally {
        }
        return dataSourcePort;
    }

    public void responseOKResult(Socket dataSourceSocket) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(dataSourceSocket.getOutputStream());
        byte[] responseBytes = CommonUtils.responseStr.getBytes();
        bos.write(responseBytes);
        bos.flush();
        dataSourceSocket.close();
    }

    private int getDataSourcePort(Socket dataSourceSocket) throws IOException {
        int datSourceSocket = -1;
        BufferedReader bfReader = new BufferedReader(new InputStreamReader(dataSourceSocket.getInputStream()));
        BufferedOutputStream bos = new BufferedOutputStream(dataSourceSocket.getOutputStream());

        String line;
        int index;
        while ((line = bfReader.readLine()) != null) {
            index = line.indexOf("port");
            if (index > -1) {
                datSourceSocket = Integer.parseInt(line.substring(index + 5, line.lastIndexOf(' ')));
                break;
            }
        }

        byte[] responseBytes = CommonUtils.responseStr.getBytes();
        bos.write(responseBytes);
        bos.flush();

        dataSourceSocket.close();
        return datSourceSocket;
    }
}
