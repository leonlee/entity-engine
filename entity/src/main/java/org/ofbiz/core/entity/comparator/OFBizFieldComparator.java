package org.ofbiz.core.entity.comparator;

import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericValue;

public class OFBizFieldComparator implements java.util.Comparator<GenericValue>
{
    private static final Logger log = Logger.getLogger(OFBizFieldComparator.class);

    String fieldname;

    public OFBizFieldComparator(String fieldname)
    {
        this.fieldname = fieldname;
    }

    public int compare(GenericValue o1, GenericValue o2)
    {
        try
        {
            if (o1 == null && o2 == null)
                return 0;
            else if (o2 == null) // any value is less than null
                return -1;
            else if (o1 == null) // null is greater than any value
                return 1;

            String s1 = o1.getString(fieldname);
            String s2 = o2.getString(fieldname);

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
