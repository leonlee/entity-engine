package org.ofbiz.core.entity.comparator;

import org.ofbiz.core.entity.GenericValue;

import java.util.Comparator;
import java.util.Date;

public class OFBizDateComparator implements Comparator<GenericValue>
{
    String fieldname;

    public OFBizDateComparator(String fieldname)
    {
        this.fieldname = fieldname;
    }

    public int compare(GenericValue o1, GenericValue o2)
    {
        if (o1 == null && o2 == null)
             return 0;
         else if (o2 == null) // any value is less than null
             return -1;
         else if (o1 == null) // null is greater than any value
             return 1;

        Date u1 = o1.getTimestamp(fieldname);
        Date u2 = o2.getTimestamp(fieldname);


        if (u1 == null && u2 == null)
            return 0;
        if (u1 == null)
            return -1;
        if (u2 == null)
            return 1;

        return u1.compareTo(u2);
    }
}
