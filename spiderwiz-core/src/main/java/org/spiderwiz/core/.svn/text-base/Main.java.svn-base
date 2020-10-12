/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spiderwiz.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.spiderwiz.admin.data.ApplicationInfo;
import org.spiderwiz.admin.data.OpResults;
import org.spiderwiz.admin.data.PageInfo;
import org.spiderwiz.admin.data.PageInfo.TableInfo;
import org.spiderwiz.admin.data.TableData;
import org.spiderwiz.admin.services.AdminServices;
import org.spiderwiz.annotation.WizMain;
import org.spiderwiz.zutils.ZConfig;
import org.spiderwiz.zutils.ZDate;
import org.spiderwiz.zutils.ZHashMap;
import org.spiderwiz.zutils.ZLog;
import org.spiderwiz.zutils.ZThread;
import org.spiderwiz.zutils.ZUtilities;

/**
 * Starting point for all <b><em>Spiderwiz</em></b> applications.
 * <p>
 * To build a Spiderwiz application start with implementing an extension of this class. To invoke the framework and set everything in
 * motion, call its {@link #init()} method. You would usually do this in a {@code main()} method of a Java Application, or,
 * if you build a Java Web Application, in an implementation of {@link javax.servlet.http.HttpServlet HttpServlet} that you run
 * with the {@link javax.servlet.annotation.WebServlet#loadOnStartup() loadOnStartup} flag. When your application terminates, you
 * should call {@link #cleanup()}. <span><a id="InitExample"></a>For example:</span>
    <pre>
    &#64;WebServlet(name="Startup", urlPatterns={"/Startup"}, loadOnStartup = 1)
    public class Startup extends HttpServlet {
        &#64;Override
        public void init() throws ServletException {
            super.init();
            (new MyMain("", "myapp.conf", "My first Spiderwiz web application", "1.00")).init();
        }

        &#64;Override
        public void destroy() {
            MyMain.getInstance().cleanup();
            super.destroy();
        }
    }</pre>
    * <p>
    * There are two places where you can put your own initialization code. The first is by overriding 
    * ways to provide your own initialization code. The first is to override
    * {@link #preStart()}, which is called once the
    * <a href="../core/doc-files/config.html">application's configuration file</a> has been loaded and the logging
    * mechanism has been established but before communication channels are established and data starts to flow.
    * The other option is to override {@link #postStart()}, which is executed after full
    * framework initialization, when communication channels have been established and data starts to flow.
    * <p>
    * The main ingredient of the framework is the <em><b>data object</b></em>, implemented as a class that extends {@link DataObject}.
    * By convention, <em>data objects</em> are implemented in organization-shared Java Class Libraries. When
    * you design your application, the first question you should ask yourself is which data object types you are going to use
    * and whether you are a producer or a consumer of each type (you can be both).
    * You tell which objects you produce by implementing {@link #getProducedObjects()} and which objects you consume by implementing
    * {@link #getConsumedObjects()}.
    * <p>
    * The next step is to implement and register data object handlers - classes that extend the shared library classes and provide
    * implementation code specific to your application. These are optional for object types that you produce and
    * essential for object types that
    * you consume, as you need to provide event handling code. You must register all the data object classes that you use,
    * whether extended or not, in
    * {@link #populateObjectFactory(java.util.List) populateObjectFactory()}.
    * <p>
    * Two other methods that you may use frequently are {@link #createTopLevelObject(java.lang.Class, java.lang.String) 
    * createTopLevelObject()} to create an object that is not a child of another object (for a
    * child object use {@link DataObject#createChild(java.lang.Class, java.lang.String) DataObject.createChild()}),
    * and {@link #createQuery(java.lang.Class) createQuery()}, which is the short way for creating a {@link QueryObject}.
    * <p>
    * Some methods are used for <b>customizing <a id=customspideradmin href="http://spideradmin.com">SpiderAdmin</a></b>. If you do
    * that then the methods that you will most frequently override are {@link #getPageInfo(java.lang.String) getPageInfo()}
    * and {@link #customAdminService(java.lang.String, java.lang.String) customAdminService()}.
    * <p>
    * See the description below for these and other methods that are used less frequently.
 */

@WizMain
public abstract class Main {
    private static final String CORE_VERSION = "Z4.00"; // Version Z4.00: Stable published version 4.0
    private static final String DEFAULT_CONFIGFILENAME = "settings.conf";

    /**
     * Object codes of predefined <em>Data Objects</em>.
     * <p>
     * The Spiderwiz framework includes a set of predefined {@link org.spiderwiz.core.DataObject data objects} that are used to
     * delegate some actions that require external resource access from an application that does not have access to the resource to
     * a peer application that does. When you use delegation, you can call the method that performs the action normally as if it
     * is executed directly by the delegating application, and the delegation mechanism will ensure that the action is actually
     * executed by the application that receives the delegate.
     * <p>
     * The class described here specifies the object codes of those predefined data objects. If you want to delegate an action
     * between applications, all you have to do is to include the object code of the object that governs the action in the list of
     * codes returned by {@link org.spiderwiz.core.Main#getProducedObjects()} of the delegator and include that code
     * in the list of codes returned by {@link org.spiderwiz.core.Main#getConsumedObjects()} of the application that shall
     * execute the action.
     */
    protected static class ObjectCodes {

        /**
         * Identifies an object that delegates mail notifications and alerts.
         * <p>
         * Use this when you want to delegate the actions of
         * {@link org.spiderwiz.core.Main#sendNotificationMail(java.lang.String, java.lang.String,
         * org.spiderwiz.zutils.ZDate, boolean) Main.sendNotificationMail()} and
         * {@link org.spiderwiz.core.Main#sendExceptionMail(java.lang.Throwable,
         * java.lang.String, java.lang.String, boolean) Main.sendExceptionMail()}.
         */
        public final static String EventReport = "ORG.SPIDERWIZ.EVRP";

        /**
         * Identifies an object that delegates parsing of imported data.
         * <p>
         * Use this when you want to delegate the action of
         * {@link DataObject#importObject(java.lang.Object, org.spiderwiz.core.ImportHandler, org.spiderwiz.zutils.ZDate)  
         * DataObject.importObject()} from an application that has access to the imported data (through
         * {@link org.spiderwiz.core.ImportHandler}) to the application that needs the data but does not have access to it.
         * <p>
         * Note that only objects that have a meaningful {@code toString()} implementation can be delegated by this mechanism.
         */
        public final static String RawImport = "ORG.SPIDERWIZ.RIM";

        /**
         * Identifies an object that delegates data export.
         * <p>
         * Use this when you want to delegate the action of
         * {@link org.spiderwiz.core.DataObject#exportObject(org.spiderwiz.core.ImportHandler, java.lang.String)
         * DataObject.exportObject()} from the application that produces the data but does not have access to the
         * external resource to an application that
         * does have access to it (through {@link org.spiderwiz.core.ImportHandler}).
         * <p>
         * Note that only objects that have a meaningful {@code toString()} implementation can be delegated by this mechanism.
         */
        public final static String RawExport = "ORG.SPIDERWIZ.REX";
    }

    /**
     * An inner class that performs background periodical tasks
     */
    private class PeriodicalTasks extends ZThread {
        private ZDate lastCall = null;
        
        @Override
        protected void doLoop() {
            if (lastCall != null && lastCall.hasCrossed(config.getStartOfDay())) {
                runDailyTask();
            }
            lastCall = ZDate.now();
            Hub.getInstance().removeObsolete();
            runPeriodicalTasks();
        }

        @Override
        protected long getLoopInterval() {
            return ZDate.MINUTE;
        }
    }
    
    private final String appName;
    private UUID appUUID = null;
    private final String configFileName, appVersion;
    private static Main myObject = null;
    private static MyConfig config, history;
    private String rootFolder = null;
    private static ZLog logger;
    private DataManager dataManager = null;
    private final Hub hub;
    private ZDate deployDate = null;
    private final QueryManager queryManager;
    private final ZHashMap<String, Class> factoryMap;
    private AlertMail alertMail;
    private boolean notEnoughDiskSpace = false;
    private final File rootFile;
    private final PeriodicalTasks generalMonitor;
    private final Archiver archiver;
    private String xAdminQueryCode = null; 
    private ZDate lastException = null;

    /**
     * Constructs an instance with root directory, configuration file name, default application name and version number.
     * @param rootFolder     the application root directory. An empty string refers to the project base directory (${basedir}).
     * @param configFileName    the application configuration file that must reside in the application root directory.
     *                          See <a href="doc-files/config.html">How to configure a Spiderwiz application</a> for the syntax and
     *                          semantics of Spiderwiz configuration files.
     * @param appName           the default application name that shows in case you do not configure an application name in
     *                          <a href="doc-files/config.html">the application's configuration file</a>. You can dynamically override
     *                          this value by implementing {@link #getAppName() getAppname()}.
     * @param appVersion        the default application version number. You can dynamically override this value by implementing
     *                          {@link #getAppVersion() getAppVersion()}.
     */
    public Main(String rootFolder, String configFileName, String appName, String appVersion) {
        this.rootFolder = rootFolder;
        rootFile = new File(rootFolder != null && !rootFolder.isEmpty() ? rootFolder : "/");
        this.configFileName = configFileName == null || configFileName.isEmpty() ? DEFAULT_CONFIGFILENAME : configFileName;
        this.appName = appName;
        this.appVersion = appVersion;
        factoryMap = new ZHashMap<>();
        hub = new Hub();
        queryManager = new QueryManager();
        queryManager.init();
        dataManager = new DataManager(queryManager);
        generalMonitor = new PeriodicalTasks();
        archiver = new Archiver();
    }

    /**
     * Returns the singleton instance of the class.
     * <p>
     * Class {@code Main} runs as a singleton whose instance is set when you call {@link #init() init()}. Use it to call the class
     * public methods from anywhere in your application.
     * @return the class singleton instance or null if this method is called before calling {@link #init()}.
     */
    public static Main getInstance() {
        return myObject;
    }
    
    /**
     * Returns an object that lets you access and manipulate the <a href="doc-files/config.html">application's configuration file</a>.
     * <p>
     * The method should be called only after you call {@link #init() init()}.
     * @return an object used to access and manipulate Spiderwiz configuration files.
     */
    public static ZConfig getConfig() {
        return config;
    }
    
    static MyConfig getMyConfig() {
        return config;
    }
    
    /**
     * Returns an object that lets you print custom log messages to the application's main logging system.
     * <p>
     * The method should be called only after you call {@link #init() init()}.
     * <p>
     * For details about logging see <a href="../core/doc-files/logging.html">Spiderwiz Logging System</a>.
     * @return the application's logging object.
     */
    public static ZLog getLogger() {
        return logger;
    }

    /**
     * Returns the application's root folder.
     * @return the full pathname of the application's root folder.
     */
    public String getRootFolder() {
        return rootFolder;
    }
    
    /**
     * Get the login parameters that will be sent to the gateway as part of the login command.
     * @return the parameters joined by commas, or null if we are the gateway.
     */
    String getLoginParams() {
        return ZUtilities.concatAll(",",
            Serializer.escapeDelimiters(getAppName()), Serializer.escapeDelimiters(getAppVersion()),
            Serializer.escapeDelimiters(getCoreVersion()));
    }
    
    /**
     * Returns the application name.
     * <p>
     * The application name is used for application monitoring and identification.
     * If the <a href="doc-files/config.html">application's configuration file</a> contains an [{@code application name}] property
     * then the value of the property is returned, otherwise the {@code appName} parameter value given to the
     * {@link #Main(java.lang.String, java.lang.String, java.lang.String, java.lang.String) class constructor} is used.
     * <p>
     * You can override this method to provide your custom application name.
     * @return the application name.
     */
    public String getAppName() {
        String configName = config.getProperty(MyConfig.APP_NAME);
        return configName == null ? appName : configName;
    }
    
    /**
     * Returns the application's version number.
     * <p>
     * This method is used by <a href="http://spideradmin.com">SpiderAdmin</a> when you use it to monitor your application.
     * <p>
     * By default, the application's version number is {@code appVersion} parameter value given to the
     * {@link #Main(java.lang.String, java.lang.String, java.lang.String, java.lang.String) class constructor}. You can
     * override this method to provide your custom version number.
     * @return the application's version number.
     */
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * Returns <em>Spiderwiz</em> library version number used to build the application.
     * <p>
     * This method is used by <a href="http://spideradmin.com">SpiderAdmin</a> when you use it to monitor your application.
     * <p>
     * Spiderwiz library version number is hard coded. You can override this method to return another value, or a value that combines
     * the library version number with another value. For instance, assuming you have a library that is used across your
     * organization that subclasses {@link Main} by {@code OrgMain}, which is then extended
     * by individual implementations. You want <a href="http://spideradmin.com">SpiderAdmin</a> to show the organization library
     * version along Spiderwiz library version. You can then implement in {@code OrgMain}:
     * <pre>
    private static const String ORG_LIB_VERSION = "14.92";
    &#64;Override
    protected String getCoreVersion() {
        return String.format("%1$s (OrgLib %2$s)", super.getCoreVersion(), ORG_LIB_VERSION);
    }</pre>
     * @return a string that represents a version number.
     */
    protected String getCoreVersion() {
        return CORE_VERSION;
    }

    /**
     * Returns the application UUID.
     * <p>
     * The application's UUID is automatically generated the first time the application runs and is stored
     * as the value of the [{@code application uuid}] property in the
     * <a href="doc-files/config.html">application's configuration file</a>. Normally you should never touch this value by hand.
     * @return the application's UUID.
     */
    public final UUID getAppUUID() {
        return appUUID;
    }

    /**
     * Returns application instance parameters.
     * <p>
     * Every Spiderwiz application instance may define a set of arbitrary parameters, which are published across the network along
     * with other information about the application. Peer applications can implement
     * {@link DataObject#filterDestination(java.util.UUID, java.lang.String, java.lang.String, java.lang.String,
     * java.util.Map) DataObject.filterDestination()} to review the parameters and filter output to the parameterized application
     * accordingly.
     * <p>
     * Override this method to define application parameters as a name-value map.
     * @return application parameters as a name-value map, or null if the application instance defines no parameters.
     */
    public Map<String, String> getAppParams() {
        return null;
    }
    
    /**
     * Initializes the class singleton, putting the application in motion.
     * <p>
     * Application initialization includes the following steps:
     * <ul>
     * <li>Set the class singleton instance.</li>
     * <li>Process the configuration file.</li>
     * <li>Start the logging mechanism.</li>
     * <li>{@link #populateObjectFactory(java.util.List) Registering} <em>data object</em> handlers.</li>
     * <li>Establish the <a href="doc-files/config.html">configured</a> network connections.</li>
     * <li>Broadcast requests for all {@link #getConsumedObjects() consumed data objects} and start processing input data. </li>
     * </ul>
     * @return true if this the first time the method is called and the instance has been properly initialized
     */
    public final boolean init() {
        if (myObject != null)
            return false;
        myObject = this;
        try {
            if (rootFolder == null)
                rootFolder = ".";
            if (!rootFolder.isEmpty() && !rootFolder.endsWith("/"))
                rootFolder += "/";
            // fill out the config table
            config = createMyConfig();
            if (!config.init(rootFolder + configFileName) ) {
                System.out.printf(CoreConsts.NO_CONFIG_FILE, new File(rootFolder + configFileName).getAbsolutePath());
                System.out.println();
                return false;
            }
            startLogging(true);
            
            // get or generate application uuid
            String uuid = config.getProperty(MyConfig.APP_UUID);
            if (uuid != null && !uuid.isEmpty()) {
                try {
                    appUUID = UUID.fromString(uuid);
                } catch (IllegalArgumentException ex) {
                    appUUID = null;
                    logger.logEvent(CoreConsts.INVALID_UUID, uuid);
                }
            }
            
            // If needed, generate a UUID and store it in the config file of the application.
            if (appUUID == null) {
                appUUID = UUID.randomUUID();
                config.addProperty(MyConfig.APP_UUID, appUUID.toString(), true);
            }
            
            // Initialize everything
            populateFactoryMap(factoryMap);
            history = createMyConfig();
            history.init(rootFolder + config.getHistoryFileName());
            alertMail = new AlertMail();
            if (!preStart())
                return false;
            logger.logEvent(CoreConsts.WELCOME, getAppName(), getAppVersion(), getCoreVersion());
            dataManager.init(factoryMap.keySet());
            hub.init();
            hub.execute();
            history.processAllProperties();
            
            // start the genera runPeriodicalTasks
            generalMonitor.execute();
            
            deployDate = ZDate.now();
            postStart();
        } catch (Exception ex) {
            sendExceptionMail(ex, CoreConsts.AlertMail.EXCEPTION_MAIN_INIT, null, true);
            return false;
        }
        return true;
    }
    
    /**
     * Override this method to run extra initialization code after full framework initialization.
     */
    protected void postStart() {}
    
    /**
     * Cleans up application resources before termination.
     * <p>
     * Call this method before your application terminates in order to release all resources and shut down network connections
     * gracefully. See the <a href="#InitExample">example above</a>.
     */
    public void cleanup() {
        queryManager.cleanup();
        generalMonitor.cleanup();
        dataManager.cleanup();
        hub.cleanup();
        archiver.cleanup();
        if (logger != null) {
            logger.logEvent("%s undeployed", getAppName());
            logger.cleanup();
        }
    }

    /**
     * Executes custom initialization code.
     * <p>
     * Override this method if you want to do anything once the application's configuration file has been loaded and the logging
     * mechanism has been established, but before communication channels are established and data starts to flow.
     * @return true if application shall continue, false if something has gone wrong and you want to abort application execution.
     */
    protected boolean preStart() {return true;}
    
    /**
     * Resets application data.
     * <p>
     * This method clears the entire <em>data object tree</em> and broadcasts a request to all peer applications to reset the
     * objects that it consumes. The method will be called daily at the time set by [{@code start of day}] property of the
     * <a href="doc-files/config.html">application's configuration file</a> (unless you override
     * {@link #runDailyTask() runDailyTask()}). You can also call this method on other occasions, for instance when you want to
     * promote a change in {@link #getAppParams()}, which affects the behavior of
     * {@link DataObject#filterDestination(java.util.UUID, java.lang.String, java.lang.String, java.lang.String, java.util.Map)
     * filterDestination()} of peer applications.
     */
    public final void reset() {
        getRootObject().cleanup();
        hub.resetConsumedObjects();
    }

    /**
     * Registers the application as a producer of the listed <em>data object</em> types.
     * <p>
     * Implement this method to tell Spiderwiz runtime engine which data object types your application produces. The produced data
     * objects are identified by their <em>Object Codes</em>, i.e. the static
     * <a href="../core/DataObject.html#ObjectCode">{@code ObjectCode}</a> field defined for the object class. You can also specify
     * {@link org.spiderwiz.core.Main.ObjectCodes predefined object codes}.
     * <p>
     * This is an abstract method that you must implement. If your application does not produce any data object then return an empty
     * array.
     * <p>
     * Note that this method is called after the <a href="doc-files/config.html">application's configuration file</a> is processed,
     * so it can return dynamic values depending on the configuration.
     * <p>
     * Example:
     * <pre>
    &#64;Override
    protected String[] getProducedObjects() {
        return getConfig().isPropertySet("is producer") ? new String[]{
            MyFirstObject.ObjectCode, MySecondObject.ObjectCode
        } : new String[]{
            ObjectCodes.EventReport
        };
    }</pre>
     * @return an array of {@link String String} containing the codes of the produced objects.
     */
    protected abstract String[] getProducedObjects();
    
    /**
     * Registers the application as a consumer of the listed <em>data object</em> types.
     * <p>
     * Implement this method to tell Spiderwiz runtime engine which data object types your application consumes. The consumed data
     * objects are identified by their <em>Object Codes</em>, i.e. the static
     * <a href="../core/DataObject.html#ObjectCode">{@code ObjectCode}</a> field defined for the object class. You can also specify
     * {@link org.spiderwiz.core.Main.ObjectCodes predefined object codes}.
     * <p>
     * This is an abstract method that you must implement. If your application does not consume any data object then return an empty
     * array.
     * <p>
     * Note that this method is called after the <a href="doc-files/config.html">application's configuration file</a> is processed,
     * so it can return dynamic values depending on the configuration.
     * <p>
     * Example:
     * <pre>
    &#64;Override
    protected String[] getConsumedObjects() {
        return getConfig().isPropertySet("is producer") ? new String[]{
            ObjectCodes.EventReport
        } : new String[]{
            MyFirstObject.ObjectCode, MySecondObject.ObjectCode
        };
    }</pre>
     * @return an array of {@link String String} containing the codes of the consumed objects.
     */
    protected abstract String[] getConsumedObjects();
    
    /**
     * Populate the factory map.
     * @param factoryMap
     */
    private void populateFactoryMap(Map<String, Class> factoryMap) throws NoSuchFieldException, IllegalAccessException {
        ArrayList<Class<? extends DataObject>> factoryList = new ArrayList<>();
        populateAdminClass(factoryList);
        populateObjectFactory(factoryList);
        for (Class type : factoryList)
            factoryMap.put(DataObject.getObjectCode(type), type);
    }
    
    /**
     * Registers application-specific object handlers.
     * <p>
     * Override this method to add your <em>data object</em> implementations to the framework's object factory. This is done by adding
     * the {@link Class class} object of the implementation classes to the given {@code factoryList} parameter.
     * <p>
     * To preserve predefined object handlers and handlers registered up in the hierarchy of {@link org.spiderwiz.core.Main Main}
     * subclasses, call the {@code super} instance of this method.
     * <p>
     * Typically, object handlers implement either a <em>producer</em> or a <em>consumer</em> subclass of a <em>data object</em>
     * (class that extends {@link org.spiderwiz.core.DataObject DataObject}).
     * <p>
     * Example:
     * <pre>
    &#64;Override
    protected void populateObjectFactory(List&lt;Class&lt;? extends DataObject&gt;&gt; factoryList) {
        super.populateObjectFactory(factoryList);
        if (getConfig().isPropertySet("is producer"))
            factoryList.add(MyFirstObjectProducer.class);
        else
            factoryList.add(MyFirstObjectConsumer.class);
    }</pre>
     * @param factoryList   a List of elements of type {@link Class Class}, modeled to accept only classes that extend
     * {@link org.spiderwiz.core.DataObject}.
     */
    protected void populateObjectFactory(List<Class<? extends DataObject>> factoryList) {
        factoryList.add(EventReport.class);
        factoryList.add(RawImport.class);
        factoryList.add(RawExport.class);
    }
    
    /**
     * Create a data object
     * @param objectType    data object type (e.g. EngineReading.ObjectType).
     * @return              a new object of the requested type
     */
    final DataObject createDataObject(String objectType)  {
        try {
            Class dataClass = factoryMap.get(objectType);
            if (dataClass != null) {
                DataObject object = (DataObject)dataClass.getDeclaredConstructor().newInstance();
                object.setOriginUUID(appUUID);
                return object;
            }
            throw new NoSuchElementException(String.format(CoreConsts.INVALID_OBJECT_TYPE, objectType));
        } catch (Exception ex) {
            sendExceptionMail(ex, String.format(CoreConsts.AlertMail.UNKNOWN_OBJECT_ERROR, objectType), null, true);
        }
        return null;
    }
    
    /**
     * Creates a top level <em>data object</em>.
     * <p>
     * Top level data objects are objects whose {@link org.spiderwiz.core.DataObject#getParentCode() getParentCode()} method returns
     * null. These objects, unless defined {@link org.spiderwiz.core.DataObject#isDisposable() disposable}, are stored as children of
     * a predefined {@link #getRootObject() root object}. Use this method to create a top level data object.
     * To create a child object, use {@link org.spiderwiz.core.DataObject#createChild(java.lang.Class, java.lang.String)
     * createChild()}.
     * <p>
     * The method accepts two parameters: the {@link Class Class} of the object you want to create (that must extend
     * {@link org.spiderwiz.core.DataObject DataObject}) and the {@link org.spiderwiz.core.DataObject#getObjectID() object ID}. The ID
     * may be null if the class is a singleton or <em>disposable</em>.
     * <p>
     * If the given class is not disposable, the method first tries to locate an object with the same object code and ID (or an
     * existing singleton if ID is null). If none exists, or if the class is disposable, the method creates a new object with the
     * given ID. Note that the actual class that will be created is the class registered by
     * {@link #populateObjectFactory(java.util.List) populateObjectFactory()}, which may be a subclass of the given {@code type}.
     * The type of the returned object is the type specified in the parameter.
     * <p>
     * The given class type must contain or inherit a public static field named {@code ObjectCode}, otherwise the method will throw
     * an exception.
     * @param <T>   class type of the object you want to create.
     * @param type  the class of the object you want to create.
     * @param id    the object ID of the object. Can be null if the object is a singleton or disposable.
     * @return      the found or created object, or null if the object could not be created or it is not defined as a top level object.
     * @throws java.lang.NoSuchFieldException       if class <em>type</em> doesn't contain a static <em>ObjectCode</em> field.
     * @throws java.lang.IllegalAccessException     if <em>ObjectCode</em> field of class <em>type</em> is not public.
     */
    public static <T extends DataObject> T createTopLevelObject(Class<T> type, String id)
        throws NoSuchFieldException, IllegalAccessException
    {
        return getInstance().getRootObject().createChild(type, id);
    }
    
    /**
     * Creates a <em>query object</em>.
     * <p>
     * Query objects are data objects that extend {@link org.spiderwiz.core.QueryObject QueryObject}, used for round trip messaging.
     * By default, they are {@link #createTopLevelObject(java.lang.Class, java.lang.String) top level} and
     * {@link org.spiderwiz.core.QueryObject#isDisposable() disposable}. Use this method to create a top level disposable query object.
     * To create a child query object, use {@link org.spiderwiz.core.DataObject#createChild(java.lang.Class, java.lang.String)
     * createChild()}. To create an indisposable top-level query object,
     * use {@link #createTopLevelObject(java.lang.Class, java.lang.String) createTopLevelObject()}.
     * <p>
     * The method accepts one parameter: the {@link Class Class} of the object you want to create (that must extend
     * {@link org.spiderwiz.core.QueryObject QueryObject}).
     * <p>
     * Note that the actual class that will be created is the class registered by
     * {@link #populateObjectFactory(java.util.List) populateObjectFactory()}, which may be a subclass of the given {@code type}.
     * The type of the returned object is the type specified in the parameter.
     * <p>
     * The given class type must contain or inherit a public static field named {@code ObjectCode}, otherwise the method will throw
     * an exception.
     * @param <T>   class type of the object you want to create.
     * @param type  the class of the object you want to create.
     * @return      the found or created object, or null if the object could not be created or it is not defined as a top level object.
     * @throws java.lang.NoSuchFieldException       if class <em>type</em> doesn't contain a static <em>ObjectCode</em> field.
     * @throws java.lang.IllegalAccessException     if <em>ObjectCode</em> field of class <em>type</em> is not public.
     */
    public final <T extends QueryObject> T createQuery(Class<T> type) throws NoSuchFieldException, IllegalAccessException {
        DataObject object = createDataObject(DataObject.getObjectCode(type));
        if (object == null)
            return null;
        return type.cast(object);
    }
    
    /**
     * Returns the root <em>data object</em>.
     * <p>
     * The 'root' data object is a predefined extension of {@link org.spiderwiz.core.DataObject DataObject} that stores all
     * {@link #createTopLevelObject(java.lang.Class, java.lang.String) top level objects}.
     * Call it when you need to search or manipulate the system object tree.
     * @return the system root data object.
     */
    public final DataObject getRootObject() {
        return dataManager == null ? null : dataManager.getRootObject();
    }
    
    /**
     * Handles the event that the <a href="../core/doc-files/config.html">application's configuration file</a> is modified and
     * reloaded, through <a href="http://spideradmin.com">SpiderAdmin</a> or by custom code.
     * <p>
     * Override this method if your application flow depends on configuration parameters that might be changed.
     */
    protected void onConfigChange() {}

    /**
     * If included in the application's classpath, Add XAdminQuery to the factory map (if not included yet)
     * @param factoryMap 
     */
    private void populateAdminClass(List<Class<? extends DataObject>> factoryList) {
        try {
            Class<QueryObject> xclass = (Class<QueryObject>)Class.forName(CoreConsts.XADMIN_CLASSPATH);
            xAdminQueryCode = DataObject.getObjectCode(xclass);
            factoryList.add(xclass);
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException ex) {
            Main.getLogger().logEvent(CoreConsts.NEED_INCLUDE_XADMIN);
        }
    }
    
    /**
     * Override this if you want to use your own version of MyConfig.
     * @return MyConfig object or an object derived from it.
     */
    private MyConfig createMyConfig() {
        return new MyConfig();
    }
    
    /**
     * Load or reload application's configuration.
     * @return true if succeeded.
     * @throws Exception
     */
    private boolean loadConfig () throws Exception {
        if (!config.reload())
            return false;
        hub.reloadConfig();
        return true;
    }
    
    /**
     * Starts application logging.
     * <p>
     * This method is called during the {@link #init() initialization} phase of the application. You can override it to implement
     * custom logging initialization in addition to the default one. Don't forget to call the {@code super} instance of this method.
     * <p>
     * By default, log files are appended to. If for any reason you want to rewrite the files, you can call the {@code super} instance
     * of this method with {@code append} parameter set to {@code false}.
     * @param append    true if content of new logs shall be appended to the files containing old logs, otherwise the files will be
     *                  rewritten.
     */
    protected void startLogging(boolean append) {
        if (logger != null)
            logger.cleanup();
        logger = new ZLog(rootFolder, config, append);
        logger.init(MyConfig.LOGFOLDER);
    }

    private OpResults createOpResults(String status){
        return new OpResults(status);
    }
    
    /**
     * Reloads the application's configuration file.
     * <p>
     * This method is called from <a href="http://spideradmin.com">SpiderAdmin</a> after the
     * <a href="doc-files/config.html">application's configuration file</a> is modified in order to apply modifications, i.e. cause
     * configuration-dependent code to be affected. You can override it if you implement custom configuration. Do not forget to call
     * the {@code super} instance of this method and return the object returned by it.
     * @return {@link org.spiderwiz.admin.data.OpResults OpResults} object (will be documented in a future release).
     */
    public OpResults reloadConfig() {
        String opStatus = CoreConsts.OpResults.StatusCodes.OK;
        try {
            loadConfig();
        } catch (Exception ex) {
            opStatus = ex.getMessage();
        }
        return createOpResults(opStatus);
    }

    /**
     * Flushes all open log files.
     * <p>
     * This method is called from <a href="http://spideradmin.com">SpiderAdmin</a> upon request of its user. You can override it if
     * you implement custom logging and you want to flush your log files on that occasion.
     * Do not forget to call the {@code super} instance of this method and return the object returned by it.
     * @return {@link org.spiderwiz.admin.data.OpResults OpResults} object (will be documented in a future release).
     */
    public OpResults flushLogs() {
        String opStatus = CoreConsts.OpResults.StatusCodes.OK;
        try {
            logger.flush();
            hub.flushAllLogs();
        } catch (Exception ex) {
            opStatus = ex.getMessage();
        }
        return createOpResults(opStatus);
    }

    /**
     * Executes custom periodical tasks.
     * <p>
     * This method is called once a minute from a dedicated thread. It does nothing by default, but you can override it to do anything
     * that you want.
     */
    protected void runPeriodicalTasks() {}
    
    /**
     * Executes the daily task.
     * <p>
     * This method is called once a day at the time set by [{@code start of day}] property in the
     * <a href="doc-files/config.html">application's configuration file</a>. By default it
     * calls {@link #resetCounters()} and {@link #reset()}. You can override it to do something else or something more.
     */
    protected void runDailyTask() {
        resetCounters();
        reset();
    }
    
    /**
     * Resets communication monitoring counters.
     */
    public final void resetCounters() {
        hub.reset();
    }
    
    AlertMail getAlertMail() {
        return alertMail;
    }
    
    /**
     * Sends a notification mail.
     * <p>
     * The method sends a notification mail on behalf of the application. You can call this method from anywhere in your application.
     * The method is also called internally by the Spiderwiz framework on events like disconnection and reconnection of communication
     * sessions. You can configure the exact conditions to send a notification through the
     * <a href="doc-files/config.html">application's configuration file</a>.
     * <p>
     * If the application has access to a mail server and a mail system and mail connection
     * properties are defined in the <a href="doc-files/config.html">application's configuration file</a>,
     * the mail is sent to the address(es) specified in [{@code mail to}] property with CC to the address(es)
     * specified in [{@code cc to}] property.
     * <p>
     * A mail can be sent on behalf of the application even if it does not have mail access. To do so, include
     * {@link org.spiderwiz.core.Main.ObjectCodes#EventReport ObjectCodes.EventReport} in the list of object codes returned by
     * {@link #getProducedObjects() getProducedObjects()}, then include
     * {@link org.spiderwiz.core.Main.ObjectCodes#EventReport ObjectCodes.EventReport} in the list of object codes returned by
     * {@link #getConsumedObjects() getConsumedObjects()} of any peer application that do have mail access. The application with
     * the mail access will trap the notification and mail it to the addresses defined in its own configuration file on behalf of
     * the application without mail access.
     * @param msg               the message to send. Will appear as the mail subject and as a title inside the mail body.
     * @param additionalInfo    multi-line additional information. Separate lines by the newline character ('\n'). Can be null if
     *                          not required.
     * @param eventTime         time of the notified event if needed, null otherwise.
     * @param alert             if true, the title in the mail body will appear in red, otherwise it will appear in green.
     */
    public void sendNotificationMail(String msg, String additionalInfo, ZDate eventTime, boolean alert) {
        alertMail.sendNotificationMail(getAppName(), ZUtilities.getMyIpAddress(), msg, null, additionalInfo, null, eventTime, alert);
        EventReport obj = Main.getInstance().createEventReport();
        if (obj != null)
            obj.commitEvent(msg, additionalInfo, null, alert);
    }
    
    void monitorDiskSpace() {
        int minDiskSpace = config.getIntProperty(MyConfig.MINIMUM_DISK_SPACE);
        boolean b = minDiskSpace > 0 && rootFile.getFreeSpace() < minDiskSpace * 1000000000L;
        if (b & !notEnoughDiskSpace)
            sendNotificationMail(
                String.format(CoreConsts.AlertMail.NO_DISKSPACE_BODY, minDiskSpace), null, null, true);
        notEnoughDiskSpace = b;
    }

    /**
     * Sends an alert mail on thrown exceptions.
     * <p>
     * The method sends an exception alert mail on behalf of the application. You can call this method when you catch an exception
     * in your application. The method is also called internally when exceptions are caught inside the Spiderwiz framework.
     * <p>
     * There are two types of exceptions - critical and non-critical. Every call to this method for a critical exception
     * will be handled and a mail message will be sent. Non-critical exceptions are handled only periodically, at the time 
     * interval defined  by [{@code exception alert rate}] property in the configuration file.
     * <p>
     * If the application has access to a mail server and a mail system and mail connection
     * properties are defined in the <a href="doc-files/config.html">application's configuration file</a>,
     * the mail is sent to the address(es) specified in [{@code mail exception to}] property with CC to the address(es)
     * specified in [{@code cc exception to}] property.
     * <p>
     * A mail can be sent on behalf of the application even if it does not have mail access. To do so, include
     * {@link org.spiderwiz.core.Main.ObjectCodes#EventReport ObjectCodes.EventReport} in the list of object codes returned by
     * {@link #getProducedObjects() getProducedObjects()}, then include
     * {@link org.spiderwiz.core.Main.ObjectCodes#EventReport ObjectCodes.EventReport} in the list of object codes returned by
     * {@link #getConsumedObjects() getConsumedObjects()} of any peer application that do have mail access. The application with
     * the mail access will trap the notification and mail it to the addresses defined in its own configuration file on behalf of
     * the application without mail access.
     * @param ex                the caught exception object.
     * @param msg               the message to send. Will appear as the mail subject and as a title inside the mail body.
     * @param additionalInfo    multi-line additional information. Separate lines by the newline character ('\n'). Can be null if
     *                          not needed.
     * @param critical          true if the exception is critical.
     */
    public void sendExceptionMail(Throwable ex, String msg, String additionalInfo, boolean critical) {
        if (!critical) {
            int alertRate = Main.getMyConfig().getExceptionAlertRate();
            if (lastException != null && alertRate > 0 && lastException.elapsed() < alertRate)
                return;
            lastException = ZDate.now();
        }
        String stackTrace = null;
        if (additionalInfo != null) {
            String[] infoLines = additionalInfo.split("\\n");
            boolean first = false;
            for (String line : infoLines) {
                logger.log(line, !first);
                first = false;
            }
        }
        if (ex != null) {
            logger.logException(ex);
            stackTrace = ZUtilities.stackTraceToString(ex);
        }
        // produce mail alerts
        AlertMail mail = getAlertMail();
        if (mail != null)
            mail.sendNotificationMail(getAppName(), ZUtilities.getMyIpAddress(), msg, null, additionalInfo, stackTrace, null, true);
        EventReport obj = Main.getInstance().createEventReport();
        if (obj != null)
            obj.commitEvent(msg, additionalInfo, stackTrace, true);
    }
    
    /**
     * Called when a communication session could not be established.
     * <p>
     * When Spiderwiz framework fails to establish a connection it prints a log message. If you want to do anything more,
     * override this method.
     * @param remoteAddress     the remote address that could not be reached.
     * @param e                 the exception that was thrown in the failed connection attempt if applicable, null otherwise.
     */
    protected void onConnectFailed(String remoteAddress, Exception e){}
    
    ZDate getDeployDate() {
        return deployDate;
    }

    MyConfig getHistory() {
        return history;
    }
    
    /**
     * Called when a <em>data object</em> of a specific type needs a reset.
     * <p>
     * Spiderwiz keeps the integrity of your data by sequencing the distributed data objects. When a receiver detects that an object
     * has gone out of sequence, it issues an Object Reset request. Upon reception of this request for a specific object type, the
     * producer of the lost objects should resend all the objects of that type. Normally this is done internally by the framework,
     * but there are cases, for instance when a data object is defined {@link org.spiderwiz.core.DataObject#isDisposable() disposable}
     * the application may need to do a reset from an external source (e.g. a database). To do that, override this method. In this
     * case make sure to return {@code true}.
     * <p>
     * Another use of this method is to modify the {@link Resetter} argument before it is used for automatic reset by the framework
     * (see the class description for modifiable properties). In this case return {@code false}.
     * @param resetter  An object that contains information about the object type that needs a reset
                        and provides a mechanism for streaming the reset data to its destination.
     * @return true if this method handled the reset request, false if it is left to the framework to do automatic object reset.
     */
    protected boolean onObjectReset(Resetter resetter) {
        return false;
    }
    
    /**
     * Called when a <em>data object</em> reset is completed.
     * <p>
     * When you use a {@link org.spiderwiz.core.Resetter Resetter} to {@link #onObjectReset(org.spiderwiz.core.Resetter) 
     * reset a data object} and you call {@link org.spiderwiz.core.Resetter#endOfData() Resetter.endOfData()} when you are done,
     * this method will be called when the resetter object is fully flushed for delivery. Override it if you want to do something in
     * this case.
     * @param resetter  an object that contains information about the object type that needs a reset
                        and provides a mechanism for streaming the recovered data to its destination.
     */
    protected void onResetCompleted(Resetter resetter) {
    }
    
    final Archiver getArchiver() {
        return archiver;
    }

    String getxAdminQueryCode() {
        return xAdminQueryCode;
    }

    /**
     * Create an EventReport object but only if we are defined as a producer of this object
     * @return an EventReport object or null
     */
    final EventReport createEventReport() {
        return DataManager.getInstance().isProducingObject(EventReport.ObjectCode) ?
            (EventReport)createDataObject(EventReport.ObjectCode) : null;
    }
    
    /**
     * Creates an import/export data handler.
     * <p>
     * Spiderwiz framework includes an import/export mechanism to interact with external data, implemented in
     * {@link org.spiderwiz.core.ImportHandler ImportHandler} class. If necessary, you can subclass it. If you do then
     * you need to make sure that your subclass will be instantiated and used instead of the default one. This is
     * done by overriding this method. If you do not override it, the default {@link org.spiderwiz.core.ImportHandler ImportHandler}
     * will be created.
     * @return a new instance of {@link org.spiderwiz.core.ImportHandler ImportHandler} or a subclass of it.
     * @see ImportHandler
     */
    protected ImportHandler createImportHandler() {return new ImportHandler();}
    
    /**
     * Adds a hook for a graceful termination of a command line application.
     * <p>
     * If you use Spiderwiz to build a command line Java application, you can call this method to add a hook for a graceful
     * termination.The method will:
     * <ul>
     * <li>{@link Runtime#addShutdownHook(java.lang.Thread) Add a shutdown hook} that calls {@link #cleanup() cleanup()}
     * upon application termination.</li>
     * <li>Read lines from {@link System#in the standard input stream} until the command "exit" is encountered, then terminate
     * the application.</li>
     * </ul>
     * <p>
     * Call this method after you call {@link #init() init()}. Note that the method runs on its caller thread, so if you have other
     * things to do after calling it, start a new thread and call the method from it.
     */
    public void commandLineHook() {
        // Install a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                cleanup();
            }
        });

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            while (!"exit".equalsIgnoreCase(br.readLine())) {
            }
            System.exit(0);
        } catch (IOException ex) {
        }
    }
    
    /**
     * Returns the application's status.
     * <p>
     * Used by <a href="http://spideradmin.com">SpiderAdmin</a> to show the status of the application.
     * By default the method returns "OK". Override the method if you want it to return something else.
     * @return string describing the application status.
     */
    protected String getStatus(){
        return CoreConsts.OpResults.StatusCodes.OK;
    }
    
    /**
     * Returns general information about the application.
     * <p>
     * Used by <a href="http://spideradmin.com">SpiderAdmin</a>. The method returns an object that contains:
     * <ul>
     * <li>Application name</li>
     * <li>Application version</li>
     * <li>Version of Spiderwiz framework library used to build the application</li>
     * <li>Application's IP address</li>
     * <li>Application's deploy time</li>
     * <li>Application's status</li>
     * <li>Application's root folder</li>
     * <li>Application's UUID.</li>
     * </ul>
     * @return an object containing application information.
     */
    public ApplicationInfo getApplicationInfo() {
        return new ApplicationInfo(getAppName(), getAppVersion(), getCoreVersion(), ZUtilities.getMyIpAddress(),
            deployDate, getStatus(), 0, 0, getRootFolder(),
            getAppUUID().toString());
    }
    
    /**
     * Returns the layout of the application's administration page.
     * <p>
     * Used by <a href="http://spideradmin.com">SpiderAdmin</a>. The method returns an object that specifies the elements
     * displayed in the application's administration page. Override it to customize the administration page layout.
     * <p>
     * You can customize a page by adding control buttons and tables to it. You can assign actions to buttons and fill tables
     * with data by overriding {@link #customAdminService(java.lang.String, java.lang.String) customAdminService()}.
     * @param userID    If not null the page will display only nodes connecting with this user ID.
     * @return an object containing page layout information.
     */
    public PageInfo getPageInfo(String userID) {
        return AdminServices.getPageInfo();
    }

    /**
     * Returns the structure of the Server Information table displayed in the application's administration page.
     * <p>
     * Used by <a href="http://spideradmin.com">SpiderAdmin</a>. The method returns an object that specifies the structure
     * of the Server Information table displayed in the application's administration page.
     * Override it to customize the table layout.
     * @return an object containing table structure information.
     */
    public TableInfo getServerInfoTableStructure() {
        return AdminServices.getServerInfoTableStructure();
    }

    /**
     * Requests data for the one-row Server Information table displayed in the application's administration page.
     * <p>
     * Used by <a href="http://spideradmin.com">SpiderAdmin</a>. The method fills data in the Server Information table
     * displayed in the application's administration page. You can override it to customize the data, for instance if you want to
     * delete or add columns. In this case you will also need to override
     * {@link #getServerInfoTableStructure() getServerInfoTableStructure()}.
     *
     * @param row an object to fill in data for one row of an administration table.
     */
    protected void addServerInfoRowData(TableData.RowData row) {
        AdminServices.addServerInfoRowData(row);
    }

    /**
     * Returns data for the Server Information table displayed in the application's administration page.
     * <p>
     * Used by <a href="http://spideradmin.com">SpiderAdmin</a>. The method returns an object that contains the data to display in
     * the Server Information table displayed in the application's administration page.
     * Override it to customize the table data.
     * @return an object containing table data.
     */
    public TableData getServerInfoTableData() {
        TableData td = new TableData();
        addServerInfoRowData(td.addRow());
        return td;
    }
    
    /**
     * Returns the structure of the Applications table displayed in the application's administration page.
     * <p>
     * Used by <a href="http://spideradmin.com">SpiderAdmin</a>. The method returns an object that specifies the structure
     * of the Applications table (describing peer applications of the current application) displayed in the application's
     * administration page.
     * Override it to customize the table layout.
     * @param tableTitle        the shown title of the table. Can be customized.
     * @param tableService      a tag that identifies the table. Shall not be touched.
     * @return an object containing table structure information.
     */
    public TableInfo getApplicationsTableStructure(String tableTitle, String tableService) {
        return AppInfo.getTableStructure(tableTitle, tableService);
    }

    /**
     * Returns the structure of the connected nodes tables displayed in the application's administration page.
     * <p>
     * Used by <a href="http://spideradmin.com">SpiderAdmin</a>. The method returns an object that specifies the structure
     * of the tables that give information about connected nodes (Producers or Consumers) displayed in the application's
     * administration page.
     * Override it to customize the table layout.
     * @param tableTitle        the shown title of the table. Can be customized.
     * @param tableService      a tag that identifies the table. Shall not be touched.
     * @return an object containing table structure information.
     */
    public TableInfo getConnectedNodesTableStructure(String tableTitle, String tableService) {
        return DataNodeInfo.getTableStructure(tableTitle, tableService);
    }

    /**
     * Returns the structure of the Imports Channels table displayed in the application's administration page.
     * <p>
     * Used by <a href="http://spideradmin.com">SpiderAdmin</a>. The method returns an object that specifies the structure
     * of the Imports Channels table displayed in the application's administration page.
     * Override it to customize the table layout.
     * @param tableTitle        the shown title of the table. Can be customized.
     * @param tableService      a tag that identifies the table. Shall not be touched.
     * @return an object containing table structure information.
     */
    public TableInfo getImportsTableStructure(String tableTitle, String tableService) {
        return ImportInfo.getTableStructure(tableTitle, tableService);
    }
    
    /**
     * Returns data for the Applications table displayed in the application's administration page.
     * <p>
     * Used by <a href="http://spideradmin.com">SpiderAdmin</a>. The method returns an object that contains the data to display in
     * the Applications table (describing peer applications of the current application) displayed in the application's administration
     * page. Override it to customize the table data.
     * @param userID        if not null the query is only for nodes connecting with this user ID.
     * @return an object containing table data.
     */
    public TableData getApplicationsTableData(String userID) {
        return hub.getAppsInfo(userID);
    }

    /**
     * Returns data for the connected nodes tables displayed in the application's administration page.
     * <p>
     * Used by <a href="http://spideradmin.com">SpiderAdmin</a>. The method returns an object that contains the data to display in
     * the tables that present information about connected nodes (Producers or Consumers) displayed in the application's administration
     * page. Override it to customize the table data.
     * @param consumers     if true, the request is for consumers, otherwise it is for producers.
     * @param userID        if not null the query is only for nodes connecting with this user ID.
     * @return an object containing table data.
     */
    public TableData getConnectedNodesData(boolean consumers, String userID) {
        return hub.getDnInfo(consumers, userID);
    }

    /**
     * Returns data for the Import Channels table displayed in the application's administration page.
     * <p>
     * Used by <a href="http://spideradmin.com">SpiderAdmin</a>. The method returns an object that contains the data to display in
     * the Import Channels table displayed in the application's administration page. Override it to customize the table data.
     * @return an object containing table data.
     */
    public TableData getImporNodesData() {
        return hub.getImportInfo();
    }

    /**
     * Does a custom administration action.
     * <p>
     * The method is used by <a href="http://spideradmin.com">SpiderAdmin</a> when the application's administration page is
     * customized through {@link #getPageInfo(java.lang.String) getPageInfo()}. Override this method to assign actions to buttons
     * and fill custom tables with data.
     * @param serviceTag    the tag assigned to your custom element.
     * @param userID        if not null the request pertains to this user ID.
     * @return an object that shall be {@link org.spiderwiz.admin.data.OpResults} if the tag identifies a button or
     * {@link org.spiderwiz.admin.data.TableData} if it identifies a table.
     */
    public Object customAdminService(String serviceTag, String userID) {
        return null;
    }
}
