import java.util.Random;

/**
 * ABSTRACTION: GameCharacter is an abstract class. It defines what EVERY
 * character has (name, level, energy) and what EVERY character must be able to
 * do (attack, specialMove, ...). It does NOT say how - each subclass decides.
 */
abstract class GameCharacter {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 99;
    public static final int MAX_ENERGY = 100;
    public static final int SPECIAL_COST = 30;

    private String name;
    private int level;
    private int energy = MAX_ENERGY;
    private int hp;
    protected final Random rng = new Random();

    public GameCharacter(String name, int level) {
        this.name = name;
        this.level = level;
        this.hp = getMaxHp();
    }

    // ---------- Abstract methods: every subclass MUST implement these ----------
    public abstract ActionResult attack();
    public abstract ActionResult specialMove();
    public abstract String getType();          // e.g. "Warrior"
    public abstract String getAbility();       // a short stat shown in the GUI
    protected abstract int getBasePower();     // how strong this class is
    protected abstract int getBaseHp();        // how tough this class is

    // ---------- Concrete methods: shared by all characters ----------
    public int getPower() {
        return getBasePower() + level * 2;
    }

    // ---------- HP (hit points) ----------
    public int getMaxHp() {
        return getBaseHp() + level * 10;
    }

    /** Damage removed from every hit taken. Warriors override this with their armor. */
    public int getDefense() {
        return level / 5;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    /** Takes a hit (reduced by defense, minimum 1). Returns the damage actually taken. */
    public int takeDamage(int raw) {
        int dmg = Math.max(1, raw - getDefense());
        hp = Math.max(0, hp - dmg);
        return dmg;
    }

    /** Restores HP (can't exceed max, can't revive a defeated hero). Returns HP gained. */
    public int heal(int amount) {
        if (!isAlive()) {
            return 0;
        }
        int before = hp;
        hp = Math.min(getMaxHp(), hp + amount);
        return hp - before;
    }

    /** Randomizes a damage number by +/-10% so fights aren't identical every time. */
    protected int roll(int base) {
        return (int) Math.round(base * (0.9 + rng.nextDouble() * 0.2));
    }

    public String showName() {
        return "Character: " + name;
    }

    public String rest() {
        energy = MAX_ENERGY;
        boolean revived = !isAlive();
        hp = Math.min(getMaxHp(), hp + Math.max(1, getMaxHp() / 4));
        return revived
                ? name + " gets back up with " + hp + " HP and full energy."
                : name + " rests and recovers all energy and some HP.";
    }

    protected boolean spendEnergy(int cost) {
        if (energy < cost) {
            return false;
        }
        energy -= cost;
        return true;
    }

    protected void gainEnergy(int amount) {
        energy = Math.min(MAX_ENERGY, energy + amount);
    }

    protected ActionResult notEnoughEnergy() {
        return ActionResult.failed(name + " is too tired for a special move! (needs " + SPECIAL_COST + " energy)");
    }

    // ---------- Getters / setters ----------
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getLevel() { return level; }
    public void setLevel(int newLevel) {
        int oldMax = getMaxHp();
        this.level = newLevel;
        int gain = getMaxHp() - oldMax;
        // leveling up adds the extra HP; leveling down trims HP to the new maximum
        hp = isAlive() ? Math.max(1, Math.min(getMaxHp(), hp + Math.max(0, gain))) : 0;
    }
    public int getEnergy() { return energy; }
    public int getHp() { return hp; }

    @Override
    public String toString() {
        return name + " (" + getType() + ", Lv " + level + ")";
    }
}
