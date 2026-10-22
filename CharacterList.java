import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * ABSTRACT DATA TYPE (ADT): a singly LINKED LIST of GameCharacters.
 *
 * An ADT is defined by WHAT you can do with it, not how it is stored:
 *   add, remove, find, search, get, size ...
 * The GUI only calls those operations - it never touches the nodes directly.
 * Inside, the data lives in chained Node objects:
 *
 *   head -> [Brader] -> [GabMids] -> [Lyra] -> null
 */
public class CharacterList implements Iterable<GameCharacter> {

    // One link in the chain: holds a character and points to the next node.
    private static class Node {
        GameCharacter data;
        Node next;

        Node(GameCharacter data) {
            this.data = data;
        }
    }

    private Node head;
    private Node tail;
    private int size;

    /** Adds a character at the end of the list. */
    public void add(GameCharacter c) {
        Node node = new Node(c);
        if (head == null) {
            head = node;
            tail = node;
        } else {
            tail.next = node;
            tail = node;
        }
        size++;
    }

    /** Removes the character with this name (ignores upper/lowercase). Returns it, or null. */
    public GameCharacter remove(String name) {
        Node previous = null;
        Node current = head;
        while (current != null) {
            if (current.data.getName().equalsIgnoreCase(name)) {
                if (previous == null) {
                    head = current.next;        // removing the first node
                } else {
                    previous.next = current.next;
                }
                if (current == tail) {
                    tail = previous;            // removing the last node
                }
                size--;
                return current.data;
            }
            previous = current;
            current = current.next;
        }
        return null;
    }

    /** Finds the character with exactly this name (ignores upper/lowercase), or null. */
    public GameCharacter find(String name) {
        for (Node n = head; n != null; n = n.next) {
            if (n.data.getName().equalsIgnoreCase(name)) {
                return n.data;
            }
        }
        return null;
    }

    /** Returns a NEW list of every character whose name or type contains the keyword. */
    public CharacterList search(String keyword) {
        CharacterList results = new CharacterList();
        String k = keyword.trim().toLowerCase();
        for (Node n = head; n != null; n = n.next) {
            String name = n.data.getName().toLowerCase();
            String type = n.data.getType().toLowerCase();
            if (name.contains(k) || type.contains(k)) {
                results.add(n.data);
            }
        }
        return results;
    }

    public GameCharacter get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index " + index + ", size " + size);
        }
        Node n = head;
        for (int i = 0; i < index; i++) {
            n = n.next;
        }
        return n.data;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /** Lets us write: for (GameCharacter c : list) { ... } */
    @Override
    public Iterator<GameCharacter> iterator() {
        return new Iterator<>() {
            private Node current = head;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public GameCharacter next() {
                if (current == null) {
                    throw new NoSuchElementException();
                }
                GameCharacter data = current.data;
                current = current.next;
                return data;
            }
        };
    }
}
