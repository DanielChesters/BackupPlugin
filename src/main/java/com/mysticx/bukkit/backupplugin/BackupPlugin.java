package com.mysticx.bukkit.backupplugin;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final Map<Player, Boolean> debugees = new HashMap<Player, Boolean>();

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
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        com.mysticx.bukkit.backupplugin.Configuration config = new com.mysticx.bukkit.backupplugin.Configuration(getDataFolder(), "config.properties");
        config.load();

        String separator = System.getProperty("file.separator");

        // some important values
        String world = config.getString("level-name", "world");
        String backupFolder = config.getString("backup-path", "world-backups");
        String mapperPath = config.getString("mapper-executable", "mcmap" + separator + "mcmap.exe");
        String mapFolder = config.getString("map-path", "world-maps");
        String mapOptions = config.getString("map-options", "-png -file $o $w;-night -png -file $o $w");
        int autobackupPeriod = config.getInt("autobackup-period", 0);
        int automapPeriod = config.getInt("automap-period", 0);
        int cacheLifetime = config.getInt("cache-lifetime", 30);
        String tempdir = config.getString("temp-path", backupFolder + "/temp");
        String loglevel = config.getString("log-level", "INFO");
        String timeUnit = config.getString("time-unit", "MINUTES");
        int numBackups = config.getInt("backup-history", 5);
        boolean useLatest = config.getBoolean("use-latest", false);
        String firstRun = config.getString("first-run", "1200");
        String admins = config.getString("authorized-users", "");

        MessageHandler.setLogLevel(loglevel);
        MessageHandler.setDebug(config.getBoolean("debug", false));

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
            TimeUnit tu = TimeUnit.valueOf(timeUnit);
            this.timeunit = tu;
        } catch (Exception e) {
            MessageHandler.warning("Failed to parse time-unit, using default.");
        }

        // init cache
        this.cc = CacheControl.getInstance();
        this.cc.setWorld(world);
        this.cc.setTimeUnit(timeunit);
        this.cc.setCacheLifetime(cacheLifetime);
        this.cc.setTempDir(new File(tempdir));
        this.cc.setCacheHistory(numBackups);

        // init BackupUnit
        this.bu = new BackupUnit(this.getServer(), new File(backupFolder), true);
        this.bu.addObserver(this);

        // init MapperUnit
        this.mu = new MapperUnit(this.getServer(), new File(mapFolder), false);
        this.mu.setMapperPath(new File(mapperPath));
        this.mu.setUseLatest(useLatest);
        this.mu.addObserver(this);

        String[] parameters = mapOptions.split(";");
        this.mu.setMapOptions(parameters);

        // init scheduler
        scheduler.shutdownNow();
        scheduler = Executors.newScheduledThreadPool(2);

        // schedule timer
        long backupDelay = -1;
        long mapDelay = -1;

        try {
            long timeToExecuteB = calcNextPointOfTime(firstRun, "HHmm", TimeUnit.MILLISECONDS.convert(autobackupPeriod, timeunit));
            backupDelay = timeToExecuteB - System.currentTimeMillis();

            long timeToExecuteM = calcNextPointOfTime(firstRun, "HHmm", TimeUnit.MILLISECONDS.convert(automapPeriod, timeunit));
            mapDelay = timeToExecuteM - System.currentTimeMillis();
        } catch (ParseException pe) {
            MessageHandler.log(Level.WARNING, "Failed to parse firstRun, disabled automatic execution", pe);
        }

        if (backupDelay >= 0 && autobackupPeriod > 0) {
            setupTimer(bu, backupDelay, autobackupPeriod, this.timeunit);
        }

        if (mapDelay >= 0 && automapPeriod > 0) {
            setupTimer(mu, mapDelay, automapPeriod, this.timeunit);
        }

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bukkit.plugin.Plugin#onDisable()
     */
    public void onDisable() {
        MessageHandler.log(Level.INFO, "BackupPlugin disabled!");
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
        if (period <= 0) {
            return 0;
        }

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

        MessageHandler.info("Finished setting up a thread: " + r.getClass().getSimpleName() + " Next run in: " + TimeUnit.MINUTES.convert(delay, TimeUnit.MILLISECONDS) + " minutes.");
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
    private List<String> authorizedUsers = new ArrayList<String>();

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
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if ("backup".equals(commandName) && canUseCommand(player, "BackupPlugin.backup")) {
                return backup(args, player.getName());
            } else if ("map".equals(commandName) && canUseCommand(player, "BackupPlugin.map")) {
                return map(args, player.getName());
            } else if ("breload".equals(commandName) && canUseCommand(player, "BackupPlugin.admin")) {
                return reload(player.getName());
            } else if ("loglevel".equals(commandName) && canUseCommand(player, "BackupPlugin.admin")) {
                return loglevel(args, player.getName(), player);
            }
        }
        return true;
    }

    private boolean loglevel(String[] args, String name, Player player) {
        if (args.length == 1) {
            MessageHandler.info(name + " is changing log level to " + args[0]);
            String message;
            if (MessageHandler.setLogLevel(args[0])) {
                message = "Done!";
            } else {
                message = "Failed!";
            }
            if (player == null) {
                MessageHandler.info(message);
            } else {
                player.sendMessage(message);
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean reload(String name) {
        String broadcast = name + " triggered config reload.";
        MessageHandler.info(broadcast);
        MessageHandler.broadcast(broadcast);

        this.load();

        return true;
    }

    private boolean map(String[] args, String name) {
        if (args.length > 1) {
            return false;
        } else {
            boolean force = false;
            if (args.length == 1) {
                force = Boolean.valueOf(args[0]);
            }
            String broadcast = name + " triggered world mapping.";
            MessageHandler.info(broadcast + " force = " + force);
            MessageHandler.broadcast(broadcast);

            this.performMapping(force);
            return true;
        }
    }

    private boolean backup(String[] args, String name) {
        if (args.length > 1) {
            return false;
        } else {
            boolean force = false;
            if (args.length == 1) {
                force = Boolean.valueOf(args[0]);
            }
            String broadcast = name + " triggered world backup.";
            MessageHandler.info(broadcast + " force = " + force);
            MessageHandler.broadcast(broadcast);

            this.performBackup(force);
            return true;
        }
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
        // check for Permissions
        if (permissions != null) {
            return permissions.getHandler().permission(player, permission);
        // no Permissions
        } else {
            return this.isAuthorized(player.getName());
        }
    }
}
