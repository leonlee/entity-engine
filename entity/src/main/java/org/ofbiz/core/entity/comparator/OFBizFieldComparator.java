package org.ofbiz.core.entity.comparator;

import org.ofbiz.core.entity.GenericValue;
import org.apache.log4j.Category;

public class OFBizFieldComparator implements java.util.Comparator
{
    private static final Category log = Category.getInstance(OFBizFieldComparator.class);

    String fieldname;

    public OFBizFieldComparator(String fieldname)
    {
        this.fieldname = fieldname;
    }

    public int compare(Object o1, Object o2)
    {
        try
        {
            GenericValue i1 = (GenericValue) o1;
            GenericValue i2 = (GenericValue) o2;

            if (i1 == null && i2 == null)
                return 0;
            else if (i2 == null) // any value is less than null
                return -1;
            else if (i1 == null) // null is greater than any value
                return 1;

            String s1 = i1.getString(fieldname);
            String s2 = i2.getString(fieldname);

            if (s1 == null && s2 == null)
                return 0;
            else if (s2 == null) // any value is less than null
                return -1;
            else if (s1 == null) // null is greater than any value
                return 1;
            else
                return s1.compareToIgnoreCase(s2);
        }
        catch (Exception e)
        {
            log.error("Exception: " + e, e);
        }
        return 0;
    }
}
