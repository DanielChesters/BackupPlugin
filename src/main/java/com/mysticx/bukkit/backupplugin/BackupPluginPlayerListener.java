package com.mysticx.bukkit.backupplugin;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

import com.bukkit.authorblues.GroupUsers.GroupUsers;

/**
 * Handle events for all Player related events
 * @author MysticX
 */
public class BackupPluginPlayerListener extends PlayerListener {
    private final BackupPlugin plugin;
    private Plugin groupUsersPlugin = null;
    
    public BackupPluginPlayerListener(BackupPlugin instance) {
        plugin = instance;
    }

    
    private boolean loadedGroupUsers = false;
    
    private void loadGroupUsersOnce() {
		if (loadedGroupUsers)
			return;
		
    	this.groupUsersPlugin = plugin.getServer().getPluginManager().getPlugin("GroupUsers");
		
		if (groupUsersPlugin==null) {
			MessageHandler.log(Level.FINE, "no group users plugin found, falling back to own config!");
		} else {
			MessageHandler.log(Level.FINE, "group users plugin found, using hey0's users.txt and group.txt!");
		}
		
		loadedGroupUsers=true;
    }
    
	@Override
	public void onPlayerCommand(PlayerChatEvent event) {
		loadGroupUsersOnce();
			
		Player player = event.getPlayer();

		// check if player can use command
//		if (!plugin.isAuthorized(player.getName())) {
//			MessageHandler.log(Level.FINEST,"Unauthorized player: "+player.getName());
//			return;
//		}

		String[] split = event.getMessage().split(" ");
		
		// check for backup command
		if (split[0].equals("/backup") && canUseCommand(player, "/backup")) {
			if (split.length > 2) {
				player.sendMessage(ChatColor.RED
						+ "Correct usage is: /backup <force> (optional)");
			} else {
				boolean force = false;
				if (split.length == 2)
					force = Boolean.valueOf(split[1]);

				String broadcast = player.getName()
						+ " triggered world backup.";
				MessageHandler.info(broadcast + " force = " + force);
				MessageHandler.broadcast(broadcast);

				plugin.performBackup(force);
			}

			event.setCancelled(true);
			return;
		}

		// check for map command
		if (split[0].equals("/map") && canUseCommand(player, "/map")) {
			if (split.length > 2) {
				player.sendMessage(ChatColor.RED
						+ "Correct usage is: /map <force> (optional)");
			} else {
				boolean force = false;
				if (split.length == 2)
					force = Boolean.valueOf(split[1]);

				String broadcast = player.getName()
						+ " triggered world mapping.";
				MessageHandler.info(broadcast + " force = " + force);
				MessageHandler.broadcast(broadcast);

				plugin.performMapping(force);
			}
			
			event.setCancelled(true);
			return;		
		}

		// check for reload command
		if (split[0].equals("/breload") && canUseCommand(player, "/breload")) {
			String broadcast = player.getName() + " triggered config reload.";
			MessageHandler.info(broadcast);
			MessageHandler.broadcast(broadcast);

			plugin.load();
			
			event.setCancelled(true);
			return;
		}

		// check for log level command
		if (split[0].equals("/loglevel") && split.length == 2 && canUseCommand(player, "/loglevel")) {
			MessageHandler.info(player.getName() + " is changing log level to "
					+ split[1]);
			boolean b = MessageHandler.setLogLevel(split[1]);
			if (b)
				player.sendMessage("Done!");
			else
				player.sendMessage("Failed!");
			
			event.setCancelled(true);
			return;
		}

		// no match
		event.setCancelled(false);
	}
	
	/**
	 * Checks if a given player can use a given command
	 * (Tries to user Group Users Plugin first, own config only if there is no plugin)
	 * 
	 * @param player
	 * @param command
	 * @return
	 */
	private boolean canUseCommand(Player player, String command) {
		// check for groupUserPlugin
		if (groupUsersPlugin!=null) {
			  GroupUsers groupUsers = (GroupUsers) groupUsersPlugin;
	          if (groupUsers.playerCanUseCommand(player, command)) {
	        	  return true;
	          } else {
	        	  return false;
	          }
	    // no groupUsersPlugin
		} else {
			return plugin.isAuthorized(player.getName());
		}
	}
    
}

