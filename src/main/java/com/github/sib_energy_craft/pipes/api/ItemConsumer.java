package com.github.sib_energy_craft.pipes.api;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

/**
 * @since 0.0.1
 * @author sibmaks
 */
public interface ItemConsumer {

    /**
     * Can consumer consume all or part of item stack.
     *
     * @param itemStack stack to consume
     * @param direction consuming direction
     * @return true - can consume - false - otherwise
     */
    boolean canConsume(@NotNull ItemStack itemStack, @NotNull Direction direction);

    /**
     * Consume item stack from passed direction
     *
     * @param itemStack stack to consume
     * @param direction consuming direction
     * @return not consumed items
     */
    @NotNull ItemStack consume(@NotNull ItemStack itemStack, @NotNull Direction direction);
}
