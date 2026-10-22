import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * GUI (Java Swing): the main window of the Game Character Manager.
 *
 *  - LEFT:  a search box and a table of all characters (VIEW + SEARCH)
 *  - RIGHT: an animated avatar, a form (CREATE / UPDATE / REMOVE),
 *           and Attack / Special / Rest buttons with a battle log.
 *
 * The characters are stored in our own ADT, CharacterList (a linked list).
 * Removed characters go onto a stack so the last removal can be undone.
 */
public class CharacterApp extends JFrame {

    // ---- Theme colors ----
    static final Color BG = new Color(0x1B1B2B);
    static final Color PANEL = new Color(0x26263A);
    static final Color FIELD = new Color(0x33334D);
    static final Color TEXT = new Color(0xECECF5);
    static final Color MUTED = new Color(0x9A9AB5);
    static final Color ERROR = new Color(0xFF6B6B);
    static final Color OK = new Color(0x6BE08F);

    static final String[] TYPES = {"Warrior", "Mage", "Assassin", "Healer", "Archer"};

    // ---- Data (the ADTs) ----
    private final CharacterList roster = new CharacterList();
    private final Deque<GameCharacter> removedStack = new ArrayDeque<>(); // used as a STACK

    // ---- Widgets ----
    private final DefaultTableModel tableModel =
            new DefaultTableModel(new String[]{"Name", "Type", "Level", "HP", "Ability"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JComboBox<String> typeBox = new JComboBox<>(TYPES);
    private final JSpinner levelSpinner = new JSpinner(
            new SpinnerNumberModel(1, GameCharacter.MIN_LEVEL, GameCharacter.MAX_LEVEL, 1));
    private final JLabel statusLabel = new JLabel(" ");
    private final JTextArea logArea = new JTextArea();
    private final AvatarPanel avatar = new AvatarPanel();
    private AccentHeader header;
    private final JButton undoButton = makeButton("Undo Remove", new Color(0x5C6BC0));

    private final JButton fightButton = makeButton("Fight!", new Color(0xC0392B));
    private final JButton attackButton = makeButton("Attack", new Color(0xD35400));
    private final JButton specialButton = makeButton("Special Move", new Color(0x8E44AD));
    private final JButton restButton = makeButton("Rest", new Color(0x16A085));

    private Battle battle;          // the current fight (null = no fight)
    private boolean busy;           // true while the enemy is "thinking" (buttons locked)

    private GameCharacter selected;
    private boolean refreshing; // true while we rebuild the table (ignore selection events)

    public CharacterApp() {
        super("Game Character Manager");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        setIconImage(makeWindowIcon());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);

        // Starting characters (the two from the original project, plus two new types)
        roster.add(new Warrior("Brader", 5));
        roster.add(new Mage("GabMids", 7));
        roster.add(new Assassin("Shade", 8));
        roster.add(new Healer("Sol", 6));
        roster.add(new Archer("Lyra", 4));
        refreshTable(null);
        updateButtons();

        setSize(1020, 780);
        setMinimumSize(new Dimension(900, 740));
        setLocationRelativeTo(null);
    }

    // =====================================================================
    //  Building the window
    // =====================================================================

    private JComponent buildHeader() {
        JLabel title = new JLabel("  ⚔  Game Character Manager");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        title.setForeground(TEXT);
        title.setBorder(new EmptyBorder(14, 10, 14, 10));
        header = new AccentHeader();
        header.add(title, BorderLayout.WEST);
        JLabel hint = new JLabel("Create, view, search, update and remove heroes   ");
        hint.setForeground(MUTED);
        header.add(hint, BorderLayout.EAST);
        return header;
    }

    private JComponent buildCenter() {
        JPanel center = new JPanel(new GridLayout(1, 2, 14, 0));
        center.setBackground(BG);
        center.setBorder(new EmptyBorder(14, 14, 14, 14));
        center.add(buildLeft());
        center.add(buildRight());
        return center;
    }

    private JComponent buildLeft() {
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setBackground(BG);

        // Search bar
        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setBackground(BG);
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setForeground(TEXT);
        styleField(searchField);
        searchRow.add(searchLabel, BorderLayout.WEST);
        searchRow.add(searchField, BorderLayout.CENTER);
        left.add(searchRow, BorderLayout.NORTH);

        // Live search: the table updates on every key typed
        searchField.getDocument().addDocumentListener(new SimpleDocListener(() -> refreshTable(selected)));

        // Table
        table.setBackground(PANEL);
        table.setForeground(TEXT);
        table.setGridColor(BG);
        table.setRowHeight(28);
        table.setSelectionBackground(new Color(0x4A4A78));
        table.setSelectionForeground(Color.WHITE);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setBackground(FIELD);
        table.getTableHeader().setForeground(TEXT);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(2).setMaxWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setCellRenderer(new TypeRenderer());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !refreshing) {
                onRowSelected();
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(PANEL);
        scroll.setBorder(BorderFactory.createLineBorder(FIELD));
        left.add(scroll, BorderLayout.CENTER);

        return left;
    }

    private JComponent buildRight() {
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBackground(BG);

        avatar.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Fixed height: without a minimum, a long battle log squeezes the avatar panel smaller.
        avatar.setPreferredSize(new Dimension(100, 250));
        avatar.setMinimumSize(new Dimension(100, 250));
        avatar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        right.add(avatar);
        right.add(Box.createVerticalStrut(10));

        // ---- Form ----
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(PANEL);
        form.setBorder(new EmptyBorder(10, 12, 10, 12));
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.setMinimumSize(new Dimension(100, 130));
        form.setPreferredSize(new Dimension(100, 130));
        form.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        styleField(nameField);
        styleCombo(typeBox);
        styleSpinner(levelSpinner);
        addFormRow(form, 0, "Name:", nameField);
        addFormRow(form, 1, "Type:", typeBox);
        addFormRow(form, 2, "Level (1-99):", levelSpinner);
        right.add(form);
        right.add(Box.createVerticalStrut(8));

        // ---- CRUD buttons ----
        JPanel crud = new JPanel(new GridLayout(1, 5, 6, 0));
        crud.setBackground(BG);
        crud.setAlignmentX(Component.LEFT_ALIGNMENT);
        crud.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JButton add = makeButton("Add", new Color(0x2E9E5B));
        JButton update = makeButton("Update", new Color(0x2F7FD1));
        JButton remove = makeButton("Remove", new Color(0xC0392B));
        JButton clear = makeButton("Clear", new Color(0x666680));
        add.addActionListener(e -> onAdd());
        update.addActionListener(e -> onUpdate());
        remove.addActionListener(e -> onRemove());
        undoButton.addActionListener(e -> onUndo());
        clear.addActionListener(e -> onClear());
        undoButton.setEnabled(false);
        crud.add(add);
        crud.add(update);
        crud.add(remove);
        crud.add(undoButton);
        crud.add(clear);
        right.add(crud);

        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setBorder(new EmptyBorder(6, 2, 6, 2));
        right.add(statusLabel);

        // ---- Battle buttons ----
        JPanel battleRow = new JPanel(new GridLayout(1, 4, 6, 0));
        battleRow.setBackground(BG);
        battleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        battleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        fightButton.addActionListener(e -> onFightOrFlee());
        attackButton.addActionListener(e -> doAction("attack"));
        specialButton.addActionListener(e -> doAction("special"));
        restButton.addActionListener(e -> doAction("rest"));
        battleRow.add(fightButton);
        battleRow.add(attackButton);
        battleRow.add(specialButton);
        battleRow.add(restButton);
        right.add(battleRow);
        right.add(Box.createVerticalStrut(8));

        // ---- Battle log ----
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBackground(PANEL);
        logArea.setForeground(TEXT);
        logArea.setMargin(new Insets(6, 8, 6, 8));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        logScroll.setPreferredSize(new Dimension(100, 110));   // scrolls instead of growing
        logScroll.setBorder(BorderFactory.createLineBorder(FIELD));
        right.add(logScroll);

        return right;
    }

    // =====================================================================
    //  USER INPUT: create / update / remove / undo
    // =====================================================================

    private void onAdd() {
        String name = nameField.getText().trim();
        Integer level = readLevel();
        if (level == null) {
            return;
        }
        String error = validateName(name, null);
        if (error != null) {
            showError(error);
            return;
        }
        GameCharacter c = createCharacter((String) typeBox.getSelectedItem(), name, level);
        roster.add(c);
        refreshTable(c);
        showOk("Added " + c + ".");
        log("+ " + c.showName() + " joined the roster.");
    }

    private void onUpdate() {
        if (selected == null) {
            showError("Select a character in the table first.");
            return;
        }
        String name = nameField.getText().trim();
        Integer level = readLevel();
        if (level == null) {
            return;
        }
        String error = validateName(name, selected);
        if (error != null) {
            showError(error);
            return;
        }
        // The type can't change (a Warrior stays a Warrior) - only name and level.
        if (!selected.getType().equals(typeBox.getSelectedItem())) {
            showError("A character's type can't be changed. Remove it and add a new one.");
            typeBox.setSelectedItem(selected.getType());
            return;
        }
        selected.setName(name);
        selected.setLevel(level);
        refreshTable(selected);
        showOk("Updated " + selected + ".");
        log("~ Updated " + selected + ".");
    }

    private void onRemove() {
        if (selected == null) {
            showError("Select a character in the table first.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Remove " + selected.getName() + "?", "Confirm remove",
                JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        GameCharacter removed = roster.remove(selected.getName());
        removedStack.push(removed);              // STACK push: remember it for Undo
        undoButton.setEnabled(true);
        refreshTable(null);
        onClear();
        showOk("Removed " + removed.getName() + ". You can undo this.");
        log("- " + removed.getName() + " left the roster.");
    }

    private void onUndo() {
        if (removedStack.isEmpty()) {
            return;
        }
        GameCharacter back = removedStack.pop();  // STACK pop: last removed comes back first
        if (roster.find(back.getName()) != null) {
            showError("Can't undo: another character is now named " + back.getName() + ".");
            removedStack.push(back);
            return;
        }
        roster.add(back);
        undoButton.setEnabled(!removedStack.isEmpty());
        refreshTable(back);
        showOk("Restored " + back.getName() + ".");
        log("* " + back.getName() + " is back!");
    }

    private void onClear() {
        nameField.setText("");
        typeBox.setSelectedIndex(0);
        levelSpinner.setValue(1);
        table.clearSelection();
        selected = null;
        show(null);
        statusLabel.setText(" ");
    }

    // =====================================================================
    //  BATTLE: Fight / Attack / Special / Rest
    // =====================================================================

    private void onFightOrFlee() {
        if (selected == null) {
            showError("Select a character in the table first.");
            return;
        }
        if (battle != null) {                                   // Flee
            log("↩ " + selected.getName() + " flees from the " + battle.getEnemy().getName() + ".");
            endBattle();
            return;
        }
        if (!selected.isAlive()) {
            showError(selected.getName() + " is defeated. Press Rest to get back up.");
            return;
        }
        Enemy enemy = Enemy.random(selected.getLevel());
        battle = new Battle(selected, enemy, roster);
        avatar.setEnemy(enemy);
        log("⚔ A wild " + enemy.getName() + " (Lv " + enemy.getLevel() + ", HP " + enemy.getMaxHp() + ") appears!");
        statusLabel.setText(" ");
        updateButtons();
    }

    private void endBattle() {
        battle = null;
        busy = false;
        avatar.setEnemy(null);
        updateButtons();
    }

    private void updateButtons() {
        boolean fighting = battle != null;
        boolean alive = selected != null && selected.isAlive();
        fightButton.setText(fighting ? "Flee" : "Fight!");
        fightButton.setEnabled(!busy);
        attackButton.setEnabled(fighting && !busy);
        specialButton.setEnabled(fighting && !busy);
        restButton.setEnabled(!busy && selected != null && (fighting || !alive || selected.getHp() < selected.getMaxHp()
                || selected.getEnergy() < GameCharacter.MAX_ENERGY));
    }

    private void doAction(String action) {
        if (selected == null) {
            showError("Select a character in the table first.");
            return;
        }
        if (busy) {
            return;
        }
        statusLabel.setText(" ");

        // Resting with no fight going on: just recover (and get back up if defeated).
        if (battle == null) {
            if (action.equals("rest")) {
                log(selected.rest());
                avatar.flash("REST", new Color(0x4FC3F7));
                afterAction();
            } else {
                showError("Press Fight! to start a battle first.");
            }
            return;
        }

        ActionResult result;
        switch (action) {
            case "attack":  result = battle.playerAttack();  break;
            case "special": result = battle.playerSpecial(); break;
            default:        result = battle.playerRest();    break;
        }
        flushBattleLog();
        if (!result.success) {                       // too tired: no turn used
            avatar.flash("TIRED", MUTED);
            return;
        }
        avatar.flash(result.tag, action.equals("special") && result.heal > 0
                ? new Color(0x6BE08F) : action.equals("rest") ? new Color(0x4FC3F7) : new Color(0xFF9F43));
        if (result.damage > 0 || result.stun) {
            avatar.flashEnemy(result.stun ? "STUNNED" : "HIT", new Color(0xFF9F43));
        }
        afterAction();

        if (battle.isOver()) {
            endBattle();
            afterAction();
            return;
        }
        // The enemy answers after a short pause, so you can see what happened.
        busy = true;
        updateButtons();
        final Battle thisBattle = battle;
        Timer wait = new Timer(750, e -> {
            if (battle == thisBattle) {               // ignore if the fight changed meanwhile
                enemyTurn();
            }
        });
        wait.setRepeats(false);
        wait.start();
    }

    private void enemyTurn() {
        if (battle == null) {                        // the player fled meanwhile
            return;
        }
        ActionResult r = battle.enemyTurn();
        flushBattleLog();
        avatar.flashEnemy(r.tag, new Color(0xFF6B6B));
        if (r.damage > 0) {
            avatar.hurt();
        }
        busy = false;
        if (battle.isOver()) {
            endBattle();
        }
        afterAction();
    }

    private void flushBattleLog() {
        for (String line : battle.drainLog()) {
            log(line);
        }
    }

    /** Refreshes the table row, the avatar panel and the buttons after anything changed. */
    private void afterAction() {
        refreshTable(selected);
        show(selected);
        updateButtons();
    }

    // =====================================================================
    //  VALIDATION
    // =====================================================================

    /** Returns an error message, or null if the name is fine. */
    private String validateName(String name, GameCharacter ignore) {
        if (name.isEmpty()) {
            return "Name can't be empty.";
        }
        if (name.length() < 2 || name.length() > 20) {
            return "Name must be 2 to 20 characters long.";
        }
        if (!name.matches("[A-Za-z0-9 ]+")) {
            return "Name can only contain letters, numbers and spaces.";
        }
        GameCharacter existing = roster.find(name);
        if (existing != null && existing != ignore) {
            return "A character named \"" + existing.getName() + "\" already exists.";
        }
        return null;
    }

    /** Reads the level box. Returns null (and shows an error) if it isn't a valid number 1-99. */
    private Integer readLevel() {
        // Read the raw text so we can reject things like "5.5", "-3", "abc" or "" ourselves.
        String raw = ((JSpinner.DefaultEditor) levelSpinner.getEditor()).getTextField().getText().trim();
        if (!raw.matches("\\d{1,3}")) {
            showError("Level must be a whole number from " + GameCharacter.MIN_LEVEL
                    + " to " + GameCharacter.MAX_LEVEL + ".");
            return null;
        }
        int level = Integer.parseInt(raw);
        if (level < GameCharacter.MIN_LEVEL || level > GameCharacter.MAX_LEVEL) {
            showError("Level must be between " + GameCharacter.MIN_LEVEL
                    + " and " + GameCharacter.MAX_LEVEL + ".");
            return null;
        }
        levelSpinner.setValue(level);
        return level;
    }

    // =====================================================================
    //  Helpers
    // =====================================================================

    /** Creates the right subclass for the chosen type. (POLYMORPHISM: all are GameCharacters.) */
    private GameCharacter createCharacter(String type, String name, int level) {
        switch (type) {
            case "Mage":     return new Mage(name, level);
            case "Assassin": return new Assassin(name, level);
            case "Archer": return new Archer(name, level);
            case "Healer":   return new Healer(name, level);
            default:       return new Warrior(name, level);
        }
    }

    /** Rebuilds the table from the list (filtered by the search box) and re-selects a row. */
    private void refreshTable(GameCharacter toSelect) {
        refreshing = true;
        tableModel.setRowCount(0);
        String keyword = searchField.getText().trim();
        CharacterList view = keyword.isEmpty() ? roster : roster.search(keyword);
        int rowToSelect = -1;
        int row = 0;
        for (GameCharacter c : view) {
            tableModel.addRow(new Object[]{c.getName(), c.getType(), c.getLevel(), hpText(c), c.getAbility()});
            if (c == toSelect) {
                rowToSelect = row;
            }
            row++;
        }
        refreshing = false;
        if (rowToSelect >= 0) {
            table.setRowSelectionInterval(rowToSelect, rowToSelect);
            onRowSelected();
        } else if (toSelect == null || toSelect != selected) {
            // selection was filtered out or cleared
            if (table.getSelectedRow() < 0 && selected != null && view.find(selected.getName()) == null) {
                selected = null;
                show(null);
            }
        }
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        String name = (String) tableModel.getValueAt(row, 0);
        selected = roster.find(name);
        if (selected != null) {
            nameField.setText(selected.getName());
            typeBox.setSelectedItem(selected.getType());
            levelSpinner.setValue(selected.getLevel());
            show(selected);
            statusLabel.setText(" ");
        }
    }

    /** Shows a character in the avatar panel and tints the header with its class color. */
    private void show(GameCharacter c) {
        if (battle != null && c != battle.getHero()) {   // picked someone else: the fight is abandoned
            log("↩ " + battle.getHero().getName() + " leaves the fight.");
            endBattle();
        }
        avatar.setCharacter(c);
        header.setAccent(c == null ? new Color(0x5C6BC0) : colorFor(c.getType()));
        updateButtons();
    }

    private static String hpText(GameCharacter c) {
        return c.isAlive() ? c.getHp() + "/" + c.getMaxHp() : "DEFEATED";
    }

    private void showError(String msg) {
        statusLabel.setForeground(ERROR);
        statusLabel.setText("⚠ " + msg);
    }

    private void showOk(String msg) {
        statusLabel.setForeground(OK);
        statusLabel.setText("✔ " + msg);
    }

    private void log(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private static void addFormRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(4, 0, 4, 10);
        c.anchor = GridBagConstraints.WEST;
        JLabel l = new JLabel(label);
        l.setForeground(TEXT);
        form.add(l, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, 4, 0);
        form.add(field, c);
    }

    private static JButton makeButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(8, 6, 8, 6));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static void styleField(JTextField f) {
        f.setBackground(FIELD);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x4A4A6A)), new EmptyBorder(4, 6, 4, 6)));
    }

    private static void styleCombo(JComboBox<String> box) {
        box.setBackground(FIELD);
        box.setForeground(TEXT);
    }

    private static void styleSpinner(JSpinner s) {
        JFormattedTextField tf = ((JSpinner.DefaultEditor) s.getEditor()).getTextField();
        tf.setBackground(FIELD);
        tf.setForeground(TEXT);
        tf.setCaretColor(TEXT);
        s.setBorder(BorderFactory.createLineBorder(new Color(0x4A4A6A)));
    }

    static Color colorFor(String type) {
        switch (type) {
            case "Warrior": return new Color(0xE74C3C);
            case "Mage":    return new Color(0x9B7BFF);
            case "Assassin": return new Color(0xFF5FA2);
            case "Archer":  return new Color(0x2ECC71);
            case "Healer":  return new Color(0xF1C40F);
            default:        return Color.GRAY;
        }
    }

    // =====================================================================
    //  Small inner classes
    // =====================================================================

    /** Colors the "Type" column text. */
    private static class TypeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel,
                                                       boolean focus, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, value, sel, focus, row, col);
            setText("\u25CF  " + value);
            c.setForeground(colorFor(String.valueOf(value)));
            return c;
        }
    }

    /** Runs a piece of code whenever the text in a field changes. */
    private static class SimpleDocListener implements javax.swing.event.DocumentListener {
        private final Runnable action;

        SimpleDocListener(Runnable action) {
            this.action = action;
        }

        public void insertUpdate(javax.swing.event.DocumentEvent e) { action.run(); }
        public void removeUpdate(javax.swing.event.DocumentEvent e) { action.run(); }
        public void changedUpdate(javax.swing.event.DocumentEvent e) { action.run(); }
    }

    /** Header bar with a gradient that takes on the color of the selected character's class. */
    private static class AccentHeader extends JPanel {
        private Color accent = new Color(0x5C6BC0);
        private Color shown = accent;           // eased toward "accent" for a smooth color fade
        private final Timer fade;

        AccentHeader() {
            super(new BorderLayout());
            setOpaque(false);
            // ANIMATION: slide the shown color toward the target color a little every 30 ms
            fade = new Timer(30, e -> {
                shown = blend(shown, accent, 0.18f);
                repaint();
                if (shown.equals(accent)) {
                    ((Timer) e.getSource()).stop();
                }
            });
        }

        void setAccent(Color c) {
            accent = c;
            fade.start();
        }

        private static Color blend(Color a, Color b, float t) {
            int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
            int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
            int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
            if (Math.abs(b.getRed() - r) < 2 && Math.abs(b.getGreen() - g) < 2 && Math.abs(b.getBlue() - bl) < 2) {
                return b;
            }
            return new Color(r, g, bl);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Color tint = new Color(shown.getRed(), shown.getGreen(), shown.getBlue(), 110);
            g.setPaint(new GradientPaint(0, 0, PANEL, getWidth(), 0, tint));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(shown);
            g.fillRect(0, getHeight() - 3, getWidth(), 3);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** A small drawn icon for the window title bar (no image file needed). */
    private static Image makeWindowIcon() {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x5C6BC0));
        g.fillOval(2, 2, 60, 60);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(18, 46, 46, 18);
        g.drawLine(20, 30, 34, 44);
        g.dispose();
        return img;
    }

    /** A word that floats up and fades (ATTACK, CRIT!, HEAL, ...). */
    private static class FloatText {
        String text;
        Color color = Color.WHITE;
        float age = 1f;                // 0 -> 1 while it rises and fades; 1 = not showing

        void start(String t, Color c) { text = t; color = c; age = 0f; }
        void tick() { if (age < 1f) { age += 0.035f; } }
        boolean active() { return text != null && age < 1f; }
    }

    /**
     * Draws the selected character: a colored badge with an icon, name, level,
     * HP bar and energy bar. During a fight the enemy appears in a strip at the bottom.
     * ANIMATIONS: the badge "breathes", shakes and glows when you act, HP bars slide
     * smoothly, and words float up and fade.
     */
    private static class AvatarPanel extends JPanel {
        private GameCharacter character;
        private Enemy enemy;
        private float pulse = 0f;                 // 1.0 right after an action, fades to 0
        private float hurtPulse = 0f;             // 1.0 right after the hero is hit
        private float enemyPulse = 0f;            // 1.0 right after the enemy acts / is hit
        private double phase = 0;                 // drives the breathing
        private final FloatText heroText = new FloatText();
        private final FloatText enemyText = new FloatText();
        private double shownHeroHp;               // eased toward the real HP for a smooth bar
        private double shownEnemyHp;

        AvatarPanel() {
            setBackground(PANEL);
            Timer timer = new Timer(33, e -> {
                phase += 0.12;
                pulse = Math.max(0f, pulse - 0.05f);
                hurtPulse = Math.max(0f, hurtPulse - 0.05f);
                enemyPulse = Math.max(0f, enemyPulse - 0.05f);
                heroText.tick();
                enemyText.tick();
                if (character != null) {
                    shownHeroHp += (character.getHp() - shownHeroHp) * 0.2;
                    if (Math.abs(character.getHp() - shownHeroHp) < 0.5) { shownHeroHp = character.getHp(); }
                }
                if (enemy != null) {
                    shownEnemyHp += (enemy.getHp() - shownEnemyHp) * 0.2;
                    if (Math.abs(enemy.getHp() - shownEnemyHp) < 0.5) { shownEnemyHp = enemy.getHp(); }
                }
                if (character != null) {
                    repaint();
                }
            });
            timer.start();
        }

        void setCharacter(GameCharacter c) {
            if (c != character && c != null) {
                shownHeroHp = c.getHp();
            }
            this.character = c;
            repaint();
        }

        void setEnemy(Enemy e) {
            this.enemy = e;
            if (e != null) {
                shownEnemyHp = e.getMaxHp();
            }
            repaint();
        }

        void flash(String text, Color color) {
            pulse = 1f;
            heroText.start(text, color);
        }

        void flashEnemy(String text, Color color) {
            enemyPulse = 1f;
            enemyText.start(text, color);
        }

        void hurt() {
            hurtPulse = 1f;
        }

        private static Color hpColor(double fraction) {
            if (fraction > 0.5) { return new Color(0x4CD964); }
            if (fraction > 0.25) { return new Color(0xFFC107); }
            return new Color(0xFF5252);
        }

        private void bar(Graphics2D g, int x, int y, int w, int h, double fraction, Color fill) {
            g.setColor(FIELD);
            g.fillRoundRect(x, y, w, h, h, h);
            g.setColor(fill);
            g.fillRoundRect(x, y, (int) Math.round(w * Math.max(0, Math.min(1, fraction))), h, h, h);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            if (character == null) {
                g.setColor(MUTED);
                g.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 14));
                String msg = "Select a character to see it here";
                g.drawString(msg, (w - g.getFontMetrics().stringWidth(msg)) / 2, h / 2);
                g.dispose();
                return;
            }

            boolean alive = character.isAlive();
            Color color = alive ? colorFor(character.getType()) : new Color(0x55556B);
            double shakeAmount = Math.max(pulse * 0.6, hurtPulse);
            int shake = (int) Math.round(Math.sin(shakeAmount * 40) * shakeAmount * 7);
            int cx = 90 + shake;
            int cy = 80;
            int breathe = alive ? (int) Math.round(Math.sin(phase) * 2) : 0;
            int r = 44 + breathe + Math.round(pulse * 8);

            // soft glow rings (red flash when hurt)
            Color glow = hurtPulse > 0 ? new Color(0xFF5252) : color;
            for (int i = 3; i >= 1; i--) {
                int alpha = Math.min(255, 22 + Math.round(Math.max(pulse, hurtPulse) * 40));
                g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), alpha));
                int rr = r + i * 7;
                g.fillOval(cx - rr, cy - rr, rr * 2, rr * 2);
            }
            g.setPaint(new GradientPaint(cx - r, cy - r, color.brighter(), cx + r, cy + r, color.darker().darker()));
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
            g.setColor(new Color(255, 255, 255, 70));
            g.setStroke(new BasicStroke(2f));
            g.drawOval(cx - r + 4, cy - r + 4, r * 2 - 8, r * 2 - 8);
            drawIcon(g, character.getType(), cx, cy);
            if (!alive) {
                g.setColor(new Color(0xFF5252));
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
                String d = "DEFEATED";
                g.drawString(d, cx - g.getFontMetrics().stringWidth(d) / 2, cy + r + 18);
            }

            // text
            int tx = 175;
            int barW = Math.max(60, w - tx - 24);
            g.setColor(TEXT);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            g.drawString(character.getName(), tx, 34);
            g.setColor(color);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            g.drawString(character.getType() + "  •  Level " + character.getLevel(), tx, 54);
            g.setColor(MUTED);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            g.drawString(character.getAbility() + "   Power " + character.getPower()
                    + "   Defense " + character.getDefense(), tx, 74);

            // HP bar
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            g.setColor(TEXT);
            g.drawString("HP " + character.getHp() + "/" + character.getMaxHp(), tx, 96);
            bar(g, tx, 101, barW, 12, shownHeroHp / character.getMaxHp(), hpColor(shownHeroHp / character.getMaxHp()));
            // energy bar
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            g.setColor(MUTED);
            g.drawString("Energy " + character.getEnergy() + "/" + GameCharacter.MAX_ENERGY, tx, 130);
            bar(g, tx, 135, barW, 10, character.getEnergy() / (double) GameCharacter.MAX_ENERGY, new Color(0x29B6F6));

            // floating hero word
            if (heroText.active()) {
                int alpha = Math.round(255 * (1f - heroText.age));
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
                g.setColor(new Color(heroText.color.getRed(), heroText.color.getGreen(), heroText.color.getBlue(), alpha));
                g.drawString(heroText.text, 90 - g.getFontMetrics().stringWidth(heroText.text) / 2,
                        cy - 52 - Math.round(heroText.age * 24));
            }

            // enemy strip
            if (enemy != null) {
                int ey = 165;
                g.setColor(new Color(0x1F1F31));
                g.fillRoundRect(10, ey, w - 20, h - ey - 10, 14, 14);
                g.setColor(new Color(0xFF5252));
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(10, ey, w - 20, h - ey - 10, 14, 14);

                int ex = 34 + (int) Math.round(Math.sin(enemyPulse * 40) * enemyPulse * 6);
                int ecy = ey + 42;
                g.setPaint(new GradientPaint(ex - 24, ecy - 24, new Color(0xFF8A80), ex + 24, ecy + 24, new Color(0x7F1D1D)));
                g.fillOval(ex - 22, ecy - 22, 44, 44);
                g.setColor(Color.WHITE);                       // angry face
                g.fillOval(ex - 12, ecy - 8, 8, 8);
                g.fillOval(ex + 4, ecy - 8, 8, 8);
                g.setColor(new Color(0x1F1F31));
                g.fillOval(ex - 10, ecy - 6, 4, 4);
                g.fillOval(ex + 6, ecy - 6, 4, 4);
                g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawArc(ex - 9, ecy + 4, 18, 12, 20, 140);

                int etx = 76;
                int ebw = Math.max(60, w - etx - 34);
                g.setColor(TEXT);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
                g.drawString(enemy.getName() + "  •  Level " + enemy.getLevel(), etx, ey + 26);
                String status = enemy.isStunned() ? "  STUNNED" : enemy.isGuarding() ? "  GUARDING" : "";
                g.setColor(new Color(0xFFB74D));
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                g.drawString(status, etx + 12 + g.getFontMetrics(new Font(Font.SANS_SERIF, Font.BOLD, 15))
                        .stringWidth(enemy.getName() + "  •  Level " + enemy.getLevel()), ey + 26);
                g.setColor(MUTED);
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
                g.drawString("HP " + enemy.getHp() + "/" + enemy.getMaxHp(), etx, ey + 44);
                bar(g, etx, ey + 50, ebw, 12, shownEnemyHp / enemy.getMaxHp(), new Color(0xFF5252));

                if (enemyText.active()) {
                    int alpha = Math.round(255 * (1f - enemyText.age));
                    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
                    g.setColor(new Color(enemyText.color.getRed(), enemyText.color.getGreen(), enemyText.color.getBlue(), alpha));
                    g.drawString(enemyText.text, w - 30 - g.getFontMetrics().stringWidth(enemyText.text),
                            ey + 28 - Math.round(enemyText.age * 14));
                }
            }
            g.dispose();
        }

        /** A simple shape for each class, drawn with lines and shapes (no image files needed). */
        private void drawIcon(Graphics2D g, String type, int cx, int cy) {
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (type) {
                case "Warrior":   // sword
                    g.drawLine(cx - 18, cy + 18, cx + 18, cy - 18);
                    g.drawLine(cx - 14, cy + 2, cx - 2, cy + 14);
                    break;
                case "Mage":      // wizard hat
                    g.fillPolygon(new int[]{cx, cx - 18, cx + 18}, new int[]{cy - 24, cy + 14, cy + 14}, 3);
                    g.drawLine(cx - 26, cy + 16, cx + 26, cy + 16);
                    break;
                case "Assassin":  // dagger pointing up
                    g.fillPolygon(new int[]{cx, cx + 8, cx - 8}, new int[]{cy - 28, cy + 2, cy + 2}, 3);
                    g.drawLine(cx - 14, cy + 5, cx + 14, cy + 5);
                    g.drawLine(cx, cy + 5, cx, cy + 22);
                    break;
                case "Archer":    // bow and arrow
                    g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawArc(cx - 26, cy - 24, 40, 48, -90, 180);
                    g.drawLine(cx - 6, cy - 24, cx - 6, cy + 24);
                    g.drawLine(cx - 14, cy, cx + 24, cy);
                    g.drawLine(cx + 24, cy, cx + 16, cy - 7);
                    g.drawLine(cx + 24, cy, cx + 16, cy + 7);
                    break;
                default:          // healer: cross
                    g.fillRoundRect(cx - 7, cy - 22, 14, 44, 6, 6);
                    g.fillRoundRect(cx - 22, cy - 7, 44, 14, 6, 6);
                    break;
            }
        }
    }

    // =====================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CharacterApp().setVisible(true));
    }
}
