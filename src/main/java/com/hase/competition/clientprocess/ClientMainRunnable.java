package com.hase.competition.clientprocess;

import com.hase.competition.CommonUtils;
import com.hase.competition.Constants;
import com.hase.competition.tcp.socket.DataSourceServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClientMainRunnable implements Runnable {
    final Logger LOGGER = LoggerFactory.getLogger(Thread.currentThread().getName());
    private static List<Map<String, List<String>>> RING_CACHES = new ArrayList<>();
    private static Lock LOCK_FOR_RING_CACHES = new ReentrantLock();
    private static Condition CONDITION_FOR_RING_CACHES = LOCK_FOR_RING_CACHES.newCondition();

    // Ring Cache Size
    private static int CACHE_TOTAL = 30;
    private int port = 0;

    static {
        for (int i = 0; i < CACHE_TOTAL; i++) {
            RING_CACHES.add(new ConcurrentHashMap<>());
        }
    }

    public ClientMainRunnable(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        LOGGER.info("start main thread");
        try {
            ServerSocket serverSocket = null;
            //listen scoring program ready and setParameters request
            serverSocket = new ServerSocket(port);
            DataSourceServerSocket dss = new DataSourceServerSocket(serverSocket);
            int dataSourcePort = dss.getDataSourcePort();
            SynchronousQueue<String> dataBlockSynQueue = new SynchronousQueue<>();
            SynchronousQueue<List<String>> spansSynQueue = new SynchronousQueue<>();
            SynchronousQueue<Set<String>> uploadSynQueue = new SynchronousQueue<>();
            //upload wrong trace span to server
            startUploadTraceIdThread(uploadSynQueue);
            // read data by http range mode
            startReadDataThread(dataSourcePort, dataBlockSynQueue);
            //process data by start two thread
            startProcessDataTwoThread(dataBlockSynQueue, spansSynQueue, uploadSynQueue);

            //provide query interface for backend to query
            startQueryWrongSpanListThread(serverSocket);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }

        LOGGER.info("end main thread");
    }

    private void startQueryWrongSpanListThread(ServerSocket serverSocket){
        new Thread(new QuerySpanServiceRunnable(serverSocket, RING_CACHES, LOCK_FOR_RING_CACHES, CONDITION_FOR_RING_CACHES),
                "startQueryWrongSpanListThread").start();
    }

    private void startUploadTraceIdThread(SynchronousQueue<Set<String>> uploadSynQueue) {
        int uploadRemotePort = this.port == 8000 ? 8003 : 8004;
        new Thread(new SendWrongTraceIdsRunnable(uploadRemotePort, uploadSynQueue), "startUploadTraceIdThread").start();
    }

    private void startProcessDataTwoThread(SynchronousQueue<String> dataBlockSynQueue, SynchronousQueue<List<String>> spansSynQueue, SynchronousQueue<Set<String>> uploadSynQueue) {
        ProcessDataFirstRoundRunnable processDataFirstRunnable = new ProcessDataFirstRoundRunnable(dataBlockSynQueue, spansSynQueue);
        new Thread(processDataFirstRunnable, "startProcessDataTwoThread1").start();

        ProcessDataSecondRoundRunnable processDataSecondRunnable = new ProcessDataSecondRoundRunnable(RING_CACHES, spansSynQueue, uploadSynQueue,
                LOCK_FOR_RING_CACHES, CONDITION_FOR_RING_CACHES);
        new Thread(processDataSecondRunnable, "startProcessDataTwoThread2").start();
    }

    private void startReadDataThread(int dataSourcePort, SynchronousQueue<String> dataBlockSynQueue) {
        String dataPath = CommonUtils.getDataPath(port, dataSourcePort);
        long rangeSize = 4 * Constants.MB_SIZE;
        long rangeStep = 4 * Constants.MB_SIZE;
        long rangeValueStart = 0;
        RetrieveRemoteDataRunnable readDataRunnable = new RetrieveRemoteDataRunnable(dataPath, rangeSize, rangeStep, rangeValueStart, dataBlockSynQueue);
        new Thread(readDataRunnable, "ReadDataThread").start();
    }
}
