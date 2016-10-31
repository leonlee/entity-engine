/*
 * $Id: GeneralRuntimeException.java,v 1.1 2005/04/01 05:58:06 sfarquhar Exp $
 *
 *  Copyright (c) 2001 The Open For Business Project - www.ofbiz.org
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


import java.io.PrintStream;
import java.io.PrintWriter;


/**
 * Base OFBiz Runtime Exception, provides nested exceptions, etc
 *
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version 1.0
 * @created July 12, 2002
 */
public class GeneralRuntimeException extends RuntimeException {
    /**
     * Creates new <code>GeneralException</code> without detail message.
     */
    public GeneralRuntimeException() {
        super();
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public GeneralRuntimeException(String msg) {
        super(msg);
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message and nested Exception.
     *
     * @param msg the detail message.
     */
    public GeneralRuntimeException(String msg, Throwable nested) {
        super(msg, nested);
    }

    /**
     * Returns the detail message, including the message from the nested exception if there is one.
     */
    public String getMessage() {
        final Throwable nested = getCause();
        if (nested != null)
            return super.getMessage() + " (" + nested.getMessage() + ")";
        else
            return super.getMessage();
    }

    /**
     * Returns the detail message, NOT including the message from the nested exception.
     */
    public String getNonNestedMessage() {
        return super.getMessage();
    }

    /**
     * Returns the nested exception if there is one, or {@code null} if there is not.
     * <p>
     * Note that this behaviour is different from that of {@link GeneralException}, which will return
     * {@code this} instead of {@code null} when no nested exception has been specified.
     * </p>
     */
    public Throwable getNested() {
        return getCause();
    }

    // The following pointless stubs are retained only to preserve binary compatibility.

    /**
     * Prints the composite message to System.err.
     */
    public void printStackTrace() {
        super.printStackTrace();
    }

    /**
     * Prints the composite message and the embedded stack trace to the specified stream ps.
     */
    public void printStackTrace(PrintStream ps) {
        super.printStackTrace(ps);
    }

    /**
     * Prints the composite message and the embedded stack trace to the specified print writer pw.
     */
    public void printStackTrace(PrintWriter pw) {
        super.printStackTrace(pw);
    }
}
