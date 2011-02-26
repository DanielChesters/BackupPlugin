package com.mysticx.bukkit.backupplugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

public final class Configuration {

    private Properties properties;
    private File file;

    public Configuration(File dataFolder, String name) {
        this.properties = new Properties();
        this.file = new File(dataFolder, name);

        if (file.exists()) {
            load();
        }
        else {
            save();
        }
    }

    public void load() {
        try {
            final FileInputStream inStream = new FileInputStream(this.file);
            try {
                this.properties.load(inStream);
            } catch (IOException ioex) {
                MessageHandler.log(Level.SEVERE, "Can't load config!", ioex);
            } finally {
                inStream.close();
            }
        } catch (IOException ioex) {
            MessageHandler.log(Level.SEVERE, "Can't load config!", ioex);
        }
    }

    public void save() {
        try {
            final FileOutputStream out = new FileOutputStream(this.file);
            try {
                this.properties.store(out, "BackupPlugin Config File");
            } catch (IOException ioex) {
                MessageHandler.log(Level.SEVERE, "Can't save config!", ioex);
            } finally {
                out.close();
            }
        } catch (IOException ioex) {
            MessageHandler.log(Level.SEVERE, "Can't save config!", ioex);
        }
    }

    public void removeKey(String key) {
        this.properties.remove(key);
        save();
    }

    public boolean keyExists(String key) {
        return this.properties.containsKey(key);
    }

    public String getString(String key) {
        if (this.properties.containsKey(key)) {
            return this.properties.getProperty(key);
        }

        return "";
    }

    public String getString(String key, String value) {
        if (this.properties.containsKey(key)) {
            return this.properties.getProperty(key);
        }
        setString(key, value);
        return value;
    }

    public void setString(String key, String value) {
        this.properties.setProperty(key, value);
        save();
    }

    public int getInt(String key) {
        if (this.properties.containsKey(key)) {
            return Integer.parseInt(this.properties.getProperty(key));
        }

        return 0;
    }

    public int getInt(String key, int value) {
        if (this.properties.containsKey(key)) {
            return Integer.parseInt(this.properties.getProperty(key));
        }

        setInt(key, value);
        return value;
    }

    public void setInt(String key, int value) {
        this.properties.setProperty(key, String.valueOf(value));
        save();
    }

    public double getDouble(String key) {
        if (this.properties.containsKey(key)) {
            return Double.parseDouble(this.properties.getProperty(key));
        }

        return 0.0D;
    }

    public double getDouble(String key, double value) {
        if (this.properties.containsKey(key)) {
            return Double.parseDouble(this.properties.getProperty(key));
        }

        setDouble(key, value);
        return value;
    }

    public void setDouble(String key, double value) {
        this.properties.setProperty(key, String.valueOf(value));
        save();
    }

    public long getLong(String key) {
        if (this.properties.containsKey(key)) {
            return Long.parseLong(this.properties.getProperty(key));
        }

        return 0L;
    }

    public long getLong(String key, long value) {
        if (this.properties.containsKey(key)) {
            return Long.parseLong(this.properties.getProperty(key));
        }

        setLong(key, value);
        return value;
    }

    public void setLong(String key, long value) {
        this.properties.setProperty(key, String.valueOf(value));
        save();
    }

    public boolean getBoolean(String key) {
        if (this.properties.containsKey(key)) {
            return Boolean.parseBoolean(this.properties.getProperty(key));
        }

        return false;
    }

    public boolean getBoolean(String key, boolean value) {
        if (this.properties.containsKey(key)) {
            return Boolean.parseBoolean(this.properties.getProperty(key));
        }

        setBoolean(key, value);
        return value;
    }

    public void setBoolean(String key, boolean value) {
        this.properties.setProperty(key, String.valueOf(value));
        save();
    }
}
