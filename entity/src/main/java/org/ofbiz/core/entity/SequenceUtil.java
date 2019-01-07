/*
 * $Id: SequenceUtil.java,v 1.4 2005/11/14 00:54:37 amazkovoi Exp $
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
import org.ofbiz.core.entity.model.ModelField;

import java.util.Hashtable;
import java.util.Map;

/**
 * Sequence Utility to get unique sequences from named sequence banks
 * Uses a collision detection approach to safely get unique sequenced ids in banks from the database
 *
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version $Revision: 1.4 $
 * @since 2.0
 */
public class SequenceUtil {

    public static final String module = SequenceUtil.class.getName();

    private final Map<String, SequenceBank> sequences = new Hashtable<>();
    private final String helperName;
    private final String tableName;
    private final String nameColName;
    private final String idColName;
    private final boolean clustering;

    public SequenceUtil(String helperName, ModelEntity seqEntity, String nameFieldName, String idFieldName, boolean clustering) {
        this.helperName = helperName;
        if (seqEntity == null) {
            throw new IllegalArgumentException("The sequence model entity was null but is required.");
        }
        this.tableName = seqEntity.getTableName(helperName);

        ModelField nameField = seqEntity.getField(nameFieldName);

        if (nameField == null) {
            throw new IllegalArgumentException("Could not find the field definition for the sequence name field " + nameFieldName);
        }
        this.nameColName = nameField.getColName();

        ModelField idField = seqEntity.getField(idFieldName);

        if (idField == null) {
            throw new IllegalArgumentException("Could not find the field definition for the sequence id field " + idFieldName);
        }
        this.idColName = idField.getColName();

        this.clustering = clustering;
    }

    public Long getNextSeqId(String seqName) {
        SequenceBank bank = sequences.get(seqName);

        if (bank == null) {
            bank = constructSequenceBank(seqName);
        }

        return bank.getNextSeqId();
    }

    /**
     * this is hit if we can't get one from the cache, must be synchronized
     */
    private synchronized SequenceBank constructSequenceBank(final String seqName) {
        // check the cache first in-case someone has already populated
        return sequences.computeIfAbsent(seqName, key -> new SequenceBank(key, clustering,
                new SequenceBank.SequenceTableParams()
                        .withHelperName(helperName)
                        .withTableName(tableName)
                        .withIdColName(idColName)
                        .withNameColName(nameColName)));
    }
}
