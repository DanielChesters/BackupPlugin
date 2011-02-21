package com.mysticx.bukkit.backupplugin;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijikokun.bukkit.Permissions.Permissions;

/**
 * BackupPlugin for Bukkit 2011-01-18
 *
 * BackupPlugin by MysticX is licensed under a Creative Commons
 * Attribution-NonCommercial-ShareAlike 3.0 Unported License.
 * http://creativecommons.org/licenses/by-nc-sa/3.0/
 *
 * Permissions beyond the scope of this license may be available at
 * http://forum.hey0.net/showthread.php?tid=179
 *
 * @author MysticX
 */
public class BackupPlugin extends JavaPlugin implements Observer {

    private final HashMap<Player, Boolean> debugees = new HashMap<Player, Boolean>();

    // config
    // private Configuration config;
    private TimeUnit timeunit = TimeUnit.MINUTES;

    // cache control
    private CacheControl cc;

    // Units
    private BackupUnit bu;
    private MapperUnit mu;

    private Permissions permissions = null;

    /*
     * (non-Javadoc)
     *
     * @see org.bukkit.plugin.Plugin#onEnable()
     */
    public void onEnable() {
        MessageHandler.setServer(getServer());

        PluginDescriptionFile pdfFile = this.getDescription();
        MessageHandler.info(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");

        Plugin plugin = this.getServer().getPluginManager().getPlugin("Permissions");

        if (plugin == null) {
            MessageHandler.log(Level.INFO, "No group users plugin found, falling back to own config!");
        } else {
            permissions = ((Permissions)plugin);
            MessageHandler.log(Level.INFO, "Permissions plugin found, using Permission config");
        }

        load();
    }

    /**
     * Loads property files and initializes some other stuff
     *
     * @return true if successful
     */
    protected boolean load() {
        // TODO: use Bukkit config when its finally working!
        com.mysticx.bukkit.backupplugin.Configuration config = new com.mysticx.bukkit.backupplugin.Configuration("BackupPlugin.properties");
        config.load();

        String separator = System.getProperty("file.separator");

        // some important values
        String world = config.getString("level-name", "world");
        String backup_folder = config.getString("backup-path", "world-backups");
        String mapper_path = config.getString("mapper-executable", "mcmap" + separator + "mcmap.exe");
        String map_folder = config.getString("map-path", "world-maps");
        String map_options = config.getString("map-options", "-png -file $o $w;-night -png -file $o $w");
        Integer autobackup_period = config.getInt("autobackup-period", 0);
        Integer automap_period = config.getInt("automap-period", 0);
        Integer cache_lifetime = config.getInt("cache-lifetime", 30);
        String tempdir = config.getString("temp-path", backup_folder + "/temp");
        String loglevel = config.getString("log-level", "INFO");
        String time_unit = config.getString("time-unit", "MINUTES");
        Integer num_backups = config.getInt("backup-history", 5);
        Boolean useLatest = config.getBoolean("use-latest", false);
        String firstRun = config.getString("first-run", "1200");
        String admins = config.getString("authorized-users", "");

        MessageHandler.setLogLevel(loglevel);

        // authorized users
        authorizedUsers = new ArrayList<String>();

        String[] access = admins.split(";");

        StringBuilder logInfo = new StringBuilder();

        for (String name : access) {
            if (!name.isEmpty()) {
                authorizedUsers.add(name.toLowerCase());
                logInfo.append(name).append(", ");
            }
        }

        MessageHandler.log(Level.FINE, String.format("There are %d user(s) in the authorized-users list: %s", authorizedUsers.size(), logInfo));

        // timeUnit
        try {
            TimeUnit tu = TimeUnit.valueOf(time_unit);
            this.timeunit = tu;
        } catch (Exception e) {
            MessageHandler.warning("Failed to parse time-unit, using default.");
        }

        // init cache
        this.cc = CacheControl.getInstance();
        this.cc.setWorld(world);
        this.cc.setTimeUnit(timeunit);
        this.cc.setCacheLifetime(cache_lifetime);
        this.cc.setTempDir(new File(tempdir));
        this.cc.setCacheHistory(num_backups);

        // init BackupUnit
        this.bu = new BackupUnit(this.getServer(), new File(backup_folder), true);
        this.bu.addObserver(this);

        // init MapperUnit
        this.mu = new MapperUnit(this.getServer(), new File(map_folder), false);
        this.mu.setMapperPath(new File(mapper_path));
        this.mu.setUseLatest(useLatest);
        this.mu.addObserver(this);

        String[] parameters = map_options.split(";");
        this.mu.setMapOptions(parameters);

        // init scheduler
        scheduler.shutdownNow();
        scheduler = Executors.newScheduledThreadPool(2);

        // schedule timer
        long backup_delay = -1;
        long map_delay = -1;

        try {
            long timeToExecuteB = calcNextPointOfTime(firstRun, "HHmm", TimeUnit.MILLISECONDS.convert(autobackup_period, timeunit));
            backup_delay = timeToExecuteB - System.currentTimeMillis();

            long timeToExecuteM = calcNextPointOfTime(firstRun, "HHmm", TimeUnit.MILLISECONDS.convert(automap_period, timeunit));
            map_delay = timeToExecuteM - System.currentTimeMillis();
        } catch (ParseException pe) {
            MessageHandler.log(Level.WARNING, "Failed to parse firstRun, disabled automatic execution", pe);
        }

        if (autobackup_period != null && backup_delay >= 0 && autobackup_period > 0) {
            setupTimer(bu, backup_delay, autobackup_period, this.timeunit);
        }

        if (automap_period != null && map_delay >= 0 && automap_period > 0) {
            setupTimer(mu, map_delay, automap_period, this.timeunit);
        }

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bukkit.plugin.Plugin#onDisable()
     */
    public void onDisable() {
        System.out.println("BackupPlugin disabled!");
    }

    /**
     * Checks whether player is in debug mode or not
     *
     * @param player
     * @return
     */
    public boolean isDebugging(final Player player) {
        if (debugees.containsKey(player)) {
            return debugees.get(player);
        } else {
            return false;
        }
    }

    /**
     * Sets debug status of player
     *
     * @param player
     * @param value
     */
    public void setDebugging(final Player player, final boolean value) {
        debugees.put(player, value);
    }

    /**
     *
     * @param time
     * @param pattern
     * @param period
     * @return next scheduled point in time, 0 if there is none
     * @throws ParseException
     */
    private long calcNextPointOfTime(String time, String pattern, long period) throws ParseException {
        if (period <= 0)
            return 0;

        DateFormat df = new SimpleDateFormat(pattern);
        df.setLenient(true);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(df.parse(time));
        calendar.set(Calendar.SECOND, 0);

        MessageHandler.log(Level.FINEST, "firstRun: " + calendar.toString());

        long nextRun = calendar.getTimeInMillis();

        while (nextRun < System.currentTimeMillis()) {
            MessageHandler.log(Level.FINEST, "Date is in the past, adding some  minutes: " + period / 1000 / 60);
            nextRun += period;
        }

        return nextRun;
    }

    /**
     * Scheduled Executor for plugin units
     */
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * Starts a new timer with given Runnable and times
     *
     * @param r
     *            the Runnable object
     * @param delay
     *            in milliseconds
     * @param period
     *            period
     * @param TimeUnit
     *            TimeUnit
     * @return
     */
    private boolean setupTimer(Runnable r, long delay, Integer period, TimeUnit tu) {
        scheduler.scheduleAtFixedRate(r, tu.convert(delay, TimeUnit.MILLISECONDS), period, tu);

        MessageHandler.info("Finished setting up a thread: " + r.getClass() + " Next run in: " + TimeUnit.MINUTES.convert(delay, TimeUnit.MILLISECONDS) + " minutes.");
        return true;
    }

    /**
     * update only happens after a manual unit run, reset force for scheduler
     * afterwards
     */
    @Override
    public void update(Observable arg0, Object arg1) {
        if (arg0 instanceof PluginUnit) {
            PluginUnit pu = (PluginUnit) arg0;
            pu.resetForce();
        }
    }

    /**
     * Backups current world
     *
     * @param force
     *            true disables cache usage
     *
     * @return true if successful
     */
    protected void performBackup(boolean force) {
        bu.setForce(force);
        scheduler.execute(bu);
    }

    /**
     * Creates map of current world
     *
     * @param force
     *            true disables cache usage
     *
     * @return true if successful
     */
    protected void performMapping(boolean force) {
        mu.setForce(force);
        scheduler.execute(mu);
    }

    // authorized users go in here
    List<String> authorizedUsers = new ArrayList<String>();

    /**
     * checks if an user is authorized to use ingame commands
     *
     * @param userName
     * @return
     */
    protected boolean isAuthorized(String userName) {
        return authorizedUsers.contains(userName.toLowerCase());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender
     * , org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        String commandName = cmd.getName().toLowerCase();
        Player player = (Player) sender;
        if ("backup".equals(commandName) && canUseCommand(player, "BackupPlugin.backup")) {
            if (args.length > 1) {
                return false;
            } else {
                boolean force = false;
                if (args.length == 1) {
                    force = Boolean.valueOf(args[0]);
                }
                String broadcast = player.getName() + " triggered world backup.";
                MessageHandler.info(broadcast + " force = " + force);
                MessageHandler.broadcast(broadcast);

                this.performBackup(force);
                return true;
            }
        } else if ("map".equals(commandName) && canUseCommand(player, "BackupPlugin.map")) {
            if (args.length > 1) {
                return false;
            } else {
                boolean force = false;
                if (args.length == 1) {
                    force = Boolean.valueOf(args[0]);
                }
                String broadcast = player.getName() + " triggered world mapping.";
                MessageHandler.info(broadcast + " force = " + force);
                MessageHandler.broadcast(broadcast);

                this.performMapping(force);
                return true;
            }
        } else if ("breload".equals(commandName) && canUseCommand(player, "BackupPlugin.admin")) {
            String broadcast = player.getName() + " triggered config reload.";
            MessageHandler.info(broadcast);
            MessageHandler.broadcast(broadcast);

            this.load();

            return true;
        } else if ("loglevel".equals(commandName) && canUseCommand(player, "BackupPlugin.admin")) {
            if (args.length == 1) {
                MessageHandler.info(player.getName() + " is changing log level to " + args[0]);
                if (MessageHandler.setLogLevel(args[0])) {
                    player.sendMessage("Done!");
                } else {
                    player.sendMessage("Failed!");
                }
            } else {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Checks if a given player can use a given command (Tries to user Permissions
     * Plugin first, own config only if there is no plugin)
     *
     * @param player
     * @param permission
     * @return
     */
    private boolean canUseCommand(Player player, String permission) {
        // check for groupUserPlugin
        if (permissions != null) {
            if (permissions.getHandler().permission(player, permission)) {
                return true;
            } else {
                return false;
            }
            // no groupUsersPlugin
        } else {
            return this.isAuthorized(player.getName());
        }
    }
}
