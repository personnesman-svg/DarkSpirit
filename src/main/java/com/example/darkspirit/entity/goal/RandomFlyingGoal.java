package com.example.darkspirit.entity.goal;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/**
 * Цель случайного перемещения Духа тьмы в воздухе.
 */
public class RandomFlyingGoal extends RandomStrollGoal {

    public RandomFlyingGoal(
            PathfinderMob mob,
            double speedModifier,
            int interval) {
        super(mob, speedModifier, interval);
    }

    /**
     * Выбирает случайную точку для полёта.
     *
     * horizontalDist — расстояние по горизонтали.
     * verticalDist — расстояние по вертикали.
     */
    @Override
    protected Vec3 getPosition() {

        // Выбираем случайную точку в трёх измерениях.
        return DefaultRandomPos.getPos(this.mob, 8, 5);
    }
}