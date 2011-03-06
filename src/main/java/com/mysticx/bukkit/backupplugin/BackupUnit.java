package com.mysticx.bukkit.backupplugin;

import java.io.File;
import java.util.logging.Level;

import org.bukkit.Server;
import org.bukkit.World;

/**
 * BackupPlugin
 *
 * Backup Unit
 *
 * @author MysticX
 *
 */
public final class BackupUnit extends PluginUnit {

    /**
     * Default constructor
     */
    public BackupUnit(Server instance, File workdir) {
        super(instance, workdir);
        this.name = "BackupUnit";
    }

    /**
     * Default constructor
     */
    public BackupUnit(Server instance, File workdir, boolean force) {
        super(instance, workdir, force);
        this.name = "BackupUnit";
    }

    /**
     * creates backup of world data
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

        MessageHandler.log(Level.INFO, "Starting backup process..");

        etc.savePlayers();

        try {
            final String worldname = this.getWorkDir().getName();
            final World world = etc.getWorld(worldname);
            if (world == null) {
                MessageHandler.warning(String.format("World %s don't exist", worldname));
            } else {
                world.save();
                // generate filename
                String filename = generateFilename(".zip");

                File outputFile = new File(this.getWorkDir(), filename);

                if (cc.persistCache(outputFile, this.isForce())) {
                    MessageHandler.log(Level.INFO, "Backup sucessfull");
                } else {
                    MessageHandler.log(Level.WARNING, "Backup failed");
                }
            }

        } catch (Exception e) {
            MessageHandler.log(Level.SEVERE, "Error during backup: ", e);
        } finally {
            setChanged();
            notifyObservers();
        }
    }

}
