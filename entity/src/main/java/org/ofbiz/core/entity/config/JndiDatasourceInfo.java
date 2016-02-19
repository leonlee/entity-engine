package org.ofbiz.core.entity.config;

/**
 * JNDI data source info
 */
public class JndiDatasourceInfo {
    private final String jndiName;
    private final String jndiServerName;

    public JndiDatasourceInfo(final String jndiName, final String jndiServerName) {
        this.jndiName = jndiName;
        this.jndiServerName = jndiServerName;
    }

    public String getJndiName() {
        return jndiName;
    }

    public String getJndiServerName() {
        return jndiServerName;
    }
}
