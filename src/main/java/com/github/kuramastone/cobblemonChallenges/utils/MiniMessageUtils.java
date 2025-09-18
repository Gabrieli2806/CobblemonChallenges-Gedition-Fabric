package com.github.kuramastone.cobblemonChallenges.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;

/**
 * Utility class for parsing MiniMessage formatted strings and converting them to Adventure Components
 */
public class MiniMessageUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.builder()
                .resolver(StandardTags.color())
                .resolver(StandardTags.decorations())
                .resolver(StandardTags.gradient())
                .resolver(StandardTags.rainbow())
                .resolver(StandardTags.reset())
                .resolver(StandardTags.newline())
                .resolver(StandardTags.transition())
                .build())
            .build();

    /**
     * Parse a MiniMessage string into an Adventure Component
     * @param miniMessage The MiniMessage formatted string
     * @return Adventure Component
     */
    public static Component parse(String miniMessage) {
        if (miniMessage == null || miniMessage.isEmpty()) {
            return Component.empty();
        }

        try {
            return MINI_MESSAGE.deserialize(miniMessage);
        } catch (Exception e) {
            // Fallback to plain text if parsing fails
            return Component.text(miniMessage);
        }
    }

    /**
     * Parse a MiniMessage string with custom placeholders
     * @param miniMessage The MiniMessage formatted string
     * @param placeholders Key-value pairs for placeholder replacement
     * @return Adventure Component
     */
    public static Component parse(String miniMessage, Object... placeholders) {
        if (miniMessage == null || miniMessage.isEmpty()) {
            return Component.empty();
        }

        // Replace placeholders first
        String processed = miniMessage;
        if (placeholders.length > 0 && placeholders.length % 2 == 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                String placeholder = placeholders[i].toString();
                String value = placeholders[i + 1].toString();
                processed = processed.replace(placeholder, value);
            }
        }

        return parse(processed);
    }

    /**
     * Convert a MiniMessage string directly to a Minecraft Component
     * @param miniMessage The MiniMessage formatted string
     * @return Minecraft Component
     */
    public static net.minecraft.network.chat.Component toMinecraftComponent(String miniMessage) {
        Component adventureComponent = parse(miniMessage);
        return FabricAdapter.adapt(adventureComponent);
    }

    /**
     * Convert a MiniMessage string with placeholders directly to a Minecraft Component
     * @param miniMessage The MiniMessage formatted string
     * @param placeholders Key-value pairs for placeholder replacement
     * @return Minecraft Component
     */
    public static net.minecraft.network.chat.Component toMinecraftComponent(String miniMessage, Object... placeholders) {
        Component adventureComponent = parse(miniMessage, placeholders);
        return FabricAdapter.adapt(adventureComponent);
    }
}