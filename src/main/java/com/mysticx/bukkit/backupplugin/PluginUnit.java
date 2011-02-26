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
    protected Server etc;
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
        this.etc = instance;
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
        this.etc = instance;
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
    public String getName() {
        return this.name;
    }

    /**
     * sets plugin work path
     *
     * @param folder
     */
    public void setWorkDir(File workPath) {
        this.workPath = workPath;
    }

    /**
     *
     * @return working directory
     */
    protected File getWorkDir() {
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
    protected int calculateTimeDifference(long start, long end) {
        Long time = (end - start) / 1000;
        return time.intValue();
    }

    /**
     * Generates a generic filename
     *
     * @param suffix
     * @return the filename
     */
    public String generateFilename(String suffix) {
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
    public boolean isEnabled() {
        return this.isEnabled;
    }

    /**
     *
     * @return true if the unit is forced
     */
    public boolean isForce() {
        return this.isForce;
    }

    /**
     * Enables or disables a unit
     *
     * @param bool
     */
    public void setEnabled(boolean bool) {
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
    public void setForce(boolean bool) {
        this.isForce = bool;
    }

    /**
     * do nothing
     */
    protected void sleepaWhile(long millis) {
        try {
            MessageHandler.log(Level.FINE, getName() + " going to sleep for " + (millis / 1000) + " seconds..");
            Thread.sleep(millis);
            MessageHandler.log(Level.FINE, getName() + " woke up from sleep as expected.");
        } catch (InterruptedException e) {
            MessageHandler.log(Level.WARNING, getName() + "woke up too early!", e);
        }
    }

    /**
     * Saves the world and disables saving! Remember to enable saving again
     * afterwards
     */
    protected void saveWorld() {
        // TODO: save world and disable saving for mapping process
        // etc.getServer().useConsoleCommand("save-on"); // Ensures that saving
        // is enabled
        // etc.getServer().useConsoleCommand("save-all");
        // etc.getServer().useConsoleCommand("save-off");

        // sleepaWhile(sleepTime);
    }

    /**
     * resets force to initial value
     */
    public void resetForce() {
        MessageHandler.log(Level.FINE, "Resetting force to " + this.initialForce);
        this.isForce = this.initialForce;
    }

    /**
     * main functionality of Unit
     *
     */
    abstract public void run();

    /**
     * main functionality of Unit
     *
     * @param force
     */
    public void run(boolean force) {
        boolean oldforce = isForce();
        setForce(force);
        run();
        setForce(oldforce);
    }

}
