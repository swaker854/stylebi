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
package inetsoft.sree.schedule;

import java.security.Principal;
import java.util.List;

/**
 * Task listener is invoked when a task is executed. To add a listener to
 * schedule tasks, define a property, schedule.task.listener, with the
 * fully qualified name of the listener class.
 *
 * @version 11.4, 10/18/2012
 * @author InetSoft Technology Corp
 */
public interface TaskListener extends java.util.EventListener {
   /**
    * Invoked when a task is about to be started.
    * @param task the executing task.
    * @param user the principal used for executing the actions.
    */
   public void taskStarted(ScheduleTask task, Principal user);

   /**
    * Invoked when a task is finished.
    * @param task the executing task.
    * @param user the principal used for executing the actions.
    * @param errors the errors generated by the actions.
    */
   public void taskCompleted(ScheduleTask task, Principal user, 
                             List<Throwable> errors);
}

