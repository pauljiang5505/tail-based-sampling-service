package com.hase.competition.beans;

import java.util.Set;

public class ReceiveSpanBean {
    private Set<String> wrongTraceIds;
    private int batchPos;

    public Set<String> getWrongTraceIds() {
        return wrongTraceIds;
    }

    public void setWrongTraceIds(Set<String> wrongTraceIds) {
        this.wrongTraceIds = wrongTraceIds;
    }

    public int getBatchPos() {
        return batchPos;
    }

    public void setBatchPos(int batchPos) {
        this.batchPos = batchPos;
    }
}
