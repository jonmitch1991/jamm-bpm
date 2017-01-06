package org.tmr.jamm;

import org.jbpm.workflow.instance.node.WorkItemNodeInstance;
import org.kie.api.definition.process.Process;
import org.kie.api.runtime.process.ProcessInstance;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;


@Path("/jbpm")
@Produces(MediaType.TEXT_PLAIN)
public class BpmService {

    @Inject
    ProcessEngine engine;

    @GET
    public Response ping() {
        ArrayList<String> processIdList = new ArrayList<String>();
        for(Process p : engine.getProcesses()) {
            processIdList.add("Process Id: "+p.getId()+"| Process Name: "+p.getName());
        }
        if (processIdList.isEmpty()) {
            processIdList.add("No Processes Loaded!");
        }
        return Response.ok(processIdList).build();
    }

    @GET
    @Path("/start/{processId}")
    public Response start(@PathParam("processId") String processId) {
        return Response.ok("Instance Id: "+engine.startProcess(processId)).build();
    }

    @GET
    @Path("/stop/{instanceId}")
    public Response stop(@PathParam("instanceId") long instanceId) {
        engine.stopProcess(instanceId);
        return Response.ok().build();
    }

    @GET
    @Path("/list")
    public Response list() {
        ArrayList<String> instanceIdList = new ArrayList<String>();
        for(ProcessInstance i : engine.getProcessInstances()) {
            instanceIdList.add("Instance Id: "+i.getId()+"| Process Id: "+i.getProcessId()+"| State: "+Util.decodeProcessState(i.getState()));
        }
        if (instanceIdList.isEmpty()) {
            instanceIdList.add("No Process Instances!");
        }
        return Response.ok(instanceIdList).build();
    }

    @GET
    @Path("/tasks/{instanceId}")
    public Response taskList(@PathParam("instanceId") long instanceId) {
        ArrayList<String> taskInfoList = new ArrayList<String>();
        for (WorkItemNodeInstance task : engine.getTasksForProcessInstance(instanceId) ) {
            taskInfoList.add("Work Item Id: "+task.getWorkItemId()+"| Task Name: "+task.getNodeName()+"| Task State: "+Util.decodeTaskState(task.getWorkItem().getState()));
        }
        return Response.ok(taskInfoList).build();
    }

    @GET
    @Path("/complete/{workItemId}")
    public Response completeTask(@PathParam("workItemId") long workItemId) {
        engine.getKieSession().getWorkItemManager().completeWorkItem(workItemId, new HashMap<String, Object>());
        return Response.ok("Done!").build();
    }

}
