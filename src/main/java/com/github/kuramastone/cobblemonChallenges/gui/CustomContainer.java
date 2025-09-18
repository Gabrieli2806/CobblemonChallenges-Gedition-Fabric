package com.github.kuramastone.cobblemonChallenges.gui;

import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;

public class CustomContainer extends SimpleContainer {

    private ClickRunnable clickRunnable;

    public CustomContainer(int rows) {
        super(rows * 9);
    }

    public void handleClick(int pSlotId, int pDragType, ClickType pClickType, Player pPlayer) {

        try {

            // handle runnable
            if (clickRunnable == null) {
                // CobbleChallengeMod.logger.info("slot=%s, clicktype=%s, dragtype=%s".formatted(pSlotId, pClickType, pDragType));
            }
            else {
                clickRunnable.run(pClickType, pDragType, pSlotId, pPlayer);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public interface ClickRunnable {
        void run(ClickType type, int dragType, int pSlotID, Player player);
    }

    public ClickRunnable getClickRunnable() {
        return clickRunnable;
    }

    public CustomContainer setClickRunnable(ClickRunnable clickRunnable) {
        this.clickRunnable = clickRunnable;
        return this;
    }


}