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
package inetsoft.web.admin.general;

import inetsoft.sree.SreeEnv;
import inetsoft.sree.schedule.InternalScheduledTaskService;
import inetsoft.sree.security.*;
import inetsoft.storage.ClusterStorageTransfer;
import inetsoft.storage.ExternalStorageService;
import inetsoft.util.*;
import inetsoft.util.config.InetsoftConfig;
import inetsoft.web.admin.general.model.BackupDataModel;
import inetsoft.web.admin.general.model.DataSpaceSettingsModel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.security.Principal;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class DataSpaceSettingsService extends BackupSupport {
   public DataSpaceSettingsModel getModel(Principal principal) throws Exception {
      InetsoftConfig config = InetsoftConfig.getInstance();

      boolean assetWritePermission = SecurityEngine.getSecurity().checkPermission(
         principal, ResourceType.SCHEDULE_TASK, InternalScheduledTaskService.ASSET_FILE_BACKUP,
         ResourceAction.WRITE);
      String assetName = assetWritePermission ? InternalScheduledTaskService.ASSET_FILE_BACKUP : "";

      return DataSpaceSettingsModel.builder()
         .keyValueType(config.getKeyValue().getType())
         .blobType(config.getBlob().getType())
         .assetBackupTaskName(assetName)
         .build();
   }

   public static String backup(BackupDataModel model) {
      String status;
      Catalog catalog = Catalog.getCatalog();
      File file = null;
      backupLock.lock();

      try {
         deleteRedundantBackupFiles();

         // For the same backup, use the same timestamp
         String stamp = createBackupTimestamp();
         file = FileSystemService.getInstance().getCacheTempFile("backup", ".zip");

         try(OutputStream output = new FileOutputStream(file)) {
            new ClusterStorageTransfer().exportContents(output);
         }

         String path = getBackFile(model != null ? model.dataspace() : null, stamp);
         ExternalStorageService.getInstance().write(path, file.toPath());

         status = catalog.getString("Success");
      }
      catch(Exception e) {
         LOG.error("Failed to back up storage: " + e.getMessage(), e);
         status = "Failed to back up storage: " + e.getMessage();
         return status;
      }
      finally {
         if(file != null && file.exists()) {
            Tool.deleteFile(file);
         }

         backupLock.unlock();
      }

      return status;
   }

   /**
    * backup count control by property "asset.backup.count",
    */
   private static void deleteRedundantBackupFiles() {
      String backupCountProp = SreeEnv.getProperty("asset.backup.count");
      int backupCount = -1;

      try {
         backupCount = Integer.parseInt(backupCountProp);
      }
      catch(Exception ignore) {
      }

      if(backupCount < 1) {
         return;
      }

      ExternalStorageService storageService = ExternalStorageService.getInstance();
      List<String> zips = storageService.listFiles(BACKUP_FOLDER).stream()
         .filter(f -> f.endsWith(".zip") && f.contains(BACKUP_PATH_SPLIT))
         .sorted((z1, z2) -> {
            long z1Time = getTimestamp(z1);
            long z2Time = getTimestamp(z2);

            return (int) (z1Time - z2Time);
         })
         .toList();


      if(zips.size() < backupCount) {
         return;
      }

      int deleteCount = zips.size() - backupCount;

      for(int i = 0; i <= deleteCount; i++) {
         try {
            storageService.delete(zips.get(i));
         }
         catch(IOException e) {
            LOG.error("Failed to delete backup file {}", zips.get(i), e);
         }
      }
   }

   private static long getTimestamp(String fileName) {
      int index = fileName.lastIndexOf(".");

      if(index >= 0 && fileName.substring(0, index).contains(BACKUP_PATH_SPLIT)) {
         fileName = fileName.substring(0, index);
      }

      String[] pathParts = fileName.split(BACKUP_PATH_SPLIT);

      if(pathParts.length < 1) {
         return -1;
      }

      String timestamp = pathParts[pathParts.length - 1];

      try {
         return Long.parseLong(timestamp);
      }
      catch(Exception ignore) {
      }

      return -1;
   }

   private static String getBackFile(String name, String timestamp) {
      name = name == null ? "data" : name;
      int idx = name.indexOf(".zip");

      if(StringUtils.isEmpty(timestamp)) {
         timestamp = createBackupTimestamp();
      }

      if(idx < 0) {
         name += BACKUP_PATH_SPLIT + timestamp + ".zip";
      }
      else {
         String prefix = name.substring(0, idx);
         name = prefix + BACKUP_PATH_SPLIT + timestamp + ".zip";
      }

      name = BACKUP_FOLDER + "/" + name;
      name = ExternalStorageService.getInstance().getAvailableFile(name, 1);
      return name;
   }

   // Backups are in a fixed folder to ensure that we exclude backup files on our second backup.
   private static final String BACKUP_FOLDER = "backup";
   private static final String BACKUP_PATH_SPLIT = "-";

   private static final Lock backupLock = new ReentrantLock();
   private static final Logger LOG = LoggerFactory.getLogger(DataSpaceSettingsService.class);
}
