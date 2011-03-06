package com.mysticx.bukkit.backupplugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;

import org.bukkit.Server;
import org.bukkit.World;

/**
 * Backup Plugin
 *
 * Mapper Unit
 *
 * @author MysticX
 *
 */
public final class MapperUnit extends PluginUnit {

    // values
    private File mapperPath;
    private String[] mapOptions;
    private boolean useLatest;

    /**
     * Default constructor
     */
    public MapperUnit(Server instance, File workdir) {
        super(instance, workdir);
        this.name = "MapperUnit";
    }

    /**
     * Default constructor
     */
    public MapperUnit(Server instance, File workdir, boolean force) {
        super(instance, workdir, force);
        this.name = "MapperUnit";
    }

    /**
     * Sets path to mapper executable
     *
     * @param mapper_path
     */
    public void setMapperPath(File mapperTool) {
        this.mapperPath = mapperTool;
        if (mapperPath == null || !mapperPath.exists()) {
            setEnabled(false);
            MessageHandler.warning("Disabled MapperUnit, mapper_path invalid: " + mapperPath);
        }
    }

    /**
     * Sets mapping options
     *
     * @param mapOptions
     */
    public void setMapOptions(String[] mapOptions) {
        this.mapOptions = Arrays.copyOf(mapOptions, mapOptions.length);
    }

    /**
     * Enables or disables usage of latest.png
     *
     * @param true, if enabled
     */
    public void setUseLatest(boolean useLatest) {
        this.useLatest = useLatest;
    }

    /**
     * Generates maps via commandline
     *
     */
    @Override
    public void run() {

        while (!isEnabled) {
            MessageHandler.log(Level.WARNING, " is disabled. Thread goes to sleep.");
            try {
                this.wait();
            } catch (InterruptedException e) {
                MessageHandler.log(Level.WARNING, "woke up from sleep unexpectedly!", e);
            }
        }

        MessageHandler.log(Level.INFO, "Starting map generation process.. this could take a while!");

        server.savePlayers();

        // create folders
        if (!this.getWorkDir().exists()) {
            this.getWorkDir().mkdirs();
        }

        // lock cache while generating maps
        cc.getLock().lock();
        MessageHandler.log(Level.FINEST, "got lock, starting map generation");

        for (String worldname : cc.getWorlds()) {
            final World world = server.getWorld(worldname);
            if (world == null) {
                MessageHandler.warning(String.format("World %s don't exist", worldname));
            } else {
                world.save();
                File inputFolder = null;
                try {
                    // retrieve cache
                    inputFolder = cc.getCache(worldname, this.isForce());
                } catch (Exception e) {
                    MessageHandler.log(Level.SEVERE, "An error ocurred during mapping", e);
                    return;
                }
                MessageHandler.info(String.format("Mapping world : %s...", worldname));
                // do mappings
                for (int i = 0; i < mapOptions.length; i++) {
                    MessageHandler.info("Mapping pass " + (i + 1) + " of " + mapOptions.length + "...");

                    // modify parameters
                    String filename = generateFilename(".png", worldname);
                    String mapParameters = mapOptions[i];
                    mapParameters = mapParameters.replace("$o", new File(this.getWorkDir(), filename).getAbsolutePath());
                    mapParameters = mapParameters.replace("$w", inputFolder.getAbsolutePath());

                    if (mapParameters.contains("$m")) {
                        mapParameters = mapParameters.replace("$m", mapperPath.getParent());
                    }

                    MessageHandler.log(Level.FINE, "Mapper usage: " + mapperPath + " " + mapParameters);

                    // generate maps
                    executeExternal(mapperPath, mapParameters);

                    // save latest.png at first run
                    if (i == 0 && useLatest) {
                        final String worldnameEscaped = worldname.replaceAll(" ", "");
                        try {
                            iohelper.deleteFile(new File(this.getWorkDir(), String.format("latest-%s.png", worldnameEscaped)));
                            iohelper.copyFile(new File(this.getWorkDir(), filename), new File(this.getWorkDir(), String.format("latest-%s.png", worldnameEscaped)), false);
                        } catch (IOException e) {
                            MessageHandler.log(Level.WARNING, String.format("Creating latest-%s.png failed: ", worldnameEscaped), e);
                        }

                    }
                }
            }
        }
        MessageHandler.info("Mapping process finished.");
        cc.getLock().unlock();

        setChanged();
        notifyObservers();
    }

    /**
     * Executes external binaries
     *
     * @param program
     *            path to executable
     * @param arguments
     *            arguments to invoke
     * @return true if successful
     */
    private void executeExternal(File program, String arguments) {
        try {
            long start = System.currentTimeMillis();
            SysCommandExecutor cmdExecutor = new SysCommandExecutor();
            int exitStatus = cmdExecutor.runCommand(program + " " + arguments);

            if (exitStatus != 0) {
                MessageHandler.warning("Mapping failed, something went wrong while executing external code! Exit Status: " + exitStatus);
            } else {
                MessageHandler.info("Mapping successful! Executing mapper took " + calculateTimeDifference(start, System.currentTimeMillis()) + " seconds.");
            }
        } catch (Exception e) {
            MessageHandler.log(Level.SEVERE, "Error while executing external code!", e);
        }
    }

}
