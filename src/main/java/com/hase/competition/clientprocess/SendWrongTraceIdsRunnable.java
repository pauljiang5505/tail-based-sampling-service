package com.hase.competition.clientprocess;

import com.alibaba.fastjson.JSON;
import com.hase.competition.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;

public class SendWrongTraceIdsRunnable implements Runnable {

    private final int serverPort;
    private final SynchronousQueue<Set<String>> uploadSynQueue;


    public SendWrongTraceIdsRunnable(int remotePort, SynchronousQueue<Set<String>> uploadSynQueue) {
        this.serverPort = remotePort;
        this.uploadSynQueue = uploadSynQueue;
    }

    @Override
    public void run() {
        final Logger LOGGER = LoggerFactory.getLogger(Thread.currentThread().getName());

        Socket clientSocket = null;
        BufferedOutputStream bos;
        try {
            clientSocket = new Socket();
            clientSocket.setTcpNoDelay(true);

            LOGGER.info("request connect to server!");
            clientSocket.connect(new InetSocketAddress("localhost", serverPort), 3000);
            LOGGER.info("success to connect server!");

            bos = new BufferedOutputStream(clientSocket.getOutputStream());
            int batchPos = -1;
            Set<String> lastWrongTraceIds = null, currWrongTraceIds;

            while (true) {
                LOGGER.info("wait a upload");
                currWrongTraceIds = uploadSynQueue.take();

                if (currWrongTraceIds == ProcessDataSecondRoundRunnable.EXIT_FLAG_SET) {
                    if (lastWrongTraceIds != null) {
                        ++batchPos;
                        LOGGER.info("start to upload batchPos: " + batchPos);
                        CommonUtils.sendIntAndStringFromStream(bos, batchPos, JSON.toJSONString(lastWrongTraceIds));
                        LOGGER.info("suc to upload batchPos: " + batchPos);
                    }

                    LOGGER.warn("UPLOAD QUEUE is empty, start to send end flag!");
                    CommonUtils.sendIntAndStringFromStream(bos, -1, "null");

                    LOGGER.info("exit upload thread");
                    return;
                }

                if (lastWrongTraceIds != null) {
                    ++batchPos;
                    LOGGER.info("start to upload batchPos: " + batchPos);
                    CommonUtils.sendIntAndStringFromStream(bos, batchPos, JSON.toJSONString(lastWrongTraceIds));
                    LOGGER.info("suc to upload batchPos: " + batchPos);
                }
                lastWrongTraceIds = currWrongTraceIds;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (clientSocket != null) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
