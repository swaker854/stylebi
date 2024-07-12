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
package inetsoft.sree.internal;

import inetsoft.mv.MVDef;
import inetsoft.mv.MVManager;
import inetsoft.sree.*;
import inetsoft.sree.internal.cluster.Cluster;
import inetsoft.sree.schedule.*;
import inetsoft.sree.security.*;
import inetsoft.uql.XPrincipal;
import inetsoft.uql.util.Identity;
import inetsoft.util.*;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.locks.Lock;

/**
 * DataCycleManager handles the creation of pregenerated tasks at runtime.
 *
 * @since 7.0
 * @author InetSoft Technology Corp
 */
@SingletonManager.Singleton(DataCycleManager.Reference.class)
public class DataCycleManager implements ScheduleExt, PropertyChangeListener {
   /**
    * Data Cycle task.
    */
   public static final String TASK_PREFIX = "DataCycle Task: ";

   /**
    * Creates a new instance of DataCycleManager.
    */
   public DataCycleManager() {
      ScheduleManager.getScheduleManager().addScheduleExt(this);
      init();

      try {
         RepletRegistry.getRegistry().addPropertyChangeListener(this);
      }
      catch(Exception ex) {
         LOG.error("Failed to add property change listener to replet registry", ex);
      }

      MVManager.getManager().addPropertyChangeListener(this);
   }

   /**
    * Get the single instance of DataCycleManager that is mapped to the
    * current thread group.
    * @return the DataCycleManager instance.
    */
   public static DataCycleManager getDataCycleManager() {
      return SingletonManager.getInstance(DataCycleManager.class);
   }

   /**
    * Initialize the Manager by reading in all the existing data cycles.
    */
   private void init() {
      // read in the properties specified for cycles in the EM
      try {
         load();
         generateTasks();
      }
      catch(Exception ex) {
         LOG.error("Failed to initialize data cycle manager", ex);
      }
   }

   /**
    * Load data cycle file cycle.xml described by the property cycle.file.
    */
   public synchronized void load() throws Exception {
      String afile = getCycleFileName();
      Document doc = null;
      DataSpace space = DataSpace.getDataSpace();

      try(InputStream fis = space.getInputStream(null, afile)) {
         dmgr.addChangeListener(space, null, afile, changeListener);

         if(fis != null) {
            doc = Tool.parseXML(fis);
         }
      }

      if(doc == null) {
         return;
      }

      Element dcycleNode = (Element) doc.getElementsByTagName("dcycle").item(0);
      this.dcycle = Tool.getValue(dcycleNode);
      NodeList cycles = doc.getElementsByTagName("DataCycle");

      for(int i = 0; i < cycles.getLength(); i++) {
         Element elem = (Element) cycles.item(i);
         String name = elem.getAttribute("name");
         String orgId = elem.getAttribute("orgId");
         boolean enabled = "false".equals(elem.getAttribute("enabled"));

         if(name == null) {
            throw new IOException("DataCycle Name missing in XML: " + afile);
         }

         if(orgId == null || orgId.isEmpty()) {
            orgId = OrganizationManager.getInstance().getCurrentOrgID();
         }

         NodeList cnodes = Tool.getChildNodesByTagName(elem, "Condition");

         if(cnodes.getLength() == 0) {
            LOG.info("No condition in data cycle, ignored: " + name);
            continue;
         }

         Vector<ScheduleCondition> conds = new Vector<>();

         for(int j = 0; j < cnodes.getLength(); j++) {
            Element cond = (Element) cnodes.item(j);
            String type = cond.getAttribute("type");

            if(type == null) {
               throw new IOException("Condition type missing in XML: " + afile);
            }

            ScheduleCondition condition;

            if(type.equals("TimeCondition")) {
               condition = new TimeCondition();
               ((TimeCondition) condition).parseXML(cond);
            }
            else {
               throw new IOException("Unknown condition type: " + type);
            }

            conds.add(condition);
         }

         DataCycleId cycleId = new DataCycleId(name, orgId);
         dataCycleMap.put(cycleId, conds);
         cycleStatusMap.put(cycleId, !enabled);

         Element cinfo = Tool.getChildNodeByTagName(elem, "CycleInfo");
         CycleInfo cycleInfo = new CycleInfo(name, orgId);

         if(cinfo != null) {
            cycleInfo.parseXML(cinfo);
            cycleInfo.setName(name);
         }

         cycleInfoMap.put(cycleId, cycleInfo);
      }
   }

   /**
    * Method listens to Property change event.
    */
   @Override
   public void propertyChange(PropertyChangeEvent evt) {
      String name = evt.getPropertyName();

      if(RepletRegistry.CHANGE_EVENT.equals(name)) {
         generateTasks();
      }
      else if(MVManager.MV_CHANGE_EVENT.equals(name)) {
         generateTasks();
      }
   }

   /**
    * Method will be invoked when a replet is removed.
    * @param replet the specified replet.
    * @param owner the specified user.
    */
   @Override
   public boolean repletRemoved(String replet, String owner) {
      return false;
   }

   /**
    * Method will be invoked when a replet is renamed.
    * @param oreplet the specified old replet.
    * @param nreplet the specified new replet.
    * @param owner the specified user.
    */
   @Override
   public boolean repletRenamed(String oreplet, String nreplet, String owner) {
      return false;
   }

   /**
    * Method will be invoked when a viewsheet is renamed.
    * @param oviewsheet the specified old viewsheet.
    * @param nviewsheet the specified new viewsheet.
    * @param owner the specified user.
    */
   @Override
   public boolean viewsheetRenamed(String oviewsheet, String nviewsheet,
                                   String owner)
   {
      return false;
   }

   /**
    * Method will be invoked when an archive is renamed.
    * @param opath the specified old archive path.
    * @param npath the specified new archive path.
    * @param owner the specified user.
    * @return <tt>true</tt> if the extension changed, <tt>false</tt> otherwise.
    */
   @Override
   public boolean archiveRenamed(String opath, String npath, String owner) {
      return false;
   }

   /**
    * Method will be invoked when a folder is renamed.
    * @param opath the specified old folder path.
    * @param npath the specified new folder path.
    * @param owner the specified user.
    * @return <tt>true</tt> if the extension changed, <tt>false</tt> otherwise.
    */
   @Override
   public boolean folderRenamed(String opath, String npath, String owner) {
      return false;
   }

   /**
    * Get the pregenerated tasks for the ScheduleManager.
    * @return Vector of ScheduleTask.
    */
   @Override
   public List<ScheduleTask> getTasks() {
      return pregeneratedTasks;
   }

   @Override
   public Iterator<ScheduleTask> iterator() {
      return pregeneratedTasks.iterator();
   }

   /**
    * Internal method used to set pregeneratedTasks.
    */
   private void generateTasks() {
      // don't load in secondary schedulers
      if(Scheduler.getSchedulerCount() != 1 &&
         "true".equals(System.getProperty("ScheduleServer")))
      {
         return;
      }

      List<ScheduleTask> tasks = new ArrayList<>();
      Enumeration<DataCycleId> cycles = getDataCycles();
      Map<DataCycleId, Boolean> cycleStatusMap = new HashMap(this.cycleStatusMap);

      while(cycles.hasMoreElements()) {
         DataCycleId cycle = cycles.nextElement();
         String orgId = cycle.orgId;
         IdentityID identityID = new IdentityID(XPrincipal.SYSTEM, getOrgName(orgId));
         ScheduleTask task = new ScheduleTask(identityID.convertToKey() + "__" + TASK_PREFIX + cycle.name);
         task.setEditable(false);
         task.setRemovable(false);

         if(cycleStatusMap.containsKey(cycle)) {
            task.setEnabled(cycleStatusMap.get(cycle));
         }

         task.setOwner(identityID);
         task.setCycleInfo(getCycleInfo(cycle.name, cycle.orgId));

         for(int i = 0; i < getConditionCount(cycle.name, cycle.orgId); i++) {
            task.addCondition(getCondition(cycle.name, cycle.orgId, i));
         }

         generateMVActions(task, cycle.name, tasks);

         if(task.getActionCount() == 0) {
            continue;
         }

         tasks.add(task);
      }

      synchronized(this) {
         pregeneratedTasks.clear();

         for(ScheduleTask task : tasks) {
            if(!pregeneratedTasks.contains(task)) {
               pregeneratedTasks.add(task);
               taskAdded(task);
            }
         }
      }

      ScheduleManager.getScheduleManager().reloadExtensions();
   }

   private String getOrgName(String orgID) {
      SecurityProvider provider = SecurityEngine.getSecurity().getSecurityProvider();

      for(String orgName : provider.getOrganizations()) {
         Organization organization = provider.getOrganization(orgName);

         if(organization != null && Tool.equals(organization.getOrganizationID(), orgID)) {
            return orgName;
         }
      }

      return null;
   }

   /**
    * Send a message when cycle task is created so it will show up in EM.
    */
   private void taskAdded(ScheduleTask task) {
      ScheduleTaskMessage message = new ScheduleTaskMessage();
      message.setTaskName(task.getName());
      message.setTask(task);
      message.setAction(ScheduleTaskMessage.Action.ADDED);

      try {
         Cluster.getInstance().sendMessage(message);
      }
      catch(Exception ex) {
         LOG.debug("Failed to send task message", ex);
      }
   }

   /**
    * Generate emv actions.
    */
   private void generateMVActions(ScheduleTask task, String cycle,
                                  List<ScheduleTask> tasks)
   {
      MVManager manager = MVManager.getManager();
      MVDef[] mvs = manager.list(false);
      ScheduleTask task2 = new ScheduleTask(task.getOwner().convertToKey() + "__" + TASK_PREFIX + cycle + " Stage 2");
      task2.setEditable(false);
      task2.setRemovable(false);
      task2.setEnabled(task.isEnabled());
      task2.setOwner(task.getOwner());
      CycleInfo cycleInfo = task.getCycleInfo();
      task2.setCycleInfo(cycleInfo);
      task2.addCondition(new CompletionCondition(task.getName()));

      for(MVDef def : mvs) {
         String vsId = def.getVsId();
         int index = vsId.lastIndexOf('^');
         String mvOrgID = index == -1 ?
            Organization.getDefaultOrganizationID() : vsId.substring(index + 1);

         if(!cycle.equals(def.getCycle()) || (cycleInfo != null && !Tool.equals(cycleInfo.getOrgId(), mvOrgID))) {
            continue;
         }

         MVAction action = new MVAction(def);

         // Association MV needs to be created after regular MV so the
         // data in base MV is up-to-date
         if(def.isAssociationMV()) {
            task2.addAction(action);
         }
         else {
            task.addAction(action);
         }
      }

      if(task2.getActionCount() > 0) {
         tasks.add(task2);
      }
   }

   /**
    * Delete a given task.
    */
   @Override
   public boolean deleteTask(String name) {
      return false;
   }

   /**
    * Enable/Disable a given task.
    */
   @Override
   public boolean setEnable(String name, String orgId, boolean enable) {
      cycleStatusMap.put(new DataCycleId(name, orgId), enable);
      return false;
   }

   /**
    * Check if a given task is enabled.
    */
   @Override
   public boolean isEnable(String name, String orgId) {
      Boolean enabled = cycleStatusMap.get(new DataCycleId(name, orgId));
      return enabled == null ? containsTask(name, orgId) : enabled;
   }

   /**
    * Check if contains a task.
    */
   @Override
   public boolean containsTask(String name, String orgId) {
      return findTask(name, orgId) != null;
   }

   /**
    * Get the cycle file name, cycle.xml, described by the property cycle.file.
    * @return the cycle file as a File.
    */
   @SuppressWarnings("WeakerAccess")
   public static String getCycleFileName() {
      return SreeEnv.getProperty("cycle.file");
   }

   /**
    * Save a list of cycles to the 'cycle.file'.
    */
   public synchronized void save() throws Exception {
      String afile = getCycleFileName();
      DataSpace space = DataSpace.getDataSpace();
      dmgr.removeChangeListener(space, null, afile, changeListener);

      try(DataSpace.Transaction tx = space.beginTransaction();
          OutputStream output = tx.newStream(null, afile))
      {
         PrintWriter writer = createWriter(output);
         writer.println("<?xml version=\"1.0\"?>");
         writer.println("<DataCycles>");
         writer.println("<Version>" + FileVersions.CYCLE + "</Version>");

         for(DataCycleId cycleId : dataCycleMap.keySet()) {
            String name = cycleId.name;
            String orgId = cycleId.orgId;
            ScheduleTask task = findTask(TASK_PREFIX + name, orgId);
            Boolean enabled = cycleStatusMap.get(new DataCycleId(name, orgId));
            boolean init = task == null && enabled == null;

            if(init) {
               cycleStatusMap.put(cycleId, Boolean.TRUE);
               enabled = cycleStatusMap.get(new DataCycleId(name, orgId));
            }
            else if(task != null) {
               //cycleStatusMap.put(name, task.isEnabled());
               enabled = !cycleStatusMap.containsKey(new DataCycleId(name, orgId)) ||
                  cycleStatusMap.get(new DataCycleId(name, orgId));
            }

            String encodedName = Encode.forXmlAttribute(name);
            String encodedOrgId = Encode.forXmlAttribute(orgId);
            String encodedEnabled = Encode.forXmlAttribute(enabled.toString());
            writer.println("<DataCycle name=\"" + encodedName + "\"" +
               " orgId=\"" + encodedOrgId + "\"" +
               " enabled=\"" + encodedEnabled + "\">");

            for(int i = 0; i < getConditionCount(name, orgId); i++) {
               ScheduleCondition cond = getCondition(name, orgId, i);

               if(cond instanceof TimeCondition) {
                  ((TimeCondition) cond).writeXML(writer);
               }
            }

            CycleInfo cycleInfo = cycleInfoMap.get(new DataCycleId(name, orgId));

            if(cycleInfo != null) {
               cycleInfo.writeXML(writer);
            }

            writer.println("</DataCycle>");
         }

         if(dcycle != null) {
            writer.println("<dcycle>");
            writer.print("<![CDATA[" + Encode.forCDATA(dcycle) + "]]>");
            writer.println("</dcycle>");
         }

         writer.println("</DataCycles>");
         writer.flush();
         tx.commit();
      }
      catch(Throwable ex) {
         init();
         throw new Exception("Failed to save data cycle file", ex);
      }
      finally {
         if(space != null) {
            dmgr.addChangeListener(space, null, afile, changeListener);
         }
      }

      generateTasks();
   }

   private PrintWriter createWriter(OutputStream output) {
      return new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
   }

   /**
    * Find the task by name.
    */
   private ScheduleTask findTask(String name, String orgId) {
      for(ScheduleTask task : pregeneratedTasks) {
         if(task.getName().equals(name)) {
            if(task.getCycleInfo() == null || task.getCycleInfo().getOrgId().equals(orgId)) {
               return task;
            }
         }
      }

      return null;
   }

   /**
    * Add condition to specified data cycle.
    */
   public void addCondition(String name, String orgId, ScheduleCondition sc) {
      Vector<ScheduleCondition> conds = dataCycleMap.get(new DataCycleId(name, orgId));

      if(conds == null) {
         conds = new Vector<>();
         dataCycleMap.put(new DataCycleId(name, orgId), conds);
      }

      conds.add(sc);
   }

   public void setConditions(String name, String orgId, List<ScheduleCondition> conditions) {
      Vector<ScheduleCondition> conditionVector = new Vector<>();

      for(ScheduleCondition condition : conditions) {
         conditionVector.add(condition);
      }

      setConditions(name, orgId, conditionVector);
   }
   /**
    * Set conditions to specified data cycle.
    */
   public void setConditions(String name, String orgId, Vector<ScheduleCondition> conds) {
      dataCycleMap.put(new DataCycleId(name, orgId), conds);
   }

   /**
    * Set a condition to specified data cycle..
    */
   public void setCondition(String name, String orgId, ScheduleCondition cond, int index) {
      getConditions(name, orgId).setElementAt(cond, index);
   }

   /**
    * Remove the data cycle with specified name from the data cycle map.
    */
   public void removeDataCycle(String name, String orgId) {
      dataCycleMap.remove(new DataCycleId(name, orgId));
      cycleStatusMap.remove(new DataCycleId(name, orgId));
   }

   /**
    * Remove the data cycle with specified name from the data cycle map.
    * @param checkDependency if checkDependency when remove dataCycle.
    */
   public void removeDataCycle(String name, String orgId, boolean checkDependency)
      throws Exception
   {
      if(name != null && !name.trim().equals("") && checkDependency) {
         if(!hasPregeneratedDependency(name)) {
            dataCycleMap.remove(new DataCycleId(name, orgId));
            cycleStatusMap.remove(new DataCycleId(name, orgId));
         }
         // warning if some replet uses the cycle
         else {
            throw new Exception(Catalog.getCatalog().getString(
               "em.scheduler.deleteCycleError"));
         }
      }
      else {
         throw new Exception(Catalog.getCatalog().getString(
            "designer.property.emptyNullError"));
      }
   }

   /**
    * Check if the cycle is used.
    */
   public boolean hasPregeneratedDependency(String cycle) {
      if(cycle == null) {
         return false;
      }

      MVManager manager = MVManager.getManager();
      MVDef[] mvs = manager.list(false);

      for(MVDef mv : mvs) {
         if(cycle.equals(mv.getCycle())) {
            return true;
         }
      }

      return false;
   }

   /**
    * Get all the data cycles.
    */
   public Enumeration<DataCycleId> getDataCycles() {
      return Collections.enumeration(dataCycleMap.keySet());
   }

   /**
    * Get the data cycles in an organization.
    */
   public Enumeration<String> getDataCycles(String orgID) {
      return Collections.enumeration(
         dataCycleMap.keySet().stream()
            .filter(id -> id.orgId.equals(orgID))
            .map(DataCycleId::name)
            .toList());
   }

   /**
    * Get the time conditions of the specified data cycle.
    */
   public Vector<ScheduleCondition> getConditions(String name, String orgId) {
      return dataCycleMap.get(new DataCycleId(name, orgId));
   }

   /**
    * Get the total number of the data cycles.
    */
   public int getDataCycleCount() {
      return dataCycleMap.size();
   }

   /**
    * Get the specified condition.
    */
   public ScheduleCondition getCondition(String name, String orgId, int index) {
      return getConditions(name, orgId).elementAt(index);
   }

   /**
    * Remove the specified condition.
    */
   public void removeCondition(String name, String orgId, int index) {
      getConditions(name, orgId).removeElementAt(index);
   }

   /**
    * Get condition count of specified data cycle.
    */
   public int getConditionCount(String name, String orgId) {
      Vector<ScheduleCondition> conds = getConditions(name, orgId);
      return conds == null ? 0 : conds.size();
   }

   /**
    * Get the specified cycle info.
    */
   public CycleInfo getCycleInfo(String name, String orgId) {
      return cycleInfoMap.get(new DataCycleId(name, orgId));
   }

   /**
    * Set cycle info for an cycle.
    */
   public void setCycleInfo(String name, String orgId, CycleInfo cycleInfo) {
      cycleInfoMap.put(new DataCycleId(name, orgId), cycleInfo);
   }

   /**
    * Refreshes the cycle data.
    */
   public void refresh() {
      synchronized(cycleStatusMap) {
         dataCycleMap = new LinkedHashMap<>();
         cycleStatusMap = new LinkedHashMap<>();
         init();
         MVManager.getManager().setDefaultCycle(dcycle);
      }
   }

   /**
    * Listener added to be notified if cycle.file is changed on disk.
    */
   private DataChangeListener changeListener = new DataChangeListener() {
      @Override
      public void dataChanged(DataChangeEvent e) {
         LOG.debug(e.toString());
         refresh();
      }
   };

   /**
    * Triggered when identity removed.
    * @param identity the specified identity.
    */
   @Override
   public void identityRemoved(Identity identity) {
      // do nothing
   }

   /**
    * Triggered when identity renamed.
    * @param oname the specified original name.
    * @param identity the specified identity.
    */
   @Override
   public void identityRenamed(String oname, Identity identity) {
      // do nothing
   }

   /**
    * Set the default cycle.
    */
   public void setDefaultCycle(String dcycle) {
      this.dcycle = dcycle;
   }

   /**
    * Get the default cycle.
    */
   public String getDefaultCycle() {
      return dcycle;
   }

   public static class CycleInfo implements XMLSerializable, Serializable {
      public CycleInfo() {
      }

      public CycleInfo(String name, String orgId) {
         this.name = name;
         this.orgId = orgId;
      }

      public boolean isStartNotify() {
         return startNotify;
      }

      public void setStartNotify(boolean startNotify) {
         this.startNotify = startNotify;
      }

      public String getStartEmail() {
         return startEmail;
      }

      public void setStartEmail(String startEmail) {
         this.startEmail = startEmail;
      }

      public boolean isEndNotify() {
         return endNotify;
      }

      public void setEndNotify(boolean endNotify) {
         this.endNotify = endNotify;
      }

      public String getEndEmail() {
         return endEmail;
      }

      public void setEndEmail(String endEmail) {
         this.endEmail = endEmail;
      }

      public boolean isFailureNotify() {
         return failureNotify;
      }

      public void setFailureNotify(boolean failureNotify) {
         this.failureNotify = failureNotify;
      }

      public String getFailureEmail() {
         return failureEmail;
      }

      public void setFailureEmail(String failureEmail) {
         this.failureEmail = failureEmail;
      }

      public boolean isExceedNotify() {
         return exceedNotify;
      }

      public void setExceedNotify(boolean exceedNotify) {
         this.exceedNotify = exceedNotify;
      }

      public int getThreshold() {
         return threshold;
      }

      public void setThreshold(int threshold) {
         this.threshold = threshold;
      }

      public String getExceedEmail() {
         return exceedEmail;
      }

      public void setExceedEmail(String exceedEmail) {
         this.exceedEmail = exceedEmail;
      }

      public String getName() {
         return name;
      }

      public void setName(String name) {
         this.name = name;
      }

      public String getOrgId() {
         return orgId;
      }

      public void setOrgId(String orgId) {
         this.orgId = orgId;
      }

      /**
       * Get created time.
       * @return created time.
       */
      public long getCreated() {
         return created;
      }

      /**
       * Set created time.
       * @param created the specified created time.
       */
      public void setCreated(long created) {
         this.created = created;
      }

      /**
       * Get last modified.
       * @return last modified time.
       */
      public long getLastModified() {
         return modified;
      }

      /**
       * Set last modified time.
       * @param modified the specified last modified time.
       */
      public void setLastModified(long modified) {
         this.modified = modified;
      }

      /**
       * Get the created person.
       * @return the created person.
       */
      public String getCreatedBy() {
         return createdBy;
      }

      /**
       * Set the created person
       * @param createdBy the created person.
       */
      public void setCreatedBy(String createdBy) {
         this.createdBy = createdBy;
      }

      /**
       * Get last modified person.
       * @return last modified person.
       */
      public String getLastModifiedBy() {
         return modifiedBy;
      }

      /**
       * Set last modified person.
       * @param modifiedBy the specified last modified person.
       */
      public void setLastModifiedBy(String modifiedBy) {
         this.modifiedBy = modifiedBy;
      }

      public String toString() {
         return startNotify + ", " + startEmail + ", " +
            endNotify + ", " +  endEmail + ", " +
            failureNotify + ", " + failureEmail + ", " +
            exceedNotify + ", " + threshold + ", " + exceedEmail + "," +
            createdBy + "," + created + "," + modified + "," + modifiedBy;
      }

      @Override
      public void parseXML(Element tag) throws Exception {
         name = Tool.getAttribute(tag, "name");
         orgId = Tool.getAttribute(tag, "orgId");
         startNotify = "true".equals(Tool.getAttribute(tag, "startNotify"));
         startEmail = Tool.getAttribute(tag, "startEmail");
         endNotify = "true".equals(Tool.getAttribute(tag, "endNotify"));
         endEmail = Tool.getAttribute(tag, "endEmail");
         exceedNotify = "true".equals(Tool.getAttribute(tag, "exceedNotify"));
         exceedEmail = Tool.getAttribute(tag, "exceedEmail");
         failureNotify = "true".equals(Tool.getAttribute(tag, "failureNotify"));
         failureEmail = Tool.getAttribute(tag, "failureEmail");
         threshold = Integer.parseInt(Tool.getAttribute(tag, "threshold"));
         createdBy = Tool.getAttribute(tag, "createdBy");
         modifiedBy = Tool.getAttribute(tag, "modifiedBy");
         String val = Tool.getAttribute(tag, "created");

         if(val != null) {
            this.created = Long.parseLong(val);
         }

         val = Tool.getAttribute(tag, "modified");

         if(val != null) {
            this.modified = Long.parseLong(val);
         }
      }

      @Override
      public void writeXML(PrintWriter writer) {
         String infos = "name=\"" + Encode.forXmlAttribute(name) + "\" " +
            "orgId=\"" + Encode.forXmlAttribute(orgId) + "\" " +
            "startNotify=\"" + startNotify + "\" " +
            "endNotify=\"" + endNotify + "\" " +
            "failureNotify=\"" + failureNotify + "\" " +
            "exceedNotify=\"" + exceedNotify + "\" " +
            "threshold=\"" + threshold + "\"";

         if(startEmail != null) {
            infos += " startEmail=\"" + startEmail + "\"";
         }

         if(endEmail != null) {
            infos += " endEmail=\"" + endEmail + "\"";
         }

         if(failureEmail != null) {
            infos += " failureEmail=\"" + failureEmail + "\"";
         }

         if(exceedEmail != null) {
            infos += " exceedEmail=\"" + exceedEmail + "\"";
         }

         if(createdBy != null) {
            infos += " createdBy=\"" + createdBy + "\"";
         }

         if(created != 0) {
            infos += " created=\"" + created + "\"";
         }

         if(modifiedBy != null) {
            infos += " modifiedBy=\"" + modifiedBy + "\"";
         }

         if(modified != 0) {
            infos += " modified=\"" + modified + "\"";
         }

         writer.print("<CycleInfo " + infos + "/>");
      }

      private boolean startNotify;
      private String startEmail;
      private boolean endNotify;
      private String endEmail;
      private boolean failureNotify;
      private String failureEmail;
      private boolean exceedNotify;
      private String exceedEmail;
      private int threshold;
      private String name;
      private String orgId;
      private long created;
      private long modified;
      private String createdBy;
      private String modifiedBy;
   }

   private static final Logger LOG =
      LoggerFactory.getLogger(DataCycleManager.class);
   private static DataChangeListenerManager dmgr =
      new DataChangeListenerManager();

   private Map<DataCycleId, Vector<ScheduleCondition>> dataCycleMap =
      new LinkedHashMap<>();
   private Map<DataCycleId, Boolean> cycleStatusMap = new LinkedHashMap<>();
   private Vector<ScheduleTask> pregeneratedTasks = new Vector<>();
   private Map<DataCycleId, CycleInfo> cycleInfoMap = new HashMap<>();
   private String dcycle;

   public record DataCycleId(String name, String orgId) {}

   public static final class Reference
      extends SingletonManager.Reference<DataCycleManager>
   {
      @Override
      public DataCycleManager get(Object... parameters) {
         // prevent deadlock caused by scheduler manager and replet engine initialization
         SingletonManager.getInstance(ScheduleManager.class);

         if(manager == null) {
            Lock lock = Cluster.getInstance().getLock(Scheduler.INIT_LOCK);
            lock.lock();

            try {
               if(manager == null) {
                  manager = new DataCycleManager();
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

      private DataCycleManager manager;
   }
}