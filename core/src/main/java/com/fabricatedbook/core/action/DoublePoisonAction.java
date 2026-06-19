package com.fabricatedbook.core.action;

import com.fabricatedbook.core.buff.BuffHook;
import com.fabricatedbook.core.entity.AbstractEntity;

import java.util.List;

/**
 * Doubles poison stacks at execution time so earlier queued poison is included.
 */
public class DoublePoisonAction implements CombatAction {

    private final List<AbstractEntity> targets;
    private final int multiplier;
    private boolean finished;

    public DoublePoisonAction(List<AbstractEntity> targets) {
        this(targets, 2);
    }

    public DoublePoisonAction(List<AbstractEntity> targets, int multiplier) {
        this.targets = targets;
        this.multiplier = Math.max(2, multiplier);
        this.finished = false;
    }

    @Override
    public void execute() {
        for (AbstractEntity target : targets) {
            if (target == null || !target.isAlive()) continue;
            int currentPoison = poisonStack(target);
            if (currentPoison <= 0) continue;
            target.removeBuff("Poison");
            new ApplyBuffAction(target, "Poison", currentPoison * multiplier).execute();
        }
        finished = true;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public String getDescription() {
        return multiplier == 2 ? "中毒层数翻倍" : "中毒层数翻两倍";
    }

    private int poisonStack(AbstractEntity target) {
        for (BuffHook buff : target.getBuffs()) {
            if ("Poison".equals(buff.getBuffName())) {
                return buff.getStack();
            }
        }
        return 0;
    }
}
