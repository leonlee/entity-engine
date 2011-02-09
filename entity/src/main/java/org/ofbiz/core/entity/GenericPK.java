/*
 * $Id: GenericPK.java,v 1.1 2005/04/01 05:58:02 sfarquhar Exp $
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
package org.ofbiz.core.entity;

import org.ofbiz.core.entity.model.ModelEntity;

import java.util.Map;

/**
 * Generic Entity Primary Key Object
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Revision: 1.1 $
 * @since      2.0
 */
public class GenericPK extends GenericEntity {

    /** Creates new GenericPK */
    public GenericPK(ModelEntity modelEntity) {
        super(modelEntity);
    }

    /** Creates new GenericPK from existing Map */
    public GenericPK(ModelEntity modelEntity, Map<String, ?> fields) {
        super(modelEntity, fields);
    }

    /** Creates new GenericPK from existing GenericPK */
    public GenericPK(GenericPK value) {
        super(value);
    }

    /** Clones this GenericPK, this is a shallow clone & uses the default shallow HashMap clone
     *@return Object that is a clone of this GenericPK
     */
    public Object clone() {
        GenericPK newEntity = new GenericPK(this);

        newEntity.setDelegator(internalDelegator);
        return newEntity;
    }
}
