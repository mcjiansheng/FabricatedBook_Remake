package com.fabricatedbook.core.action;

import com.fabricatedbook.core.buff.BuffHook;
import com.fabricatedbook.core.buff.Withering;
import com.fabricatedbook.core.entity.AbstractEntity;

import java.util.List;

/**
 * Triggers existing Withering buffs when this action reaches the queue.
 */
public class TriggerWitheringAction implements CombatAction {

    private final List<AbstractEntity> targets;
    private final int times;
    private boolean finished;

    public TriggerWitheringAction(AbstractEntity target, int times) {
        this(List.of(target), times);
    }

    public TriggerWitheringAction(List<AbstractEntity> targets, int times) {
        this.targets = targets;
        this.times = Math.max(1, times);
        this.finished = false;
    }

    @Override
    public void execute() {
        for (AbstractEntity target : targets) {
            if (target == null || !target.isAlive()) continue;
            for (int i = 0; i < times; i++) {
                Withering withering = findWithering(target);
                if (withering == null || withering.getStack() <= 0) {
                    break;
                }
                withering.detonate(target);
            }
        }
        finished = true;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public String getDescription() {
        return "引爆凋零";
    }

    private Withering findWithering(AbstractEntity target) {
        for (BuffHook buff : target.getBuffs()) {
            if (buff instanceof Withering withering) {
                return withering;
            }
        }
        return null;
    }
}
