/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.virtualization.config;

import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.config.support.*;
import org.jvnet.hk2.annotations.Decorate;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.*;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Provides configuration for a group of machines.
 */
@Configured
@Create(value = "create-group-manager", resolver = Virtualization.VirtResolver.class, i18n = @I18n("org.glassfish.virtualization.create-group-manager"))
@Decorate(targetType = Domain.class, methodName = "getExtensions", with = { Create.class } )
public interface GroupConfig extends ConfigBeanProxy {

    @Attribute(key = true)
    String getName();

    @Param(name="name", primary = true)
    void setName(String name);

    /**
     * Semicolon list of port names used by the group master machine to receive administrative communication on.
     *  possible values are a single port name like br0, eth0, eth1... or multiple ports values like br0, eth0 and so
     * on.
     *
     * When multiple values are provided, the ports will be queried  in the order of their
     * definition until a valid IP address is obtained..
     *
     * @return list of port name used by this group master to receive admin command.
     */
    @Attribute
    String getPortName();
    @Param(name="portName")
    void setPortName(String portName);


    /**
     * Returns the virtualization technology used to interface with low-level machine creation/deletion/etc...
     * Possible values are libvirt, jclouds, deltacloud.
     *
     * So far only libvirt is supported.
     * @see org.glassfish.virtualization.config.Virtualization#getName()
     *
     * @return  the virtualization solution.
     */
    @Attribute(reference = true)
    Virtualization getVirtualization();

    /**
     * Sets the virtualization technology used to interface the low-level virtual machine creation.
     *
     * @param virt  the virtualization interface
     */
    void setVirtualization(Virtualization virt);

    @Attribute
    String getSubNet();

    @Param(name="subNet")
    void setSubNet(String subNet);

    @Element
    @NotNull
    VirtUser getUser();
    @Create(value="create-group-user", resolver = GroupResolver.class, i18n = @I18n("org.glassfish.virtualization.create-machine"))
    void setUser(VirtUser user);

    @Element
    @Create(value = "create-machine", resolver = GroupResolver.class, decorator = MachineConfig.Decorator.class, i18n = @I18n("org.glassfish.virtualization.create-machine"))
    @Listing(value = "list-machines", resolver = GroupResolver.class, i18n = @I18n("org.glassfish.virtualization.list-machines"))
    @Delete(value="delete-machine", resolver= WithinGroupResolver.class, i18n = @I18n("org.glassfish.virtualization.delete-machine"))
    List<MachineConfig> getMachines();

    @Service
    public class GroupResolver implements CrudResolver {
        @Param(name="group")
        String group;

        @Inject
        Virtualizations virt;

        @Override
        public <T extends ConfigBeanProxy> T resolve(AdminCommandContext context, Class<T> type)  {
            for (GroupConfig provider : virt.getGroupConfigs()) {
                if (provider.getName().equals(group)) {
                    return (T) provider;
                }
            }
            context.getActionReport().failure(context.getLogger(), "Cannot find a group by the name of " + group);
            return null;
        }
    }

    @Service
    public class WithinGroupResolver extends GroupResolver {
        @Param(primary=true)
        String name;

        @Override
        public <T extends ConfigBeanProxy> T resolve(AdminCommandContext context, Class<T> type)  {
            GroupConfig group = (GroupConfig) super.resolve(context,type);
            if (group!=null) {
                for (MachineConfig machineConfig : group.getMachines()) {
                    if (machineConfig.getName().equals(name)) {
                        return (T) machineConfig;
                    }
                }
                context.getActionReport().failure(context.getLogger(), "Cannot find a machine by the name of " + group);
            }
            return null;
        }

    }
}
