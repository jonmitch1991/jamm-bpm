package org.tmr.jamm;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jbpm.process.instance.impl.demo.DoNothingWorkItemHandler;
import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.jbpm.workflow.instance.node.WorkItemNodeInstance;
import org.kie.api.KieBase;
import org.kie.api.definition.process.Process;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.manager.InternalRuntimeManager;
import org.kie.internal.runtime.manager.context.EmptyContext;

@ApplicationScoped
public class ProcessEngine {

    @Inject
    private RuntimeManagerFactory runtimeManagerFactory;

    private RuntimeManager runtimeManager;

    @PostConstruct
    void init() {
        runtimeManagerFactory.toString();

        runtimeManager = runtimeManagerFactory.newSingletonRuntimeManager(
                            RuntimeEnvironmentBuilder.Factory.get()
                                //.newDefaultInMemoryBuilder()
                                // We use an 'EmptyBuilder' because the DefaultBuilder uses persistence for audit records
                                // even when persistence is 'false' <sigh>
                                .newEmptyBuilder()
                                .addAsset(ResourceFactory.newClassPathResource("process.bpmn"), ResourceType.BPMN2)
                                .persistence(false)
                                .get()
                        );

        // Here we define a custom Task Handler. In standard jBPM there is no 'Manual Task' only a 'Human Task'
        // So we add a handler that does 'nothing' letting us complete tasks in code.
        WorkItemManager wim = runtimeManager.getRuntimeEngine(EmptyContext.get()).getKieSession().getWorkItemManager();
        wim.registerWorkItemHandler("Manual Task", new DoNothingWorkItemHandler());
    }

    /**
     * Returns all available process definitions for this process engine
     * @return
     */
    public Collection<org.kie.api.definition.process.Process> getProcesses() {

        return getKieBase().getProcesses();
    }

    /**
     * Starts a new process instance
     * @return
     */
    public long startProcess(String processId) {
        Process p = getKieBase().getProcess(processId);
        if (p == null) {
            return -1;
        }
        return getKieSession().startProcess(processId).getId();
    }

    /**
     * Stops a process instance
     * @return
     */
    public boolean stopProcess(long instanceId) {
        boolean result = false;
        try {
            getKieSession().abortProcessInstance(instanceId);
            result = true;
        } catch (IllegalArgumentException e) {
            // do nothing
        }
        return result;
    }

    /**
     * Returns all available process instances for this process engine
     * @return
     */
    public Collection<ProcessInstance> getProcessInstances() {
         return getKieSession().getProcessInstances();
    }

    /**
     * Returns current tasks for this process instance
     * @return
     */
    public Collection<WorkItemNodeInstance> getTasksForProcessInstance(long instanceId) {
        ArrayList<WorkItemNodeInstance> tasks = new  ArrayList<WorkItemNodeInstance>();
        WorkflowProcessInstance wpi = (WorkflowProcessInstance) getKieSession().getProcessInstance(instanceId);
        for (NodeInstance node : wpi.getNodeInstances()) {
            if (node instanceof WorkItemNodeInstance) {
                tasks.add((WorkItemNodeInstance) node);
            }
        }

        return tasks;
    }


    /**
     * Returns <code>RuntimeManager</code> for this process engine
     * @return
     */
    public RuntimeManager getRuntimeManager() {

        return runtimeManager;
    }

    /**
     * Returns <code>KieSession</code> for this process engine
     * @return
     */
    public KieSession getKieSession() {

        return ((InternalRuntimeManager)runtimeManager).getRuntimeEngine(EmptyContext.get()).getKieSession();
    }

    /**
     * Returns <code>KieBase</code> for this process engine
     * @return
     */
    public KieBase getKieBase() {

        return ((InternalRuntimeManager)runtimeManager).getEnvironment().getKieBase();
    }

}
