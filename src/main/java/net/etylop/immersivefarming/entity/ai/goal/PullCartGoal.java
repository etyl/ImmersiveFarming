package net.etylop.immersivefarming.entity.ai.goal;


import net.etylop.immersivefarming.world.IFWorld;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class PullCartGoal extends Goal {
    private final Entity mob;

    public PullCartGoal(final Entity entity) {
        this.mob = entity;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return IFWorld.get(this.mob.level()).map(w -> w.isPulling(this.mob)).orElse(false);
    }
}
