package com.jrasp.agent.module.deserialization.algorithm.impl;

import com.jrasp.agent.api.ProcessControlException;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import com.jrasp.agent.api.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author jrasp
 * 包括 json、yaml
 * TODO yaml 已经测试　需要找一个cve场景来验证
 */
public class JsonAlgorithm implements Algorithm {

    private final RaspLog logger;

    private Integer jsonBlackListAction = 0;

    // json反序列化类白名单
    private Set<String> jsonWhiteClassList = new HashSet<String>();

    // json反序列化类黑名单
    private Set<String> jsonBlackClassList = new HashSet<String>(Arrays.asList(
            "org.apache.commons.collections.Transformer",
            "java.lang.Thread",
            "java.net.Socket",
            "java.net.URL",
            "java.net.InetAddress",
            "java.lang.Class",
            "oracle.jdbc.rowset.OracleJDBCRowSet",
            "oracle.jdbc.connector.OracleManagedConnectionFactory",
            "java.lang.UNIXProcess",
            "java.lang.AutoCloseable",
            "java.lang.Runnable",
            "java.util.EventListener",
            "java.io.PrintWriter",
            "java.io.FileInputStream",
            "java.io.FileOutputStream",
            "java.util.PriorityQueue"
    ));

    // json反序列化包黑名单
    private Set<String> jsonBlackPackageList = new HashSet<String>(Arrays.asList(
            "org.apache.commons.collections.functors",
            "org.apache.commons.collections4.functors",
            "org.apache.commons.collections4.comparators",
            "org.python.core",
            "org.apache.tomcat",
            "org.apache.xalan",
            "javax.xml",
            "org.springframework",
            "org.apache.commons.beanutils",
            "org.codehaus.groovy.runtime",
            "javax.net",
            "com.mchange",
            "org.apache.wicket.util",
            "java.util.jar",
            "org.mozilla.javascript",
            "java.rmi",
            "java.util.prefs",
            "com.sun",
            "java.util.logging",
            "org.apache.bcel",
            "org.apache.commons.fileupload",
            "org.hibernate",
            "org.jboss",
            "org.apache.myfaces.context.servlet",
            "org.apache.ibatis.datasource",
            "org.apache.log4j",
            "org.apache.logging",
            "org.apache.commons.dbcp",
            "com.ibatis.sqlmap.engine.datasource",
            "javassist",
            "oracle.net",
            "com.alibaba.fastjson.annotation",
            "com.zaxxer.hikari",
            "ch.qos.logback",
            "com.mysql.cj.jdbc.admin",
            "org.apache.ibatis.parsing",
            "org.apache.ibatis.executor",
            "com.caucho"
    ));

    public JsonAlgorithm(RaspLog logger) {
        this.logger = logger;
    }

    public JsonAlgorithm(RaspLog logger, Map<String, String> configMaps) {
        this.logger = logger;
        this.jsonBlackListAction = ParamSupported.getParameter(configMaps, "jsonBlackListAction", Integer.class, jsonBlackListAction);
    }

    @Override
    public String getType() {
        return "json-yaml-deserialization";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (parameters != null && parameters.length >= 1) {
            String className = (String) parameters[0];
            if (jsonWhiteClassList.contains(className)) {
                return;
            }
            // 类名称匹配
            if (jsonBlackClassList.contains(className)) {
                doCheck(context, className, jsonBlackListAction, "deserialization class hit black list, class: " + className, 90);
                return;
            }
            // 包名称匹配
            String pkg = StringUtils.isContainsPackage(className, jsonBlackPackageList);
            if (pkg != null) {
                doCheck(context, className, jsonBlackListAction, "deserialization class hit black list, package: " + pkg, 80);
                return;
            }
        }
        return;
    }

    @Override
    public String getDescribe() {
        return "json/yaml deserialization algorithm";
    }

    private void doCheck(Context context, String className, int action, String message, int level) throws ProcessControlException {
        boolean enableBlock = action == 1;
        AttackInfo attackInfo = new AttackInfo(context, className, enableBlock, getType(), getDescribe(), message, level);
        logger.attack(attackInfo);
        if (enableBlock) {
            ProcessControlException.throwThrowsImmediately(new RuntimeException("json/yaml deserialization attack block by rasp."));
        }
    }
}
