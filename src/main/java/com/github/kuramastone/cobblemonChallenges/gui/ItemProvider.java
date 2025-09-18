package com.github.kuramastone.cobblemonChallenges.gui;

import com.github.kuramastone.bUtilities.configs.ItemConfig;
import com.github.kuramastone.cobblemonChallenges.utils.FabricAdapter;
import net.kyori.adventure.text.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface ItemProvider {

    ItemStack build();

    ItemProvider copy();

    class ItemWrapper implements ItemProvider {
        private ItemStack sample;

        public ItemWrapper(ItemStack sample) {
            this.sample = sample;
        }

        @Override
        public ItemStack build() {
            return sample.copy();
        }

        @Override
        public ItemProvider copy() {
            return new ItemWrapper(sample.copy());
        }
    }

    class ItemConfigProvider implements ItemProvider {

        private ItemConfig config;

        public ItemConfigProvider(ItemConfig config) {
            this.config = config;
        }

        @Override
        public ItemStack build() {
            return null;
        }

        @Override
        public ItemProvider copy() {
            return new ItemConfigProvider(config.copy());
        }
    }

}
