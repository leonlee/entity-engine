/*
 * $Id: GeneralException.java,v 1.1 2005/04/01 05:58:05 sfarquhar Exp $
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
 * Base OFBiz Exception, provides nested exceptions, etc
 *
 * @author <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version 1.0
 * @created November 5, 2001
 */
public class GeneralException extends Exception {
    /**
     * Creates new <code>GeneralException</code> without detail message.
     */
    public GeneralException() {
        super();
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public GeneralException(String msg) {
        super(msg);
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message and nested Exception.
     *
     * @param msg the detail message.
     */
    public GeneralException(String msg, Throwable nested) {
        super(msg, nested);
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message and nested Exception.
     */
    public GeneralException(Throwable nested) {
        super(nested);
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
     * Returns the nested exception if there is one, or {@code this} if there is not.
     * <p>
     * Note: In earlier versions, this class incorrectly documented that it returned {@code null} when there
     * was no nested exception.  This was a lie; it has always returned {@code this} for that case.  Note also
     * that this behaviour is not consistent with that of {@link GeneralRuntimeException}.
     * </p>
     */
    public Throwable getNested() {
        final Throwable nested = getCause();
        if (nested == null)
            return this;
        return nested;
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

