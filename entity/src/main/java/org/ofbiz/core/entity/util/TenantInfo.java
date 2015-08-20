package org.ofbiz.core.entity.util;

import org.ofbiz.core.entity.config.DatasourceInfo;

/**
 * Information related to the current tenant.
 * Basically allows you to grab the datasource info that
 */
public class TenantInfo
{
    public static final ThreadLocal<DatasourceInfo> TENANT_DATASOURCE = new ThreadLocal<>();

    public static DatasourceInfo getTenantDatasource()
    {
        return TENANT_DATASOURCE.get();
    }
}
