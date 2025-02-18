package com.jrasp.agent.module.expression.algorithm.impl;

import com.jrasp.agent.api.ProcessControlException;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;

import java.util.Map;

/**
 * SPEL 检测算法
 *
 * @author jrasp
 */
public class SpelAlgorithm implements Algorithm {

    /**
     * spel表达式检测最小长度
     */
    private Integer spelMinLength = 30;

    /**
     * spel表达式限制最大长度
     */
    private Integer spelMaxLimitLength = 200;

    /**
     * spel语句黑名单 检测算法的默认行为：记录
     */
    private Integer spelBlackListAction = 0;

    /**
     * spel长度限制算法 检测算法的默认行为：记录
     */
    private Integer spelMaxLimitLengthAction = 0;

    /**
     * spel语句黑名单
     */
    private String[] spelBlackList = {
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "javax.script.ScriptEngineManager",
            "java.lang.System",
            "org.springframework.cglib.core.ReflectUtils",
            "java.io.File",
            "javax.management.remote.rmi.RMIConnector"
    };

    private final RaspLog logger;

    public SpelAlgorithm(RaspLog logger) {
        this.logger = logger;
    }

    public SpelAlgorithm(RaspLog logger, Map<String, String> configMaps) {
        this.logger = logger;
        this.spelMinLength = ParamSupported.getParameter(configMaps, "spelMinLength", Integer.class, spelMinLength);
        this.spelMaxLimitLength = ParamSupported.getParameter(configMaps, "spelMaxLimitLength", Integer.class, spelMaxLimitLength);
        this.spelBlackListAction = ParamSupported.getParameter(configMaps, "spelBlackListAction", Integer.class, spelBlackListAction);
        this.spelMaxLimitLengthAction = ParamSupported.getParameter(configMaps, "spelMaxLimitLengthAction", Integer.class, spelMaxLimitLengthAction);
        this.spelBlackList = ParamSupported.getParameter(configMaps, "spelBlackArray", String[].class, spelBlackList);
    }

    @Override
    public String getType() {
        return "spel";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        String expression = (String) parameters[0];
        if (expression != null && expression.length() >= spelMinLength) {
            // 检测算法1: 黑名单
            if (this.spelBlackListAction > -1) {
                for (String s : spelBlackList) {
                    if (expression.contains(s)) {
                        doAction(context, expression, spelBlackListAction, "expression hit black list, black class: " + s, 90);
                        return;
                    }
                }
            }

            // 检测算法2: 最大长度限制
            if (this.spelMaxLimitLengthAction > -1) {
                if (expression.length() >= spelMaxLimitLength) {
                    doAction(context, expression, spelBlackListAction, "the length of the expression exceeds the max length, length: " + expression.length(), 80);
                }
            }
        }
    }

    @Override
    public String getDescribe() {
        return "spel check algorithm";
    }

    private void doAction(Context context, String expression, int action, String message, int level) throws ProcessControlException {
        boolean enableBlock = action == 1;
        AttackInfo attackInfo = new AttackInfo(context, expression, enableBlock, getType(), getDescribe(), message, level);
        logger.attack(attackInfo);
        if (enableBlock) {
            ProcessControlException.throwThrowsImmediately(new RuntimeException("spel expression block by rasp."));
        }
    }
}
