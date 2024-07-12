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
package inetsoft.web.admin.logviewer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
public class LogSettingController {
   @Autowired
   public LogSettingController(LogSettingService logSettingService) {
      this.logSettingService = logSettingService;
   }

   @GetMapping("/api/em/log/setting/get-configuration")
   public LogSettingsModel getLogSettings() {
      return logSettingService.getConfiguration();
   }

   @PostMapping("/api/em/log/setting/set-configuration")
   public void setLogSettings(@RequestBody() LogSettingsModel model, Principal principal) {
      logSettingService.setConfiguration(model, principal);
   }

   private final LogSettingService logSettingService;
}
