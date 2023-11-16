package com.molean.staticmap;

import org.bukkit.plugin.java.JavaPlugin;

public final class StaticMap extends JavaPlugin {

    @Override
    public void onEnable() {
        new StaticMapListener(this);
        this.saveDefaultConfig();
        this.reloadConfig();

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
