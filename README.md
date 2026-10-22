# Game Character Manager

A Java Swing program where you can create game characters, manage them in a list, and make them fight random enemies.

This is an improved version of my original java-game-character project. The first version was a console program with two hard-coded characters (Warrior and Mage). This version has a window, more character types, an HP system, and battles.

## What changed from the first version

| Original project | This version |
| --- | --- |
| 2 characters written directly in `main` | Add as many characters as you want while the program runs |
| Console output only | A Swing window with a table, form, buttons and a battle log |
| Warrior and Mage only | Warrior, Mage, Assassin, Healer and Archer |
| No health or enemies | HP, defense, and fights against random enemies |
| No data structure | A linked list for the characters and a stack for Undo Remove |
| No input checking | Input validation with error messages |

## Character types

| Type | What makes it different | Special move |
| --- | --- | --- |
| Warrior | Armor that goes up with level and blocks damage | Shield Bash: double damage and stuns the enemy |
| Mage | Strong attacks, low HP | Lightning Storm: 3 bolts |
| Assassin | The first attack from stealth is a backstab (triple damage) | Shadow Step: hits and hides again |
| Archer | Chance to land a critical hit (higher level, more crits) | Arrow Rain: 5 arrows |
| Healer | Weak attack | Healing Light: heals every living hero, costs no energy |

Every character has energy from 0 to 100. Attacking gives energy, a special move costs 30, and Rest refills it.

## How fights work

- Every character has HP. Max HP depends on the class and goes up by 10 per level.
- Defense lowers the damage from each hit. Warriors have the most.
- Click Fight! to meet a random enemy (Slime, Goblin, Skeleton, Orc, or a rare Dragon) close to your hero's level.
- Fights are turn based. You pick Attack, Special Move or Rest, then the enemy takes its turn.
- The enemy picks a random move each turn: strike, heavy smash, guard, heal itself, or stumble. Some moves can miss.
- Guard makes the enemy take half damage. A stunned enemy skips its turn.
- If you win, your hero goes up one level. If your HP reaches 0 the hero is defeated and has to Rest to get back up.
- Flee ends the fight. Selecting a different character also ends it.

## How to run

You need Java 17 or newer. Check with `java -version`.

```
javac *.java
java Main
```

You can also open the folder in IntelliJ, Eclipse or VS Code and run `Main.java`.

`java ConsoleDemo` runs a version with no window, where each hero fights an enemy automatically.

## How to use it

1. Add: type a name, pick a type, set a level, then click Add.
2. View: every character shows up in the table. Click a row to see it in the panel at the top right.
3. Search: type in the search box to filter by name or type.
4. Update: click a row, change the name or level, then click Update. The type cannot be changed.
5. Remove: click a row and click Remove. Undo Remove brings back the last one you removed.
6. Fight: select a character and click Fight!, then use Attack, Special Move and Rest.
7. Recover: outside a fight, Rest restores some HP and all energy.

## Input validation

- The name cannot be empty, must be 2 to 20 characters, and can only have letters, numbers and spaces.
- Names must be unique. "Zed" and "zed" count as the same name.
- The level must be a whole number from 1 to 99.
- Update, Remove and Fight need a selected character.
- Errors show in red under the buttons and the program does not crash.

## How the four concepts are used

### 1. Abstraction (GameCharacter.java)

`GameCharacter` is an abstract class. It holds what every character has in common: name, level, energy, HP and defense. It also lists methods that every subclass must write for itself: `attack()`, `specialMove()`, `getType()`, `getAbility()`, `getBasePower()` and `getBaseHp()`.

It says what a character can do, but not how. Warrior, Mage, Assassin, Healer and Archer each do it their own way. The battle code just calls `hero.attack()` and the correct version runs, no matter which class the hero is. This is called polymorphism.

### 2. Abstract Data Type (CharacterList.java)

`CharacterList` is a linked list I wrote myself. Each node holds a character and points to the next node. The rest of the program only uses its methods (`add`, `remove`, `find`, `search`, `get`, `size`) and never touches the nodes directly.

The program also uses a stack for Undo Remove. When a character is removed it is pushed onto the stack, and Undo pops the most recent one off. The last one removed is the first one restored.

### 3. GUI (CharacterApp.java)

The window is built with Java Swing. It uses a JFrame, JTable, JTextField, JComboBox, JSpinner, JButton and JTextArea. The character picture and the HP bars are drawn with Graphics2D, and a Swing Timer runs the animations.

### 4. User input (CharacterApp.java)

The user gives input through the form, the buttons, the table and the search box. `validateName()` and `readLevel()` check the name and level before a character is created or changed.

## Project files

| File | What it does |
| --- | --- |
| `GameCharacter.java` | Abstract base class (energy, HP, defense) |
| `Warrior.java`, `Mage.java`, `Assassin.java`, `Healer.java`, `Archer.java` | The five character types |
| `ActionResult.java` | Holds the result of a move (damage, healing, stun, message) |
| `Enemy.java` | The enemies and their random moves |
| `Battle.java` | The fight rules: turns, damage, win and lose |
| `CharacterList.java` | The linked list that stores the characters |
| `CharacterApp.java` | The window, input checking and animations |
| `Main.java` | Starts the program |
| `ConsoleDemo.java` | Console version of a fight, no window |
