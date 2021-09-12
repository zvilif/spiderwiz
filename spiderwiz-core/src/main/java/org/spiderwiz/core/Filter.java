package org.spiderwiz.core;

/**
 * A base class for implementing <em>Data Object Filters</em>.
 * <p>
 * A Spiderwiz <em>filter</em> is an object that lets you traverse the <em>data object tree</em>, or a branch of it, to collect data
 * objects of a specific type that meet a specific condition. You apply the filter by calling
 * {@link org.spiderwiz.core.DataObject#getFilteredChildren(org.spiderwiz.core.Filter) DataObject.getFilteredChildren()} on the parent
 * of the branch you want to filter. If you want to apply the filter on the entire data object tree, call it on its
 * {@link org.spiderwiz.core.Main#getRootObject() Root Object}.
 * <p>
 * Filtering works as follows. First, the object being filtered is checked for the existence of direct children of the specified type.
 * If there are any, {@link #filterObject(org.spiderwiz.core.DataObject) filterObject()} is applied on them and the children that
 * returns {@code true} are collected. If the filtered object does not have direct children of the specified type,
 * {@link #filterParent(org.spiderwiz.core.DataObject) filterParent()} is applied on every child of the filtered object, regardless of
 * its type, and if the method returns {@code true} then the filtering mechanism is applied on the child recursively.
 *
 * @param <T> the type of data objects maintained by an implementation of this filter.
 */
public abstract class Filter<T extends DataObject> {
    private final String objectCode;

    /**
     * Constructs a filter for a specific <em>data object type</em>.
     * @param <T>       the type of data objects this filter applies to.
     * @param type      the filter applies to objects that are instances of this class.
     * @throws java.lang.NoSuchFieldException       if class <em>type</em> does not contain a static field of name <em>ObjectCode</em>.
     * @throws java.lang.IllegalAccessException     if <em>ObjectCode</em> field is not public.
     */
    public <T extends DataObject> Filter(Class<T> type) throws NoSuchFieldException, IllegalAccessException {
        objectCode = DataObject.getObjectCode(type);
    }

    final String getObjectCode() {
        return objectCode;
    }
    
    /**
     * Implement this method to return {@code true} if the filtered object passes the filter.
     * @param object    the filtered object.
     * @return true if and only if the filtered object passes the filter.
     */
    protected abstract boolean filterObject(T object);
    
    /**
     * Implement this method to return {@code true} if filtering shall be applied on the branch under this object.
     * @param parent   the parent of the filtered branch.
     * @return true if and only if filtering shall be applied on the branch under this object.
     */
    protected boolean filterParent(DataObject parent) {return true;}
}
