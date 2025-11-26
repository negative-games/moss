package com.example.myplugin;

import games.negative.moss.paper.MossPaper;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyPlugin extends MossPaper {

    private static final Logger log = LoggerFactory.getLogger(MyPlugin.class);

    @Override
    public void onEnable() {
        super.onEnable(); // ALWAYS KEEP THIS

        // Custom bean registration if needed, such as Commands, Listeners, etc.
        PluginManager listeners = getServer().getPluginManager();
        invokeBeans(Listener.class, listener -> listeners.registerEvents(listener, this), (listener, e) -> {
            // Throw an error using SLF4J logger on failure
            log.error("Failed to register listener: {}", listener.getClass().getSimpleName(), e);
        });
    }
}
