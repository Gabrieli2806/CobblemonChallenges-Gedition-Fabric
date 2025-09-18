package com.github.kuramastone.cobblemonChallenges.utils;

import com.github.kuramastone.bUtilities.ComponentEditor;
import com.github.kuramastone.bUtilities.configs.ItemConfig;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import net.kyori.adventure.text.Component;
import net.minecraft.world.item.ItemStack;

public class FabricAdapter {

    public static ItemStack toItemStack(ItemConfig config) {
        return ItemUtils.createItemStack(config);
    }

    public static net.minecraft.network.chat.Component adapt(ComponentEditor componentEditor) {
        return adapt(componentEditor.build());
    }

    public static net.minecraft.network.chat.Component adapt(Component adventureComponent) {
        // Use Adventure Platform Fabric's native conversion via FabricServerAudiences
        return CobbleChallengeMod.instance.adventure().toNative(adventureComponent);
    }
}
