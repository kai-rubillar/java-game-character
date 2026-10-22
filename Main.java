import javax.swing.SwingUtilities;

/** Starts the Game Character Manager (the Swing window). */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CharacterApp().setVisible(true));
    }
}
