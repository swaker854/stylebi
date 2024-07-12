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
package inetsoft.web.admin.schedule.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Data transfer object that represents the {@link CompletionConditionModel} for the
 * schedule dialog
 */
@Value.Immutable
@JsonSerialize(as = ImmutableCompletionConditionModel.class)
@JsonDeserialize(as = ImmutableCompletionConditionModel.class)
public abstract class CompletionConditionModel extends ScheduleConditionModel {
   @Nullable
   public abstract String taskName();

   public static CompletionConditionModel.Builder builder() {
      return new CompletionConditionModel.Builder();
   }

   public static class Builder extends ImmutableCompletionConditionModel.Builder {
   }
}