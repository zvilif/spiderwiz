package org.spiderwiz.core;

/**
 * Provide the root for storing the entire object hierarchy.
 * @author zvil
 */
class RootObject extends DataObject{
    public static String ObjectCode = null;
    
    @Override
    protected String getParentCode() {
        return null;
    }

    @Override
    protected boolean isDisposable() {
        return false;
    }
}
