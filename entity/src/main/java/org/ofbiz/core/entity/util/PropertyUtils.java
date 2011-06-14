package org.ofbiz.core.entity.util;

import java.util.Properties;

/**
 * Utility class for methods that operate on Property instances.
 */
public class PropertyUtils
{
    /**
     * Returns a copy of the given Properties instance.
     *
     * @param properties a Properties instance
     * @return a copy of the given Properties instance, or null
     */
    public static Properties copyOf(Properties properties)
    {
        if (properties == null)
        {
            return null;
        }

        Properties copy = new Properties();
        for (String key : properties.stringPropertyNames())
        {
            copy.setProperty(key, properties.getProperty(key));
        }

        return copy;
    }

    private PropertyUtils()
    {
        // prevent instantiation
    }
}
