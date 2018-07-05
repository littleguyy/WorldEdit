/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.extent.transform;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.LazyBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.registry.state.value.DirectionalStateValue;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Transforms blocks themselves (but not their position) according to a
 * given transform.
 */
public class BlockTransformExtent extends AbstractDelegateExtent {

    private static final double RIGHT_ANGLE = Math.toRadians(90);

    private final Transform transform;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public BlockTransformExtent(Extent extent, Transform transform) {
        super(extent);
        checkNotNull(transform);
        this.transform = transform;
    }

    /**
     * Get the transform.
     *
     * @return the transform
     */
    public Transform getTransform() {
        return transform;
    }

    /**
     * Transform a block without making a copy.
     *
     * @param block the block
     * @param reverse true to transform in the opposite direction
     * @return the same block
     */
    private <T extends BlockStateHolder> T transformBlock(T block, boolean reverse) {
        transform(block, reverse ? transform.inverse() : transform);
        return block;
    }

    @Override
    public BlockState getBlock(Vector position) {
        return transformBlock(super.getBlock(position), false);
    }

    @Override
    public LazyBlock getLazyBlock(Vector position) {
        return transformBlock(super.getLazyBlock(position), false);
    }

    @Override
    public BaseBlock getFullBlock(Vector position) {
        return transformBlock(super.getFullBlock(position), false);
    }

    @Override
    public boolean setBlock(Vector location, BlockStateHolder block) throws WorldEditException {
        return super.setBlock(location, transformBlock(block, true));
    }


    /**
     * Transform the given block using the given transform.
     *
     * <p>The provided block is modified.</p>
     *
     * @param block the block
     * @param transform the transform
     * @return the same block
     */
    public static <T extends BlockStateHolder> T transform(T block, Transform transform) {
        return transform(block, transform, block);
    }

    /**
     * Transform the given block using the given transform.
     *
     * @param block the block
     * @param transform the transform
     * @param changedBlock the block to change
     * @return the changed block
     */
    private static <T extends BlockStateHolder> T transform(T block, Transform transform, T changedBlock) {
        checkNotNull(block);
        checkNotNull(transform);

        Map<String, ? extends Property> states = WorldEdit.getInstance().getPlatformManager()
                .queryCapability(Capability.GAME_HOOKS).getRegistries().getBlockRegistry().getStates(block);

        if (states == null) {
            return changedBlock;
        }

        for (Property property : states.values()) {
            if (property instanceof DirectionalProperty) {
                DirectionalStateValue value = (DirectionalStateValue) block.getState(property);
                if (value != null) {
                    DirectionalStateValue newValue = getNewStateValue((DirectionalProperty) property, transform, value.getDirection());
                    if (newValue != null) {
                        changedBlock.with(property, newValue);
                    }
                }
            }
        }

        return changedBlock;
    }

    /**
     * Get the new value with the transformed direction.
     *
     * @param state the state
     * @param transform the transform
     * @param oldDirection the old direction to transform
     * @return a new state or null if none could be found
     */
    @Nullable
    private static DirectionalStateValue getNewStateValue(DirectionalProperty state, Transform transform, Vector oldDirection) {
        Vector newDirection = transform.apply(oldDirection).subtract(transform.apply(Vector.ZERO)).normalize();
        DirectionalStateValue newValue = null;
        double closest = -2;
        boolean found = false;

        for (DirectionalStateValue v : state.getValues()) {
            if (v.getDirection() != null) {
                double dot = v.getDirection().normalize().dot(newDirection);
                if (dot >= closest) {
                    closest = dot;
                    newValue = v;
                    found = true;
                }
            }
        }

        if (found) {
            return newValue;
        } else {
            return null;
        }
    }

}
