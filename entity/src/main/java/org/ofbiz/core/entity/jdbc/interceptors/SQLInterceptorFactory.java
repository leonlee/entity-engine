package org.ofbiz.core.entity.jdbc.interceptors;

/**
 * {@link SQLInterceptorFactory} is responsible for generating {@link SQLInterceptor}s when asked.
 * <p/>
 * An instance of this class will be loaded from THE class path resource called "ofbiz-database.properties" and must
 * have public no-arges constructor.
 * <p/>
 * The factory is responsible for giving out {@link SQLInterceptor}s.  Its these objects that are used as call backs for
 * each SQL statement that is executed.  An SQLInterceptor is asked for for every new SQL statement.  So its up to the
 * factory to decide whether to allocate a new one, re-use them or make them stateless.  But remember that new gen
 * allocation is blindingly fast, so the suggestion is to allocate a new one every time.
 */
public interface SQLInterceptorFactory
{
    /**
     * This is called to generate a new {@link SQLInterceptor} ready to be called back when SQL statements are executed.
     *
     * @param ofbizHelperName the name of the OFBIZ entity helper eg a named {@link
     * org.ofbiz.core.entity.GenericHelper}
     * @return an SQLInterceptor to use.  If null is returned, then no interception is possible.
     */
    SQLInterceptor newSQLInterceptor(String ofbizHelperName);
}
