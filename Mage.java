/** A spell caster. Unique trait: MANA, and a special move that strikes 3 times. */
class Mage extends GameCharacter {
    public Mage(String name, int level) {
        super(name, level);
    }

    @Override
    protected int getBaseHp() {
        return 40;
    }

    @Override
    protected int getBasePower() {
        return 15;
    }

    @Override
    public String getType() {
        return "Mage";
    }

    @Override
    public String getAbility() {
        return "Mana " + (50 + getLevel() * 5);
    }

    @Override
    public ActionResult attack() {
        gainEnergy(10);
        int dmg = roll(getPower());
        return ActionResult.hit(getName() + " casts a fireball for " + dmg + " damage!", dmg, "ATTACK");
    }

    @Override
    public ActionResult specialMove() {
        if (!spendEnergy(SPECIAL_COST)) {
            return notEnoughEnergy();
        }
        int dmg = roll(getPower()) + roll(getPower()) + roll(getPower());   // 3 separate bolts
        return ActionResult.hit(getName() + " uses Lightning Storm! 3 bolts hit for "
                + dmg + " total damage!", dmg, "SPECIAL!");
    }
}
