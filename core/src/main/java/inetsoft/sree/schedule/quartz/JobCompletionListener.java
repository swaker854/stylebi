/*
 * inetsoft-core - StyleBI is a business intelligence web application.
 * Copyright © 2024 InetSoft Technology (info@inetsoft.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package inetsoft.sree.schedule.quartz;

import inetsoft.sree.internal.SUtil;
import inetsoft.sree.internal.cluster.Cluster;
import inetsoft.sree.schedule.Scheduler;
import inetsoft.sree.schedule.*;
import inetsoft.sree.security.IdentityID;
import inetsoft.uql.util.Identity;
import inetsoft.util.*;
import inetsoft.util.audit.ActionRecord;
import inetsoft.util.audit.Audit;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Quartz job listener that updates the status table and handles triggering
 * jobs for tasks that use a completion condition.
 *
 * @since 12.2
 */
public class JobCompletionListener extends JobListenerSupport {
   public JobCompletionListener(String name) {
      if(name == null) {
         throw new IllegalArgumentException("The listener name cannot be null");
      }

      this.name = name;
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public void jobToBeExecuted(JobExecutionContext context) {
      try {
         JobKey jobKey = context.getJobDetail().getKey();
         String taskName = jobKey.getName();
         TaskActivity activity = new TaskActivity(taskName);
         Scheduler scheduler = Scheduler.getScheduler();
         Catalog catalog = Catalog.getCatalog();
         scheduler.updateRunning(jobKey, activity, null, context, catalog);
         scheduler.updateNextRun(jobKey, activity, true, catalog);
         TaskActivityMessage message = new TaskActivityMessage();
         message.setTaskName(taskName);
         message.setActivity(activity);
         Cluster.getInstance().sendMessage(message);
      }
      catch(Exception e) {
         LOG.warn("Failed to notify cluster of task starting", e);
      }
   }

   @Override
   public void jobWasExecuted(JobExecutionContext context,
                              JobExecutionException jobException)
   {
      JobKey key = context.getJobDetail().getKey();
      String taskName = key.getName();

      long lastRunTime = context.getFireTime().getTime();
      long lastRunEndTime = lastRunTime + context.getJobRunTime();
      String error = null;
      Scheduler.Status status;

      if(Boolean.TRUE.equals(
         context.getJobDetail().getJobDataMap().get("inetsoft.cancelled")))
      {
         status = Scheduler.Status.INTERRUPTED;
      }
      else if(jobException == null) {
         status = Scheduler.Status.FINISHED;
      }
      else {
         status = Scheduler.Status.FAILED;

         StringWriter buffer = new StringWriter();
         PrintWriter writer = new PrintWriter(buffer);
         jobException.printStackTrace(writer); //NOSONAR
         writer.close();
         error = buffer.toString();
      }

      boolean runNow = context.getMergedJobDataMap().getBoolean("runNow");
      Principal principal = null;

      try {
         //Bug #29106, Add record to audit and show it in Schedule History audit report.
         String actionName = ActionRecord.ACTION_NAME_FINISH;
         String objectType = ActionRecord.OBJECT_TYPE_TASK;
         ScheduleTask taskValue = (ScheduleTask)
            context.getJobDetail().getJobDataMap().get(ScheduleTask.class.getName());
         Identity identity = taskValue != null ? taskValue.getIdentity() : null;
         IdentityID owner = taskValue != null ? taskValue.getOwner() : null;
         String addr = Tool.getIP();

         // Bug #40798, don't audit logins for internal tasks
         if(!ScheduleManager.isInternalTask(taskValue.getName())) {
            if(identity == null) {
               principal = SUtil.getPrincipal(owner, addr, true);
            }
            else {
               principal = SUtil.getPrincipal(identity, addr, true);
            }
         }

         ActionRecord finishActionRecord = SUtil.getActionRecord(
            principal, actionName, taskName, objectType);

         if(finishActionRecord != null) {
            String seeScheduleLog = Catalog.getCatalog().getString("em.task.runStatus");
            String actionError = status ==Scheduler.Status.FAILED ?
               jobException.getMessage() + "." + seeScheduleLog : null;
            finishActionRecord.setActionStatus(status == Scheduler.Status.FINISHED ?
                                                  ActionRecord.ACTION_STATUS_SUCCESS :
                                                  ActionRecord.ACTION_STATUS_FAILURE);
            finishActionRecord.setActionError(status == Scheduler.Status.FINISHED ?
                                                 seeScheduleLog : actionError);
            Audit.getInstance().auditAction(finishActionRecord, principal);
         }

         if(status == Scheduler.Status.FAILED) {
            LOG.debug("Failed to run task " + taskName + ": " + error, jobException);
         }

         ScheduleStatusDao dao = ScheduleStatusDao.getInstance();
         ScheduleStatusDao.Status previousStatus = dao.getStatus(taskName);
         ScheduleStatusDao.Status newStatus =
            dao.setStatus(taskName, status, lastRunTime, lastRunEndTime, error, runNow);

         try {
            Catalog catalog = Catalog.getCatalog();
            TaskActivity activity = new TaskActivity(taskName);
            Scheduler scheduler = Scheduler.getScheduler();
            scheduler.updateNextRun(key, activity, false, catalog);
            scheduler.updateLastRun(activity, newStatus, catalog);
            TaskActivityMessage message = new TaskActivityMessage();
            message.setTaskName(taskName);
            message.setActivity(activity);
            Cluster.getInstance().sendMessage(message);
         }
         catch(Exception e) {
            LOG.warn("Failed to notify cluster of task completion", e);
         }

         Map<String, Long> statusCache = new HashMap<>();

         for(JobKey jobKey : context.getScheduler()
             .getJobKeys(GroupMatcher.jobGroupEquals(Scheduler.GROUP_NAME)))
         {
            JobDetail jobDetail = context.getScheduler().getJobDetail(jobKey);
            ScheduleTask task = (ScheduleTask) jobDetail.getJobDataMap()
               .get(ScheduleTask.class.getName());

            if(!task.isEnabled()) {
               continue;
            }

            boolean isDependant = false;

            for(Enumeration<String> e = task.getDependency(); e.hasMoreElements(); )
            {
               if(e.nextElement().equals(taskName)) {
                  isDependant = true;
                  break;
               }
            }

            if(isDependant) {
               isDependant = task.getConditionStream().anyMatch(cond -> cond instanceof CompletionCondition &&
                  ((CompletionCondition) cond).getTaskName().equals(taskName));
            }

            if(isDependant) {
               boolean triggerDependant = true;

               for(Enumeration<String> e = task.getDependency(); e.hasMoreElements(); )
               {
                  String parentTaskName = e.nextElement();
                  Long parentEndTime = null;

                  if(statusCache.containsKey(parentTaskName)) {
                     parentEndTime = statusCache.get(parentTaskName);
                  }
                  else {
                     ScheduleStatusDao.Status parentStatus =
                        dao.getStatus(parentTaskName);

                     if(parentStatus != null &&
                        parentStatus.getStatus() == Scheduler.Status.FINISHED)
                     {
                        parentEndTime = parentStatus.getEndTime();
                     }

                     statusCache.put(parentTaskName, parentEndTime);
                  }

                  if(parentEndTime == null || previousStatus != null &&
                     previousStatus.getStartTime() > parentEndTime)
                  {
                     // failed or hasn't run since the previous execution
                     triggerDependant = false;
                     break;
                  }
               }

               if(triggerDependant) {
                  try {
                     context.getScheduler().triggerJob(
                        new JobKey(task.getName(), Scheduler.GROUP_NAME));
                  }
                  catch(SchedulerException e) {
                     LOG.error("Failed to trigger dependent job '" +
                        task.getName() + "'", e);
                  }
               }
            }
         }
      }
      catch(Exception e) {
         LOG.error("Failed to find dependant tasks", e);
      }

      if(context.getTrigger() instanceof TimeConditionTrigger) {
         TimeCondition condition =
            ((TimeConditionTrigger) context.getTrigger()).getCondition();

         if(condition.getType() == TimeCondition.AT) {
            try {
               context.getScheduler().unscheduleJob(context.getTrigger().getKey());
            }
            catch(SchedulerException e) {
               LOG.warn("Failed to remove run once trigger", e);
            }
         }
      }
      else if(context.getTrigger() instanceof SimpleTrigger) {
         try {
            context.getScheduler().unscheduleJob(context.getTrigger().getKey());
         }
         catch(SchedulerException ex) {
            LOG.warn("Failed to remove run now trigger", ex);
         }
      }

      ScheduleTask task = (ScheduleTask) context.getJobDetail().getJobDataMap()
         .get(ScheduleTask.class.getName());

      if(task != null && status == Scheduler.Status.FINISHED && task.isDeleteIfNoMoreRun() &&
         !task.hasNextRuntime(System.currentTimeMillis()))
      {
         // make sure task is removed on all nodes
         Cluster.getInstance().submitAll(new DeleteTask(taskName, principal));

         try {
            context.getScheduler().deleteJob(key);
         }
         catch(Exception e) {
            LOG.warn("Failed to remove schedule job", e);
         }
      }
   }

   @Override
   public void jobExecutionVetoed(JobExecutionContext context) {
      super.jobExecutionVetoed(context);

      ScheduleStatusDao dao = ScheduleStatusDao.getInstance();
      JobKey key = context.getJobDetail().getKey();
      String taskName = key.getName();

      try {
         Catalog catalog = Catalog.getCatalog();
         TaskActivity activity = new TaskActivity(taskName);
         Scheduler scheduler = Scheduler.getScheduler();
         scheduler.updateNextRun(key, activity, false, catalog);
         scheduler.updateLastRun(activity, dao.getStatus(taskName), catalog);
         TaskActivityMessage message = new TaskActivityMessage();
         message.setTaskName(taskName);
         message.setActivity(activity);
         Cluster.getInstance().sendMessage(message);
      }
      catch(Exception e) {
         LOG.warn("Failed to notify cluster of task veto", e);
      }
   }

   private static class DeleteTask implements Callable<Object>, Serializable {
      public DeleteTask(String taskName, Principal principal) {
         this.taskName = taskName;
         this.principal = principal;
      }

      @Override
      public Object call() {
         try {
            if(principal != null) {
               ThreadContext.setContextPrincipal(principal);
            }

            ScheduleManager.getScheduleManager().removeScheduleTask(taskName, null);
         }
         catch(Exception e) {
            LOG.warn("Failed to remove schedule task", e);
         }

         return null;
      }

      private final String taskName;
      private final Principal principal;
   }

   private final String name;
   private static final Logger LOG = LoggerFactory.getLogger(JobCompletionListener.class);
}