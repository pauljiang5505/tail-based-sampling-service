package com.hase.competition.backendprocess;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hase.competition.CommonUtils;
import com.hase.competition.beans.TraceIdBatch;
import com.hase.competition.beans.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class ReceieveWrongTraceIdsRunnable implements Runnable {

    private final int receivePort;//8003/8004
    private final int clientPort;//8000/8001

    private final BlockingQueue<TraceIdBatch> traceIdBatchQueue;

    public ReceieveWrongTraceIdsRunnable(int listenPort, BlockingQueue<TraceIdBatch> traceIdBatchQueue) {
        this.receivePort = listenPort;
        this.clientPort = listenPort == 8003 ? 8000: 8001;
        this.traceIdBatchQueue = traceIdBatchQueue;
    }

    @Override
    public void run() {
        final Logger LOGGER = LoggerFactory.getLogger(Thread.currentThread().getName());

        ServerSocket serverSocket = null;
        Socket commSocket = null;
        BufferedInputStream bis;

        try {
            LOGGER.info("start to listen port: " + receivePort);
            serverSocket = new ServerSocket(receivePort);
            commSocket = serverSocket.accept();
            commSocket.setTcpNoDelay(true);

            LOGGER.info("create a connect with: " + clientPort);
            commSocket.shutdownOutput();
            bis = new BufferedInputStream(commSocket.getInputStream());

            byte[] dataBytes = new byte[1024];
            byte[] lenAndBathPosBytes = new byte[8];

            while (true) {
                Tuple result = new Tuple();
                dataBytes = CommonUtils.parseInputStream(bis, lenAndBathPosBytes, dataBytes, result);
                if (dataBytes == null){
                    LOGGER.error("illegal end!");
                    return;
                }

                int batchPos = result.getNumber();
                String content = result.getContent();

                if (batchPos == -1 && "null".equals(content)) {
                    TraceIdBatch traceIdBatch = new TraceIdBatch(); // exit batchPos = -1
                    traceIdBatchQueue.put(traceIdBatch);
                    break;
                }

                Set<String> wrongTraceIds = JSON.parseObject(content, new TypeReference<Set<String>>() {});

                LOGGER.info(String.format("accept WrongTraceIds, batchPos: %d, form: %s, wrongTraceId size: %d", batchPos, clientPort, wrongTraceIds.size()));

                TraceIdBatch traceIdBatch = new TraceIdBatch();
                traceIdBatch.setBatchPos(batchPos);
                traceIdBatch.getWrongTraceIds().addAll(wrongTraceIds);

                // enqueue
                traceIdBatchQueue.put(traceIdBatch);
            }
            LOGGER.info("exit accept wrongTraceId thread");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (commSocket != null) {
                    commSocket.close();
                }
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
