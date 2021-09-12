package org.spiderwiz.admin.data;

import com.sun.management.OperatingSystemMXBean;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.spiderwiz.annotation.WizField;
import org.spiderwiz.annotation.WizSerializable;
import org.spiderwiz.zutils.ZDate;

/**
 * Provides information about an application for the use of <a href="http://spideradmin.com">SpiderAdmin</a>.
 * <p>
 * The class is used internally by the SpiderAdmin agent and is rarely accessed explicitly. It is documented here for those
 * rare cases.
 */
@WizSerializable
public class ApplicationInfo {
    @WizField private String applicationName;
    @WizField private String version;
    @WizField private String coreVersion;
    @WizField private String serverLocation;
    @WizField private ZDate deployTime;
    @WizField private String status;
    @WizField private int progressedUploadTime;
    @WizField private int expectedUploadTime;
    @WizField private String applicationUUID;

    private String applicationRootFileName = null;
    
    /**
     * Default class constructor.
     */
    public ApplicationInfo() {
    }

    /**
     * Class constructor that sets class properties.
     * @param applicationName               Application name.
     * @param version                       Application version.
     * @param coreVersion                   Version of the Spiderwiz framework used by the application.
     * @param serverLocation                IP address of the machine that runs the application.
     * @param deployTime                    The time the application was deployed.
     * @param status                        User defined application status.
     * @param progressedUploadTime          Time in seconds that the application's deploy file uploading is in progress.
     * @param expectedUploadTime            Remaining time in seconds for the application's deploy file uploading.
     * @param applicationRootFileName       Name of the application root folder.
     * @param applicationUUID               The application UUID.
     */
    public ApplicationInfo(String applicationName, String version, String coreVersion, String serverLocation, ZDate deployTime,
        String status, int progressedUploadTime, int expectedUploadTime,
        String applicationRootFileName, String applicationUUID
    ) {
        this.applicationName = applicationName;
        this.version = version;
        this.coreVersion = coreVersion;
        this.serverLocation = serverLocation;
        this.deployTime = deployTime;
        this.status = status;
        this.progressedUploadTime = progressedUploadTime;
        this.expectedUploadTime = expectedUploadTime;
        this.applicationUUID = applicationUUID;
        this.applicationRootFileName = applicationRootFileName;
    }

    /**
     * Gets the value of the applicationName property.
     * <p>
     * The {@code applicationName} property holds the value returned by the application's
     * {@link org.spiderwiz.core.Main#getAppName() Main.getAppName()} method.
     * @return the applicationName property
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Sets the value of the applicationName property.
     * <p>
     * The {@code applicationName} property holds the value returned by the application's
     * {@link org.spiderwiz.core.Main#getAppName() Main.getAppName()} method.
     * @param value
     */
    public void setApplicationName(String value) {
        this.applicationName = value;
    }

    /**
     * Gets the value of the version property.
     * <p>
     * The {@code version} property holds the value returned by the application's
     * {@link org.spiderwiz.core.Main#getAppVersion() Main.getAppVersion()} method.
     * @return the version property
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the value of the version property.
     * <p>
     * The {@code version} property holds the value returned by the application's
     * {@link org.spiderwiz.core.Main#getAppVersion() Main.getAppVersion()} method.
     * @param value
     */
    public void setVersion(String value) {
        this.version = value;
    }

    /**
     * Gets the value of the coreVersion property.
     * <p>
     * The {@code coreVersion} property holds the value returned by the application's
     * {@link org.spiderwiz.core.Main#getCoreVersion()  Main.getCoreVersion()} method.
     * @return the coreVersion property
     */
    public String getCoreVersion() {
        return coreVersion;
    }

    /**
     * Sets the value of the coreVersion property.
     * <p>
     * The {@code coreVersion} property holds the value returned by the application's
     * {@link org.spiderwiz.core.Main#getCoreVersion()  Main.getCoreVersion()} method.
     * @param value
     */
    public void setCoreVersion(String value) {
        this.coreVersion = value;
    }

    /**
     * Gets the value of the serverLocation property.
     * <p>
     * The {@code serverLocation} property holds the IP address of the machine that the application runs on.
     * @return the serverLocation property
     */
    public String getServerLocation() {
        return serverLocation;
    }

    /**
     * Sets the value of the serverLocation property.
     * <p>
     * The {@code serverLocation} property holds the IP address of the machine that the application runs on.
     * @param value
     */
    public void setServerLocation(String value) {
        this.serverLocation = value;
    }

    /**
     * Gets the value of the deployTime property.
     * <p>
     * The {@code deployTime} property holds the value returned by the application's
     * {@link org.spiderwiz.core.Main#getDeployDate() Main.getDeployDate()} method.
     * @return the deployTime property
     */
    public ZDate getDeployTime() {
        return deployTime;
    }

    /**
     * Sets the value of the deployTime property.
     * <p>
     * The {@code deployTime} property holds the value returned by the application's
     * {@link org.spiderwiz.core.Main#getDeployDate() Main.getDeployDate()} method.
     * @param value
     */
    public void setDeployTime(ZDate value) {
        this.deployTime = value;
    }

    /**
     * Gets the value of the status property.
     * <p>
     * The {@code status} property holds the value returned by the application's
     * {@link org.spiderwiz.core.Main#getStatus() Main.getStatus()} method.
     * @return the status property
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     * <p>
     * The {@code status} property holds the value returned by the application's
     * {@link org.spiderwiz.core.Main#getStatus() Main.getStatus()} method.
     * @param value
     */
    public void setStatus(String value) {
        this.status = value;
    }

    /**
     * Gets the value of the progressedUploadTime property.
     * <p>
     * The {@code progressedUploadTime} property holds the
     * time in seconds that the application's deploy file uploading is in progress.
     * @return the progressedUploadTime property
     */
    public int getProgressedUploadTime() {
        return progressedUploadTime;
    }

    /**
     * Sets the value of the progressedUploadTime property.
     * <p>
     * The {@code progressedUploadTime} property holds the
     * time in seconds that the application's deploy file uploading is in progress.
     * @param value
     */
    public void setProgressedUploadTime(int value) {
        this.progressedUploadTime = value;
    }

    /**
     * Gets the value of the expectedUploadTime property.
     * <p>
     * The {@code expectedUploadTime} property holds the
     * remaining time in seconds for the application's deploy file uploading.
     * @return the expectedUploadTime property
     */
    public int getExpectedUploadTime() {
        return expectedUploadTime;
    }

    /**
     * Sets the value of the expectedUploadTime property.
     * <p>
     * The {@code expectedUploadTime} property holds the
     * remaining time in seconds for the application's deploy file uploading.
     * @param value
     */
    public void setExpectedUploadTime(int value) {
        this.expectedUploadTime = value;
    }

    /**
     * Gets the value of the applicationUUID property.
     * <p>
     * The {@code applicationUUID} property holds the value returned by the application's
     * {@link org.spiderwiz.core.Main#getAppUUID() Main.getAppUUID()} method.
     * @return the applicationUUID property
     */
    public String getApplicationUUID() {
        return applicationUUID;
    }

    /**
     * Sets the value of the applicationUUID property.
     * <p>
     * The {@code applicationUUID} property holds the value returned by the application's
     * {@link org.spiderwiz.core.Main#getAppUUID() Main.getAppUUID()} method.
     * @param value
     */
    public void setApplicationUUID(String value) {
        this.applicationUUID = value;
    }

    /**
     * Gets the percentage of the CPU currently in use at the machine that the application runs on.
     * @return the percentage of the CPU as a String.
     */
    public String getUsedCpu() {
        return String.valueOf(calcUsedCpu());
    }

    /**
     * Gets the amount of available JVM memory in MB at the machine that the application runs on.
     * @return the amount of available JVM memory as a String.
     */
    public String getAvailableJvmMemory() {
        return String.valueOf(calcAvailableJvmMemory());
    }

    /**
     * Gets the amount of available disk space in GB of the hard disk that contains the application's root folder.
     * @return the amount of available disk space as a String.
     */
    public String getAvailableDiskSpace() {
        return getAvailableDiskSpace(applicationRootFileName);
    }

    private double calcUsedCpu() {
        try {
            OperatingSystemMXBean osMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            return new BigDecimal(osMXBean.getSystemCpuLoad() * 100).setScale(2, RoundingMode.HALF_UP).doubleValue();
        } catch (Exception e) {
            return -1;
        }
    }

    private double calcAvailableJvmMemory() {
        return getBytesAs(Runtime.getRuntime().freeMemory(), "MB");
    }

    private double getBytesAs(long bytes, String as) {
        int devider = 1024 * 1024;
        devider *= as.equals("GB") ? 1024 : 1;
        return new BigDecimal((double) bytes / devider).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String getAvailableDiskSpace(String filename) {
        return String.valueOf(getBytesAs( new File(filename != null && !filename.isEmpty() ? filename : "/").getFreeSpace(), "GB"));
    }
}
