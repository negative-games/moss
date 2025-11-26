package com.example.myplugin.listener;

import com.example.myplugin.MyPlugin;
import games.negative.moss.spring.SpringComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@SpringComponent
public class MyListener implements Listener {

    private final MyPlugin plugin;

    public MyListener(MyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerLogin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.sendRichMessage("<green>Hello!!");

        plugin.getLogger().info("Player " + player.getName() + " has joined the server.");
    }

}
