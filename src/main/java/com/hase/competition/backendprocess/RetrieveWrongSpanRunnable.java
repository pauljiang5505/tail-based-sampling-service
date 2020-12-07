package com.hase.competition.backendprocess;

import com.hase.competition.CommonUtils;
import com.hase.competition.beans.*;
import com.hase.competition.serializer.SerializerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

public class RetrieveWrongSpanRunnable implements Runnable {

    private final int querySpanServicePort;
    private final SynchronousQueue<TraceIdBatch> traceIdBatchSynQueue;

    private SynchronousQueue<Map<String, List<String>>> synQueue = new SynchronousQueue<>();

    public RetrieveWrongSpanRunnable(int querySpanServicePort, SynchronousQueue<TraceIdBatch> traceIdBatchSynQueue) {
        this.querySpanServicePort = querySpanServicePort;
        this.traceIdBatchSynQueue = traceIdBatchSynQueue;
    }

    public SynchronousQueue<Map<String, List<String>>> getSynQueue() {
        return synQueue;
    }

    @Override
    public void run() {
        final Logger LOGGER = LoggerFactory.getLogger(Thread.currentThread().getName());

        Socket clientSocket = null;
        BufferedOutputStream bos;
        BufferedInputStream bis;
        byte[] contentBytes = new byte[1024];
        byte[] batchIDBytes = new byte[8];

        try {
            clientSocket = new Socket();
            clientSocket.setTcpNoDelay(true);

            LOGGER.info("request connect!");
            clientSocket.connect(new InetSocketAddress("localhost", this.querySpanServicePort), 3000);
            LOGGER.info("suc to connect!");

            bos = new BufferedOutputStream(clientSocket.getOutputStream());
            bis = new BufferedInputStream(clientSocket.getInputStream());

            while (true) {
                LOGGER.info("wait traceIdBatch");
                TraceIdBatch traceIdBatch = traceIdBatchSynQueue.take();

                int batchPos = traceIdBatch.getBatchPos();
                if (batchPos == -1) {  // exit
                    break;
                }

                QuerySpanBean querySpanBean= new QuerySpanBean();
                querySpanBean.setBatchPos(batchPos);
                querySpanBean.setWrongTraceIds(traceIdBatch.getWrongTraceIds());
                LOGGER.info("send query span request, batchPos: " + batchPos);
                LOGGER.info("send query span request, querySpanBean: " + querySpanBean.toString());
                byte[] object = SerializerUtils.serialize(querySpanBean);
                LOGGER.info("send query span request, serialize: querySpanBean  " + object);
                bos.write(CommonUtils.intToBytesArray(object.length));
                bos.write(object);
                bos.flush();

                Tuple result = new Tuple();
                contentBytes = CommonUtils.parseInputStream(bis, batchIDBytes, contentBytes, result);
                if (contentBytes == null) {
                    LOGGER.error("socket ended!");
                    return;
                }

//                //read msg length
//                byte[] lengthByte = new byte[4];
//                int isReadDone = bis.read(lengthByte);
//                if (isReadDone == -1) {
//                    LOGGER.error("socket end!");
//                    return;
//                }
//                int length = CommonUtils.bytesToInt(lengthByte,0);
//                //read all of object data
//                byte[] dataBytes = new byte[length];
//                Thread.sleep(10);
//                bis.read(dataBytes);
//                LOGGER.info("send query span request, serialize: receive byte size  " + length);
//                SendTraceListBean sendTraceListBean = SerializerUtils.deserialize(dataBytes,SendTraceListBean.class);
//                LOGGER.info("receive query span result, sendTraceListBean: " + sendTraceListBean.toString());
//
//                Map<String, List<String>> traceMap = new HashMap<>();
//                List<SendTraceBean> lists = sendTraceListBean.getTraceList();
//                for ( SendTraceBean bean: lists) {
//                    traceMap.put(bean.getTraceId(),bean.getSpanList());
//                }
                LOGGER.info("suc to get span, batchPos: " + batchPos);
                synQueue.put(JSON.parseObject(result.getContent(), new TypeReference<Map<String, List<String>>>() {}));
                LOGGER.info("suc to enqueue span, batchPos: " + batchPos);
            }
            LOGGER.info("exit query span thread");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
