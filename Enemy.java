import java.util.Random;

/**
 * A monster to fight. Its moves are RANDOM: every turn it rolls the dice and may
 * strike, smash, guard, heal itself or even stumble. Stronger types are rarer.
 */
class Enemy {
    private static final Random RNG = new Random();

    // name, hp multiplier, power multiplier
    private static final Object[][] KINDS = {
        {"Slime", 0.8, 0.7}, {"Goblin", 1.0, 1.0}, {"Skeleton", 1.0, 1.1},
        {"Orc", 1.4, 1.2}, {"Dragon", 2.0, 1.5}
    };

    private final String name;
    private final int level;
    private final int maxHp;
    private final int power;
    private int hp;
    private boolean guarding;
    private boolean stunned;

    Enemy(String name, int level, double hpMult, double powMult) {
        this.name = name;
        this.level = level;
        this.maxHp = (int) Math.round((50 + level * 17) * hpMult);
        this.power = (int) Math.round((8 + level * 2.3) * powMult);
        this.hp = maxHp;
    }

    /** Makes a random enemy whose level is close to the hero's. */
    static Enemy random(int heroLevel) {
        int level = Math.max(1, Math.min(GameCharacter.MAX_LEVEL, heroLevel + RNG.nextInt(3) - 1));
        int roll = RNG.nextInt(100);
        int kind = roll < 25 ? 0 : roll < 55 ? 1 : roll < 80 ? 2 : roll < 95 ? 3 : 4;
        Object[] k = KINDS[kind];
        return new Enemy((String) k[0], level, (Double) k[1], (Double) k[2]);
    }

    /** Takes damage (halved while guarding). Returns the damage actually taken. */
    int takeDamage(int raw) {
        int dmg = Math.max(1, guarding ? raw / 2 : raw);
        hp = Math.max(0, hp - dmg);
        return dmg;
    }

    void stun() { stunned = true; }

    /**
     * The enemy's turn: a RANDOM move. The damage in the result is raw; the Battle
     * applies the hero's defense and appends the final number to the message.
     */
    ActionResult act(String heroName) {
        if (stunned) {
            stunned = false;
            guarding = false;
            return ActionResult.nothing(name + " is stunned and loses its turn!", "STUNNED");
        }
        guarding = false;
        int roll = RNG.nextInt(100);
        if (roll < 45) {                                   // Strike
            if (RNG.nextInt(100) < 10) {
                return ActionResult.nothing(name + " swings at " + heroName + " but misses!", "MISS");
            }
            return ActionResult.hit(name + " strikes " + heroName, vary(power), "STRIKE");
        } else if (roll < 65) {                            // Heavy smash (can whiff)
            if (RNG.nextInt(100) < 30) {
                return ActionResult.nothing(name + " winds up a HEAVY SMASH... and whiffs!", "MISS");
            }
            return ActionResult.hit(name + " lands a HEAVY SMASH on " + heroName,
                    vary((int) Math.round(power * 1.7)), "SMASH!");
        } else if (roll < 80) {                            // Guard
            guarding = true;
            return ActionResult.nothing(name + " braces behind a guard (takes half damage).", "GUARD");
        } else if (roll < 92) {                            // Self heal (only if hurt)
            if (hp < maxHp * 0.7) {
                int amount = Math.max(5, maxHp / 6);
                hp = Math.min(maxHp, hp + amount);
                return ActionResult.healing(name + " mends its wounds and recovers " + amount + " HP.",
                        amount, false, "HEAL");
            }
            return ActionResult.hit(name + " strikes " + heroName, vary(power), "STRIKE");
        }
        return ActionResult.nothing(name + " stumbles over its own feet.", "STUMBLE");
    }

    private static int vary(int base) {
        return (int) Math.round(base * (0.85 + RNG.nextDouble() * 0.3));
    }

    boolean isAlive() { return hp > 0; }
    String getName() { return name; }
    int getLevel() { return level; }
    int getHp() { return hp; }
    int getMaxHp() { return maxHp; }
    boolean isGuarding() { return guarding; }
    boolean isStunned() { return stunned; }
}
