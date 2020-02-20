/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spiderwiz.admin.xml;

import com.sun.management.OperatingSystemMXBean;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 *
 * @author Nuriel
 */
public final class ApplicationInfoEx extends ApplicationInfo {
    private String applicationRootFileName;

    public ApplicationInfoEx() {
        this(null);
    }

    public ApplicationInfoEx(String applicationRootFileName) {
        this.applicationRootFileName = applicationRootFileName;
    }
    
    public ApplicationInfoEx(String applicationName, String version, String coreVersion, String serverLocation,
        String deployTime, String status, int progressedUploadTime, int expectedUploadTime,
        String applicationRootFileName, String applicationUUID) {
        this(applicationRootFileName);
        setApplicationName(applicationName);
        setVersion(version);
        setCoreVersion(coreVersion);
        setServerLocation(serverLocation);
        setDeployTime(deployTime);
        setStatus(status);
        setProgressedUploadTime(progressedUploadTime);
        setExpectedUploadTime(expectedUploadTime);
        setApplicationAddress(applicationUUID);
    }

    @Override
    public String getUsedCpu() {
        return String.valueOf(calcUsedCpu());
    }

    private double calcUsedCpu() {
        try {
            OperatingSystemMXBean osMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            return new BigDecimal(osMXBean.getSystemCpuLoad() * 100).setScale(2, RoundingMode.HALF_UP).doubleValue();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     *
     * @return available JVM memory in MB.
     */
    @Override
    public String getAvailableJvmMemory() {
        return String.valueOf(calcAvailableJvmMemory());
    }

    private double calcAvailableJvmMemory() {
        return getBytesAs(Runtime.getRuntime().freeMemory(), "MB");
    }

    @Override
    public String getAvailableDiskSpace() {
        return getAvailableDiskSpace(applicationRootFileName);
    }

    public String getAvailableDiskSpace(String filename) {
        return String.valueOf(getBytesAs( new File(filename != null && !filename.isEmpty() ? filename : "/").getFreeSpace(), "GB"));
    }

    private double getBytesAs(long bytes, String as) {
        int devider = 1024 * 1024;
        devider *= as.equals("GB") ? 1024 : 1;
        return new BigDecimal((double) bytes / devider).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
