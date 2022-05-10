/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * Configurable Debug logging wrapper class
 */
public final class Debug {

    private static final String NO_MODULE = "NoModule";  // set to null for previous behavior

    public static final int ALWAYS = 0;
    public static final int VERBOSE = 1;
    public static final int TIMING = 2;
    public static final int INFO = 3;
    public static final int IMPORTANT = 4;
    public static final int WARNING = 5;
    public static final int ERROR = 6;
    public static final int FATAL = 7;

    private static final String[] LEVEL_PROPS = {"", "print.verbose", "print.timing", "print.info", "print.important", "print.warning",
            "print.error", "print.fatal"};

    private static final MessageLogger[] LEVEL_LOGS = {Logger::trace, Logger::debug, Logger::trace, Logger::info, Logger::info, Logger::warn, Logger::error, Logger::error};

    private static final Map<String, Integer> LEVEL_STRING_MAP = new HashMap<>();

    private static final boolean[] LEVEL_ON_CACHE = new boolean[8]; // this field is not thread safe

    private static final Logger ROOT = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    private static PrintStream printStream = System.out;
    private static PrintWriter printWriter = new PrintWriter(printStream);

    static {
        LEVEL_STRING_MAP.put("verbose", Debug.VERBOSE);
        LEVEL_STRING_MAP.put("timing", Debug.TIMING);
        LEVEL_STRING_MAP.put("info", Debug.INFO);
        LEVEL_STRING_MAP.put("important", Debug.IMPORTANT);
        LEVEL_STRING_MAP.put("warning", Debug.WARNING);
        LEVEL_STRING_MAP.put("error", Debug.ERROR);
        LEVEL_STRING_MAP.put("fatal", Debug.FATAL);
        LEVEL_STRING_MAP.put("always", Debug.ALWAYS);

        // initialize LEVEL_ON_CACHE
        Properties properties = FlexibleProperties.makeFlexibleProperties(UtilURL.fromResource("debug"));
        for (int i = 0; i < LEVEL_ON_CACHE.length; i++) {
            LEVEL_ON_CACHE[i] = (i == Debug.ALWAYS || "true".equalsIgnoreCase(properties.getProperty(LEVEL_PROPS[i])));
        }
    }

    public static Logger getLogger(String module) {
        if (module != null && module.length() > 0) {
            return LoggerFactory.getLogger(module);
        } else {
            return ROOT;
        }
    }

    public static PrintStream getPrintStream() {
        return printStream;
    }

    public static void setPrintStream(PrintStream printStream) {
        Debug.printStream = printStream;
        Debug.printWriter = new PrintWriter(printStream);
    }

    public static PrintWriter getPrintWriter() {
        return printWriter;
    }

    /**
     * Gets an Integer representing the level number from a String representing the level name; will return null if not found
     */
    public static Integer getLevelFromString(String levelName) {
        if (levelName == null) return null;
        return LEVEL_STRING_MAP.get(levelName.toLowerCase());
    }

    /**
     * Gets an int representing the level number from a String representing the level name; if level not found defaults to Debug.INFO
     */
    public static int getLevelFromStringWithDefault(String levelName) {
        Integer levelInt = getLevelFromString(levelName);
        if (levelInt == null) {
            return Debug.INFO;
        } else {
            return levelInt;
        }
    }

    public static void log(int level, Throwable t, String msg, String module) {
        log(level, t, msg, module, "org.ofbiz.core.util.Debug");
    }

    public static void log(int level, Throwable t, String msg, String module, String callingClass) {
        log(level, t, msg, module, callingClass, new Object[0]);
    }

    public static void log(int level, Throwable t, String msg, String module, String callingClass, Object... params) {
        if (isOn(level)) {
            if (msg != null && params.length > 0) {
                StringBuilder sb = new StringBuilder();
                Formatter formatter = new Formatter(sb);
                formatter.format(msg, params);
                msg = sb.toString();
                formatter.close();
            }
            // log
            LEVEL_LOGS[level].log(getLogger(module), msg, t);
        }
    }

    public static boolean isOn(int level) {
        return LEVEL_ON_CACHE[level];
    }

    public static void log(String msg) {
        log(Debug.ALWAYS, null, msg, NO_MODULE);
    }

    public static void log(String msg, String module) {
        log(Debug.ALWAYS, null, msg, module);
    }

    public static void log(Throwable t) {
        log(Debug.ALWAYS, t, null, NO_MODULE);
    }

    public static void log(Throwable t, String msg) {
        log(Debug.ALWAYS, t, msg, NO_MODULE);
    }

    public static void log(Throwable t, String msg, String module) {
        log(Debug.ALWAYS, t, msg, module);
    }

    public static boolean verboseOn() {
        return isOn(Debug.VERBOSE);
    }

    public static void logVerbose(String msg) {
        log(Debug.VERBOSE, null, msg, NO_MODULE);
    }

    public static void logVerbose(String msg, String module) {
        log(Debug.VERBOSE, null, msg, module);
    }

    public static void logVerbose(Throwable t) {
        log(Debug.VERBOSE, t, null, NO_MODULE);
    }

    public static void logVerbose(Throwable t, String msg) {
        log(Debug.VERBOSE, t, msg, NO_MODULE);
    }

    public static void logVerbose(Throwable t, String msg, String module) {
        log(Debug.VERBOSE, t, msg, module);
    }

    public static boolean timingOn() {
        return isOn(Debug.TIMING);
    }

    public static void logTiming(String msg) {
        log(Debug.TIMING, null, msg, NO_MODULE);
    }

    public static void logTiming(String msg, String module) {
        log(Debug.TIMING, null, msg, module);
    }

    public static void logTiming(Throwable t) {
        log(Debug.TIMING, t, null, NO_MODULE);
    }

    public static void logTiming(Throwable t, String msg) {
        log(Debug.TIMING, t, msg, NO_MODULE);
    }

    public static void logTiming(Throwable t, String msg, String module) {
        log(Debug.TIMING, t, msg, module);
    }

    public static boolean infoOn() {
        return isOn(Debug.INFO);
    }

    public static void logInfo(String msg) {
        log(Debug.INFO, null, msg, NO_MODULE);
    }

    public static void logInfo(String msg, String module) {
        log(Debug.INFO, null, msg, module);
    }

    public static void logInfo(Throwable t) {
        log(Debug.INFO, t, null, NO_MODULE);
    }

    public static void logInfo(Throwable t, String msg) {
        log(Debug.INFO, t, msg, NO_MODULE);
    }

    public static void logInfo(Throwable t, String msg, String module) {
        log(Debug.INFO, t, msg, module);
    }

    public static boolean importantOn() {
        return isOn(Debug.IMPORTANT);
    }

    public static void logImportant(String msg) {
        log(Debug.IMPORTANT, null, msg, NO_MODULE);
    }

    public static void logImportant(String msg, String module) {
        log(Debug.IMPORTANT, null, msg, module);
    }

    public static void logImportant(Throwable t) {
        log(Debug.IMPORTANT, t, null, NO_MODULE);
    }

    public static void logImportant(Throwable t, String msg) {
        log(Debug.IMPORTANT, t, msg, NO_MODULE);
    }

    public static void logImportant(Throwable t, String msg, String module) {
        log(Debug.IMPORTANT, t, msg, module);
    }

    public static boolean warningOn() {
        return isOn(Debug.WARNING);
    }

    public static void logWarning(String msg) {
        log(Debug.WARNING, null, msg, NO_MODULE);
    }

    public static void logWarning(String msg, String module) {
        log(Debug.WARNING, null, msg, module);
    }

    public static void logWarning(Throwable t) {
        log(Debug.WARNING, t, null, NO_MODULE);
    }

    public static void logWarning(Throwable t, String msg) {
        log(Debug.WARNING, t, msg, NO_MODULE);
    }

    public static void logWarning(Throwable t, String msg, String module) {
        log(Debug.WARNING, t, msg, module);
    }

    public static boolean errorOn() {
        return isOn(Debug.ERROR);
    }

    public static void logError(String msg) {
        log(Debug.ERROR, null, msg, NO_MODULE);
    }

    public static void logError(String msg, String module) {
        log(Debug.ERROR, null, msg, module);
    }

    public static void logError(Throwable t) {
        log(Debug.ERROR, t, null, NO_MODULE);
    }

    public static void logError(Throwable t, String msg) {
        log(Debug.ERROR, t, msg, NO_MODULE);
    }

    public static void logError(Throwable t, String msg, String module) {
        log(Debug.ERROR, t, msg, module);
    }

    public static boolean fatalOn() {
        return isOn(Debug.FATAL);
    }

    public static void logFatal(String msg) {
        log(Debug.FATAL, null, msg, NO_MODULE);
    }

    public static void logFatal(String msg, String module) {
        log(Debug.FATAL, null, msg, module);
    }

    public static void logFatal(Throwable t) {
        log(Debug.FATAL, t, null, NO_MODULE);
    }

    public static void logFatal(Throwable t, String msg) {
        log(Debug.FATAL, t, msg, NO_MODULE);
    }

    public static void logFatal(Throwable t, String msg, String module) {
        log(Debug.FATAL, t, msg, module);
    }

    public static void set(int level, boolean on) {
        LEVEL_ON_CACHE[level] = on;
    }

    private interface MessageLogger {
        void log(Logger logger, String message, Throwable throwable);
    }
}
