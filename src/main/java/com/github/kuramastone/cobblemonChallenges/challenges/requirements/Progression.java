package com.github.kuramastone.cobblemonChallenges.challenges.requirements;

import com.github.kuramastone.bUtilities.yaml.YamlConfig;

import java.util.UUID;

public interface Progression<T> {
    boolean isCompleted();

    void progress(Object obj);

    boolean matchesMethod(Object obj);

    double getPercentageComplete();

    Progression loadFrom(UUID uuid, YamlConfig configurationSection);

    void writeTo(YamlConfig configurationSection);

    String getProgressString();

    boolean meetsCriteria(T obj);

    Class<T> getType();

}
