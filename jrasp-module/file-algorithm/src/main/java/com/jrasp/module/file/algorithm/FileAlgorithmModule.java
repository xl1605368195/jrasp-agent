package com.jrasp.module.file.algorithm;

import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.ModuleLifecycleAdapter;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.module.file.algorithm.impl.FileDeleteAlgorithm;
import com.jrasp.module.file.algorithm.impl.FileListAlgorithm;
import com.jrasp.module.file.algorithm.impl.FileReadAlgorithm;
import com.jrasp.module.file.algorithm.impl.FileUploadAlgorithm;
import org.kohsuke.MetaInfServices;

import java.util.Map;

/**
 * @author jrasp
 */
@MetaInfServices(Module.class)
@Information(id = "file-algorithm", author = "jrasp")
public class FileAlgorithmModule extends ModuleLifecycleAdapter implements Module {

    @RaspResource
    private RaspLog logger;

    @RaspResource
    private AlgorithmManager algorithmManager;

    private volatile FileDeleteAlgorithm fileDeleteAlgorithm;

    private volatile FileListAlgorithm fileListAlgorithm;

    private volatile FileReadAlgorithm fileReadAlgorithm;

    private volatile FileUploadAlgorithm fileUploadAlgorithm;

    // 其他算法实例这里添加

    @Override
    public boolean update(Map<String, String> configMaps) {
        fileDeleteAlgorithm = new FileDeleteAlgorithm(configMaps, logger);
        fileListAlgorithm = new FileListAlgorithm(configMaps, logger);
        fileReadAlgorithm = new FileReadAlgorithm(configMaps, logger);
        fileUploadAlgorithm = new FileUploadAlgorithm(configMaps, logger);
        algorithmManager.register(fileListAlgorithm, fileDeleteAlgorithm, fileReadAlgorithm, fileUploadAlgorithm);
        return false;
    }

    @Override
    public void loadCompleted() {
        fileDeleteAlgorithm = new FileDeleteAlgorithm(logger);
        fileListAlgorithm = new FileListAlgorithm(logger);
        fileReadAlgorithm = new FileReadAlgorithm(logger);
        fileUploadAlgorithm = new FileUploadAlgorithm(logger);
        algorithmManager.register(fileListAlgorithm, fileDeleteAlgorithm, fileReadAlgorithm, fileUploadAlgorithm);
    }

    @Override
    public void onUnload() throws Throwable {
        if (fileDeleteAlgorithm != null) {
            algorithmManager.destroy(fileDeleteAlgorithm);
            fileDeleteAlgorithm = null;
        }
        if (fileListAlgorithm != null) {
            algorithmManager.destroy(fileListAlgorithm);
            fileListAlgorithm = null;
        }
        if (fileReadAlgorithm != null) {
            algorithmManager.destroy(fileReadAlgorithm);
        }
        if (fileUploadAlgorithm != null) {
            algorithmManager.destroy(fileUploadAlgorithm);
        }
        logger.info("file-algorithm onUnload success.");
    }
}
