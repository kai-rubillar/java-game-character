/** A ranged fighter. Unique trait: attacks have a chance to be CRITICAL hits. */
class Archer extends GameCharacter {
    public Archer(String name, int level) {
        super(name, level);
    }

    @Override
    protected int getBaseHp() {
        return 50;
    }

    @Override
    protected int getBasePower() {
        return 10;
    }

    @Override
    public String getType() {
        return "Archer";
    }

    @Override
    public String getAbility() {
        return "Crit " + Math.min(60, 10 + getLevel()) + "%";
    }

    @Override
    public ActionResult attack() {
        gainEnergy(10);
        int critChance = Math.min(60, 10 + getLevel());
        int dmg = roll(getPower());
        if (rng.nextInt(100) < critChance) {
            return ActionResult.hit(getName() + " fires an arrow... CRITICAL HIT for " + (dmg * 2) + " damage!",
                    dmg * 2, "CRIT!");
        }
        return ActionResult.hit(getName() + " fires an arrow for " + dmg + " damage.", dmg, "ATTACK");
    }

    @Override
    public ActionResult specialMove() {
        if (!spendEnergy(SPECIAL_COST)) {
            return notEnoughEnergy();
        }
        int dmg = roll(getPower() * 4);
        return ActionResult.hit(getName() + " uses Arrow Rain! 5 arrows deal " + dmg + " total damage!",
                dmg, "SPECIAL!");
    }
}
