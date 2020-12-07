package com.hase.competition.beans;

import java.util.List;

public class SendTraceBean {
    private String traceId;
    private List<String> spanList;

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public List<String> getSpanList() {
        return spanList;
    }

    public void setSpanList(List<String> spanList) {
        this.spanList = spanList;
    }
}
