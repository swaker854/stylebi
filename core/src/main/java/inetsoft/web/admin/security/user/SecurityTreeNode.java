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
package inetsoft.web.admin.security.user;

import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import inetsoft.sree.security.IdentityID;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableSecurityTreeNode.class)
@JsonDeserialize(as = ImmutableSecurityTreeNode.class)
public interface SecurityTreeNode {
   IdentityID identityID();
   String label();
   int type();
   List<SecurityTreeNode> children();
   boolean root();
   @Nullable
   String organization();

   @Value.Default
   default boolean readOnly() {
      return false;
   }

   static Builder builder() {
      return new Builder();
   }

   final class Builder extends ImmutableSecurityTreeNode.Builder {
   }
}
