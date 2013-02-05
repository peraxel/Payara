/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.concurrent.runtime.deployer;


import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.logging.LogDomains;
import org.glassfish.concurrent.config.ManagedExecutorService;
import org.glassfish.concurrent.runtime.ConcurrentRuntime;
import org.glassfish.resourcebase.resources.api.ResourceConflictException;
import org.glassfish.resourcebase.resources.api.ResourceDeployer;
import org.glassfish.resourcebase.resources.api.ResourceDeployerInfo;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.glassfish.resourcebase.resources.naming.ResourceNamingService;
import org.glassfish.resourcebase.resources.util.ResourceUtil;
import org.glassfish.resources.naming.SerializableObjectRefAddr;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.RefAddr;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@ResourceDeployerInfo(ManagedExecutorService.class)
@Singleton
public class ManagedExecutorServiceDeployer implements ResourceDeployer {

    @Inject
    private ResourceNamingService namingService;

    @Inject
    ConcurrentRuntime concurrentRuntime;

    // logger for this deployer
    private static Logger _logger = LogDomains.getLogger(ManagedExecutorServiceDeployer.class, LogDomains.RSR_LOGGER);

    @Override
    public void deployResource(Object resource, String applicationName, String moduleName) throws Exception {
        ManagedExecutorService managedExecutorServiceRes = (ManagedExecutorService) resource;

        if (managedExecutorServiceRes == null) {
            _logger.log(Level.INFO, "core.resourcedeploy_error");
            return;
        }

        String jndiName = managedExecutorServiceRes.getJndiName();
        String contextInfo = managedExecutorServiceRes.getContextInfo();

        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "ManagedExecutorServiceDeployer.deployResource() : jndi-name ["+jndiName+"], " +
                    " context-info ["+contextInfo+"]");
        }


        ResourceInfo resourceInfo = new ResourceInfo(managedExecutorServiceRes.getJndiName(), applicationName, moduleName);
        ManagedExecutorServiceConfig config = new ManagedExecutorServiceConfig(managedExecutorServiceRes);

        javax.naming.Reference ref= new  javax.naming.Reference(
                javax.enterprise.concurrent.ManagedExecutorService.class.getName(),
                "org.glassfish.concurrent.runtime.deployer.ConcurrentObjectFactory",
                null);
        RefAddr addr = new SerializableObjectRefAddr(ManagedExecutorServiceConfig.class.getName(), config);
        ref.add(addr);
        RefAddr resAddr = new SerializableObjectRefAddr(ResourceInfo.class.getName(), resourceInfo);
        ref.add(resAddr);

        try {
            // Publish the object ref
            namingService.publishObject(resourceInfo, ref, true);
        } catch (Exception ex) {
            // TODO add log message
//            _logger.log(Level.SEVERE, "mailrsrc.create_obj_error", resourceInfo);
//            _logger.log(Level.SEVERE, "mailrsrc.create_obj_error_excp", ex);
        }
    }

    @Override
    public void deployResource(Object resource) throws Exception {
        ManagedExecutorService ManagedExecutorServiceResource =
                (ManagedExecutorService) resource;
        ResourceInfo resourceInfo = ResourceUtil.getResourceInfo(ManagedExecutorServiceResource);
        deployResource(resource, resourceInfo.getApplicationName(), resourceInfo.getModuleName());
    }

    @Override
    public void undeployResource(Object resource) throws Exception {
        ManagedExecutorService ManagedExecutorServiceResource =
                (ManagedExecutorService) resource;
        ResourceInfo resourceInfo = ResourceUtil.getResourceInfo(ManagedExecutorServiceResource);
        undeployResource(resource, resourceInfo.getApplicationName(), resourceInfo.getModuleName());
    }

    @Override
    public void undeployResource(Object resource, String applicationName, String moduleName) throws Exception {
        ManagedExecutorService ManagedExecutorServiceRes = (ManagedExecutorService) resource;
        ResourceInfo resourceInfo = new ResourceInfo(ManagedExecutorServiceRes.getJndiName(), applicationName, moduleName);
        namingService.unpublishObject(resourceInfo, ManagedExecutorServiceRes.getJndiName());
    }

    @Override
    public void redeployResource(Object resource) throws Exception {
        undeployResource(resource);
        deployResource(resource);
    }

    @Override
    public void enableResource(Object resource) throws Exception {
        deployResource(resource);
    }

    @Override
    public void disableResource(Object resource) throws Exception {
        undeployResource(resource);
    }

    @Override
    public boolean handles(Object resource) {
        return resource instanceof ManagedExecutorService;
    }

    @Override
    public boolean supportsDynamicReconfiguration() {
        return false;
    }

    @Override
    public Class[] getProxyClassesForDynamicReconfiguration() {
        return new Class[0];
    }

    @Override
    public boolean canDeploy(boolean postApplicationDeployment, Collection<Resource> allResources, Resource resource) {
        if (handles(resource)) {
            if (!postApplicationDeployment) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void validatePreservedResource(Application oldApp, Application newApp, Resource resource, Resources allResources) throws ResourceConflictException {
        // do nothing
    }
}