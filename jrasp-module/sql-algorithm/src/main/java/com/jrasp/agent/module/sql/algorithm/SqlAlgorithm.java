package com.jrasp.agent.module.sql.algorithm;

import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.ModuleLifecycleAdapter;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.module.sql.algorithm.impl.MySqlAlgorithm;
import org.kohsuke.MetaInfServices;

import java.util.Map;

/**
 * sql注入检测算法
 * 支持的 sql 中间件：mysql
 * 其他类型数据库请自行实现
 */
@MetaInfServices(Module.class)
@Information(id = "sql-algorithm", author = "jrasp")
public class SqlAlgorithm extends ModuleLifecycleAdapter implements Module {

    @RaspResource
    private RaspLog logger;

    @RaspResource
    private AlgorithmManager algorithmManager;

    private volatile MySqlAlgorithm mySqlAlgorithm;

    // 其他算法实例这里添加
    @Override
    public boolean update(Map<String, String> configMaps) {
        mySqlAlgorithm = new MySqlAlgorithm(configMaps, logger);
        algorithmManager.register(mySqlAlgorithm);
        return false;
    }

    @Override
    public void loadCompleted() {
        mySqlAlgorithm = new MySqlAlgorithm(logger);
        algorithmManager.register(mySqlAlgorithm);
    }

    @Override
    public void onUnload() throws Throwable {
        if (mySqlAlgorithm != null) {
            algorithmManager.destroy(mySqlAlgorithm);
            mySqlAlgorithm = null;
        }
        logger.info("sql algorithm onUnload success.");
    }
}
