package com.github.sib_energy_craft.pipes.utils;

import com.github.sib_energy_craft.pipes.api.ItemConsumer;
import com.github.sib_energy_craft.pipes.api.ItemSupplier;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 0.0.1
 * @author sibmaks
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PipeUtils {
    private static final Direction[] DIRECTIONS = Direction.values();

    /**
     * Supply item from supplier to any of consumer to all directions except passed one
     *
     * @param world game world
     * @param pos block position
     * @param itemSupplier item supplier
     * @param consumedDirection last consumed direction. Item not supplying in this direction
     * @return true - item was consumed, false - otherwise
     */
    public static boolean supplyToAllExcept(@NotNull World world,
                                            @NotNull BlockPos pos,
                                            @NotNull ItemSupplier itemSupplier,
                                            @NotNull Direction consumedDirection) {
        for (var supplyingDirection : DIRECTIONS) {
            if(supplyingDirection == consumedDirection) {
                continue;
            }
            var itemConsumer = getItemConsumer(world, pos.offset(supplyingDirection));
            if(itemConsumer == null) {
                continue;
            }
            if(supply(world, pos, itemSupplier, supplyingDirection)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Supply item from supplier to consumer in specific direction
     *
     * @param world game world
     * @param pos block position
     * @param itemSupplier item supplier
     * @param supplyingDirection item supplying direction
     * @return true - item was consumed, false - otherwise
     * @since 0.0.4
     */
    public static boolean supply(@NotNull World world,
                                 @NotNull BlockPos pos,
                                 @NotNull ItemSupplier itemSupplier,
                                 @NotNull Direction supplyingDirection) {
        var itemConsumer = getItemConsumer(world, pos.offset(supplyingDirection));
        if(itemConsumer == null) {
            return false;
        }
        var stacksToConsume = itemSupplier.canSupply(supplyingDirection);
        for (var stackToConsume : stacksToConsume) {
            if (!itemConsumer.canConsume(stackToConsume, supplyingDirection)) {
                continue;
            }
            var stackToTransfer = new ItemStack(stackToConsume.getItem(), 1);
            if (!itemSupplier.supply(stackToTransfer, supplyingDirection)) {
                continue;
            }
            var notTransferred = itemConsumer.consume(stackToTransfer, supplyingDirection);
            if (notTransferred.isEmpty()) {
                return true;
            }
            itemSupplier.returnStack(stackToConsume, supplyingDirection);
        }
        return false;
    }

    /**
     * Enabled aggressive block sorting<br/>
     * Lifted jumps to return sites<br/>
     * Transfer item to inventory from specific side
     *
     * @param to distance inventory
     * @param stack transferring stack
     * @param side consuming side
     *
     * @return not transfered stack
     */
    @NotNull
    public static ItemStack transfer(@NotNull Inventory to,
                                     @NotNull ItemStack stack,
                                     @NotNull Direction side) {
        if (to instanceof SidedInventory sidedInventory) {
            int[] is = sidedInventory.getAvailableSlots(side);
            int i = 0;
            while (i < is.length) {
                if (stack.isEmpty()) return stack;
                stack = transfer(to, stack, is[i], side);
                ++i;
            }
            return stack;
        }
        int j = to.size();
        int i = 0;
        while (i < j) {
            if (stack.isEmpty()) {
                return stack;
            }
            stack = transfer(to, stack, i, side);
            ++i;
        }
        return stack;
    }

    private static boolean canInsert(@NotNull Inventory inventory,
                                     @NotNull ItemStack stack,
                                     int slot,
                                     @Nullable Direction side) {
        if (!inventory.isValid(slot, stack)) {
            return false;
        }
        return !(inventory instanceof SidedInventory sidedInventory) || sidedInventory.canInsert(slot, stack, side);
    }

    /**
     * Transfer block to inventory
     *
     * @param to distance inventory
     * @param stack stack to insert
     * @param slot slot to insert
     * @param side consumed side
     * @return not consumed items or {@link ItemStack#EMPTY}
     */
    @NotNull
    public static ItemStack transfer(@NotNull Inventory to,
                                      @NotNull ItemStack stack,
                                      int slot,
                                      @NotNull Direction side) {
        var inventoryStack = to.getStack(slot);
        if (canInsert(to, stack, slot, side)) {
            boolean transferred = false;
            if (inventoryStack.isEmpty()) {
                if(to instanceof ItemConsumer itemConsumer) {
                    itemConsumer.consume(stack, side);
                } else {
                    to.setStack(slot, stack);
                }
                stack = ItemStack.EMPTY;
                transferred = true;
            } else if (canMergeItems(inventoryStack, stack)) {
                int i = stack.getMaxCount() - inventoryStack.getCount();
                int j = Math.min(stack.getCount(), i);
                stack.decrement(j);
                inventoryStack.increment(j);
            }
            if (transferred) {
                to.markDirty();
            }
        }
        return stack;
    }

    @Nullable
    public static ItemConsumer getItemConsumer(@NotNull World world,
                                               @NotNull BlockPos pos) {
        return getItemConsumer(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    @Nullable
    private static ItemConsumer getItemConsumer(@NotNull World world,
                                            double x,
                                            double y,
                                            double z) {
        var blockPos = BlockPos.ofFloored(x, y, z);
        var blockState = world.getBlockState(blockPos);
        if (blockState.hasBlockEntity()) {
            var blockEntity = world.getBlockEntity(blockPos);
            if(blockEntity instanceof ItemConsumer itemConsumer) {
                return itemConsumer;
            }
        }
        return null;
    }

    /**
     * Get item supplier
     *
     * @param world game world
     * @param pos supplier position
     * @return supplier or null if no supplier
     */
    @Nullable
    public static ItemSupplier getItemSupplier(@NotNull World world,
                                               @NotNull BlockPos pos) {
        return getItemSupplier(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    @Nullable
    private static ItemSupplier getItemSupplier(@NotNull World world,
                                            double x,
                                            double y,
                                            double z) {
        var blockPos = BlockPos.ofFloored(x, y, z);
        var blockState = world.getBlockState(blockPos);
        if (blockState.hasBlockEntity()) {
            var blockEntity = world.getBlockEntity(blockPos);
            if(blockEntity instanceof ItemSupplier itemSupplier) {
                return itemSupplier;
            }
        }
        return null;
    }

    /**
     * Check can item stacks be merged or not
     *
     * @param first 1st item
     * @param second 2nd item
     * @return true - item stacks can be merged, false - otherwise
     */
    public static boolean canMergeItems(@NotNull ItemStack first,
                                         @NotNull ItemStack second) {
        if (!first.isOf(second.getItem())) {
            return false;
        }
        if (first.getDamage() != second.getDamage()) {
            return false;
        }
        if (first.getCount() >= first.getMaxCount()) {
            return false;
        }
        return ItemStack.areNbtEqual(first, second);
    }

    /**
     * Check is inventory has space for item stack
     *
     * @param inventory inventory to insert
     * @param itemStack stack to insert
     * @return true - has space, false - otherwise
     */
    public static boolean hasSpaceFor(@NotNull Inventory inventory, @NotNull ItemStack itemStack) {
        for(int i = 0; i < inventory.size(); i++) {
            if (!inventory.isValid(i, itemStack)) {
                continue;
            }
            var inventoryStack = inventory.getStack(i);
            if (inventoryStack.isEmpty() || inventoryStack.isItemEqual(itemStack) &&
                    inventoryStack.getCount() < inventoryStack.getMaxCount()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Consume item stack into inventory
     *
     * @param inventory inventory to insert
     * @param itemStack stack to insert
     * @return not consumed stack or {@link ItemStack#EMPTY}
     */
    public static @NotNull ItemStack consume(@NotNull Inventory inventory, @NotNull ItemStack itemStack) {
        ItemStack stackToInsert = null;
        Integer slotToInsert = null;
        for(int i = 0; i < inventory.size(); i++) {
            var inventoryStack = inventory.getStack(i);
            if(canMergeItems(inventoryStack, itemStack)) {
                stackToInsert = inventoryStack;
                break;
            }
            if(slotToInsert == null && inventoryStack.isEmpty()) {
                slotToInsert = i;
            }
        }
        if(stackToInsert == null && slotToInsert == null) {
            return itemStack;
        }
        if(stackToInsert != null) {
            int maxCount = stackToInsert.getMaxCount();
            int sumCount = stackToInsert.getCount() + itemStack.getCount();
            stackToInsert.setCount(Math.min(maxCount, sumCount));
            if (sumCount > maxCount) {
                var lost = new ItemStack(itemStack.getItem(), sumCount - maxCount);
                return consume(inventory, lost);
            }
        } else {
            inventory.setStack(slotToInsert, itemStack);
        }
        inventory.markDirty();
        return ItemStack.EMPTY;
    }

    /**
     * Merge 2 items into 1st one
     *
     * @param toInsert distance item stack
     * @param itemStack stack to merge
     * @return not merged stack or {@link ItemStack#EMPTY}
     */
    public static @NotNull ItemStack mergeItems(@NotNull ItemStack toInsert, @NotNull ItemStack itemStack) {
        int maxCount = toInsert.getMaxCount();
        int sumCount = toInsert.getCount() + itemStack.getCount();
        toInsert.setCount(Math.min(maxCount, sumCount));
        if (sumCount > maxCount) {
            return new ItemStack(itemStack.getItem(), sumCount - maxCount);
        }
        return ItemStack.EMPTY;
    }

    /**
     * Supply requested items from inventory
     *
     * @param inventory inventory to supply
     * @param requested requested item
     * @return true - items was supplied, false - otherwise
     */
    public static boolean supply(@NotNull Inventory inventory, @NotNull ItemStack requested) {
        var items = new HashMap<Item, Integer>();
        for(int i = 0; i < inventory.size(); i++) {
            var inventoryStack = inventory.getStack(i);
            var inventoryItem = inventoryStack.getItem();
            var count = items.computeIfAbsent(inventoryItem, it -> 0);
            items.put(inventoryItem, count + inventoryStack.getCount());
        }
        var contains = items.get(requested.getItem());
        int requestedCount = requested.getCount();
        if(contains == null || contains == 0 || contains < requestedCount) {
            return false;
        }
        for(int i = 0; i < inventory.size(); i++) {
            var inventoryStack = inventory.getStack(i);
            if (!inventoryStack.isItemEqual(requested)) {
                continue;
            }
            int toRemove = Math.min(inventoryStack.getCount(), requestedCount);
            inventoryStack.decrement(toRemove);
            requestedCount -= toRemove;
            if(requestedCount <= 0) {
                break;
            }
        }
        inventory.markDirty();
        return true;
    }

}
