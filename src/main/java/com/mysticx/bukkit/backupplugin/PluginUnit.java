package com.mysticx.bukkit.backupplugin;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.logging.Level;

import org.bukkit.Server;

/**
 * Abstract class for PluginUnits similar to a "plugin-in-plugin" system
 *
 * @author MysticX
 *
 */
public abstract class PluginUnit extends Observable implements Runnable {

    protected String name;

    // Objects
    protected CacheControl cc;
    protected Server server;
    protected IOHelper iohelper;
    protected File workPath;

    // values
    protected boolean isEnabled;
    protected boolean isForce;
    protected boolean initialForce;

    /**
     * Default constructor initializes some stuff, force = false
     *
     * @param workdir
     *            working directory
     */
    public PluginUnit(Server instance, File workdir) {
        this.cc = CacheControl.getInstance();
        this.server = instance;
        this.iohelper = IOHelper.getInstance();
        this.isEnabled = true;
        this.isForce = false;
        this.initialForce = false;
        this.workPath = workdir;
    }

    /**
     * Default constructor initializes some stuff
     *
     * @param workdir
     *            working directory
     * @param force
     *            true if cache rebuild should be forced at execution
     */
    public PluginUnit(Server instance, File workdir, boolean force) {
        this.cc = CacheControl.getInstance();
        this.server = instance;
        this.iohelper = IOHelper.getInstance();
        this.isEnabled = true;
        this.isForce = force;
        this.initialForce = force;
        this.workPath = workdir;
    }

    /**
     *
     * @return name of PluginUnit
     */
    public final String getName() {
        return this.name;
    }

    /**
     * sets plugin work path
     *
     * @param folder
     */
    public final void setWorkDir(File workPath) {
        this.workPath = workPath;
    }

    /**
     *
     * @return working directory
     */
    protected final File getWorkDir() {
        return workPath;
    }

    /**
     * calculates elapsed time between two values in ms
     *
     * @param start
     * @param end
     *
     * @return elapsed time in seconds
     */
    protected final int calculateTimeDifference(long start, long end) {
        Long time = (end - start) / 1000;
        return time.intValue();
    }

    /**
     * Generates a generic filename
     *
     * @param suffix
     * @return the filename
     */
    public final String generateFilename(String suffix) {
        // generate filename
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmssSSSS");
        String time = sdf.format(new Date(System.currentTimeMillis()));

        // escape spaces in world name
        String worldname = cc.getWorld().getName().replaceAll(" ", "");

        String filename = (worldname + "_" + time + suffix);
        MessageHandler.log(Level.FINEST, "Generated filename: " + filename);
        return filename;
    }

    /**
     *
     * @return true if the unit is enabled
     */
    public final boolean isEnabled() {
        return this.isEnabled;
    }

    /**
     *
     * @return true if the unit is forced
     */
    public final boolean isForce() {
        return this.isForce;
    }

    /**
     * Enables or disables a unit
     *
     * @param bool
     */
    public final void setEnabled(boolean bool) {
        this.isEnabled = bool;
        if (bool) {
            notifyAll();
        }
    }

    /**
     * Enables or disables cache force
     *
     * @param bool
     */
    public final void setForce(boolean bool) {
        this.isForce = bool;
    }

    /**
     * do nothing
     */
    protected final void sleepaWhile(long millis) {
        try {
            MessageHandler.log(Level.FINE, getName() + " going to sleep for " + (millis / 1000) + " seconds..");
            Thread.sleep(millis);
            MessageHandler.log(Level.FINE, getName() + " woke up from sleep as expected.");
        } catch (InterruptedException e) {
            MessageHandler.log(Level.WARNING, getName() + "woke up too early!", e);
        }
    }

    /**
     * resets force to initial value
     */
    public final void resetForce() {
        MessageHandler.log(Level.FINE, "Resetting force to " + this.initialForce);
        this.isForce = this.initialForce;
    }

    /**
     * main functionality of Unit
     *
     */
    public abstract void run();

    /**
     * main functionality of Unit
     *
     * @param force
     */
    public final void run(boolean force) {
        boolean oldforce = isForce();
        setForce(force);
        run();
        setForce(oldforce);
    }

}
