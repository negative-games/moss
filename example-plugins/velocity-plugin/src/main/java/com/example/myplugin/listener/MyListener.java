package com.example.myplugin.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import games.negative.moss.spring.SpringComponent;
import games.negative.moss.velocity.spring.Listener;

@SpringComponent
public class MyListener implements Listener {

    // Moss-Velocity comes with a custom "Listener" interface which automatically
    // registers the class as an event listener.

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();
        player.sendRichMessage("<green>Welcome to the server, " + player.getUsername() + "!</green>");
    }

}
