package com.fabricatedbook.core.action;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * Triggers withering on a target and heals the caster by the actual damage dealt.
 */
public class WitheringDrainAction implements CombatAction {

    private final AbstractEntity source;
    private final AbstractEntity target;
    private boolean finished;
    private int drained;

    public WitheringDrainAction(AbstractEntity source, AbstractEntity target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public void execute() {
        TriggerWitheringAction trigger = new TriggerWitheringAction(target, 1);
        trigger.execute();
        drained = trigger.getTotalDamage();
        if (source != null && source.isAlive() && drained > 0) {
            source.heal(drained);
        }
        finished = true;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public String getDescription() {
        return "引爆凋零并吸血";
    }

    public int getDrained() {
        return drained;
    }
}
