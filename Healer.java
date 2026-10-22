/** A support character. Unique trait: its special move HEALS instead of damaging. */
class Healer extends GameCharacter {
    public Healer(String name, int level) {
        super(name, level);
    }

    @Override
    protected int getBaseHp() {
        return 60;
    }

    @Override
    protected int getBasePower() {
        return 6;
    }

    @Override
    public String getType() {
        return "Healer";
    }

    @Override
    public String getAbility() {
        return "Heal " + (20 + getLevel() * 3);
    }

    @Override
    public ActionResult attack() {
        gainEnergy(15);
        int dmg = roll(getPower());
        return ActionResult.hit(getName() + " taps the enemy with a staff for " + dmg + " damage.", dmg, "ATTACK");
    }

    @Override
    public ActionResult specialMove() {
        // Healing Light is free: the Healer is the one class that never runs out of it.
        int amount = 20 + getLevel() * 3;
        gainEnergy(20);
        return ActionResult.healing(getName() + " uses Healing Light! The party recovers up to "
                + amount + " HP each.", amount, true, "HEAL");
    }
}
