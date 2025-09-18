package com.github.kuramastone.cobblemonChallenges.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

// Call this when the server is starting, not after
public interface RegisterMessagesEvent {

    Event<RegisterMessagesEvent> EVENT = EventFactory.createArrayBacked(RegisterMessagesEvent.class, (callbacks) -> () -> {
        for (RegisterMessagesEvent event : callbacks) {
            event.onRegistration();
        }

    });

    void onRegistration();

}
