package com.github.kuramastone.cobblemonChallenges.utils;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.data.JsonDataRegistry;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.animations.ActionEffectContext;
import com.cobblemon.mod.common.api.moves.animations.ActionEffects;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ByteUtils {

    public static byte[] convertListToByteArray(List<String> stringList) {
        List<Byte> byteList = new ArrayList<>();

        for(String string : stringList) {
            byte[] stringData = string.getBytes(StandardCharsets.UTF_8);
            byteList.add((byte) stringData.length);
            for (byte stringDatum : stringData) { byteList.add(stringDatum); }
        }

        byte[] array = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            array[i] = byteList.get(i);
        }

        return array;
    }

}
