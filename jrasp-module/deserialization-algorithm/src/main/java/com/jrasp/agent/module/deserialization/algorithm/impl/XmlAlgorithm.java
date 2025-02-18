package com.jrasp.agent.module.deserialization.algorithm.impl;

import com.jrasp.agent.api.ProcessControlException;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import com.jrasp.agent.api.util.StackTrace;
import com.jrasp.agent.api.util.StringUtils;

import java.util.*;

/**
 * @author jrasp
 */
public class XmlAlgorithm implements Algorithm {

    private final RaspLog logger;

    private Integer xmlBlackListAction = 0;

    //  xml反序列化类黑名单
    private Set<String> xmlBlackClassList = new HashSet<String>(Arrays.asList(
            "java.io.PrintWriter", "java.io.FileInputStream", "java.io.FileOutputStream", "java.util.PriorityQueue",
            "javax.sql.rowset.BaseRowSet", "javax.activation.DataSource", "java.nio.channels.Channel", "java.io.InputStream",
            "java.lang.ProcessBuilder", "java.lang.Runtime", "javafx.collections.ObservableList", "java.beans.EventHandler", "sun.swing.SwingLazyValue", "java.io.File"
    ));

    // xml反序列化包黑名单
    private Set<String> xmlBlackPackageList = new HashSet<String>(Arrays.asList(
            "sun.reflect", "sun.tracing", "com.sun.corba", "javax.crypto", "jdk.nashorn.internal",
            "sun.awt.datatransfer", "com.sun.tools", "javax.imageio", "com.sun.rowset"
    ));

    //  xml反序列化关键字黑名单
    private List<String> xmlBlackKeyList = Arrays.asList(
            ".jndi.", ".rmi.", ".bcel.", ".xsltc.trax.TemplatesImpl", ".ws.client.sei.",
            "$URLData", "$LazyIterator", "$GetterSetterReflection", "$PrivilegedGetter", "$ProxyLazyValue", "$ServiceNameIterator"
    );

    public XmlAlgorithm(RaspLog logger) {
        this.logger = logger;
    }

    public XmlAlgorithm(RaspLog logger, Map<String, String> configMaps) {
        this.logger = logger;
        this.xmlBlackListAction = ParamSupported.getParameter(configMaps, "xmlBlackListAction", Integer.class, xmlBlackListAction);
    }

    @Override
    public String getType() {
        return "xml-deserialization";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (parameters != null && parameters.length >= 1) {
            String className = (String) parameters[0];
            // 类名称匹配
            if (xmlBlackClassList.contains(className)) {
                doAction(context, className, xmlBlackListAction, "deserialization class hit black list, class: " + className, 90);
                return;
            }

            // 包名称匹配
            String pkg = StringUtils.isContainsPackage(className, xmlBlackPackageList);
            if (pkg != null) {
                doAction(context, className, xmlBlackListAction, "deserialization class hit black list, package: " + pkg, 80);
                return;
            }

            // 关键字黑名单
            for (String key : xmlBlackKeyList) {
                if (className.contains(key)) {
                    doAction(context, className, xmlBlackListAction, "deserialization class hit black list, key: " + key, 50);
                    return;
                }
            }
        }
    }

    @Override
    public String getDescribe() {
        return "xml deserialization algorithm";
    }

    private void doAction(Context context, String className, int action, String message, int level) throws ProcessControlException {
        boolean enableBlock = action == 1;
        AttackInfo attackInfo = new AttackInfo(context, className, enableBlock, getType(), getDescribe(), message, level);
        logger.attack(attackInfo);
        if (enableBlock) {
            ProcessControlException.throwThrowsImmediately(new RuntimeException("xml deserialization attack block by rasp."));
        }
    }

}
