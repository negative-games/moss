package com.example.myplugin;

import games.negative.moss.bungee.MossBungee;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.PluginManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MyPlugin extends MossBungee {

    @Override
    public void loadInitialComponents(AnnotationConfigApplicationContext context) {
        // Registering a custom Bean in the initialization phase
        // so it can be used by other classes!
        context.registerBean(ProxyServer.class, ProxyServer::getInstance);
    }

    @Override
    public void onEnable() {
        super.onEnable(); // ALWAYS KEEP THIS

        // Custom bean registration if needed, such as Commands, Listeners, etc.
        PluginManager listeners = ProxyServer.getInstance().getPluginManager();
        invokeBeans(Listener.class, listener -> listeners.registerListener(this, listener), (listener, e) -> {
            // Throw an error if a listener fails to register
            getLogger().severe("Failed to register listener: " + listener.getClass().getSimpleName());
        });
    }
}
