package org.ofbiz.core.entity;

/**
 * A transformation upon an entity.
 *
 * @since 1.0.41
 */
public interface Transformation {

    /**
     * Transforms the given entity in place.
     *
     * @param entity the entity to transform (never null)
     */
    void transform(GenericValue entity);
}
