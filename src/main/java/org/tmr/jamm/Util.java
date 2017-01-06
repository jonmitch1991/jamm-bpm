package org.tmr.jamm;

import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;

public class Util {

    private Util() {}

    public static final String decodeProcessState(int state) {
        switch (state) {
            case ProcessInstance.STATE_PENDING:
                return "PENDING";
            case ProcessInstance.STATE_ACTIVE:
                return "ACTIVE";
            case ProcessInstance.STATE_COMPLETED:
                return "COMPLETED";
            case ProcessInstance.STATE_ABORTED:
                return "ABORTED";
            case ProcessInstance.STATE_SUSPENDED:
                return "SUSPENDED";
            default:
                return "UNKNOWN";
        }
    }

    public static final String decodeTaskState(int state) {
        switch (state) {
            case WorkItem.PENDING:
                return "PENDING";
            case WorkItem.ACTIVE:
                return "ACTIVE";
            case WorkItem.COMPLETED:
                return "COMPLETED";
            case WorkItem.ABORTED:
                return "ABORTED";
            default:
                return "UNKNOWN";
        }
    }
}
