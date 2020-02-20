package org.spiderwiz.core;

/**
 * An implementation of {@link org.spiderwiz.core.Filter} that catches all <em>data objects</em> of a specific type.
 * @param <T> the type of data objects maintained by this filter.
 */
public class CatchAllFilter<T extends DataObject> extends Filter<T>{

    /**
     * Constructs a filter for a specific <em>data object type</em>.
     * @param <T>       the type of data objects this filter applies to.
     * @param type      the filter applies to objects that are instances of this class.
     * @throws java.lang.NoSuchFieldException       if class <em>type</em> does not contain a static field of name <em>ObjectCode</em>.
     * @throws java.lang.IllegalAccessException     if <em>ObjectCode</em> field is not public.
     */
    public <T extends DataObject> CatchAllFilter(Class<T> type) throws NoSuchFieldException, IllegalAccessException {
        super(type);
    }

    /**
     * Returns {@code true} for every object.
     * @param object    the object checked by the method.
     * @return true
     */
    @Override
    protected boolean filterObject(T object) {
        return true;
    }
}
