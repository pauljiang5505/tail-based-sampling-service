package com.hase.competition.beans;

import java.util.List;

public class SendTraceListBean {
    private List<SendTraceBean> traceList;

    public List<SendTraceBean> getTraceList() {
        return traceList;
    }

    public void setTraceList(List<SendTraceBean> traceList) {
        this.traceList = traceList;
    }

    @Override
    public String toString() {
        return "SendTraceListBean{" +
                "traceList=" + traceList +
                '}';
    }
}
