package com.hase.competition.clientprocess;

import com.hase.competition.CommonUtils;
import com.hase.competition.beans.QuerySpanBean;
import com.alibaba.fastjson.JSON;
import com.hase.competition.serializer.SerializerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class QuerySpanServiceRunnable implements Runnable {
    private final List<Map<String, List<String>>> ringCaches;
    private final Lock lockForRingCaches;
    private final Condition conditionForRingCaches;
    private int port = 0;
    private ServerSocket serverSocket;


    public QuerySpanServiceRunnable(ServerSocket serverSocket, List<Map<String, List<String>>> ringCaches, Lock lockForRingCaches, Condition conditionForRingCaches) {
        this.serverSocket = serverSocket;
        this.ringCaches = ringCaches;
        this.lockForRingCaches = lockForRingCaches;
        this.conditionForRingCaches = conditionForRingCaches;
    }

    @Override
    public void run() {
        final Logger LOGGER = LoggerFactory.getLogger(Thread.currentThread().getName());
        try {
            Socket backendSocket = serverSocket.accept();
            backendSocket.setTcpNoDelay(true);
            BufferedInputStream bis = new BufferedInputStream(backendSocket.getInputStream());
            BufferedOutputStream bos = new BufferedOutputStream(backendSocket.getOutputStream());
            while (true) {
                LOGGER.info("get a request, starrrrrrrr" );

                //read msg length
                byte[] lengthByte = new byte[4];
                int isReadDone = bis.read(lengthByte);
                if (isReadDone == -1) {
                    LOGGER.error("illegal end!");
                    return;
                }
                int length = CommonUtils.bytesToInt(lengthByte,0);

                //read all of object data
                byte[] dataBytes = new byte[length];
                bis.read(dataBytes);
                if (dataBytes == null) {
                    LOGGER.error("illegal end!");
                    return;
                }
                QuerySpanBean querySpanBean = SerializerUtils.deserialize(dataBytes, QuerySpanBean.class);
                if (querySpanBean == null){
                    LOGGER.info("socket closed by peer, exit thread");
                    return;
                }

                int batchPos = querySpanBean.getBatchPos();
                Set<String> wrongTraceIds = querySpanBean.getWrongTraceIds();
                LOGGER.info("get a request, batchPos: " + batchPos);
                LOGGER.info("get a request, wrongTraceIds: " + wrongTraceIds);

                String responseStr = JSON.toJSONString(getWrongTracing(wrongTraceIds, batchPos));
                CommonUtils.sendIntAndStringFromStream(bos, batchPos, responseStr);
//                Map<String, List<String>> traceMap = getWrongTracing(wrongTraceIds,batchPos);
//                SendTraceListBean trace = new SendTraceListBean();
//                List<SendTraceBean> traceList = new ArrayList<>();
//                for (Map.Entry<String, List<String>> entry : traceMap.entrySet()) {
//                    SendTraceBean bean = new SendTraceBean();
//                    bean.setTraceId(entry.getKey());
//                    bean.setSpanList(entry.getValue());
//                    traceList.add(bean);
//                    trace.setTraceList(traceList);
//                }
//                byte[] object = SerializerUtils.serialize(trace);

//                LOGGER.info("get a request, wrongTraceIds:object " + object.length);
//                bos.write(CommonUtils.intToBytesArray(object.length));
//                bos.write(object);
//                bos.flush();

                LOGGER.info("suc to repose, batchPos: " + batchPos);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Map<String, List<String>> getWrongTracing(Set<String> traceIdList, int batchPos) {
        int total = ringCaches.size();
        int curr = batchPos % total;
        int pre = (curr - 1) == -1 ? total - 1 : curr - 1;
        int next = (curr + 1) == total ? 0 : curr + 1;
        Map<String, List<String>> wrongTraceIdMap;
        if (traceIdList != null) {
            wrongTraceIdMap = getWrongTracesAll(traceIdList, pre, curr, next);
        } else {
            wrongTraceIdMap = new HashMap<>();
        }

        if (batchPos > 0) {
            Map<String, List<String>> preTraceIdMap = ringCaches.get(pre);
            if (preTraceIdMap.size() > 0) {
                preTraceIdMap.clear();
                lockForRingCaches.lock();
                try {
                    conditionForRingCaches.signalAll();
                } finally {
                    lockForRingCaches.unlock();
                }
            }
        }
        return wrongTraceIdMap;
    }

    private Map<String, List<String>> getWrongTracesAll(Iterable<String> wrongTraceIds, int pre, int curr, int next) {
        Map<String, List<String>> wrongTraceIdMap = new HashMap<>();
        getWrongTracesWithBatch(wrongTraceIds, pre, wrongTraceIdMap);
        getWrongTracesWithBatch(wrongTraceIds, curr, wrongTraceIdMap);
        getWrongTracesWithBatch(wrongTraceIds, next, wrongTraceIdMap);
        return wrongTraceIdMap;
    }

    private void getWrongTracesWithBatch(Iterable<String> wrongTraceIds, int pos, Map<String, List<String>> wrongTraceIdMap) {
        Map<String, List<String>> traceMap = ringCaches.get(pos);
        for (String wrongTraceId : wrongTraceIds) {
            List<String> spanList = traceMap.get(wrongTraceId);
            if (spanList != null && spanList.size() > 0) {
                // one trace may cross to batch (e.g batch size 20000, span1 in line 19999, span2 in line 20001)
                List<String> existSpanList = wrongTraceIdMap.get(wrongTraceId);
                if (existSpanList != null)
                    existSpanList.addAll(spanList);
                else
                    wrongTraceIdMap.put(wrongTraceId, spanList);
            }
        }
    }
}
