/** A tough melee fighter. Unique trait: ARMOR that grows with level. */
class Warrior extends GameCharacter {
    public Warrior(String name, int level) {
        super(name, level);
    }

    @Override
    protected int getBaseHp() {
        return 80;
    }

    @Override
    protected int getBasePower() {
        return 12;
    }

    @Override
    public String getType() {
        return "Warrior";
    }

    /** Armor really protects: it cuts every hit taken. */
    @Override
    public int getDefense() {
        return (20 + getLevel()) / 4;
    }

    @Override
    public String getAbility() {
        return "Armor " + (20 + getLevel());
    }

    @Override
    public ActionResult attack() {
        gainEnergy(10);
        int dmg = roll(getPower());
        return ActionResult.hit(getName() + " swings a mighty sword for " + dmg + " damage!", dmg, "ATTACK");
    }

    @Override
    public ActionResult specialMove() {
        if (!spendEnergy(SPECIAL_COST)) {
            return notEnoughEnergy();
        }
        int dmg = roll(getPower() * 2);
        return ActionResult.stunningHit(getName() + " uses Shield Bash! " + dmg
                + " damage and the enemy is stunned!", dmg, "SPECIAL!");
    }
}
