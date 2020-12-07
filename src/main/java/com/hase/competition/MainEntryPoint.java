package com.hase.competition;

import com.hase.competition.backendprocess.ServerMainRunnable;
import com.hase.competition.clientprocess.ClientMainRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainEntryPoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainEntryPoint.class.getName());
    public static void main(String[] args) {
        String portStr = System.getProperty("server.port", "8080");
        int port = Integer.parseInt(portStr);

        if (CommonUtils.isClientProcess()) {
            LOGGER.info("start-up a client, port: " + port);
            new Thread(new ClientMainRunnable(port)).start();
        } else if (CommonUtils.isBackendProcess()) {
            LOGGER.info("start-up a backend, port: " + port);
            new Thread(new ServerMainRunnable(port)).start();
        }
    }
}
