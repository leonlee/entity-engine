/*
 * $Id: UtilTimer.java,v 1.1 2005/04/01 05:58:06 sfarquhar Exp $
 *
 *  Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.core.util;

/**
 * Timer  handling utility
 * Utility class for simple reporting of the progress of a process. Steps are labelled, and the time between each label
 * (or message) and the time since the start are reported in each call to timerString.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Revision: 1.1 $
 * @since      2.0
 */
public class UtilTimer {

    long realStartTime;
    long startTime;
    long lastMessageTime;
    String lastMessage = null;
    boolean log = false;

    /** Default constructor. Starts the timer.
     */
    public UtilTimer() {
        lastMessageTime = realStartTime = startTime = System.currentTimeMillis();
        lastMessage = "Begin";
    }

    /** Creates a string with information including the passed message, the last passed message and the time since the last call, and the time since the beginning
     * @param message A message to put into the timer String
     * @return A String with the timing information, the timer String
     */
    public String timerString(String message) {
        return timerString(message, null);
    }

    /** Creates a string with information including the passed message, the last passed message and the time since the last call, and the time since the beginning
     * @param message A message to put into the timer String
     * @param module The debug/log module/thread to use, can be null for root module
     * @return A String with the timing information, the timer String
     */
    public String timerString(String message, String module) {
        // time this call to avoid it interfering with the main timer
        long tsStart = System.currentTimeMillis();

        String retString = "[[" + message + "- total:" + secondsSinceStart() +
            ",since last(" + ((lastMessage.length() > 20) ? (lastMessage.substring(0, 17) + "...") : lastMessage) + "):" +
            secondsSinceLast() + "]]";

        lastMessage = message;
        if (log) Debug.log(Debug.TIMING, null, retString, module, "org.ofbiz.core.util.UtilTimer");

        // have lastMessageTime come as late as possible to just time what happens between calls
        lastMessageTime = System.currentTimeMillis();
        // update startTime to disclude the time this call took
        startTime += (lastMessageTime - tsStart);

        return retString;
    }

    /** Returns the number of seconds since the timer started
     * @return The number of seconds since the timer started
     */
    public double secondsSinceStart() {
        return ((double) timeSinceStart()) / 1000.0;
    }

    /** Returns the number of seconds since the last time timerString was called
     * @return The number of seconds since the last time timerString was called
     */
    public double secondsSinceLast() {
        return ((double) timeSinceLast()) / 1000.0;
    }

    /** Returns the number of milliseconds since the timer started
     * @return The number of milliseconds since the timer started
     */
    public long timeSinceStart() {
        long currentTime = System.currentTimeMillis();

        return currentTime - startTime;
    }

    /** Returns the number of milliseconds since the last time timerString was called
     * @return The number of milliseconds since the last time timerString was called
     */
    public long timeSinceLast() {
        long currentTime = System.currentTimeMillis();

        return currentTime - lastMessageTime;
    }

    /** Sets the value of the log member, denoting whether log output is off or not
     * @param log The new value of log
     */
    public void setLog(boolean log) {
        this.log = log;
    }

    /** Gets the value of the log member, denoting whether log output is off or not
     * @return The value of log
     */
    public boolean getLog() {
        return log;
    }

    /** Creates a string with information including the passed message, the time since the last call,
     * and the time since the beginning.  This version allows an integer level to be specified to
     * improve readability of the output.
     * @param level Integer specifying how many levels to indent the timer string so the output can be more easily read through nested method calls.
     * @param message A message to put into the timer String
     * @return A String with the timing information, the timer String
     */
    public String timerString(int level, String message) {
        // String retString =  "[[" + message + ": seconds since start: " + secondsSinceStart() + ",since last(" + lastMessage + "):" + secondsSinceLast() + "]]";

        StringBuffer retStringBuf = new StringBuffer();

        for (int i = 0; i < level; i++) {
            retStringBuf.append("| ");
        }
        retStringBuf.append("(");

        String timeSinceStartStr = String.valueOf(timeSinceStart());

        // int spacecount = 5 - timeSinceStartStr.length();
        // for (int i=0; i < spacecount; i++) { retStringBuf.append(' '); }
        retStringBuf.append(timeSinceStartStr + ",");

        String timeSinceLastStr = String.valueOf(timeSinceLast());

        // spacecount = 4 - timeSinceLastStr.length();
        // for (int i=0; i < spacecount; i++) { retStringBuf.append(' '); }
        retStringBuf.append(timeSinceLastStr);

        retStringBuf.append(")");
        int spacecount = 12 + (2 * level) - retStringBuf.length();

        for (int i = 0; i < spacecount; i++) {
            retStringBuf.append(' ');
        }
        retStringBuf.append(message);

        // lastMessageTime = (new Date()).getTime();
        lastMessageTime = System.currentTimeMillis();
        // lastMessage = message;

        String retString = retStringBuf.toString();

        // if(!quiet) Debug.logInfo(retString);
        if (Debug.infoOn()) Debug.logInfo(retString);
        return retString;
    }

}
