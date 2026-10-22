/**
 * Console version: every hero fights a random enemy until one side falls.
 * Run it with:  java ConsoleDemo
 */
public class ConsoleDemo {
    public static void main(String[] args) {
        CharacterList roster = new CharacterList();
        roster.add(new Warrior("Brader", 5));
        roster.add(new Mage("GabMids", 7));
        roster.add(new Assassin("Shade", 8));
        roster.add(new Archer("Lyra", 4));
        roster.add(new Healer("Sol", 6));

        for (GameCharacter hero : roster) {
            System.out.println(hero.showName() + "  [" + hero.getType() + ", " + hero.getAbility()
                    + ", HP " + hero.getHp() + "/" + hero.getMaxHp() + "]");
            Battle battle = new Battle(hero, Enemy.random(hero.getLevel()), roster);
            System.out.println("  A wild " + battle.getEnemy().getName() + " (Lv " + battle.getEnemy().getLevel()
                    + ", HP " + battle.getEnemy().getMaxHp() + ") appears!");
            int turn = 0;
            while (!battle.isOver() && turn++ < 100) {
                // simple bot: use the special move when there is energy, otherwise attack
                ActionResult r = turn % 3 == 0 ? battle.playerSpecial() : battle.playerAttack();
                if (!r.success) {
                    battle.playerAttack();
                }
                battle.enemyTurn();
                for (String line : battle.drainLog()) {
                    System.out.println("  " + line);
                }
            }
            System.out.println("  -> " + hero.getName() + " HP " + hero.getHp() + "/" + hero.getMaxHp() + "\n");
        }
    }
}
