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

import inetsoft.sree.*;
import inetsoft.sree.internal.SUtil;
import inetsoft.sree.internal.cluster.Cluster;
import inetsoft.sree.security.*;
import inetsoft.uql.asset.*;
import inetsoft.uql.util.Identity;
import inetsoft.util.*;
import inetsoft.web.RecycleUtils;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

/**
 * Schedule manager manages schedule tasks. The schedule task might be a normal
 * schedule task or comes from a schedule extension. Scheduler will load and
 * execute the schedule tasks contained in the schedule manager.
 *
 * @version 7.0
 * @author InetSoft Technology Corp
 */
@SingletonManager.Singleton(ScheduleManager.Reference.class)
public class ScheduleManager {
   /**
    * Get the schedule manager.
    */
   public static ScheduleManager getScheduleManager() {
      return SingletonManager.getInstance(ScheduleManager.class);
   }

   public static boolean isInternalTask(String taskName) {
      return InternalScheduledTaskService.isInternalTask(taskName);
   }

   /**
    * @return the names of the internal tasks that have the write permission.
    */
   public static List<String> getWriteableInternalTaskNames() {
      return Arrays.asList(InternalScheduledTaskService.ASSET_FILE_BACKUP);
   }

   /**
    * Create a schedule manager.
    */
   public ScheduleManager() {
      extensionLock = Cluster.getInstance().getLock(EXTENSION_LOCK);
   }

   /**
    * Reload extensions. The old schedule tasks come from schedule extensions
    * will be discarded and new schedule tasks come from schedule extensions
    * will be loaded.
    */
   public void reloadExtensions() {
      boolean scheduler = "true".equals(System.getProperty("ScheduleServer"));
      extensionLock.lock();

      try {
         ScheduleClient client = ScheduleClient.getScheduleClient();
         Map<ExtTaskKey, ScheduleTask> oldExtensionTasks = new HashMap<>(extensionTasks);
         extensionTasks.clear();

         // get ext tasks from the exts
         for(ScheduleExt ext : extensions) {
            List<ScheduleTask> tasks = new ArrayList<>(ext.getTasks());

            for(ScheduleTask task : tasks) {
               ExtTaskKey key = createExtensionTaskKey(task);

               if(!extensionTasks.containsKey(key)) {
                  ScheduleTask oldTask = oldExtensionTasks.remove(key);
                  extensionTasks.put(key, task);

                  if(!scheduler && !task.equals(oldTask)) {
                     try {
                        client.taskAdded(task);
                     }
                     catch(RemoteException e) {
                        LOG.error("Failed to update scheduler with extension task: " +
                           task.getName(), e);
                     }
                  }
               }
               else {
                  LOG.warn("Duplicate task found, not added: " + task);
               }
            }
         }

         if(!scheduler) {
            for(ExtTaskKey taskKey : oldExtensionTasks.keySet()) {
               // task is no longer in the new task list, remove it
               try {
                  client.taskRemoved(taskKey.name);
               }
               catch(Exception e) {
                  LOG.error("Failed to remove extension task: " + taskKey.name, e);
               }
            }
         }
      }
      finally {
         extensionLock.unlock();
      }
   }


   private ExtTaskKey createExtensionTaskKey(ScheduleTask task) {
      String name = task.getName();
      String orgId;

      if(task.getCycleInfo() != null) {
         orgId = task.getCycleInfo().getOrgId();
      }
      else {
         orgId = OrganizationManager.getInstance().getCurrentOrgID();
      }

      return new ExtTaskKey(name, orgId);
   }

   private boolean updateExtensionEnabled(ScheduleTask task) {
      boolean changed = false;
      String orgId;
      if(task.getCycleInfo() != null) {
         orgId = task.getCycleInfo().getOrgId();
      }
      else {
         orgId = OrganizationManager.getInstance().getCurrentOrgID();
      }

      for(ScheduleExt ext : extensions) {
         // for a schedule task in a schedule extension, we
         // should not save it but only change enable option
         if(ext.containsTask(task.getName(), orgId)) {
            if(ext.isEnable(task.getName(), orgId) != task.isEnabled())
            {
               ext.setEnable(task.getName(), orgId, task.isEnabled());
               changed = true;
            }

            break;
         }
      }

      return changed;
   }

   /**
    * Save the all the schedule tasks.
    * @param tasks all the schedule tasks which might be changed.
    */
   public synchronized void save(Collection<ScheduleTask> tasks, String orgID) throws Exception {
      boolean extChanged = false;

      try {
         for(ScheduleTask task : tasks) {
            if(updateExtensionEnabled(task)) {
               extChanged = true;
               continue;
            }

            ScheduleTaskMessage.Action action;

            if(this.tasks.containsKey(getTaskIdentifier(task.getName(), orgID))) {
               action = ScheduleTaskMessage.Action.MODIFIED;
            }
            else {
               action = ScheduleTaskMessage.Action.ADDED;
            }

            this.tasks.put(getTaskIdentifier(task.getName(), orgID), task);
            ScheduleClient.getScheduleClient().taskAdded(task);
            ScheduleTaskMessage message = new ScheduleTaskMessage();
            message.setTaskName(task.getName());
            message.setTask(task);
            message.setAction(action);
            Cluster.getInstance().sendMessage(message);
         }
      }
      catch(Throwable exc) {
         throw new Exception("Failed to save schedule.xml", exc);
      }

      if(extChanged) {
         reloadExtensions();
      }
   }

   /**
    * Save the all the schedule tasks.
    * @param tasks all the schedule tasks which might be changed.
    */
   public synchronized void save(Collection<ScheduleTask> tasks) throws Exception {
      this.save(tasks, null);
   }

   /**
    * Get schedule tasks.
    */
   public Vector<ScheduleTask> getScheduleTasks() {
      Vector<ScheduleTask> list = new Vector<>(tasks.values());
      extensionLock.lock();

      try {
         for(ScheduleTask task : getFilteredExtensionTasks(OrganizationManager.getInstance().getCurrentOrgID())) {
            if(list.stream().noneMatch(t -> t != null && t.getName().equals(task.getName()))) {
               list.add(task);
            }
         }
      }
      finally {
         extensionLock.unlock();
      }

      for(int i = list.size() - 1; i >= 0; i--) {
         if(list.get(i) == null) {
            list.remove(i);
         }
      }

      return list;
   }

   /**
    * Get schedule tasks.
    */
   public Vector<ScheduleTask> getScheduleTasks(AssetEntry[] taskEntries, boolean loadExtension, boolean loadInternal) {
      Set<String> allNames =
         Arrays.stream(taskEntries).map(AssetEntry::getName).collect(Collectors.toSet());
      Vector<ScheduleTask> list = new Vector<>(tasks.values());

      for(int i = list.size() - 1; i >= 0; i--) {
         if(list.get(i) == null || (!allNames.contains(list.get(i).getName()) &&
            !(loadInternal && isInternalTask(list.get(i).getName()))))
         {
            list.remove(i);
         }
      }

      if(loadExtension) {
         extensionLock.lock();

         try {
            for(ScheduleTask task : getFilteredExtensionTasks(OrganizationManager.getInstance().getCurrentOrgID())) {
               if(list.stream().noneMatch(t -> t != null && t.getName().equals(task.getName())) &&
                  !allNames.contains(task.getName()))
               {
                  list.add(task);
               }
            }
         }
         finally {
            extensionLock.unlock();
         }
      }

      return list;
   }

   /**
    * Get a schedule task with a schedule task name.
    */
   public synchronized ScheduleTask getScheduleTask(String taskName) {
      return getScheduleTask(taskName, null);
   }

   public synchronized ScheduleTask getScheduleTask(String taskName, String orgID) {
      if(orgID == null) {
         orgID = OrganizationManager.getInstance().getCurrentOrgID();
      }

      ScheduleTask task = extensionTasks.get(new ExtTaskKey(taskName, orgID));

      if(InternalScheduledTaskService.isInternalTask(taskName)) {
         orgID = Organization.getDefaultOrganizationID();
      }

      if(task == null) {
         task = tasks.get(getTaskIdentifier(taskName, orgID), orgID);
      }

      // older version (pre 13.1) doesn't have user name as part of the task name (48791).
      if(task == null && taskName.contains(":")) {
         task = tasks.get(getTaskIdentifier(taskName.substring(taskName.indexOf(':') + 1), orgID));
      }

      return task;
   }

   /**
    * Get all the schedule task activities.
    */
   public Map<String, TaskActivity> getScheduleActivities() {
      Map<String, TaskActivity> result;

      try {
         result = ScheduleClient.getScheduleClient().getScheduleActivities();
      }
      catch(RemoteException e) {
         LOG.error("Failed to get schedule activities", e);
         result = Collections.emptyMap();
      }

      return result;
   }

   /**
    * Add a schedule extension.
    */
   public void addScheduleExt(ScheduleExt ext) {
      if(!extensions.contains(ext)) {
         extensions.add(ext);
      }
   }

   /**
    * Get all the schedule extension.
    */
   public Vector<ScheduleExt> getExtensions() {
      return extensions;
   }

   /**
    * Gets the schedule extension tasks.
    *
    * @return the extension tasks.
    */
   public Vector<ScheduleTask> getExtensionTasks() {
      return new Vector<>(extensionTasks.values());
   }

   public Set<ScheduleTask> getFilteredExtensionTasks(String orgId) {
      final String orgId0;
      if(orgId == null) {
         orgId0 = OrganizationManager.getInstance().getCurrentOrgID();
      }
      else {
         orgId0 = orgId;
      }
      return extensionTasks.keySet()
         .stream()
         .filter(key -> Tool.equals(key.orgId, orgId))
         .map(extensionTasks::get)
         .collect(Collectors.toCollection(HashSet::new));
   }

   /**
    * Get all the tasks depends on the access permission.
    */
   public Vector<ScheduleTask> getScheduleTasks(Principal principal)
      throws Exception
   {
      RepletRepository engine = SUtil.getRepletRepository();
      String[] taskNames = engine.getScheduleTasks(principal);
      Vector<ScheduleTask> vec = new Vector<>();

      for(String taskName : taskNames) {
         ScheduleTask task = engine.getScheduleTask(taskName);

         if(task != null) {
            vec.addElement(task);
         }
      }

      return vec;
   }

   public Vector<ScheduleTask> getScheduleTasks(Principal principal,
                                                Collection<ScheduleTask> allTasks)
      throws Exception
   {
      RepletRepository engine = SUtil.getRepletRepository();
      String[] taskNames = engine.getScheduleTasks(principal, allTasks);
      Vector<ScheduleTask> vec = new Vector<>();

      for(String taskName : taskNames) {
         ScheduleTask task = engine.getScheduleTask(taskName, OrganizationManager.getInstance().getCurrentOrgID());

         if(task != null) {
            vec.addElement(task);
         }
      }

      return vec;
   }

   public synchronized void setScheduleTask(String taskName, ScheduleTask task,
                                            Principal principal)
           throws Exception
   {
      setScheduleTask(taskName, task, null, principal);
   }

   public synchronized void setScheduleTask(String taskName, ScheduleTask task, AssetEntry parent,
                                            Principal principal)
      throws Exception
   {
      setScheduleTask(taskName, task, parent, ScheduleManager.isInternalTask(taskName), principal);
   }

   /**
    * Save a schedule task.
    */
   public synchronized void setScheduleTask(String taskName, ScheduleTask task, AssetEntry parent,
                                            boolean internal, Principal principal)
      throws Exception
   {
      if(task == null) {
         return;
      }

      task.setLastModified(System.currentTimeMillis());

      final RepletRepository engine = internal ? null : SUtil.getRepletRepository();
      final String orgID;
      if(internal) {
         orgID = Organization.getDefaultOrganizationID();
      }
      else {
         orgID = OrganizationManager.getInstance().getCurrentOrgID();
      }

      if(principal == null) {
         String addr = null;

         try {
            addr = Tool.getIP();
         }
         catch(Exception e) {
            LOG.warn("Failed to get local IP address", e);
         }

         IdentityID clientID = new IdentityID(ClientInfo.ANONYMOUS, Organization.getDefaultOrganizationName());
         principal = new SRPrincipal(new ClientInfo(clientID, addr),
                                     new IdentityID[0], new String[0], null, 1L);
         ((SRPrincipal) principal).setProperty("__internal__", "true");
      }

      IdentityID user = principal == null ? null : IdentityID.getIdentityIDFromKey(principal.getName());

      // if a task is not removable, it's an internally created task. the schedule permission
      // is used to control whether a user can create schedule tasks (from the gui). internal
      // tasks (such as MV ondemand) are created without user intervention, and the originating
      // task (such as creating MV) is already controlled by a permission, so we shouldn't
      // check the permission here again.
      if(!internal && task.isRemovable() &&
         !engine.checkPermission(principal, ResourceType.SCHEDULER, "*", ResourceAction.ACCESS))
      {
         throw new IOException(user + " doesn't have schedule permission.");
      }

      // @by mikec, here we shouldn't modify the task's owner.
      // For example if a task was defined by a user A, we should
      // not change it's owner to 'Admin' when admin changed the task
      // in Enterprise Manager.
      // The rule here should be, any role have permisstion on the task
      // (currently it is the admin) should be able to change the task,
      // but the owner should not be changed.
      OrganizationManager organizationManager = OrganizationManager.getInstance();
      String currOrgName = organizationManager.getCurrentOrgName();

      if(user != null && !Tool.equals(user.getOrganization(), currOrgName)) {
         IdentityID[] orgUsers = SecurityEngine.getSecurity().getOrgUsers(currOrgName);

         if(orgUsers != null) {
            IdentityID orgAdmin = Arrays.stream(orgUsers)
               .filter(id -> organizationManager.isOrgAdmin(id))
               .findFirst()
               .orElse(null);
            user = orgAdmin == null ? user : orgAdmin;
         }
      }

      if(task.getOwner() == null) {
         task.setOwner(user);
      }

      ScheduleTaskMessage.Action action;

      if(tasks.containsKey(getTaskIdentifier(taskName, orgID), orgID)) {
         action = ScheduleTaskMessage.Action.MODIFIED;
         DependencyHandler.getInstance().updateTaskDependencies(tasks.get(getTaskIdentifier(taskName, orgID)), false);
      }
      else {
         action = ScheduleTaskMessage.Action.ADDED;
      }

      tasks.put(getTaskIdentifier(taskName, orgID), task, parent, orgID);
      DependencyHandler.getInstance().updateTaskDependencies(task, true);
      ScheduleClient.getScheduleClient().taskAdded(task);
      ScheduleTaskMessage message = new ScheduleTaskMessage();
      message.setTaskName(taskName);
      message.setTask(task);
      message.setAction(action);
      Cluster.getInstance().sendMessage(message);

      IdentityID owner = task.getOwner();

      try {
         if(!internal) {
            Permission perm = new Permission();
            String orgId = getTaskOrgID(taskName);
            Set<Permission.PermissionIdentity> users = Collections.singleton(new Permission.PermissionIdentity(owner.name, orgId));
            perm.setUserGrants(ResourceAction.READ, users);
            perm.setUserGrants(ResourceAction.WRITE, users);
            perm.setUserGrants(ResourceAction.DELETE, users);
            engine.setPermission(principal, ResourceType.SCHEDULE_TASK, task.getName(), perm);
         }
      }
      catch(SRSecurityException e) {
         LOG.error("Failed to set permission on scheduled task " +
               task.getName() + " for user " + owner, e);
      }
   }

   /**
    * Remove a schedule task.
    */
   public synchronized void removeScheduleTask(String taskName,
                                               Principal principal,
                                               boolean checkDependency)
      throws Exception
   {
      if(!checkDependency) {
         removeScheduleTask(taskName, principal);
      }
      else {
         Vector<ScheduleTask> allTasks = getScheduleTasks(principal);
         boolean dependency = hasDependency(allTasks, taskName);

         if(!dependency) {
            removeScheduleTask(taskName, principal);
         }
         else {
            if(taskName == null) {
               throw new Exception(Catalog.getCatalog().getString(
                  "designer.property.emptyNullError"));
            }
            else {
               throw new Exception(Catalog.getCatalog().getString(
                  "em.schedule.task.removeDependency", taskName));
            }
         }
      }
   }

   /**
    * Remove a schedule task.
    */
   public synchronized void removeScheduleTask(String taskName,
                                               Principal principal)
      throws Exception
   {
      String orgID = getTaskOrgID(taskName);
      RepletRepository engine = SUtil.getRepletRepository();
      ScheduleTask task = getScheduleTask(taskName, orgID);

      //possible site admin created in other organization
      if(task == null) {
         task = getScheduleTask(taskName, OrganizationManager.getInstance().getCurrentOrgID());
      }

      // for dynamically created tasks (e.g. mv background creation),
      // the task may not be in schedule manager so we should not
      // throw an exception and can just silently remove it.
      if(task != null) {
         if(!task.isRemovable()) {
            throw new IOException("Task is not removable: " + taskName);
         }

         boolean adminPermission = SecurityEngine.getSecurity().checkPermission(
            principal, ResourceType.SECURITY_USER, task.getOwner(), ResourceAction.ADMIN);

         if(!engine.checkPermission(
            principal, ResourceType.SCHEDULE_TASK, taskName, ResourceAction.DELETE) &&
            principal != null && !Tool.equals(principal.getName(), task.getOwner()) &&
            isDeleteOnlyByOwner(task, principal) && !adminPermission)
         {
            throw new IOException(principal.getName() +
                                  " doesn't have delete permission for: " +
                                  taskName);
         }
      }

      // check if is an ext task
      boolean ext = false;
      Vector<ScheduleExt> extensions = getExtensions();
      boolean extChanged = false;
      orgID = OrganizationManager.getInstance().getCurrentOrgID();

      for(ScheduleExt extension : extensions) {
         if(extension.containsTask(taskName, orgID)) {
            extChanged = extension.deleteTask(taskName);
            ext = true;
            break;
         }
      }

      // not an ext task? check if a normal task
      if(!ext) {
         if(task != null) {
            tasks.removeKey(getTaskIdentifier(task.getName(), orgID), orgID);
            DependencyHandler.getInstance().updateTaskDependencies(task, false);
         }

         ScheduleClient.getScheduleClient().taskRemoved(taskName);
         ScheduleTaskMessage message = new ScheduleTaskMessage();
         message.setTaskName(taskName);
         message.setAction(ScheduleTaskMessage.Action.REMOVED);
         Cluster.getInstance().sendMessage(message);
      }

      try {
         engine.setPermission(principal, ResourceType.SCHEDULE_TASK, taskName, null);
      }
      catch(Exception ex) {
         LOG.error("Failed to clear permissions for schedule task {}", taskName, ex);
      }

      if(extChanged) {
         reloadExtensions();
      }
   }

   private String getTaskOrgID(String taskName) {
      int index = taskName.indexOf(':');

      if(index == -1) {
         return null;
      }

      SecurityProvider provider = SecurityEngine.getSecurity().getSecurityProvider();

      String ownerName = taskName.substring(0, index);
      int idx  = ownerName.lastIndexOf(IdentityID.KEY_DELIMITER);
      ownerName = idx != -1 ? ownerName.substring(0, idx) : ownerName;
      IdentityID owner = new IdentityID(ownerName, OrganizationManager.getCurrentOrgName());
      User user = provider.getUser(owner);

      if(user == null) {
         return null;
      }

      return provider.getOrganization(user.getOrganization()).getOrganizationID();
   }

   /**
    * Whether the task just delete by owner.
    * @param task
    * @param principal
    * @return
    */
   public boolean isDeleteOnlyByOwner(ScheduleTask task, Principal principal) {
      if(!isShareInGroup()) {
         return true;
      }

      if(isDeleteByOwner()) {
         return true;
      }
      else {
         return !hasShareGroupPermission(task, principal);
      }
   }

   /**
    * Whether share task in group.
    * @return
    */
   public static boolean isShareInGroup() {
      return SreeEnv.getBooleanProperty("schedule.options.shareTaskInGroup");
   }

   /**
    * Whether only delete task by owner.
    * @return
    */
   public static boolean isDeleteByOwner() {
      return SreeEnv.getBooleanProperty("schedule.options.deleteTaskOnlyByOwner");
   }

   public static boolean hasShareGroupPermission(ScheduleTask task, Principal principal) {
      return hasShareGroupPermission(task.getOwner(), principal);
   }

   public static boolean hasShareGroupPermission(IdentityID owner, Principal principal) {
      if(!SecurityEngine.getSecurity().isSecurityEnabled()) {
         return false;
      }

      boolean isShareRole = SreeEnv.getBooleanProperty("schedule.options.shareTaskInGroup");

      if(isShareRole) {
         SecurityProvider securityProvider = SecurityEngine.getSecurity().getSecurityProvider();

         if(securityProvider == null) {
            return false;
         }

         String[] taskOwnerGroups = securityProvider.getUserGroups(owner);
         String[] userGroups = securityProvider.getUserGroups(IdentityID.getIdentityIDFromKey(principal.getName()));

         if(taskOwnerGroups == null || userGroups == null) {
            return false;
         }

         for(String taskOwnerGroup : taskOwnerGroups) {
            if(ArrayUtils.contains(userGroups, taskOwnerGroup)) {
               return true;
            }
         }

         return false;
      }

      return false;
   }

   /**
    * Check whether user has the specific owner task permission.
    * @param taskOwner task owner.
    * @param principal user.
    * @param action resource action
    * @return
    */
   public static boolean hasTaskPermission(IdentityID taskOwner, Principal principal, ResourceAction action){
      SecurityProvider securityProvider = SecurityEngine.getSecurity().getSecurityProvider();
      boolean adminPermission = securityProvider.checkPermission(
         principal, ResourceType.SECURITY_USER, taskOwner.convertToKey(), ResourceAction.ADMIN);

      if(taskOwner.equals(IdentityID.getIdentityIDFromKey(principal.getName())) || adminPermission) {
         return true;
      }

      if(!isShareInGroup()) {
         return false;
      }

      if(ResourceAction.DELETE.equals(action) && isDeleteByOwner()) {
         return false;
      }

      return hasShareGroupPermission(taskOwner, principal);
   }

   /**
    * Check task dependency for rename &amp; delete action.
    */
   public boolean hasDependency(Vector<ScheduleTask> allTasks, String taskName) {
      boolean dependency = false;

      for(Iterator<ScheduleTask> i = allTasks.iterator(); i.hasNext();) {
         ScheduleTask task = i.next();
         String name = task.getName();

         if(name.equals(taskName)) {
            i.remove();
            break;
         }
      }

      for(ScheduleTask task : allTasks) {
         String name = task.getName();
         task = getScheduleTask(name);
         Enumeration<String> en = task.getDependency();

         while(en.hasMoreElements()) {
            if(taskName.equals(en.nextElement())) {
               dependency = true;
               break;
            }
         }

         if(dependency) {
            break;
         }
      }

      return dependency;
   }

   public List<AssetObject> getDependentTasks(String target, String orgId) {
      String taskName = AssetEntry.createAssetEntry(target).getName();
      return tasks.entrySet(orgId).stream()
         .filter(entry -> isDependent(taskName, entry.getValue()))
         .map(Map.Entry::getKey)
         .map(identifier -> (AssetObject) AssetEntry.createAssetEntry(identifier))
         .toList();
   }

   private boolean isDependent(String target, ScheduleTask task) {
      Enumeration<String> en = task.getDependency();

      while(en.hasMoreElements()) {
         if(target.equals(en.nextElement())) {
            return true;
         }
      }

      return false;
   }

   /**
    * Method will be invoked when a user is removed.
    */
   public synchronized void identityRemoved(Identity identity, EditableAuthenticationProvider eprovider) {
      Set<ScheduleTask> changedTasks = new HashSet<>();
      int type = identity.getType();
      IdentityID identityID = identity.getIdentityID();
      String orgName;

      switch(identity.getType()) {
         case Identity.USER:
            orgName = eprovider.getUser(identityID).getOrganization();
            break;
         case Identity.GROUP:
            orgName = eprovider.getGroup(identityID).getOrganization();
            break;
         case Identity.ORGANIZATION:
            orgName = identityID.name;
            break;
         default:
            orgName = Organization.getDefaultOrganizationName();
            break;
      }

      String orgID = SecurityEngine.getSecurity().getSecurityProvider().getOrgId(orgName);
      Iterator<ScheduleTask> i = tasks.values().iterator();

      while(i.hasNext()) {
         ScheduleTask task = i.next();

         if(task == null) {
            continue;
         }

         if(type == Identity.USER && identityID.equals(task.getOwner())) {
            i.remove();
            continue;
         }

         Identity iden = task.getIdentity();

         if(iden != null && type == iden.getType() && identityID.equals(iden.getIdentityID())) {
            task.setIdentity(null);
            changedTasks.add(task);
         }

         for(int j = 0; j < task.getActionCount(); j++) {
            ScheduleAction action = task.getAction(j);
            updateNotifications(action, identityID, task, changedTasks);
         }
      }

      try {
         save(changedTasks, orgID);
      }
      catch(Exception ex) {
         LOG.error("Failed to save schedule task file after " +
               "identity was removed: " + identityID, ex);
      }

      for(ScheduleExt ext : extensions) {
         ext.identityRemoved(identity);
      }
   }

   /**
    * Method will be invoked when a user is renamed.
    */
   public synchronized void identityRenamed(IdentityID oname, Identity identity) {
      Set<ScheduleTask> changedTasks = new HashSet<>();
      int type = identity.getType();
      String name = identity.getName();
      IdentityID id = identity.getIdentityID();
      SecurityProvider securityProvider = SecurityEngine.getSecurity().getSecurityProvider();
      String orgID = identity instanceof FSOrganization ? securityProvider.getOrganizationId(oname.name) :
               identity.getOrganization() == null ? null :
               securityProvider.getOrganizationId(identity.getOrganization());

      for(ScheduleTask task : tasks.values()) {
         if(task == null) {
            continue;
         }

         if(type == Identity.USER && oname.equals(task.getOwner())) {
            this.tasks.remove(getTaskIdentifier(task.getName(), orgID));
            task.setName(task.getName().replaceFirst(oname.name, name));
            task.setOwner(id);
            changedTasks.add(task);
         }

         Identity iden = task.getIdentity();

         if(iden != null && type == iden.getType() && oname.equals(iden.getName())) {
            this.tasks.remove(getTaskIdentifier(task.getName(), orgID));
            task.setIdentity(identity);
            changedTasks.add(task);
         }

         for(int j = 0; j < task.getActionCount(); j++) {
            ScheduleAction action = task.getAction(j);
            updateNotifications(action, id, task, changedTasks);
         }
      }

      try {
         save(changedTasks, orgID);
      }
      catch(Exception ex) {
         LOG.error("Failed to save schedule task file after " +
               "identity was renamed: " + oname + " to " + name, ex);
      }

      for(ScheduleExt ext : extensions) {
         ext.identityRenamed(oname.name, identity);
      }
   }

   private void updateNotifications(ScheduleAction action, IdentityID id,
                                    ScheduleTask task,
                                    Set<ScheduleTask> changedTasks)
   {
      if(action instanceof AbstractAction) {
         AbstractAction aaction = (AbstractAction) action;

         if(aaction.getNotifications() != null && aaction.getNotifications().length() > 0) {
            String oldNotifies = aaction.getNotifications();
            String newNotifies = Tool.arrayToString(
               Tool.remove(Tool.split(oldNotifies, ','), id.name));

            if(!Tool.equals(oldNotifies, newNotifies)) {
               aaction.setNotifications(newNotifies);
               changedTasks.add(task);
            }
         }
      }
   }

   /**
    * Method will be invoked when asset is renamed.
    * @param oentry the specified old entry.
    * @param nentry the specified new entry.
    */
   public synchronized void assetRenamed(AssetEntry oentry, AssetEntry nentry) {
      if(nentry != null && RecycleUtils.isInRecycleBin(nentry.getPath())) {
         assetRemoved(oentry);
         return;
      }

      Set<ScheduleTask> changedTasks = new HashSet<>();

      for(ScheduleTask task : tasks.values()) {
         if(task == null) {
            continue;
         }

         for(int j = task.getActionCount() - 1; j >= 0; j--) {
            if((task.getAction(j) instanceof AssetSupport)) {
               AssetSupport action = (AssetSupport) task.getAction(j);
               AssetEntry entry2 = action.getEntry();

               if(entry2.equals(oentry)) {
                  action.setEntry(nentry);
                  LOG.debug(
                     "Schedule action in task " + task.getName() +
                        " is changed for asset " + oentry + " is renamed to " +
                        nentry);
                  changedTasks.add(task);
               }
            }
            else if(task.getAction(j) instanceof BatchAction) {
               BatchAction action = (BatchAction) task.getAction(j);
               AssetEntry queryEntry = action.getQueryEntry();

               if(Tool.equals(queryEntry, oentry)) {
                  action.setQueryEntry(nentry);
                  LOG.debug(
                     "Schedule action in task " + task.getName() +
                        " is changed for asset " + oentry + " is renamed to " +
                        nentry);
                  changedTasks.add(task);
               }
            }
         }
      }

      try {
         save(changedTasks, oentry.getOrgID());
      }
      catch(Exception ex) {
         LOG.error("Failed to save schedule task file after " +
               "asset was renamed: " + oentry + " to " + nentry, ex);
      }
   }

   /**
    * Method will be invoked when asset is removed.
    * @param entry the specified asset entry.
    */
   public synchronized void assetRemoved(AssetEntry entry) {
      // Bug #65532, optimization, ignore any assets other than ws or vs
      if(entry == null || !entry.isSheet()) {
         return;
      }

      Set<ScheduleTask> changedTasks = new HashSet<>();

      for(ScheduleTask task : tasks.values()) {
         if(task == null) {
            continue;
         }

         for(int j = task.getActionCount() - 1; j >= 0; j--) {
            if(!(task.getAction(j) instanceof AssetSupport)) {
               continue;
            }

            AssetSupport action = (AssetSupport) task.getAction(j);
            AssetEntry entry2 = action.getEntry();

            if(Tool.equals(entry, entry2)) {
               task.removeAction(j);
               LOG.debug(
                           "Schedule action in task " + task.getName() +
                           " is removed for asset " + entry + " is removed");
               changedTasks.add(task);
            }
         }
      }

      try {
         save(changedTasks, entry.getOrgID());
      }
      catch(Exception ex) {
         LOG.error("Failed to save schedule task file after " +
               "asset was removed: " + entry, ex);
      }
   }

   /**
    * Method will be invoked when a replet is removed.
    * @param replet the specified replet.
    * @param owner the specified user.
    */
   public synchronized void repletRemoved(String replet, String owner) {
      Set<ScheduleTask> changedTasks = new HashSet<>();

      try {
         save(changedTasks);
      }
      catch(Exception ex) {
         LOG.error("Failed to save schedule task file after " +
               "replet was removed: " + replet, ex);
      }

      boolean extchanged = false;

      for(ScheduleExt ext : extensions) {
         extchanged = ext.repletRemoved(replet, owner) || extchanged;
      }

      if(extchanged) {
         reloadExtensions();
      }
   }

   /**
    * Method will be invoked when an archive is renamed.
    * @param opath the specified old archive path.
    * @param npath the specified new archive path.
    * @param owner the specified user.
    */
   public synchronized void archiveRenamed(String opath, String npath,
                                           String owner)
   {
      Set<ScheduleTask> changedTasks = new HashSet<>();

      try {
         save(changedTasks);
      }
      catch(Exception ex) {
         LOG.error("Failed to save schedule task file after " +
               "archive file was renamed: " + opath + " to " + npath, ex);
      }

      boolean extchanged = false;

      for(ScheduleExt ext : extensions) {
         extchanged = ext.archiveRenamed(opath, npath, owner) || extchanged;
      }

      if(extchanged) {
         reloadExtensions();
      }
   }

   /**
    * Method will be invoked when a replet is renamed.
    * @param oreplet the specified old replet.
    * @param nreplet the specified new replet.
    * @param owner the specified user.
    */
   public synchronized void repletRenamed(String oreplet, String nreplet,
                                          String owner)
   {
      if(RecycleUtils.isInRecycleBin(nreplet)) {
         repletRemoved(oreplet, owner);
         return;
      }

      Set<ScheduleTask> changedTasks = new HashSet<>();

      try {
         save(changedTasks);
      }
      catch(Exception ex) {
         LOG.error("Failed to save schedule task file after " +
               "replet was renamed: " + oreplet + " to " + nreplet, ex);
      }

      boolean extchanged = false;

      for(ScheduleExt ext : extensions) {
         extchanged = ext.repletRenamed(oreplet, nreplet, owner) || extchanged;
      }

      if(extchanged) {
         reloadExtensions();
      }
   }

   /**
    * Method will be invoked when a viewsheet is renamed.
    * @param oviewSheet the specified old viewsheet.
    * @param nviewSheet the specified new viewsheet.
    * @param owner the specified user.
    */
   public synchronized void viewSheetRenamed(String oviewSheet,
                                             String nviewSheet, String owner)
   {
      AssetEntry nentry = AssetEntry.createAssetEntry(nviewSheet);
      AssetEntry oentry = AssetEntry.createAssetEntry(oviewSheet);

      if(nentry.isViewsheet() && RecycleUtils.isInRecycleBin(nentry.getPath())) {
         viewsheetRemoved(oentry);
         return;
      }

      // Bug #65532, optimization, ignore any assets other than ws or vs
      if(nentry == null || Tool.equals(oentry, nentry) || !nentry.isSheet()) {
         return;
      }

      Set<ScheduleTask> changedTasks = new HashSet<>();

      for(ScheduleTask task : tasks.values()) {
         if(task == null) {
            continue;
         }

         for(int j = task.getActionCount() - 1; j >= 0; j--) {
            if(!(task.getAction(j) instanceof ViewsheetSupport)) {
               continue;
            }

            ViewsheetSupport action = (ViewsheetSupport) task.getAction(j);
            String name = action.getViewsheetName();

            if(name.equals(oviewSheet) &&
               (!SUtil.isMyReport(name) || Tool.equals(task.getOwner(), owner)))
            {
               action.setViewsheetName(nviewSheet);
               LOG.debug(
                           "Schedule action in task " + task.getName() +
                              " is changed for viewsheet " + name + " is renamed to " +
                              nviewSheet);
               changedTasks.add(task);
            }
         }
      }

      try {
         save(changedTasks, nentry.getOrgID());
      }
      catch(Exception ex) {
         LOG.error("Failed to save schedule task file after " +
               "viewsheet was renamed: " + oviewSheet + " to " + nviewSheet, ex);
      }

      boolean extchanged = false;

      for(ScheduleExt ext : extensions) {
         extchanged =
            ext.viewsheetRenamed(oviewSheet, nviewSheet, owner) || extchanged;
      }

      if(extchanged) {
         reloadExtensions();
      }
   }

   public synchronized void viewsheetRemoved(AssetEntry entry) {
      Set<ScheduleTask> changedTasks = new HashSet<>();

      for(ScheduleTask task : tasks.values()) {
         if(task == null) {
            continue;
         }

         for(int j = task.getActionCount() - 1; j >= 0; j--) {
            if(!(task.getAction(j) instanceof ViewsheetSupport)) {
               continue;
            }

            ViewsheetSupport action = (ViewsheetSupport) task.getAction(j);
            String vsId = action.getViewsheetName();

            if(entry.toIdentifier().equals(vsId)) {
               task.removeAction(j);
               LOG.debug(
                  "Schedule action in task " + task.getName() +
                     " is removed after viewsheet " + entry + " is removed");
               changedTasks.add(task);
            }
         }
      }

      try {
         save(changedTasks, entry.getOrgID());
      }
      catch(Exception ex) {
         LOG.error("Failed to save schedule task file after " +
                      "viewsheet was removed: " + entry, ex);
      }
   }

   /**
    * Rename bookmark in schedule actions.
    */
   public void bookmarkRenamed(String oldName, String newName, String viewsheet, IdentityID user) {
      ScheduleManager manager = ScheduleManager.getScheduleManager();
      Vector<ScheduleTask> tasks = manager.getScheduleTasks();
      Set<ScheduleTask> changedTasks = new HashSet<>();

      for(ScheduleTask task : tasks) {
         for(int j = 0; j < task.getActionCount(); j++) {
            if(task.getAction(j) instanceof ViewsheetAction) {
               ViewsheetAction vact = (ViewsheetAction) task.getAction(j);
               String[] names = vact.getBookmarks();
               IdentityID[] userNames = vact.getBookmarkUsers();

               for(int i = 0; i < names.length; i ++) {
                  if(Tool.equals(names[i], oldName) && Tool.equals(userNames[i], user) &&
                     Objects.equals(viewsheet, vact.getViewsheet()))
                  {
                     names[i] = newName;
                     vact.setBookmarks(names);
                     changedTasks.add(task);
                     break;
                  }
               }
            }
         }
      }

      try {
         String orgID = viewsheet.substring(viewsheet.lastIndexOf("^") + 1);
         save(changedTasks, orgID);
      }
      catch(Exception ex) {
         LOG.error("Failed to save schedule task file after " +
               "bookmark was renamed: " + oldName + " to " + newName, ex);
      }
   }

   /**
    * Method will be invoked when a folder is renamed.
    * @param opath the specified old folder path.
    * @param npath the specified new folder path.
    * @param owner the specified user.
    */
   public synchronized void folderRenamed(String opath, String npath,
                                          String owner)
   {
      Set<ScheduleTask> changedTasks = new HashSet<>();

      for(ScheduleTask task : tasks.values()) {
         if(task == null) {
            continue;
         }

         for(int j = task.getActionCount() - 1; j >= 0; j--) {
            ScheduleAction action = task.getAction(j);
            String prefix = opath + "/";

            if(action instanceof ViewsheetAction) {
               ViewsheetAction vsAction = (ViewsheetAction) action;
               AssetEntry vsEntry = vsAction.getViewsheetEntry();
               String path = vsEntry.getPath();

               if(path != null && path.startsWith(prefix) &&
                  (Tool.equals(vsEntry.getUser(), owner)))
               {
                  String newName = npath + "/" + path.substring(prefix.length());
                  AssetEntry newVSEntry = new AssetEntry(vsEntry.getScope(), vsEntry.getType(),
                     newName, vsEntry.getUser());
                  vsAction.setViewsheet(newVSEntry.toIdentifier());
                  LOG.info("Schedule action in task " + task.getName() +
                     " is changed for viewsheet " + path + " is renamed to " + newName + "!");
                  changedTasks.add(task);
               }
            }
         }
      }

      try {
         save(changedTasks);
      }
      catch(Exception ex) {
         LOG.error("Failed to save schedule task file after " +
               "folder was renamed: " + opath + " to " + npath, ex);
      }

      boolean extchanged = false;

      for(ScheduleExt ext : extensions) {
         extchanged = ext.folderRenamed(opath, npath, owner) || extchanged;
      }

      if(extchanged) {
         reloadExtensions();
      }
   }

   /**
    * Rename the viewsheet in schedule tasks.
    */
   public void renameSheetInSchedule(AssetEntry oentry, AssetEntry nentry)
      throws Exception
   {
      if(nentry != null && nentry.isViewsheet() && RecycleUtils.isInRecycleBin(nentry.getPath())) {
         viewsheetRemoved(oentry);
         return;
      }

      Set<ScheduleTask> changedTasks = new HashSet<>();

      for(ScheduleTask task : getScheduleTasks()) {
         for(int i = 0; i < task.getActionCount(); i++) {
            ScheduleAction action = task.getAction(i);

            if(action instanceof ViewsheetSupport) {
               ViewsheetSupport vaction = (ViewsheetSupport) action;

               if(nentry.isFolder()) {
                  String opath = oentry.getPath();
                  String npath = nentry.getPath();
                  AssetEntry ventry = AssetEntry.createAssetEntry(vaction.getViewsheetName());
                  assert ventry != null;
                  String vpath = ventry.getPath();

                  if(vpath.startsWith(opath + "/")) {
                     vpath = npath + "/" + vpath.substring(opath.length() + 1);
                     ventry = new AssetEntry(ventry.getScope(), ventry.getType(),
                                             vpath, ventry.getUser(), ventry.getOrgID());
                     vaction.setViewsheetName(ventry.toIdentifier());
                     changedTasks.add(task);
                  }
               }
               else if(Tool.equals(vaction.getViewsheetName(), oentry.toIdentifier())) {
                  vaction.setViewsheetName(nentry.toIdentifier());
                  changedTasks.add(task);
               }
            }
            else if(action instanceof BatchAction) {
               AssetEntry actionEntry = ((BatchAction) action).getQueryEntry();

               if(actionEntry != null &&
                  Tool.equals(actionEntry.toIdentifier(), oentry.toIdentifier()))
               {
                  ((BatchAction) action).setQueryEntry(nentry);
                  changedTasks.add(task);
               }
            }
         }
      }

      save(changedTasks, nentry.getOrgID());
   }

   /**
    * Gets the asset identifier for a schedule task.
    *
    * @param task the schedule task.
    *
    * @return the asset identifier.
    */
   private String getTaskIdentifier(ScheduleTask task) {
      return getTaskIdentifier(task.getName(), null);
   }

   /**
    * Gets the asset identifier for a schedule task.
    *
    * @param taskName the name of the task.
    *
    * @return the asset identifier.
    */
   private String getTaskIdentifier(String taskName, String orgID) {
      return new AssetEntry(
         AssetRepository.GLOBAL_SCOPE,
         AssetEntry.Type.SCHEDULE_TASK, "/" + taskName, null, orgID)
         .toIdentifier();
   }

   public boolean isArchiveTaskEnabled() {
      //Todo will remove it.
      return false;
   }

   public ScheduleTask getAssetBackupTask() {
      return getScheduleTask(InternalScheduledTaskService.ASSET_FILE_BACKUP, Organization.getDefaultOrganizationID());
   }

   public ScheduleTask getBalanceTask() {
      return getScheduleTask(InternalScheduledTaskService.BALANCE_TASKS, Organization.getDefaultOrganizationID());
   }

   private final ScheduleTaskMap tasks = new ScheduleTaskMap();
   private final Vector<ScheduleExt> extensions = new Vector<>();
   private final Map<ExtTaskKey, ScheduleTask> extensionTasks = new ConcurrentHashMap<>();
   private final Lock extensionLock;

   private static final Logger LOG = LoggerFactory.getLogger(ScheduleManager.class);
   private static final String EXTENSION_LOCK =
      ScheduleManager.class.getName() + ".extensionLock";

   private record ExtTaskKey(String name, String orgId) { }

   public static final class Reference
      extends SingletonManager.Reference<ScheduleManager>
   {
      @Override
      public ScheduleManager get(Object... parameters) {
         if(manager == null) {
            Lock lock = Cluster.getInstance().getLock(Scheduler.INIT_LOCK);
            lock.lock();

            try {
               if(manager == null) {
                  ScheduleManager initManager = new ScheduleManager();

                  try {
                     new InternalScheduledTaskService(initManager).initInternalTasks();
                  }
                  catch(Exception ex) {
                     LOG.error("Failed to initialize internal tasks.");
                  }

                  manager = initManager;
               }
            }
            finally {
               lock.unlock();
            }

         }

         return manager;
      }

      @Override
      public void dispose() {
         manager = null;
      }

      private ScheduleManager manager;
   }
}
