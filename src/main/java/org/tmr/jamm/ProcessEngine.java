package org.tmr.jamm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.InitialContext;

import org.jbpm.bpmn2.handler.ServiceTaskHandler;
import org.jbpm.process.instance.impl.demo.DoNothingWorkItemHandler;
import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.jbpm.workflow.instance.node.WorkItemNodeInstance;
import org.kie.api.KieBase;
import org.kie.api.definition.process.Process;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeEnvironment;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.api.task.TaskService;
import org.kie.api.task.UserGroupCallback;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.manager.InternalRuntimeManager;
import org.kie.internal.runtime.manager.context.EmptyContext;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;

@ApplicationScoped
public class ProcessEngine {

	@Inject
	private RuntimeManagerFactory runtimeManagerFactory;

	private RuntimeManager runtimeManager;

	@PostConstruct
	void init() {
		runtimeManagerFactory.toString();
		
		RuntimeEnvironment env = RuntimeEnvironmentBuilder.Factory.get().newEmptyBuilder()
				.userGroupCallback(new UserGroupCallback() {
					@Override
					public boolean existsUser(String userId) {
						return true;
					}

					@Override
					public boolean existsGroup(String groupId) {
						return true;
					}

					@Override
					public List<String> getGroupsForUser(String userId, List<String> groupIds,
							List<String> allExistingGroupIds) {
						List<String> result = new ArrayList<String>();
						if ("applicant".equals(userId)) {
							result.add("applicants");
						}
						return result;
					}
				}).addAsset(ResourceFactory.newClassPathResource("process.bpmn"), ResourceType.BPMN2).persistence(false)
				.get();

		runtimeManager = runtimeManagerFactory.newSingletonRuntimeManager(env);

		// Here we define a custom Task Handler. In standard jBPM there is no
		// 'Manual Task' only a 'Human Task'
		// So we add a handler that does 'nothing' letting us complete tasks in
		// code.
		/*
		 * WorkItemManager wim =
		 * runtimeManager.getRuntimeEngine(EmptyContext.get()).getKieSession().
		 * getWorkItemManager(); wim.registerWorkItemHandler("Manual Task", new
		 * DoNothingWorkItemHandler());
		 */

		WorkItemManager wim = runtimeManager.getRuntimeEngine(EmptyContext.get()).getKieSession().getWorkItemManager();
		wim.registerWorkItemHandler("Human Task", new DoNothingWorkItemHandler());
		

	}

	/**
	 * Returns all available process definitions for this process engine
	 * 
	 * @return
	 */
	public Collection<org.kie.api.definition.process.Process> getProcesses() {

		return getKieBase().getProcesses();
	}

	/**
	 * Starts a new process instance
	 * 
	 * @return
	 */
	public long startProcess(String processId) {
		Process p = getKieBase().getProcess(processId);
		if (p == null) {
			return -1;
		}
		return getKieSession().startProcess(processId, new HashMap<String, Object>()).getId();
	}

	public String submitCV(String instanceId, Map<String, Object> params) {		
		RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(ProcessInstanceIdContext.get(Long.valueOf(instanceId)));		
		TaskService taskService = runtimeEngine.getTaskService();

		List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner("applicant", "en-UK");
		TaskSummary task = null;

		for (TaskSummary taskSummary : tasks) {
			if (taskSummary.getName().equals("Submit CV")) {
				task = taskSummary;
				break;
			}
		}

		taskService.claim(task.getId(), "applicant");
		taskService.start(task.getId(), "applicant");

		taskService.complete(task.getId(), "applicant", params);

		return (String) params.get("cv");
	}

	/**
	 * Stops a process instance
	 * 
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
	 * 
	 * @return
	 */
	public Collection<ProcessInstance> getProcessInstances() {
		return getKieSession().getProcessInstances();
	}

	/**
	 * Returns current tasks for this process instance
	 * 
	 * @return
	 */
	public Collection<WorkItemNodeInstance> getTasksForProcessInstance(long instanceId) {
		ArrayList<WorkItemNodeInstance> tasks = new ArrayList<WorkItemNodeInstance>();
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
	 * 
	 * @return
	 */
	public RuntimeManager getRuntimeManager() {

		return runtimeManager;
	}

	/**
	 * Returns <code>KieSession</code> for this process engine
	 * 
	 * @return
	 */
	public KieSession getKieSession() {

		return ((InternalRuntimeManager) runtimeManager).getRuntimeEngine(EmptyContext.get()).getKieSession();
	}

	/**
	 * Returns <code>KieBase</code> for this process engine
	 * 
	 * @return
	 */
	public KieBase getKieBase() {

		return ((InternalRuntimeManager) runtimeManager).getEnvironment().getKieBase();
	}

}
