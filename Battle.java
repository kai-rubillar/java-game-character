import java.util.ArrayList;
import java.util.List;

/**
 * One fight between a hero and an enemy. Pure game logic (no Swing), so it can be
 * tested without a window. The GUI calls playerAttack / playerSpecial / playerRest,
 * then (if the fight isn't over) enemyTurn().
 */
class Battle {
    private final GameCharacter hero;
    private final Enemy enemy;
    private final Iterable<GameCharacter> party;   // who the Healer's Healing Light reaches
    private boolean over;
    private boolean won;
    private final List<String> lines = new ArrayList<>();

    Battle(GameCharacter hero, Enemy enemy, Iterable<GameCharacter> party) {
        this.hero = hero;
        this.enemy = enemy;
        this.party = party;
    }

    // ---------- the player's turn ----------

    ActionResult playerAttack() {
        return playerTurn(hero.attack());
    }

    ActionResult playerSpecial() {
        return playerTurn(hero.specialMove());
    }

    ActionResult playerRest() {
        String msg = hero.rest();
        return playerTurn(ActionResult.nothing(msg, "REST"));
    }

    private ActionResult playerTurn(ActionResult r) {
        if (over || !r.success) {                 // too tired: no turn is used
            if (!r.success) { lines.add(r.message); }
            return r;
        }
        StringBuilder msg = new StringBuilder(r.message);
        if (r.damage > 0) {
            boolean guarded = enemy.isGuarding();
            int dealt = enemy.takeDamage(r.damage);
            if (guarded) {
                msg.append(" ").append(enemy.getName()).append(" blocks half: only ").append(dealt).append("!");
            }
        }
        if (r.stun && enemy.isAlive()) {
            enemy.stun();
        }
        if (r.heal > 0 && r.healsParty) {
            for (GameCharacter c : party) {
                c.heal(r.heal);
            }
        }
        lines.add(msg.toString());
        if (!enemy.isAlive()) {
            over = true;
            won = true;
            lines.add(enemy.getName() + " is defeated! " + hero.getName() + " wins!");
            if (hero.getLevel() < GameCharacter.MAX_LEVEL) {
                hero.setLevel(hero.getLevel() + 1);
                lines.add("★ LEVEL UP! " + hero.getName() + " is now level " + hero.getLevel() + ".");
            }
        }
        return r;
    }

    // ---------- the enemy's turn (random move) ----------

    ActionResult enemyTurn() {
        if (over) {
            return ActionResult.nothing("", "");
        }
        ActionResult r = enemy.act(hero.getName());
        String msg = r.message;
        if (r.damage > 0) {
            int dealt = hero.takeDamage(r.damage);
            msg += " for " + dealt + " damage!";
        }
        lines.add(msg);
        if (!hero.isAlive()) {
            over = true;
            won = false;
            lines.add("✖ " + hero.getName() + " has been defeated! Rest to recover.");
        }
        return r;
    }

    /** Log lines produced since the last call (then cleared). */
    List<String> drainLog() {
        List<String> copy = new ArrayList<>(lines);
        lines.clear();
        return copy;
    }

    GameCharacter getHero() { return hero; }
    Enemy getEnemy() { return enemy; }
    boolean isOver() { return over; }
    boolean isWon() { return won; }
}
