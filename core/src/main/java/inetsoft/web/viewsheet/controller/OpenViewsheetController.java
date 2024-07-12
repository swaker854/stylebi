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
package inetsoft.web.viewsheet.controller;

import inetsoft.analytic.composition.ViewsheetService;
import inetsoft.analytic.composition.event.VSEventUtil;
import inetsoft.report.composition.RuntimeViewsheet;
import inetsoft.report.internal.LicenseException;
import inetsoft.sree.security.SRPrincipal;
import inetsoft.uql.asset.*;
import inetsoft.uql.viewsheet.Viewsheet;
import inetsoft.uql.viewsheet.ViewsheetInfo;
import inetsoft.util.ThreadContext;
import inetsoft.web.AutoSaveUtils;
import inetsoft.web.composer.vs.VSObjectTreeNode;
import inetsoft.web.composer.vs.VSObjectTreeService;
import inetsoft.web.composer.vs.command.PopulateVSObjectTreeCommand;
import inetsoft.web.composer.ws.event.OpenSheetEventValidator;
import inetsoft.web.service.LicenseService;
import inetsoft.web.viewsheet.HandleAssetExceptions;
import inetsoft.web.viewsheet.LoadingMask;
import inetsoft.web.viewsheet.command.VSDependencyChangedCommand;
import inetsoft.web.viewsheet.event.OpenViewsheetEvent;
import inetsoft.web.viewsheet.model.RuntimeViewsheetRef;
import inetsoft.web.viewsheet.model.ViewsheetRouteDataModel;
import inetsoft.web.viewsheet.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Controller that processes requests to open a viewsheet instance.
 */
@Controller
public class OpenViewsheetController {
   /**
    * Creates a new instance of <tt>ViewsheetController</tt>.
    */
   @Autowired
   public OpenViewsheetController(RuntimeViewsheetRef runtimeViewsheetRef,
                                  RuntimeViewsheetManager runtimeViewsheetManager,
                                  VSObjectTreeService vsObjectTreeService,
                                  ViewsheetService viewsheetService,
                                  VSLifecycleService vsLifecycleService,
                                  LicenseService licenseService)
   {
      this.runtimeViewsheetRef = runtimeViewsheetRef;
      this.runtimeViewsheetManager = runtimeViewsheetManager;
      this.vsObjectTreeService = vsObjectTreeService;
      this.viewsheetService = viewsheetService;
      this.vsLifecycleService = vsLifecycleService;
      this.licenseService = licenseService;
   }

   @GetMapping("api/vs/route-data")
   @ResponseBody
   public ViewsheetRouteDataModel getRouteData(@RequestParam("id") String identifier,
                                               Principal principal) throws Exception
   {
      boolean scaleToScreen = false;
      boolean fitToWidth = false;
      AssetEntry entry = AssetEntry.createAssetEntry(identifier);
      Viewsheet vs = (Viewsheet) viewsheetService.getAssetRepository().getSheet(
         entry, principal, false, AssetContent.CONTEXT);

      if(vs != null) {
         ViewsheetInfo info = vs.getViewsheetInfo();
         scaleToScreen = info.isScaleToScreen();
         fitToWidth = info.isFitToWidth();
      }

      return ViewsheetRouteDataModel.builder()
         .scaleToScreen(scaleToScreen)
         .fitToWidth(fitToWidth)
         .build();
   }

   /**
    * Validator for whether or not the viewsheet has an autosaved version.
    */
   @RequestMapping(
      value = "api/vs/open", method = RequestMethod.POST)
   @ResponseBody
   public OpenSheetEventValidator validateOpen(
      @RequestBody OpenViewsheetEvent event, Principal principal)
   {
      String id = event.getEntryId();
      AssetEntry entry = AssetEntry.createAssetEntry(id);
      boolean autoSaveFileExists = entry != null && AutoSaveUtils.getAutoSavedFile(entry, principal).exists();
      return OpenSheetEventValidator.builder()
         .autoSaveFileExists(autoSaveFileExists)
         .forbiddenSourcesMessage("")
         .build();
   }

   /**
    * Opens a viewsheet.
    *
    * @param event             the event parameters.
    * @param principal         a principal identifying the current user.
    * @param commandDispatcher the command dispatcher.
    *
    * @throws Exception if the viewsheet could not be opened.
    */
   @LoadingMask(true)
   @MessageMapping("/open")
   @HandleAssetExceptions
   public void openViewsheet(@Payload OpenViewsheetEvent event, Principal principal,
                             CommandDispatcher commandDispatcher, @LinkUri String linkUri)
      throws Exception
   {
      if(licenseService.isCpuUnlicensed()) {
         throw new LicenseException(licenseService.getCpuUnlicensedMSG());
      }

      if(principal instanceof SRPrincipal) {
         ThreadContext.setLocale(((SRPrincipal) principal).getLocale());
      }

      boolean existing = event.getRuntimeViewsheetId() != null;
      String id = vsLifecycleService.openViewsheet(
         event, principal, commandDispatcher, runtimeViewsheetRef, runtimeViewsheetManager,
         linkUri);

      if(!existing && !event.isViewer()) {
         AssetEntry entry = AssetEntry.createAssetEntry(event.getEntryId());

         if(entry.getScope() != AssetRepository.TEMPORARY_SCOPE) {
            VSEventUtil.deleteAutoSavedFile(entry, principal);
         }

         if(commandDispatcher.stream().noneMatch(c -> "CollectParametersCommand".equals(c.getType()))) {
            RuntimeViewsheet rvs = viewsheetService.getViewsheet(id, principal);
            VSObjectTreeNode tree = vsObjectTreeService.getObjectTree(rvs);
            PopulateVSObjectTreeCommand treeCommand = new PopulateVSObjectTreeCommand(tree);
            commandDispatcher.sendCommand(treeCommand);
         }
      }

      // if viewsheet is create by wizard, it must have source, so if new sheet from wizard and
      // finished, we should send command to show warning for user.
      if(event.isNewSheet()) {
         commandDispatcher.sendCommand(new VSDependencyChangedCommand(true));
      }
   }

   private final RuntimeViewsheetRef runtimeViewsheetRef;
   private final RuntimeViewsheetManager runtimeViewsheetManager;
   private final VSObjectTreeService vsObjectTreeService;
   private final ViewsheetService viewsheetService;
   private final VSLifecycleService vsLifecycleService;
   private final LicenseService licenseService;
}