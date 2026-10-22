/**
 * A stealthy striker. Unique trait: STEALTH.
 *  - The first attack from stealth is a BACKSTAB worth triple damage.
 *  - After attacking the Assassin is visible, and hits for normal damage.
 *  - Shadow Step (special) and resting bring the stealth back.
 */
class Assassin extends GameCharacter {
    private boolean stealthed = true;

    public Assassin(String name, int level) {
        super(name, level);
    }

    @Override
    protected int getBaseHp() {
        return 55;
    }

    @Override
    protected int getBasePower() {
        return 14;
    }

    @Override
    public String getType() {
        return "Assassin";
    }

    @Override
    public String getAbility() {
        return stealthed ? "Stealth ready" : "Visible";
    }

    @Override
    public ActionResult attack() {
        gainEnergy(10);
        if (stealthed) {
            stealthed = false;
            int dmg = roll(getPower() * 3);
            return ActionResult.hit(getName() + " strikes from the shadows! BACKSTAB for " + dmg + " damage!",
                    dmg, "CRIT!");
        }
        int dmg = roll(getPower());
        return ActionResult.hit(getName() + " slashes with twin daggers for " + dmg + " damage.", dmg, "ATTACK");
    }

    @Override
    public ActionResult specialMove() {
        if (!spendEnergy(SPECIAL_COST)) {
            return notEnoughEnergy();
        }
        stealthed = true;
        int dmg = roll(getPower() * 2);
        return ActionResult.hit(getName() + " uses Shadow Step! Vanishes, strikes for " + dmg
                + " damage and slips back into stealth.", dmg, "SPECIAL!");
    }

    /** Overrides the shared rest(): the Assassin also hides again. */
    @Override
    public String rest() {
        stealthed = true;
        return super.rest() + " " + getName() + " melts back into the shadows.";
    }
}
