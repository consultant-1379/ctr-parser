/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.component.aia.services.eps.core.embeddedservices;

import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.*;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.component.aia.services.eps.core.EpsInstanceManager;
import com.ericsson.component.aia.services.eps.core.modules.ModuleManager;
import com.ericsson.component.aia.services.eps.core.util.EpsProvider;
import com.ericsson.component.aia.services.eps.core.util.ModelServiceUtil;
import com.ericsson.oss.itpf.modeling.common.info.ModelInfo;
import com.ericsson.oss.itpf.modeling.modelservice.ModelService;
import com.ericsson.oss.itpf.modeling.modelservice.meta.ModelRepoBasedModelMetaInformation;
import com.ericsson.oss.itpf.sdk.resources.Resources;

/**
 * The EmbeddedEPS initialize the EPS engine and load the respective input output and steps based on provided flow. <br>
 * The EmbeddedEPS provides {@link #deployModuleFromFile(InputStream)} methods in order to load the dynamically or statically configured flow file.
 * <br>
 * With the help of helper method {@link #undeployAllModules()} Undeploy all the module whereas {@link #getDeployedModulesCount()} will return the
 * number of deployed modules.
 */
public class EmbeddedEPS {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedEPS.class);
    private final EpsProvider provider = EpsProvider.getInstance();
    private ExecutorService execService = Executors.newSingleThreadExecutor();
    private EpsInstanceManager epsInstanceManager;

    private String epsMSNameSpace = "";

    public EpsInstanceManager createEpsInstanceInNewThread() throws InterruptedException, ExecutionException {

        LOG.info("Creating eps instance");
        // Added for tests that share EpsTestUtil instances through inheritance,
        // like S1 and X2 correlations.
        if (execService.isShutdown()) {
            LOG.trace("Creating new execService");
            execService = Executors.newSingleThreadExecutor();
        }

        LOG.trace("Creating callable task to get the instane of eps.");
        final Future<EpsInstanceManager> epsFuture = execService.submit(new Callable<EpsInstanceManager>() {
            @Override
            public EpsInstanceManager call() {
                return EpsInstanceManager.getInstance();
            }
        });

        epsInstanceManager = epsFuture.get();
        epsInstanceManager.start();

        Assert.assertNotNull("Eps instance not found ", epsInstanceManager);

        LOG.trace("Embedded EPS instance created successfully {}", epsInstanceManager);
        return epsInstanceManager;
    }

    public EpsInstanceManager getEpsInstance() {
        return epsInstanceManager;
    }

    public ModelService getModelService() {
        return ModelServiceUtil.getModelService();
    }

    public void shutdownEpsInstance() {

        LOG.trace("Closing EmbeddedEPS instance");
        epsInstanceManager.getModuleManager().undeployAllModules();
        epsInstanceManager.stop();
        execService.shutdownNow();
        provider.clean();
    }

    /**
     * Deploy flow based on flow path and helper method will use the {@link Resources#getClasspathResource(String)} methods to read the file.
     *
     * @param flowPath
     *            flow path
     * @return unique identifier of module
     */
    public String deployModule(final String flowPath) {
        if (epsMSNameSpace.isEmpty()) {
            final InputStream inputStream = Resources.getClasspathResource(flowPath).getInputStream();
            Assert.assertNotNull("Resource flow not found for: " + flowPath, inputStream);
            return deployModuleFromFile(inputStream);
        } else {
            final int indexOfSlash = flowPath.lastIndexOf("/");
            String simpleFileName = flowPath;
            if (indexOfSlash != -1) {
                simpleFileName = flowPath.substring(indexOfSlash + 1, flowPath.length());
                simpleFileName = simpleFileName.replace(".xml", "");
            }
            final String urn = epsMSNameSpace + "/" + simpleFileName + "/*";
            final Collection<ModelInfo> infos = getModelService().getModelMetaInformation().getLatestModelsFromUrn(urn);
            Assert.assertTrue("URN not found " + urn, !infos.isEmpty());
            return deployModuleFromModel(infos.iterator().next().toUrn());
        }
    }

    /**
     * @param inputStream
     *            InputStream pointing to content with textual representation of module flow descriptor xml file. Must not be null.
     * @return unique identifier of module
     */
    public String deployModuleFromFile(final InputStream inputStream) {
        if (epsInstanceManager == null) {
            throw new IllegalStateException("EPS not started!");
        }
        final ModuleManager manager = epsInstanceManager.getModuleManager();
        return manager.deployModuleFromFile(inputStream);
    }

    /**
     * @param modelUrn
     *            modelUrn pointing to path where flow descriptor xml file is present. Must not be null.
     * @return unique identifier of module
     */

    public String deployModuleFromModel(final String modelUrn) {
        if (epsInstanceManager == null) {
            throw new IllegalStateException("EPS not started!");
        }
        final ModuleManager manager = epsInstanceManager.getModuleManager();
        return manager.deployModuleFromModel(modelUrn);
    }

    public boolean isModelService() {
        final String xmlRepo = System.getProperty(ModelRepoBasedModelMetaInformation.MODEL_REPO_PATH_PROPERTY);
        return (xmlRepo != null && !xmlRepo.isEmpty());
    }

    public int undeployAllModules() {
        if (epsInstanceManager == null) {
            throw new IllegalStateException("EPS not started!");
        }
        final ModuleManager manager = epsInstanceManager.getModuleManager();
        return manager.undeployAllModules();
    }

    public int getDeployedModulesCount() {
        if (epsInstanceManager == null) {
            throw new IllegalStateException("EPS not started!");
        }
        final ModuleManager manager = epsInstanceManager.getModuleManager();
        return manager.getDeployedModulesCount();
    }

}
