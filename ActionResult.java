/**
 * What happened when someone attacked, used a special move, or rested.
 * Characters and enemies RETURN this instead of just text, so the battle can
 * apply the damage / healing / stun itself.
 */
class ActionResult {
    final String message;     // shown in the battle log
    final int damage;         // raw damage dealt (before the target's defense)
    final int heal;           // HP restored (Healer's Healing Light, enemy self-heal)
    final boolean stun;       // target loses its next turn
    final boolean healsParty; // heal applies to every living hero
    final boolean success;    // false = e.g. "too tired", no turn was used
    final String tag;         // short word for the floating text (ATTACK, CRIT!, ...)

    ActionResult(String message, int damage, int heal, boolean stun,
                 boolean healsParty, boolean success, String tag) {
        this.message = message;
        this.damage = damage;
        this.heal = heal;
        this.stun = stun;
        this.healsParty = healsParty;
        this.success = success;
        this.tag = tag;
    }

    static ActionResult hit(String message, int damage, String tag) {
        return new ActionResult(message, damage, 0, false, false, true, tag);
    }

    static ActionResult stunningHit(String message, int damage, String tag) {
        return new ActionResult(message, damage, 0, true, false, true, tag);
    }

    static ActionResult healing(String message, int heal, boolean party, String tag) {
        return new ActionResult(message, 0, heal, false, party, true, tag);
    }

    static ActionResult nothing(String message, String tag) {
        return new ActionResult(message, 0, 0, false, false, true, tag);
    }

    static ActionResult failed(String message) {
        return new ActionResult(message, 0, 0, false, false, false, "TIRED");
    }
}
