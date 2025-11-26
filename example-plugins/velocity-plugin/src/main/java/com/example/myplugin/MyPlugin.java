package com.example.myplugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import games.negative.moss.velocity.MossVelocity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Plugin(
        id = "myplugin",
        name = "MyPlugin",
        version = "1.0.0",
        description = "Example Velocity plugin using MossVelocity.",
        authors = {"ericlmao"}
)
public class MyPlugin extends MossVelocity {

    private static final Logger log = LoggerFactory.getLogger(MyPlugin.class);

    @Inject
    public MyPlugin(ProxyServer server) {
        super(server);
    }

    // override this method if you need to perform actions during proxy initialization/enable phase
    // this acts as your plugin's "onEnable" method
    // but do NOT remove the super call!
    @Override
    public void onProxyInitialization(ProxyInitializeEvent event) {
        super.onProxyInitialization(event);

        log.info("Proxy Initialization Started");
    }

    // override this method if you need to perform actions during proxy shutdown/disable phase
    // this acts as your plugin's "onDisable" method
    // but do NOT remove the super call!
    @Override
    public void onProxyShutdown(ProxyShutdownEvent event) {
        super.onProxyShutdown(event);

        log.info("Plugin shutting down");
    }
}
