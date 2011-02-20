package com.mysticx.bukkit.backupplugin;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.Material;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;

/**
 * BackupPlugin block listener
 * @author MysticX
 */
public class BackupPluginBlockListener extends BlockListener {
	
    private final BackupPlugin plugin;

    public BackupPluginBlockListener(final BackupPlugin plugin) {
        this.plugin = plugin;
    }

    //put all Block related code here
    
 
}
