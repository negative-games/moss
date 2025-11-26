package com.example.myplugin.listener;

import com.example.myplugin.MyPlugin;
import games.negative.moss.spring.SpringComponent;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

@SpringComponent
public class MyListener implements Listener {

    private final ProxyServer server;

    public MyListener(ProxyServer server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        player.sendMessage("Welcome to the server, " + player.getName() + "!");

        server.getLogger().info("Player " + player.getName() + " has logged in.");
    }

}
