package org.spiderwiz.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.spiderwiz.zutils.ZDate;
import org.spiderwiz.zutils.ZHashMap;
import org.spiderwiz.zutils.ZUtilities;
import org.spiderwiz.zutils.Ztrings;

/**
 * A base class for all <em><b>Data Objects</b></em>.
 * <p>
 * The <em>Spiderwiz</em> framework paradigm is based on the idea that all the data that an application needs is already shared to
 * it through the network. The shared data has a 3-dimensional structure. The atomic unit
 * is a <em>Data Object</em>. Data objects are arranged in a <em>hierarchy</em>. Data objects may change along the <em>time</em>.
 * The applications that are connected through the network, we call them <em>Data Nodes</em>, may manipulate the object
 * tree or modify data objects (i.e. act as <em>Producers</em>), and the changes are automatically and instantly shared throughout
 * the network, reaching data nodes that declare themselves as <em>Consumers</em> of specific data object types.
 * <p>
 * Typically, classes that extend {@code DataObject} define <em>Properties</em>, specified as class fields. Property fields shall be
 * <em>serializable</em> and annotated by {@link org.spiderwiz.annotation.WizField @WizField} (see the annotation
 * description for what it takes to be serializable).
 * Producers change data object properties and {@link #commit() commit} the changes. Consumers respond to {@link #onEvent() events},
 * examine the (modified) properties and act accordingly.
 * <p><a id=ObjectCode></a>
 * <b>Object Code</b>
 * <p>
 * Classes that extend this class must define a {@code public static String} field named <b>{@code ObjectCode}</b> that identifies the
 * <em>data object type</em>. The value assigned to this field must be unique across the service mesh. Object codes may contain any
 * character except comma (','). {@code ObjectCode} values are used in methods such as
 * {@link org.spiderwiz.core.Main#getProducedObjects()}, {@link org.spiderwiz.core.Main#getConsumedObjects()} and
 * {@link #getParentCode()}.
 * <p>
 * A class may extend a data object class that has a defined type without redefining the {@code ObjectCode} field. The subclass may
 * even add <em>property fields</em>. The two classes are considered as two versions of the same data object type, which is valid.
 * However multiple versions of the same data object type shall be defined in a row, i.e. a version must contain all the property fields
 * of the previous version. Versions cannot fork in a way that two subclasses extend the same superclass and each of them adds
 * different property fields. If this happens each of the forking versions must have a unique {@code ObjectCode} value.
 * <p>
 * The class has methods for:
     * <ul>
     * <li>Getting general properties of the object.</li>
     * <li>Manipulating the object tree.</li>
     * <li>Committing object changes.</li>
     * <li>Overriding by subclasses to provide event handlers.</li>
     * <li>Custom object serialization/deserialization.</li>
     * <li>Some other actions that can be done on a data object.</li>
     * </ul>
 * @author  zvil

 */
@org.spiderwiz.annotation.WizObject
public abstract class DataObject {
    /**
     * Lossless delivery marker.
     * <p>
     * Append this value to an object code you return in {@link org.spiderwiz.core.Main#getConsumedObjects()} to signal that lossless
     * delivery is required for that object type.
     * <p>
     * When lossless delivery is requested, Spiderwiz framework constructs a buffering/acknowledgment mechanism that guarantees
     * lossless object delivery of the specific data object type from producers to the consumer that requests it. When a producer
     * detects that acknowledgments for one or more objects have been skipped, it resends the skipped objects. The mechanism works
     * end-to-end between the producer application and the consumer application, regardless of the route it takes between them.
     * <p>
     * Lossless mechanism remains active even when the consumer application goes offline. If the producer notices that the consumer of
     * a lossless object has gone offline, it keeps buffering the produced objects until the consumer application comes back to life.
     * The mechanism expires and data is lost after the consumer application disappears for over 24 hours.
     * <p>
     * Note that the lossless mechanism is orderless. Skipped objects may be sent out of order. If you need the data in guaranteed
     * order, use an appropriate transport mechanism such as <a href="https://kafka.apache.org/">Apache Kafka</a> (for which you
     * would need a {@link org.spiderwiz.core.Channel} based plug-in).
     * <p>
     * Example:
     * <pre>
    &#64;Override
    protected String[] getConsumedObjects() {
        return new String[]{
            MyLossyObject.ObjectCode, MyLosslessObject.ObjectCode + DataObject.Lossless, 
        };
    }</pre>
     */
    public final static String Lossless = "+";
    final static String RemoveIndicator = "~";
    
    private class ObjectMap extends ZHashMap<String, DataObject>{
        /**
         * Reset all object types that are children of this object
         * @param resetter
         */
        final void resetObject(Resetter resetter) {
            lockRead();
            try {
                for (DataObject obj : values()) {
                    if (resetter.isAborted())
                        return;
                    obj.resetObject(resetter);
                }
            } finally {
                unlockRead();
            }
        }
    }

    private class ChildMap extends ZHashMap<String, ObjectMap>{
        /**
         * Reset all objects of the given type
         * @param resetter
         */
        final void resetObject(Resetter resetter) {
            lockRead();
            try {
                for (ObjectMap map : values()) {
                    if (resetter.isAborted())
                        return;
                    map.resetObject(resetter);
                }
            } finally {
                unlockRead();
            }
        }
    }
    
    private class StateManager {
        private String objectID = "";
        private DataObject parent = null;
        private UUID originUUID = null;
        private String userID = null;
        private DataHandler dataChannel = null;
        private String rawCommand = null;
        private ZDate objectTime;
        private boolean removed = false;
        private String rename = null;
        private Collection<UUID> destinations = null;

        public StateManager(ZDate objectTime) {
            this.objectTime = objectTime;
        }

        synchronized String getObjectID() {
            return objectID;
        }

        synchronized void setObjectID(String objectID) {
            this.objectID = objectID;
        }

        synchronized DataObject getParent() {
            return parent;
        }

        synchronized void setParent(DataObject parent) {
            this.parent = parent;
        }

        synchronized UUID getOriginUUID() {
            return originUUID;
        }

        synchronized void setOriginUUID(UUID originUUID) {
            this.originUUID = originUUID;
        }

        synchronized String getUserID() {
            return userID;
        }

        synchronized void setUserID(String userID) {
            this.userID = userID;
        }

        synchronized DataHandler getDataChannel() {
            return dataChannel;
        }

        synchronized void setDataChannel(DataHandler dataChannel) {
            this.dataChannel = dataChannel;
            userID = dataChannel.getAppUser();
        }

        synchronized String getRawCommand() {
            return rawCommand;
        }

        synchronized void setRawCommand(String rawCommand) {
            this.rawCommand = rawCommand;
        }

        synchronized ZDate getObjectTime() {
            return objectTime;
        }

        synchronized void setObjectTime(ZDate objectTime) {
            this.objectTime = objectTime;
        }

        synchronized boolean isObsolete() {
            return removed || rename != null; 
        }

        synchronized void setRemoved(boolean removed) {
            this.removed = removed;
        }

        synchronized String getRename() {
            return rename;
        }

        synchronized void setRename(String rename) {
            this.rename = rename;
        }

        synchronized DataObject delete() {
            removed = true;
            return parent;
        }
    
        synchronized DataObject undelete() {
            removed = false;
            return parent;
        }
        
        synchronized String getNewID() {
            return rename != null ? rename : removed ? "" : null;
        }

        synchronized Collection<UUID> getDestinations() {
            return destinations;
        }

        synchronized void setDestinations(Collection<UUID> destinations) {
            this.destinations = destinations;
        }

        synchronized String getFullKey() {
            StringBuilder key = new StringBuilder();
            if (parent != null)
                key.append(parent.getFullKey());
            if (getObjectID() != null) {
                if (key.length() > 0)
                    key.append(Serializer.BAR_SEPARATOR);
                key.append(Serializer.escapeDelimiters(getObjectID()));
            }
            Serializer.removeTrailingDelimiters(key, 0, Serializer.BAR_SEPARATOR);
            return key.toString();
        }
    }
    
    private String objectCode = null;
    private final ChildMap childMap;
    private final StateManager stateManager;

    /**
     * Constructs a <em>data object</em>.
     * <p>
     * Normally you would not instantiate objects of this class directly. They are created by the framework, or you can use
     * {@link #createChild(java.lang.Class, java.lang.String) createChild()},
     * {@link org.spiderwiz.core.Main#createTopLevelObject(java.lang.Class, java.lang.String) Main.createTopLevelObject()} or
     * {@link org.spiderwiz.core.Main#createQuery(java.lang.Class) Main.createQuery()}.
     */
    protected DataObject() {
        childMap = new ChildMap();
        stateManager = new StateManager(ZDate.now());
    }
    
    /**
     * Returns the object code of the parent of this <em>data object</em>.
     * <p>
     * An abstract method that must be implemented to return the <a href="#ObjectCode"><em>Object Code</em></a> 
     * of the parent of this object. If this is a
     * {@link org.spiderwiz.core.Main#createTopLevelObject(java.lang.Class, java.lang.String) top level object}, return null.
     * @return the object code of this object's parent, or null if this is a top level object.
     */
    abstract protected String getParentCode();
    
    /**
     * Returns {@code true} if the object is disposable.
     * <p>
     * An abstract method that must be implemented to determine whether this <em>data object</em> is disposable, i.e. shall not be
     * stored in the application's <em>data object tree</em>. Disposable objects are discarded after their 
     * {@link #onEvent()} and {@link #onAsyncEvent()} methods are executed.
     * @return true if and only if the object is disposable.
     */
    abstract protected boolean isDisposable();
    
    /**
     * Returns {@code true} if this is an urgent data object.
     * <p>
     * Override this method to mark the object as <em>urgent</em>. Urgent objects have priority over other items circulated by the
     * framework, and they are also flushed to the network immediately when transmitted.
     * The default implementation returns {@code false}.
     *
     * @return true if and only if this is an urgent object.
     */
    protected boolean isUrgent() {
        return false;
    }
    
    /**
     * Returns {@code true} if the object ID is case sensitive.
     * <p>
     * Override this method if IDs of this <em>data object</em> type are case insensitive, i.e. if their case shall be ignored in object
     * lookups. By default object IDs are case sensitive.
     * @return true if and only if the object ID is case sensitive (the default).
     */
    protected boolean isCaseSensitive() {return true;}

    /**
     * Get the number of execution threads to allocate for asynchronous event processing of objects of this type.
     * <p>
     * Upon application startup, the framework allocates execution threads, separately for each Data Object type,
     * for asynchronous event processing of objects of this type. By default the number of threads for each type is the number
     * of CPUs in the machine. You can override this method to return a different value. Possible return values are: a negative
     * value to use the default, zero to eliminate asynchronous processing and process each object synchronously, and a positive
     * value to set the number of threads.
     * <p>
     * Note that if multiple threads are allocated then the order of event processing cannot be guaranteed. For this reason,
     * if a certain object type is consumed in {@link DataObject#Lossless lossless mode}, the number of threads that are allocated
     * to process it will never be more than one, regardless of the value returned by this method, since lossless delivery mandates
     * orderly processing.
     * @return  a negative value to use the default, zero to eliminate asynchronous processing and process each object synchronously,
     * and a positive value to set the number of threads.
     */
    protected int getThreadAllocation() {
        return -1;
    }

    /**
     * Returns the UUID of the object producer.
     * @return the UUID of the application that originated the object.
     */
    public final UUID getOriginUUID() {
        return stateManager.getOriginUUID();
    }

    /**
     * Returns the user ID attached to the object.
     * <p>
     * A <em>data object</em> may have an associated <em>user ID</em>. The user ID may be set manually by
     * {@link #setUserID(java.lang.String) setUserID()} or may be set by the framework when the object is received over a network
     * channel that has an associated user ID (see <a href="doc-files/config.html">application's configuration file</a> how to
     * associate a user ID to a network channel). Use this method to get the associated user ID and deal with it programmatically.
     * @return a user ID or null if none is associated.
     */
    public String getUserID() {
        return stateManager.getUserID();
    }

    /**
     * Attaches a user ID to the object.
     * <p>
     * A <em>data object</em> may have an associated <em>user ID</em>. The user ID may be set manually by this method
     * or may be set by the framework when the object is received over a network
     * channel that has an associated user ID (see <a href="doc-files/config.html">application's configuration file</a> how to
     * associate a user ID to a network channel).
     * @param userID    the user ID to attach to the object.
     */
    public void setUserID(String userID) {
        stateManager.setUserID(userID);
    }

    /**
     * Returns the object ID.
     * <p>
     * An object ID identifies an object among all children of a specific parent object that are of the same type. By default
     * the ID of a <em>data object</em> is an empty string. You can override this method to return null if no object of this type has
     * a key.
     * <p>
     * Note that object IDs can be case sensitive or case insensitive. By default they are case sensitive, but you can override
     * {@link #isCaseSensitive()} to make them insensitive.
     * @return the object ID.
     */
    public String getObjectID() {
        return stateManager.getObjectID();
    }

    /**
     * Returns the parent object of this object.
     * <p>
     * Get the parent of this object in the data object tree. If this is a top level object, the method returns null.
     * @return the parent object of this object or null.
     */
    public final DataObject getParent() {
        return stateManager.getParent();
    }

    /**
     * Returns a child of this object with specific class type and object ID.
     * @param <T>   class type of the object you are looking for.
     * @param type  the Class of the object you are looking for. The class must contain or inherit a public static String field named
     *              <em>ObjectCode</em>.
     * @param id    the ID of the child you are looking for.
     * @return if a child object with the specified class type and ID is found then return the object, otherwise return null.
     * @throws java.lang.NoSuchFieldException       if the class <em>type</em> does not contain a static <em>ObjectCode</em> field.
     * @throws java.lang.IllegalAccessException     if <em>ObjectCode</em> field of class <em>type</em> is not public.
     */
    public <T extends DataObject> T getChild(Class<T> type, String id) throws NoSuchFieldException, IllegalAccessException {
        return type.cast(getChild(getObjectCode(type), id));
    }
    
    /**
     * Creates a child <em>data object</em>.
     * <p>
     * The method instantiates an object of the specified type, assigns it the specified ID and adds it to the object tree as a
     * child of the current object. The object is added locally. To share it on the network call {@link #commit()} on the newly
     * created object.
     * @param <T>   class type of the object you want to create.
     * @param type  a Class object that must contain or inherit a public static String field named <em>ObjectCode</em> that indicates
     *              the type of the created object. The exact type that will be created is determined by your implementation of
     *              {@link org.spiderwiz.core.Main#populateObjectFactory(java.util.List) Main.populateObjectFactory()}.
     * @param id    the ID of the required child, or null if this is a single child.
     * @return      the created object, or null if the object could not be created or its parent code is not equal the current
     *              object code. If a child of this type and ID already exists return the existing object.
     * @throws java.lang.NoSuchFieldException       if the class <em>type</em> does not contain a static <em>ObjectCode</em> field.
     * @throws java.lang.IllegalAccessException     if <em>ObjectCode</em> field of class <em>type</em> is not public.
     */
    public <T extends DataObject> T createChild(Class<T> type, String id)
        throws NoSuchFieldException, IllegalAccessException
    {
        String code = getObjectCode(type);
        DataObject object = getChild(code, id);
        if (object != null)
            return type.cast(object);
        object = Main.getInstance().createDataObject(code);
        if (object == null || 
            (object.getParentCode() == null ? getObjectCode() != null : !object.getParentCode().equals(getObjectCode()))
        )
            return null;
        if (id != null && !object.isCaseSensitive())
            id = id.toLowerCase();
        object.setObjectID(id == null ? "" : id);
        object.setParent(this);
        if (object.getObjectID() != null && !object.isDisposable() && !Main.getMyConfig().isPropertySet(MyConfig.PASS_THROUGH))
            storeChild(object);
        return type.cast(object);
    }
    
    /**
     * Removes the object.
     * <p>
     * Call this method to remove the object from the <em>data object tree</em>. The method removes the object locally. To share the
     * removal with consumers on the network call {@link #commit()} after calling the method.
     * 
     * @return the removed object. If the object is not removable because it is {@link #isDisposable() disposable} or
     * it has already been removed then return null.
     */
    public DataObject remove() {
        if (!isObsolete() && !isDisposable()) {
            delete();
            return this;
        }
        return null;
    }
    
    /**
     * Renames the object.
     * <p>
     * Call this method to modify the {@link #getObjectID() object ID}. If an object with the required ID already exists, the current
     * object is not renamed and the method returns {@code null}. If the object is renamed successfully, the method returns an obsolete
     * object that retains the old ID of the renamed object. The method renames the object locally. To share the action
     * with consumers on the network call {@link #commit()} on the returned object.
     * <p>
     * Note that if you modify the object after renaming it you will need to call {@link #commit()} on the renamed object in order to
     * share the modifications on the network <b>after</b> you call the method on the obsolete object to rename it.
     * @param newID     new object ID
     * @return          the obsolete renamed object or null if the object could not be renamed.
     */
    public DataObject rename(String newID) {
        if (getParent() != null && !isDisposable() && newID != null && !newID.isEmpty()) {
            if (!isCaseSensitive())
                newID = newID.toLowerCase();
            if (!newID.equals(getObjectID()))
                return getParent().renameChild(this, newID);
        }
        return null;
    }
    
    /**
     * Returns a collection of all object offspring of a given type that adhere to given rules.
     * <p>
     * This method is used to traverse the <em>data object</em> branch under this object and collect objects that are
     * of specific type and adhere to specific rules. The type and rules are determined by the
     * {@link org.spiderwiz.core.Filter} object provided as a parameter to this method.
     * <p>
     * The method works as follows. First, it checks if the object has direct children of the specified type. If it does,
     * it walks through all of them, applies the filtering methods defined in the Filter object and collects the objects that
     * pass through it. If the current object does not have direct children of the specified type, the method is applied recursively
     * on all the children regardless of their type.
     * <p>
     * Hint: while the object offsprings are traversed using your {@link org.spiderwiz.core.Filter} implementation, you can
     * manipulate the objects that are walked through even if they do not pass through the filter.
     * 
     * @param <T>       class type of the specified filter.
     * @param filter    an implementation-specific extension of the {@link org.spiderwiz.core.Filter} object.
     * @return          a collection of objects of the type of the filter that pass through the filter.
     */
    public <T extends DataObject> List<T> getFilteredChildren(Filter<T> filter) {
        if (filter == null)
            return null;
        ArrayList<T> collection = new ArrayList<>();
        if (getObjectCode() != null && !filter.filterParent(this))
            return collection;
        ObjectMap map = childMap.get(filter.getObjectCode());
        if (map != null) {
            // If the object contains children of the filter's type apply the filter on each child and collect
            // the children that adhere to its rules.
            map.lockRead();
            try {
                for (DataObject object : map.values()) {
                    T t = (T)object;
                    if (t != null && !t.isObsolete() && filter.filterObject(t))
                        collection.add(t);
                }
            } finally {
                map.unlockRead();
            }
        } else {
            // If the object does not contain children of the filter's type go over all children
            // and apply the filter recursively.
            childMap.lockRead();
            try {
                for (ObjectMap mapIterator : childMap.values()) {
                    for (DataObject object : mapIterator.values()) {
                        if (object != null && !object.isObsolete())
                            collection.addAll(object.getFilteredChildren(filter));
                    }
                }
            } finally {
                childMap.unlockRead();
            }
        }
        return collection;
    }
    
    /**
     * Commits object changes and distributes it to all consumers.
     * <p>
     * Call this method after you modify object properties, add a new object with
     * {@link #createChild(java.lang.Class, java.lang.String) createChild()}, remove it with
     * {@link #remove()} or rename it with {@link #rename(java.lang.String) rename()}.
     * <p>
     * By default the committed object is distributed to all the consumers of this object type. However, you can restrict the
     * distribution by implementing
     * {@link #filterDestination(java.util.UUID, java.lang.String, java.lang.String, java.lang.String, java.util.Map) 
     * filterDestination()}.
     */
    public void commit() {
        commit(null);
    }
    
    /**
     * Commits object changes and distributes it to the provided list of destinations.
     * <p>
     * Use this version of the Commit method to target distribution to applications whose UUIDs are included in the list
     * defined by the {@code destinations} parameter.
     * <p>
     * If {@code destinations} is an empty string, the object will not be distributed to other applications but will
     * still be exported to foreign systems if
     * {@link #exportObject(org.spiderwiz.core.ImportHandler, java.lang.String) exportObject()} is overridden and returns a non-null
     * value.
     * <p>
     * Note that you can restrict distribution further by implementing
     * {@link #filterDestination(java.util.UUID, java.lang.String, java.lang.String, java.lang.String, java.util.Map) 
     * filterDestination()}.
     * @param destinations  a list of UUID values concatenated by a semicolon (';'). A null value indicates unrestricted
     *                      distribution.
     */
    public void commit(String destinations) {
        stateManager.setObjectTime(ZDate.now());
        Collection<UUID> uuidList = destinations == null ? null : Ztrings.split(destinations).toUUIDs();
        
        // Check first if for myself
        if (DataManager.getInstance().isConsumingObject(getObjectCode()) &&
            (uuidList == null || uuidList.contains(Main.getInstance().getAppUUID())) &&
            !isObsolete() &&
            filterDestination(
                Main.getInstance().getAppUUID(), Main.getInstance().getAppName(), null, null, Main.getInstance().getAppParams())
        )
            DataManager.getInstance().objectEvent(this);
        
        // Now propagate to others.
        try {
            propagate(false, uuidList, null);
        } catch (Exception ex) {
            Main.getInstance().sendExceptionMail(
                ex, String.format(CoreConsts.AlertMail.COMMIT_ALERT, getClass().getName()), null, false);
        }
    }
    
    /**
     * Filters a destination of object distribution.
     * <p>
     * Override this method if you want to restrict object distribution by matching object and destination properties. The
     * method is called once for each potential consumer application.
     * You can examine application information and determine whether or not to approve the distribution to the
     * candidate application. The information is provided in the method parameters as follows:
     * @param appUUID       {@link org.spiderwiz.core.Main#getAppUUID() application UUID}.
     * @param appName       {@link org.spiderwiz.core.Main#getAppName() application name}.
     * @param userID        the user IDs attached to the network channel through which the destination application is connected to the
     *                      current application. (see <a href="doc-files/config.html#UserID">application's configuration file</a> how to
     *                      associate a user ID to a network channel). In case of multiple user IDs (when connecting the same
     *                      application through multiple connections with different user IDs), the value of this parameter is a
     *                      comma-separated concatenations of all IDs. If no user ID has been specified then the value is null.
     * @param remoteAddress remote address of the destination application.
     * @param appParams     application parameter map as set by {@link org.spiderwiz.core.Main#getAppParams()}
     *                      method of the destination application. May be null if the destination application did not define any
     *                      parameters.
     * @return              true to approve distribution to this application, false to deny it. The default implementation of the
     *                      method always returns true.
     */
    protected boolean filterDestination(UUID appUUID, String appName, String userID, String remoteAddress, Map<String,String> appParams) {
        return true;
    }
    
    /**
     * A hook for performing an action before the object is distributed.
     * <p>
     * Override this method if you want to perform an action, e.g. lock a resource, before the object is distributed over the network.
     */
    protected void preDistribute() {}
    
    /**
     * A hook for performing an action after the object is distributed.
     * <p>
     * Override this method if you want to perform an action, e.g. unlock a resource, after the object is distributed over the network.
     */
    protected void postDistribute() {}
    
    /**
     * Handles an object event.
     * <p>
     * An object event occurs when a producer {@link #commit() commits} the object. If you are a consumer of the object,
     * override this method to handle the event.
     * <p>
     * This method handles the event synchronously, i.e. on the same thread that reads the object from the communication line.
     * Override {@link #onAsyncEvent()} to handle the event asynchronously.
     * @return true if the object is handled successfully and further asynchronous handling shall not take place. The default
     * implementation of this method returns false.
     */
    protected boolean onEvent() {
        return false;
    }
    
    /**
     * Handles an object event asynchronously.
     * <p>
     * An object event occurs when a producer {@link #commit() commits} the object. If you are a consumer of the object,
     * you may override this method to handle the event.
     * <p>
     * This method handles the event asynchronously, i.e. on a different thread than the one that reads the object from the
     * communication line. Override {@link #onEvent()} to handle the event synchronously.
     * @return true if the object is handled successfully, false if not. In the latter case, if the object is consumed in
     * {@link #Lossless lossless mode} its reception will not be acknowledged and that will cause the producer to resend it.
     */
    protected boolean onAsyncEvent() {
        return true;
    }

    /**
     * Handles a new object event.
     * <p>
     * This method is called when a new object is added to the <em>data object tree</em>. Override it if you need to
     * do something in this case.
     * <p>
     * The method is called in addition to {@link #onEvent()} and {@link #onAsyncEvent()} that are called whenever an object is
     * committed.
     */
    protected void onNew() {
    }
    
    /**
     * Handles an object removal event.
     * <p>
     * This method is called when the object is removed from the <em>data object tree</em>. Override it if you need
     * to do something in this case.
     * @return true to confirm the removal. If for any reason you want to refuse it and keep the object in this application's space.
     */
    protected boolean onRemoval() {
        return true;
    }
    
    /**
     * Handles an object rename event.
     * <p>
     * This method is called when the object ID has been changed. Override it if you need to do something in this
     * case.
     * @param oldID     the old object ID.
     */
    protected void onRename(String oldID) {
    }
    
    /**
     * Serializes the object.
     * <p>
     * Object serialization is the process of converting object properties into a string that can be delivered over the network.
     * You may customize the default serialization process by overriding this method.
     * <p>
     * If you override this method you may also need to override {@link #deserialize(java.lang.String) deserialize()}.
     * @param resetting     true when the object is serialized during a
     *                      {@link org.spiderwiz.core.Main#onObjectReset(org.spiderwiz.core.Resetter) reset} process.
     * @return              a string that represents object properties.
     * @throws java.lang.Exception 
     */
    protected String serialize(boolean resetting) throws Exception {
        return serialize();
    }
    
    /**
     * Deserializes the object.
     * <p>
     * Object deserialization is the process of restoring object properties from a string that was delivered over the network.
     * You may customize the default deserialization process by overriding this method.
     * <p>
     * If you override this method you may also need to override {@link #serialize(boolean) serialize()}.
     * @param fields    a string that represents the serialized object properties.
     * @return          this object if deserialization was done successfully and an {@link #onEvent() object event} shall be fired,
     *                  null otherwise.
     * @throws Exception
     */
    protected DataObject deserialize(String fields) throws Exception {
        return Serializer.getInstance().deserialize(this, fields);
    }

    /**
     * Returns true if and only if a received object shall not be propagated to other consumers.
     * <p>
     * Override this method to return true if you process the object exclusively and do not want it to be forwarded to other
     * data nodes.
     * @return true if and only if a received object shall not be propagated to other consumers. The default implementation always
     * returns false.
     */
    protected boolean onlyForMe() {return false;}
    
    /**
     * Exports the object.
     * <p>
     * Exporting a <em>data object</em> is done by converting the object to a form that can be interpreted
     * by the handler of a specific export channel. If, for instance, the handler expects data that is serialized into strings,
     * the exporting involves serialization into a format recognized by the handler. Override this method to implement object
     * conversion that is specific to the
     * channel governed by the {@code channel} parameter, which you can use to retrieve information about the channel.
     * <p>
     * This method is called when an object is {@link #commit() committed}. The {@code newID} parameter indicates the situation
     * that triggered the commit. If it is null then this is a
     * normal commit. If it contains an empty string then the object has been removed. If it contains a non-empty string then the
     * object has been renamed and the parameter contains the new name.
     * @param channel   the handler of the channel the object will be exported to.
     * @param newID     null if this object is active, empty string if it has been removed, non-empty string if it has been renamed.
     * @return          the converted object to export, or null if the object shall not be exported over the given channel.
     */
    protected Object exportObject(ImportHandler channel, String newID){
        return null;
    }
    
    /**
     * Imports the object from foreign data.
     * <p>
     * When data is imported from a foreign system, the framework creates a temporary instance of each <em>data object</em>
     * type that the application produces and calls this method to let the instance determining whether the data is relevant
     * for this type. If it is, the method sets object properties from the imported data and returns a non-null value, which
     * tells the framework to merge the object with the shared object tree.
     * <p>
     * Override this method to implement object conversion that is specific to the import channel governed by the {@code channel}
     * parameter.
     * <p>
     * If the imported data is relevant to this object type then the method shall return a string array that contains
     * the key hierarchy of the object. The first element shall contain the ID of the top ancestor of the object (excluding
     * the <em>root object</em>) and the last element shall contain the ID of the object itself.
     * If the IDs of the object as well as all its ancestors are {@code null} then you can return an empty array.
     * <p>
     * If the imported data is not relevant to this object type then return {@code null}.
     * <p>
     * If the imported data marks the deletion of an object with a certain key hierarchy then call {@link #remove()} on this
     * object and return the key hierarchy.
     * @param data          the imported data object.
     * @param channel       the handler of the channel the data is imported from.
     * @param ts            the timestamp attached to the data by the channel handler.
     * @return the key hierarchy of the imported object, or null if the imported data is not relevant to an object of this type.
     * @throws Exception
     */
    protected String[] importObject(Object data, ImportHandler channel, ZDate ts) throws Exception {return null;}

    /**
     * Returns the pathname pattern of the files where objects of this type are archived.
     * <p>
     * Spiderwiz framework has a mechanism for <em>archiving and restoring data objects</em>. Object data is compressed and stored
     * in local disc files when you call the object's {@link #archive()} method and retrieved by calling the
     * {@link
     * #restore(java.lang.String, java.lang.Object, org.spiderwiz.zutils.ZDate, org.spiderwiz.zutils.ZDate, java.lang.String...) 
     * restore()}
     * method. Note that object archiving is a background operation, designed to work efficiently without hindering the main
     * application tasks.
     * <p>
     * In order to use data object archiving, this method must be overridden to return a path that specifies the location in which
     * the archived data will be stored.
     * The returned value shall be of the form "<em>folder</em>/<em>file</em>.<em>ext</em>", where <em>folder</em> is a sub folder
     * (direct or nested) under the archive folder defined in the <a href="doc-files/config.html">application's configuration file</a>,
     * <em>file</em> is the file name and <em>ext</em> is an optional file name extension (the default is {@code arc}). Any name
     * extension is valid, but if it is either {@code .text} or {@code .txt} its content is stored <b>as plain text</b>,
     * otherwise <b>it is compressed</b> (GZipped).
     * <p>
     * The path may contain parameters, specified by the pound character ('#') followed by the parameter type, that determine how the
     * archived data is aggregated as follows:
     * <p>
     * {@code #0}, {@code #1}, {@code #2} etc.: refer to the object key where {@code #0} is the object ID, {@code #1} is the object's
     * parent ID, {@code #2} is the grandparent ID etc.
     * <p>
     * {@code #y}, {@code #m}, {@code #d}, {@code #h}: refer to the object update time where the letter that follows the pound sign
     * determines the time component - year, month, day and hour.
     * <p>
     * <b>Example:</b>
     * <p>
     * Assuming we develop a car navigation system. We define a data object called {@code Vehicle} that contains properties such as
     * GPS location, speed, heading etc. We track the vehicle through its driver's cellphone, so we use the phone number to identify
     * the vehicle. Phone numbers are grouped by country codes, so we define a data object called {@code Country} and make
     * {@code Vehicle} a child of {@code Country} by implementing:
     * <pre>
    &#64;Override
    protected String getParentCode() {
        return Country.ObjectCode;
    }</pre><p>
    * We want to archive every object update so that we will be able to reconstruct the entire vehicle journey. So we implement
    * for that class:
     * <pre>
    &#64;Override
    protected boolean onEvent() {
        try {
            archive();
        } catch (Exception ex) {
            Main.getInstance().sendExceptionMail(ex, "when archiving", null, false);
        }
        return true;
    }</pre><p>
    * We will now define the archive path pattern:
     * <pre>
    &#64;Override
    protected String getArchivePath() {
        return "vehicles/#1/#0/#y#m#d/#h;
    }</pre><p>
    * Let's say we run the application on January 20, 2017, from noon till 3pm, the phone number of one of the drivers that used
    * the application was "917.756.8000", country code "1". Assuming that we used the default "archive" folder name, then after
    * running the application all this driver's moves during this time would have been recorded in the following folders and files:
     * <ul style="list-style-image:url('doc-files/openFolderSmall.png')">
     * <li>{@code archive}
     * <ul>
     * <li>{@code vehicles}
     * <ul>
     * <li>{@code 1}
     * <ul>
     * <li>{@code 917.756.8000}
     * <ul>
     * <li>{@code 170120}
     * <ul style="list-style-image:url('doc-files/FileArchive.png')">
     * <li>{@code 12.arc}
     * </li>
     * <li>{@code 13.arc}
     * </li>
     * <li>{@code 14.arc}
     * </li>
     * </ul>
     * </li>
     * </ul>
     * </li>
     * </ul>
     * </li>
     * </ul>
     * </li>
     * </ul>
     * </ul>
     * @return a pathname pattern or null if archiving is not used for this object.
     */
    protected String getArchivePath() {return null;}

    /**
     * Archives the object.
     * <p>
     * Archives the object using the pathname pattern returned by {@link #getArchivePath()}.
     * @return true if the object has been archived, false if an archive pathname pattern is not defined.
     * @throws Exception    if archiving failed.
     * @see #getArchivePath() 
     */
    public boolean archive() throws Exception {
        String path = createArchivePath();
        if (path == null)
            return false;
        Main.getInstance().getArchiver().archive(
            path, stateManager.getObjectTime(), getTransmitPrefix(), getObjectCode(), getFullKey(), getObjectValues(false));
        return true;
    }
    
    /**
     * Restores <em>data objects</em> from an archive.
     * <p>
     * A static method used to restore a range of archived objects of a specific type from the archive folder set for that type
     * by {@link #getArchivePath()}. Each restored record will activate {@link #onRestore(java.lang.Object) onRestore()}
     * on the object. Override that method if you want to process the restored record.
     * <p>
     * The parameters of this method specify the object type and the range of the restored records. The {@code objectCode} parameter
     * identifies the object type. The {@code from} and {@code until} parameters specify the record time range.
     * The {@code ids} parameter list lets you select only specific records for restoration by specifying the ID of the
     * restored object or the ID of an ancestor of a group of objects. The first ID in the list refers to the top ancestor
     * of the object. The following IDs refer to objects down the object tree, until the object
     * itself is referred. A {@code null} value for any ID in the list means that all objects of that level should be restored.
     * @param objectCode    the <em>Object Code</em> of the objects to restore.
     * @param associated    a general associated with the restoration that will be provided in subsequent 
     *                      {@link #onRestore(java.lang.Object) onRestore()} calls. Can be null if it is not relevant.
     * @param from          starting record time for restoration. A null value means all records until 'until'.
     * @param until         ending record time for restoration. A null value means "now".
     * @param ids           identify objects or object branches to restore.
     * @return              the number of records that were restored by this call or -1 if restoration was aborted by a value 'false'
     *                      from {@link #onRestore(java.lang.Object) onRestore()}.
     * @see #getArchivePath() 
     */
    public static int restore(String objectCode, Object associated, ZDate from, ZDate until, String ... ids) {
        try {
            // determine the type of archive file tree walking
            String path = getObjectArchivePath(objectCode, ids);
            Archiver archiver = Main.getInstance().getArchiver();
            if (!isTimeDependentPath(path))
                return archiver.restorePath(objectCode, associated, from, until, path);
            return archiver.restoreByTime(objectCode, associated, from, until, path);
        } catch (Exception ex) {
            Main.getInstance().sendExceptionMail(ex, String.format(CoreConsts.AlertMail.WHEN_RESTORING, objectCode), null, false);
            return 0;
        }
    }
    
    /**
     * Handles a record restored from the object archive.
     * <p>
     * This method is called when an object record is restored from the object's archive during a call to
     * {@link #restore(java.lang.String, java.lang.Object, org.spiderwiz.zutils.ZDate, org.spiderwiz.zutils.ZDate,
     * java.lang.String...)  restore()}.
     * Override it if you want to handle the object that was reconstructed from the record.
     * @param associated    An object associated with the restoration by the restore() method.
     * @return true if restoration should continue, false if it shall abort.
     */
    protected boolean onRestore(Object associated) {
        return true;
    }
    
    /**
     * Deletes archive files.
     * <p>
     * A static method used to delete all or selected files from a <em>data object's</em> archive folder, set by
     * {@link #getArchivePath()}.
     * <p>
     * The parameters of this method specify the object type and the range of the records to delete. The {@code objectCode} parameter
     * identifies the object type. The {@code from} and {@code until} parameters specify the record time range.
     * The {@code ids} parameter list lets you select only specific records for deletion by specifying the ID of the
     * deleted object or the ID of an ancestor of a group of objects. The first ID in the list refers to the top ancestor
     * of the object. The following IDs refer to objects down the object tree, until the object
     * itself is referred. A {@code null} value for any ID in the list means that all objects of that level should be deleted.
     * <p>
     * It is important to notice that this method deletes whole files, not individual records. If records marked for deletion
     * comprises an entire archive file, the file will be deleted, but if only a partial file is marked for deletion, the file will
     * remain untouched and no records will be removed from it.
     * @param objectCode    the <em>Object Code</em> of the objects to delete.
     * @param from          starting record time for deletion. A null value means all records until 'until'.
     * @param until         ending record time for deletion. A null value means "now".
     * @param ids           identify objects or object branches to delete.
     * @return true if file deletion is successful.
     */
    public static boolean deleteFromArchive(String objectCode, ZDate from, ZDate until, String ... ids) {
        try {
            // determine the type of archive file tree walking
            String path = getObjectArchivePath(objectCode, ids);
            Archiver archiver = Main.getInstance().getArchiver();
            if (!isTimeDependentPath(path)) {
                if (from == null && until == null)
                    return archiver.deleteArchiveFile(path);
            }
            return archiver.deleteByTime(objectCode, from, until, path);
        } catch (Exception ex) {
            Main.getInstance().sendExceptionMail(ex, String.format(CoreConsts.AlertMail.WHEN_DELETING, objectCode), null, false);
        }
        return false;
    }

    /**
     * @return a comma-concatenated string of all object field values.
     * @throws java.lang.Exception
     */
    final String serialize() throws Exception {
        return Serializer.getInstance().serialize(this);
    }
    
    /**
     * Returns the raw string from which this object was deserialized.
     * <p>
     * When the object is received from a peer application on the network, this method returns the serialized object
     * values. You may want to use it for debugging purposes.
     * @return the serialized object values as delivered on the network, or null if the object was created locally.
     */
    public final String getRawCommand() {
        return stateManager.getRawCommand();
    }

    /**
     * Returns the time that was stamped on the object by a peer application.
     * <p>
     * When the object is received from a peer application on the network, this method returns the time that was stamped
     * on the serialized object by a peer application.
     * @return the timestamp that was attached to the object on the network, or null if the object was created locally.
     */
    public final ZDate getCommandTs() {
        return stateManager.getObjectTime();
    }

    /**
     * Cleans up object resources.
     * <p>
     * This method is called upon application termination or whenever a <em>data object</em> is removed from the
     * <em>data object tree</em>. You can override it if you want to do custom cleanup or data flushing before the object is removed.
     * If you do so, do not forget to call {@code super.cleanup()}.
     */
    public void cleanup(){
        synchronized(childMap) {
            childMap.values().forEach((map) -> {
                map.values().forEach((obj) -> {
                    obj.cleanup();
                });
            });
            childMap.clear();
        }
    }
    
    /**
     * Get a child of this object with a given object code and object ID.
     * @param code  Code of the object type you are looking for.
     * @param id    ID of the child you are looking for, or null if this is a single child.
     * @return if a child object with the specified code and ID is found then return the object, otherwise return null.
     */
    final DataObject getChild(String code, String id) {
        DataObject child = null;
        ObjectMap map = childMap.get(code);
        if (map != null) {
            child = map.get(id);
            // If a child object with this id is not available try again with id converted to lowercase. If this time an object
            // is available, check if the object is case sensitive. If it is, this is not what we are looking for.
            if (child == null && id != null) {
                child = map.get(id.toLowerCase());
                if (child != null && child.isCaseSensitive())
                    child = null;
            }
        }
        return child == null || child.isObsolete() ? null : child;
    }
    
    boolean isObsolete() {
        return stateManager.isObsolete();
    }

    String getRename() {
        return stateManager.getRename();
    }

    void setRename(String rename) {
        stateManager.setRename(rename);
    }

    void delete() {
        DataObject par = stateManager.delete();
        if (par != null)
            par.deleteChild(this);
    }
    
    void undelete() {
        DataObject par = stateManager.undelete();
        if (par != null)
            par.insertChild(this);
    }
    
    private void deleteChild(DataObject child) {
        String objID = child.getObjectID();
        String code = child.getObjectCode();
        synchronized(childMap) {
            ObjectMap map = childMap.get(code);
            if (map != null)
                map.remove(objID);
        }
    }
    
    private void insertChild(DataObject child) {
        String objID = child.getObjectID();
        String code = child.getObjectCode();
        synchronized(childMap) {
            ObjectMap map = childMap.get(code);
            if (map != null)
                map.putIfAbsent(objID, child);
        }
    }
    
    /**
     * Activate an import manager to export the object
     * @param importManager
     * @throws Exception 
     */
    void doExport(ImportManager importManager) throws Exception {
        if (importManager != null)
            importManager.transmitObject(this, stateManager.getNewID());
    }

    private void setObjectID(String objectID) {
        stateManager.setObjectID(objectID);
    }
    
    private void setParent(DataObject parent) {
        stateManager.setParent(parent);
    }
    
    final DataHandler getDataChannel() {
        return stateManager.getDataChannel();
    }

    final void setDataChannel(DataHandler dataChannel) {
        stateManager.setDataChannel(dataChannel);
    }

    final DataObject setRawCommand(String rawCommand) {
        stateManager.setRawCommand(rawCommand);
        return this;
    }

    final DataObject setCommandTs(ZDate commandTs) {
        stateManager.setObjectTime(commandTs);
        return this;
    }

    final DataObject setOriginUUID(UUID originUUID) {
        stateManager.setOriginUUID(originUUID);
        return this;
    }

    final Collection<UUID> getDestinations() {
        return stateManager.getDestinations();
    }

    final void setDestinations(Collection<UUID> destinations) {
        stateManager.setDestinations(destinations);
    }
    
    final void setRemoved() {
        stateManager.setRemoved(true);
    }

    /**
     * Get the parsing hierarchy of the object
     * @return a list of object codes from the root node to the current node. If a node is a normal child, i.e. a child that
     * is identified by its own index, add "+" to the object code.
     */
    final String[] getParsingHierarchy() {
        ArrayList<String> hierarcyList = new ArrayList<>();
        String code = getParentCode();
        if (code != null) {
            DataObject obj = Main.getInstance().createDataObject(code);
            hierarcyList.addAll(Arrays.asList(obj.getParsingHierarchy()));
        }
        String myCode = getObjectCode();
        if (getObjectID() != null)
            myCode += "+";
        hierarcyList.add(myCode);
        String[] result = new String[hierarcyList.size()];
        return hierarcyList.toArray(result);
    }
    
    /**
     * Process values of a child node by walking through object code hierarchy, locate the target object or create it,
     * and then let the object extract its values from the given value list.
     * @param keys              a list of keys as received in incoming command
     * @param fields            the object field values as one string separated by commas
     * @param objectHierarchy   a list of object codes expressing the hierarchy of the objects from the root until the processed child.
     * @param iStart            index of the object fields in the 'cmd' array.
     * @param remove            true if this command shall delete the object
     * @param channel           The DataHandler object this object is coming from, if any
     * @param commandUUID       UUID of the origin of the command
     * @return the processed child or null if target node was not affected by this process.
     */
    final DataObject processChild(String keys[], String fields, String[] objectHierarchy, boolean remove, DataHandler channel,
        UUID commandUUID
    ) throws Exception {
        if (objectHierarchy.length == 0)
            return null;
        String code = objectHierarchy[0];
        // check if this level of the hierarchy is identified by object key
        boolean hasKey = code.endsWith("+");
        String key = null;
        if (hasKey) {
            code = code.substring(0, code.length() - 1);
            if (keys.length > 0)
                key = keys[0];
            else
                hasKey = false;
        }
        DataObject child = key == null ? null : getChild(code, key);
        boolean newObject = false;
        if (child == null) {
            child = Main.getInstance().createDataObject(code);
            if (child == null)
                return null;
            newObject = true;
            child.setObjectID(key == null ? "" : key);
            child.setParent(this);
            child.setOriginUUID(commandUUID);
            if (child.getObjectID() != null && !child.isDisposable() && !Main.getMyConfig().isPropertySet(MyConfig.PASS_THROUGH))
                storeChild(child);
        }
        
        // If this is end of the hierarchy parse child values now, otherwise continue down the hierarchy with the child.
        DataObject result;
        if (objectHierarchy.length == 1) {
            if (channel != null) {
                child.setDataChannel(channel);
            }
            result = child.parseObject(fields, remove);
        }
        else {
            String nestedHierarcy[] = new String[objectHierarchy.length - 1];
            System.arraycopy(objectHierarchy, 1, nestedHierarcy, 0, nestedHierarcy.length);
            String nestedKeys[];
            if (hasKey) {
                nestedKeys = new String[keys.length - 1];
                System.arraycopy(keys, 1, nestedKeys, 0, nestedKeys.length);
            }
            else
                nestedKeys = keys;
            result = child.processChild(nestedKeys, fields, nestedHierarcy, remove, channel, commandUUID);
        }
        if (newObject)
            child.onNew();
        return result;
    }
    
    /**
     * Store the given object in the child map
     * @param child 
     */
    private void storeChild(DataObject child) {
        synchronized(childMap) {
            ObjectMap map = childMap.get(child.getObjectCode());
            if (map == null) {
                map = new ObjectMap();
                childMap.put(child.getObjectCode(), map);
            }
            map.put(child.getObjectID(), child);
        }
    }
    
    final String getFullKey() {
        return stateManager.getFullKey();
    }
    
    /**
     * @return a comma-concatenated list of all object values
     */
    String getObjectValues(boolean resetting) throws Exception {
        return isObsolete() ? getRename() != null ? Serializer.escapeDelimiters(getRename()) : null : serialize(resetting);
    }
    
    /**
     * Construct a comma-separated list of all object values, using the provided key array as a key
     * @param keys
     * @return 
     */
    final String constructObjectValues(String keys[]) throws Exception {
        StringBuilder result = new StringBuilder(Serializer.escapeAndConcatenate("|", keys));
        String values = serialize();
        if (values != null && !values.isEmpty()) {
            result.append(",").append(values);
        }
        return result.toString();
    }
    
    /**
     * If this object is of the type of the resetter or its child map contain objects of this type,
     * Resend them all on the given channel. 
     * @param resetter A Resetter object managing the reset of a specific object type.
     */
    final void resetObject(Resetter resetter) {
        if (!isObsolete() && !resetter.resetObject(this))
            childMap.resetObject(resetter);
    }
    
    /**
     * Propagate object values to other system nodes. 
     * @param resetting     true if object is resetting
     * @param destinations  UUIDs of the destinations data node, or null if shall be broadcast to all
     * @param originChannel the channel the object is arriving from (do not propagate to that channel)
     */
    void propagate(boolean resetting, Collection<UUID> destinations, DataHandler originChannel) throws Exception {
        try {
            preDistribute();
            Hub.getInstance().propagateObject(this, resetting, destinations, originChannel);
        } finally {
            postDistribute();
        }
    }
    
    /**
     * Modify the ID of a child of this node.
     * @param child
     * @param newID
     * @return an obsolete object that has the old child ID as a key ID and the new ID in the 'rename' field. This will be used to
     * propagate the action to peer nodes. Return null if a valid object with 'newID' already exists.
     */
    private DataObject renameChild(DataObject child, String newID) {
        String oldID = child.getObjectID();
        if (newID != null && !child.isCaseSensitive())
            newID = newID.toLowerCase();
        String code = child.getObjectCode();
        DataObject obj;
        synchronized(childMap) {
            ObjectMap map = childMap.get(code);
            if (map == null)
                return null;
            // Shift the object. Make sure that there is no object whose ID is newID
            obj = map.get(newID);
            if (obj != null && !obj.isObsolete())
                return null;
            map.remove(oldID);
            child.setObjectID(newID == null ? "" : newID);
            map.put(newID, child);
        }
        try {
            obj = createChild(child.getClass(), oldID);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            return null;
        }
        obj.setRename(newID);
        return obj;
    }
    
    /**
     * Call when a node is terminated to remove all the objects originated from that node
     * @param terminatedApp UUID of the terminated node
     */
    final void removeTerminatedChildren(UUID terminatedApp) {
        synchronized(childMap) {
            childMap.values().forEach((map) -> {
                ArrayList<DataObject> terminated = new ArrayList<>();
                map.lockRead();
                try {
                    for (DataObject object : map.values()) {
                        object.removeTerminatedChildren(terminatedApp);
                        if (terminatedApp.equals(object.getOriginUUID())) {
                            terminated.add(object);
                            object.setRemoved();
                            DataManager.getInstance().objectEvent(object);
                        }
                    }
                } finally {
                    map.unlockRead();
                }
                map.removeAll(terminated);
            });
        }
    }
    
    final String getTransmitPrefix() {
        return isObsolete() ? RemoveIndicator : myTransmitPrefix();
    }
    
    String myTransmitPrefix() {
        return isUrgent() ? "#" : "$";
    }
    
    /**
     * Parse a command by cutting out the array that contains only the object fields and parse it
     * @param fields            the object field values as one string separated by commas
     * @param remove    true if this command shall delete the object or rename it
     * @return the parsed object
     * @throws Exception 
     */
    DataObject parseObject(String fields, boolean remove) throws Exception {
        if (remove) {
            if (fields == null || fields.isEmpty())
                delete();
            else {
                DataObject par = stateManager.getParent();
                return par == null ? null : par.renameChild(this, Serializer.unescapeDelimiters(fields));
            }
            return this;
        }
        return deserialize(fields);
    }
    
    static String stripLossless(String code) {
        return code.endsWith("+") ? code.substring(0, code.length() - 1) : code;
    }
    
    static boolean isLossless(String code) {
        return code.endsWith("+");
    }
    
    private String getFullArchivePath() {
        String path = getArchivePath();
        if (path == null)
            return null;
        String ext = ZUtilities.getFileExtension(path);
        return path + (ext == null ? CoreConsts.DEFAULT_ARCHIVE_FILE_EXTENSION : "");
    }
    
    /**
     * create the specific path for this object
     * @return 
     */
    private String createArchivePath() {
        String path = getFullArchivePath();
        if (path == null)
            return null;
        String root = Main.getMyConfig().getArchiveFolder();
        if (root == null)
            return null;
        DataObject obj = this;
        int n = 0;
        do {
            int i = path.indexOf("#" + n++);
            if (i >= 0) {
                String key = obj.getObjectID();
                if (key == null)
                    key = "";
                path = path.substring(0, i) + MyUtilities.escapeNonAlphanumeric("-", key) + path.substring(i + 2);
            }
            obj = obj.getParent();
        } while (obj != null);
        return root + "/" + path;
    }
    
    /**
     * Get the path for the given 'objectCode' and inject the given keys in it
     * @param objectCode
     * @param keys
     * @return 
     */
    private static String getObjectArchivePath(String objectCode, String ... keys) {
        // Create an instance of the object and use it to get the archive path
        DataObject obj = Main.getInstance().createDataObject(objectCode);
        if (obj == null)
            return null;
        String root = Main.getMyConfig().getArchiveFolder();
        if (root == null)
            return null;
        String path = obj.getFullArchivePath();
        if (path == null)
            return null;
        path = root + "/" + path;
        // populate the path with the provided keys (if any)
        int n = keys.length;
        for (String key : keys) {
            --n;
            if (key != null)
                path = path.replace("#" + n, MyUtilities.escapeNonAlphanumeric("-", key));
        }
        return path;
    }
    
    /**
     * return true if the given path contain time parameters
     * @param path
     * @return 
     */
    private static boolean isTimeDependentPath(String path) {
        return ZUtilities.find(path, "\\#[ymdh]");
    }
    
    /**
     * Get the value of the static {@code ObjectCode} field this object contains or inherits.
     * @return the data object code
     */
    final String getObjectCode() {
        if (objectCode != null)
            return objectCode;
        try {
            return (objectCode = getObjectCode(getClass()));
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            return null;
        }
    }
    
    /**
     * Get the value of the ObjectCode static field of the given class
     * @param type
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException 
     */
    static String getObjectCode(Class type) throws NoSuchFieldException, IllegalAccessException {
        String code = (String)type.getField("ObjectCode").get(null);
        if (code == null)
            throw new NoSuchFieldException(CoreConsts.NULL_OBJECT_CODE);
        return code;
    }
    
    /**
     * @return true if this object is a QueryObject in reply state
     */
    boolean isQueryReply() {
        return (this instanceof QueryObject) && ((QueryObject)this).isReply();
    }

    /**
     * if leading character is '#' or '!' this is an urgent command.
     * @param prefix
     * @return 
     */
    static boolean isUrgentCommand(String prefix) {
        return prefix.matches("\\#|\\!");
    }
    
    /**
     * Create an EventDispatcher object for data objects of this type.
     * @param lossless  if true number of threads can be only zero or 1.
     * @return the created object.
     */
    EventDispatcher createEventDispatcher(boolean lossless) {
        int n = getThreadAllocation();
        if (lossless && n != 0)
            n = 1;
        return new EventDispatcher(n);
    }
}