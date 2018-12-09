import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.Map;

/**
 * A Persistent Implementation of a Left Leaning RedBlack Tree.
 *
 * Takes a revision based approach to persistence, where the revision may be
 * any comparable.
 *
 * Originally authored by
 *
 * @author Robert Sedgewick
 * @author Kevin Wayne
 *
 * Made persistent by
 *
 * @author Michael Davis
 */

public class PersistentRedBlackTree<Key extends Comparable<Key>, Value> {

  private enum Color { RED, BLACK }

  private enum Direction { LEFT, RIGHT }

  // saves the 'state' of a node at a revision
  private class SetRecord {
    public Comparable revision;
    public Node left, right, node;
    public Value val;
    public Color color;
    public int size;

    public SetRecord (
        Comparable revision,
        Node left,
        Node right,
        Value val,
        Color color,
        int size     ) {

      this.revision = revision;
      this.left = left;
      this.right = right;
      this.val = val;
      this.color = color;
      this.size = size;
    }

    public SetRecord (Comparable revision, SetRecord previous, Value val) {
      this.revision = revision;
      this.left = previous.left;
      this.right = previous.right;
      this.val = val;
      this.color = previous.color;
      this.size = previous.size;
    }

    public SetRecord (Comparable revision, SetRecord previous, Color color) {
      this.revision = revision;
      this.left = previous.left;
      this.right = previous.right;
      this.val = previous.val;
      this.color = color;
      this.size = previous.size;
    }

    public String toString () {
      return "revision: " + revision + ", " +
        "left: " + (left == null ? "()" : left.toString(revision)) + ", " +
        "right: " + (right == null ? "()" : right.toString(revision)) + ", " +
        "value: " + val + ", " +
        "color: " + (color == Color.RED ? "red" : "black") + ", " +
        "size: " + size;
    }
  }

  // BST helper node data type
  private class Node {
    private static final int MAX_RECORD_CHANGES = 5;

    public Key key;
    private TreeMap<Comparable, SetRecord> setRecords;

    // bookkeeping so we know (when provisioning a new node) whether we're our
    // parent's left or right child.
    public Direction direction;
    public Node parent;

    public Node (Key key) {
      this.key = key;
      this.setRecords = new TreeMap<Comparable, SetRecord>();
    }

    public Node (
        Comparable revision,
        Key key,
        Value val,
        Color color,
        int size) {

      this(key);

      setRecords.put(revision, new SetRecord(revision, null, null, val, color, size));
    }

    public Node (Key key, SetRecord record) {
      this(key);

      setRecords.put(record.revision, record);
    }

    public String toString (Comparable revision) {
      return toString(revision, this);
    }

    private String toString (Comparable revision, Node node) {
      if (node == null) return "()";

      return "({" + node.key + ":" + node.getValue(revision) + ":" +
        (node.getColor(revision) == Color.RED ? "red" : "black") + "} " +
        toString(revision, node.getLeft(revision)) + " " +
        toString(revision, node.getRight(revision)) + ")";
    }

    public Value getValue (Comparable revision) {
      SetRecord current = findRevision(revision);

      if (current == null)
        throw new NoSuchElementException("Could not find that revision!");

      return current.val;
    }

    public Node getLeft (Comparable revision) {
      SetRecord current = findRevision(revision);

      if (current == null)
        throw new NoSuchElementException("Could not find that revision!");

      return current.left;
    }

    public Node getRight (Comparable revision) {
      SetRecord current = findRevision(revision);

      if (current == null)
        throw new NoSuchElementException("Could not find that revision!");

      return current.right;
    }

    public Color getColor (Comparable revision) {
      SetRecord current = findRevision(revision);

      if (current == null)
        throw new NoSuchElementException("Could not find that revision!");

      return current.color;
    }

    public int getSize (Comparable revision) {
      SetRecord current = findRevision(revision);

      if (current == null)
        throw new NoSuchElementException("Could not find that revision!");

      return current.size;
    }

    // find the SetRecord at that revision by binary search
    // we're comparing a Comparable, so it should be pretty safe to use
    // .compareTo
    @SuppressWarnings("unchecked")
    public SetRecord findRevision (Comparable revision) {
      if (setRecords.isEmpty()) return null;

      Map.Entry<Comparable, SetRecord> ceiling =
        setRecords.ceilingEntry(revision);

      return ceiling == null ? null : ceiling.getValue();
    }

    public Node setValue(Comparable revision, Value val) {
      Node node = whichNode(revision);

      if (setRecords.isEmpty()) {
        node.setRecords.put(revision, new SetRecord(revision, null, null, val, Color.RED, 0));
      } else {
        SetRecord current = node.setRecords.lastEntry().getValue();

        SetRecord change = new SetRecord(revision, current, val);

        node.setRecords.put(revision, change);
      }

      return node;
    }

    public Node setLeft(Comparable revision, Node left) {
      Node node = whichNode(revision);

      if (left != null) {
        left.parent = node;
        left.direction = Direction.LEFT;
      }

      if (setRecords.isEmpty()) {
        node.setRecords.put(revision, new SetRecord(revision, left, null, null, Color.RED, 0));
      } else {
        SetRecord current = node.setRecords.lastEntry().getValue();

        SetRecord change =
          new SetRecord(
            revision,
            left,
            current.right,
            current.val,
            current.color,
            size(left, current.right, revision)
          );

        setRecords.put(revision, change);
      }

      return node;
    }

    public Node setRight(Comparable revision, Node right) {
      Node node = whichNode(revision);

      if (right != null) {
        right.parent = node;
        right.direction = Direction.RIGHT;
      }

      if (setRecords.isEmpty()) {
        node.setRecords.put(revision, new SetRecord(revision, null, right, null, Color.RED, 0));
      } else {
        SetRecord current = node.setRecords.lastEntry().getValue();

        SetRecord change =
          new SetRecord(
            revision,
            current.left,
            right,
            current.val,
            current.color,
            size(current.left, right, revision)
          );

        node.setRecords.put(revision, change);
      }

      return node;
    }

    private Node setColor(Comparable revision, Color color) {
      Node node = whichNode(revision);

      if (setRecords.isEmpty()) {
        node.setRecords.put(revision, new SetRecord(revision, null, null, null, color, 0));
      } else {
        SetRecord current = node.setRecords.lastEntry().getValue();

        SetRecord change = new SetRecord(revision, current, color);

        node.setRecords.put(revision, change);
      }

      return node;
    }

    private Node whichNode(Comparable revision) {
      // if we've maxed out this node, allocate a new one and tell its parent
      //
      // we can't do this for the root though because there is no parent
      if (setRecords.size() >= MAX_RECORD_CHANGES && this.parent != null) {
        System.out.println("Allocating a new node!");
        Node replacement =
          new Node(this.key, setRecords.lastEntry().getValue());

        if (this.direction == Direction.LEFT)
          this.parent.setLeft(revision, replacement);
        else
          this.parent.setRight(revision, replacement);

        return replacement;
      }
      return this;
    }

    private int size(Node left, Node right, Comparable revision) {
      int leftSize = left == null ? 0 : left.getSize(revision);
      int rightSize = right == null ? 0 : right.getSize(revision);
      // +1 because of the parent of left & right
      return leftSize + rightSize + 1;
    }
  }

  private TreeMap<Comparable, Node> rootRecords;

  /**
   * Initializes an empty symbol table.
   */
  public PersistentRedBlackTree() {
    this.rootRecords = new TreeMap<Comparable, Node>();
  }

  /***************************************************************************
   *  Node helper methods.
   ***************************************************************************/

  private boolean isRed(Node x, Comparable revision) {
    if (x == null) return false;
    return x.getColor(revision) == Color.RED;
  }

  // number of node in subtree rooted at x; 0 if x is null
  private int size(Node x, Comparable revision) {
    if (x == null) return 0;
    return x.getSize(revision);
  }


  /**
   * Returns the number of key-value pairs in this symbol table.
   * @param revision the revision of the tree from which to calculate size
   * @return the number of key-value pairs in this symbol table
   */
  public int size(Comparable revision) {
    Node root = findRoot(revision);

    return size(root, revision);
  }

  /**
   * Is this symbol table empty?
   * @param revision the revision of the tree to test for emptiness
   * @return {@code true} if this symbol table is empty and {@code false} otherwise
   */
  public boolean isEmpty(Comparable revision) {
    return findRoot(revision) == null;
  }


  /***************************************************************************
   *  Standard BST search.
   ***************************************************************************/

  /**
   * Returns the value associated with the given key.
   * @param key the key
   * @param revision the revision for which to find the key
   * @return the value associated with the given key if the key is in the symbol table
   *     and {@code null} if the key is not in the symbol table
   * @throws IllegalArgumentException if {@code key} is {@code null}
   */
  public Value get(Key key, Comparable revision) {
    if (key == null) throw new IllegalArgumentException("argument to get() is null");

    Node root = findRoot(revision);
    return get(root, key, revision);
  }

  // value associated with the given key in subtree rooted at x; null if no such key
  private Value get(Node x, Key key, Comparable revision) {
    while (x != null) {
      int cmp = key.compareTo(x.key);
      if      (cmp < 0) x = x.getLeft(revision);
      else if (cmp > 0) x = x.getRight(revision);
      else              return x.getValue(revision);
    }
    return null;
  }

  /**
   * Does this symbol table contain the given key?
   * @param key the key
   * @param revision the revision of the tree for which to search the key
   * @return {@code true} if this symbol table contains {@code key} and
   *     {@code false} otherwise
   * @throws IllegalArgumentException if {@code key} is {@code null}
   */
  public boolean contains(Key key, Comparable revision) {
    return get(key, revision) != null;
  }

  /***************************************************************************
   *  Red-black tree insertion.
   ***************************************************************************/

  /**
   * Inserts the specified key-value pair into the symbol table, overwriting the old
   * value with the new value if the symbol table already contains the specified key.
   * Deletes the specified key (and its associated value) from this symbol table
   * if the specified value is {@code null}.
   *
   * @param key the key
   * @param val the value
   * @param revision the tree revision for which to add the key/value pair
   * @throws IllegalArgumentException if {@code key} is {@code null}
   */
  public void put(Key key, Value val, Comparable revision) {
    if (key == null) throw new IllegalArgumentException("first argument to put() is null");
    if (val == null) {
      delete(key, revision);
      return;
    }

    Node root = findRoot(revision);

    if (root == null) {
      setRoot(revision, new Node(revision, key, val, Color.BLACK, 1));
    } else {
      setRoot(revision, put(root, key, val, revision));
      root.setColor(revision, Color.BLACK);
    }
  }

  // insert the key-value pair in the subtree rooted at h
  private Node put(Node subtree, Key key, Value val, Comparable revision) {
    if (subtree == null) return new Node(revision, key, val, Color.RED, 1);

    int cmp = key.compareTo(subtree.key);

    if        (cmp < 0) {
      System.out.println("setting left");
      subtree = subtree.setLeft( revision, put(subtree.getLeft(revision),  key, val, revision));
    } else if (cmp > 0) {
      System.out.println("setting right of " + subtree.toString(revision));
      subtree = subtree.setRight(revision, put(subtree.getRight(revision), key, val, revision));
    } else {
      System.out.println("setting value");
      subtree = subtree.setValue(revision, val);
    }

    // fix-up any right-leaning links
    // if right is red and left is black, rotate left
    if (isRed(subtree.getRight(revision), revision) && !isRed(subtree.getLeft(revision), revision)) {
      System.out.println("rotating left of " + subtree.toString(revision));
      subtree = rotateLeft(subtree, revision);
      // if left is red and left of left is red, rotate right
    } if (isRed(subtree.getLeft(revision), revision) && isRed(subtree.getLeft(revision).getLeft(revision), revision)) {
      System.out.println("rotating right of " + subtree.toString(revision));
      subtree = rotateRight(subtree, revision);
      // if left is red and right is red, flip colors
    } if (isRed(subtree.getLeft(revision), revision) && isRed(subtree.getRight(revision), revision)) {
      System.out.println("flipping colors of " + subtree.toString(revision));
      subtree = flipColors(subtree, revision);
      System.out.println("flipped. " + subtree.toString(revision));
    }

    return subtree;
  }

  /***************************************************************************
   *  Red-black tree deletion.
   ***************************************************************************/

  /**
   * Removes the smallest key and associated value from the symbol table.
   * @param revision the revision of the tree from which to delete
   * @throws NoSuchElementException if the symbol table is empty
   */
  public void deleteMin(Comparable revision) {
    if (isEmpty(revision)) throw new NoSuchElementException("BST underflow");

    // if both children of root are black, set root to red
    Node root = findRoot(revision);
    if (!isRed(root.getLeft(revision), revision) && !isRed(root.getRight(revision), revision))
      root.setColor(revision, Color.RED);

    setRoot(revision, deleteMin(root, revision));
    if (!isEmpty(revision)) root.setColor(revision, Color.BLACK);
  }

  // delete the key-value pair with the minimum key rooted at h
  private Node deleteMin(Node h, Comparable revision) {
    if (h.getLeft(revision) == null)
      return null;

    if (!isRed(h.getLeft(revision), revision) && !isRed(h.getLeft(revision).getLeft(revision), revision))
      h = moveRedLeft(h, revision);

    h = h.setLeft(revision, deleteMin(h.getLeft(revision), revision));
    return balance(h, revision);
  }


  /**
   * Removes the largest key and associated value from the symbol table.
   * @param revision the revision of the tree from which to delete
   * @throws NoSuchElementException if the symbol table is empty
   */
  public void deleteMax(Comparable revision) {
    if (isEmpty(revision)) throw new NoSuchElementException("BST underflow");

    // if both children of root are black, set root to red
    Node root = findRoot(revision);
    if (!isRed(root.getLeft(revision), revision) && !isRed(root.getRight(revision), revision))
      root.setColor(revision, Color.RED);

    setRoot(revision, deleteMax(root, revision));
    if (!isEmpty(revision)) root.setColor(revision, Color.BLACK);
  }

  // delete the key-value pair with the maximum key rooted at h
  private Node deleteMax(Node h, Comparable revision) {
    if (isRed(h.getLeft(revision), revision))
      h = rotateRight(h, revision);

    if (h.getRight(revision) == null)
      return null;

    if (!isRed(h.getRight(revision), revision) && !isRed(h.getRight(revision).getLeft(revision), revision))
      h = moveRedRight(h, revision);

    h = h.setRight(revision, deleteMax(h.getRight(revision), revision));

    return balance(h, revision);
  }

  /**
   * Removes the specified key and its associated value from this symbol table
   * (if the key is in this symbol table).
   *
   * @param  key the key
   * @param revision the revision of the tree from which to delete
   * @throws IllegalArgumentException if {@code key} is {@code null}
   */
  public void delete(Key key, Comparable revision) {
    if (key == null) throw new IllegalArgumentException("argument to delete() is null");
    if (!contains(key, revision)) return;

    // if both children of root are black, set root to red
    Node root = findRoot(revision);
    if (!isRed(root.getLeft(revision), revision) && !isRed(root.getRight(revision), revision))
      root.setColor(revision, Color.RED);

    setRoot(revision, delete(root, key, revision));
    if (!isEmpty(revision)) root.setColor(revision, Color.BLACK);
  }

  // delete the key-value pair with the given key rooted at h
  private Node delete(Node h, Key key, Comparable revision) {
    if (key.compareTo(h.key) < 0)  {
      if (!isRed(h.getLeft(revision), revision) && !isRed(h.getLeft(revision).getLeft(revision), revision))
        h = moveRedLeft(h, revision);
      h = h.setLeft(revision, delete(h.getLeft(revision), key, revision));
    }
    else {
      if (isRed(h.getLeft(revision), revision))
        h = rotateRight(h, revision);
      if (key.compareTo(h.key) == 0 && (h.getRight(revision) == null))
        return null;
      if (!isRed(h.getRight(revision), revision) && !isRed(h.getRight(revision).getLeft(revision), revision))
        h = moveRedRight(h, revision);
      if (key.compareTo(h.key) == 0) {
        Node x = min(h.getRight(revision), revision);
        h.key = x.key;
        h = h.setValue(revision, x.getValue(revision));
        h = h.setRight(revision, deleteMin(h.getRight(revision), revision));
      }
      else h = h.setRight(revision, delete(h.getRight(revision), key, revision));
    }
    return balance(h, revision);
  }

  /***************************************************************************
   *  Red-black tree helper functions.
   ***************************************************************************/

  // make a left-leaning link lean to the right
  private Node rotateRight(Node h, Comparable revision) {
    Node left = h.getLeft(revision);
    h = h.setLeft(revision, left.getRight(revision));
    left = left.setRight(revision, h);
    left = left.setColor(revision, left.getRight(revision).getColor(revision));
    left.getRight(revision).setColor(revision, Color.RED);

    System.out.println("rotated. " + left.toString(revision));

    // note that `left` is now the root of the subtree because of the rotation
    return left;
  }

  // make a right-leaning link lean to the left
  private Node rotateLeft(Node subtree, Comparable revision) {
    Node right = subtree.getRight(revision);
    subtree = subtree.setRight(revision, right.getLeft(revision));
    right = right.setLeft(revision, subtree);
    right = right.setColor(revision, right.getLeft(revision).getColor(revision));
    right.getLeft(revision).setColor(revision, Color.RED);

    System.out.println("rotated. " + right.toString(revision));

    // note that `right` is now the root of the subtree because of the rotation
    return right;
  }

  // flip the colors of a node and its two children
  private Node flipColors(Node h, Comparable revision) {
    h = h.setColor(revision, flipColor(h.getColor(revision)));
    h.getLeft(revision).setColor(revision, flipColor(h.getLeft(revision).getColor(revision)));
    h.getRight(revision).setColor(revision, flipColor(h.getRight(revision).getColor(revision)));
    return h;
  }

  // find the opposite of the current color
  private Color flipColor(Color c) {
    return c == Color.RED ? Color.BLACK : Color.RED;
  }

  // Assuming that h is red and both h.getLeft(revision) and h.getLeft(revision).getLeft(revision)
  // are black, make h.getLeft(revision) or one of its children red.
  private Node moveRedLeft(Node h, Comparable revision) {
    flipColors(h, revision);
    if (isRed(h.getRight(revision).getLeft(revision), revision)) {
      h = h.setRight(revision, rotateRight(h.getRight(revision), revision));
      h = rotateLeft(h, revision);
      flipColors(h, revision);
    }
    return h;
  }

  // Assuming that h is red and both h.getRight(revision) and h.getRight(revision).getLeft(revision)
  // are black, make h.getRight(revision) or one of its children red.
  private Node moveRedRight(Node h, Comparable revision) {
    flipColors(h, revision);
    if (isRed(h.getLeft(revision).getLeft(revision), revision)) {
      h = rotateRight(h, revision);
      flipColors(h, revision);
    }
    return h;
  }

  // restore red-black tree invariant
  private Node balance(Node h, Comparable revision) {
    if (isRed(h.getRight(revision), revision))
      h = rotateLeft(h, revision);
    if (isRed(h.getLeft(revision), revision) && isRed(h.getLeft(revision).getLeft(revision), revision))
      h = rotateRight(h, revision);
    if (isRed(h.getLeft(revision), revision) && isRed(h.getRight(revision), revision))
      flipColors(h, revision);

    return h;
  }

  /**
   * Returns the smallest key in the symbol table.
   * @param revision the revision of the tree from which to get the minimum
   * @return the smallest key in the symbol table
   * @throws NoSuchElementException if the symbol table is empty
   */
  public Key min(Comparable revision) {
    if (isEmpty(revision))
      throw new NoSuchElementException("calls min() with empty symbol table");
    Node root = findRoot(revision);
    return min(root, revision).key;
  }

  // the smallest key in subtree rooted at x; null if no such key
  private Node min(Node x, Comparable revision) {
    if (x.getLeft(revision) == null)
      return x;
    else
      return min(x.getLeft(revision), revision);
  }

  /**
   * Returns the largest key in the symbol table.
   * @param revision the revision of the tree from which to get the maximum
   * @return the largest key in the symbol table
   * @throws NoSuchElementException if the symbol table is empty
   */
  public Key max(Comparable revision) {
    if (isEmpty(revision))
      throw new NoSuchElementException("calls max() with empty symbol table");
    Node root = findRoot(revision);
    return max(root, revision).key;
  }

  // the largest key in the subtree rooted at x; null if no such key
  private Node max(Node x, Comparable revision) {
    if (x.getRight(revision) == null)
      return x;
    else
      return max(x.getRight(revision), revision);
  }

  private Node findRoot(Comparable revision) {
    if (rootRecords.isEmpty()) return null;

    Map.Entry<Comparable, Node> entry = rootRecords.ceilingEntry(revision);

    if (entry == null) return null;

    return entry.getValue();
  }

  private void setRoot (Comparable revision, Node node) {
    // will replace the existing entry at `revision`
    rootRecords.put(revision, node);
  }

  public String toString () {
    String str = "";

    // TODO use StringBuilder to interpose newlines
    for (Comparable revision : rootRecords.keySet())
      str += "revision " + revision + ": " + toString(revision) + "\n";

    return str;
  }

  public String toString (Comparable revision) {
    return findRoot(revision).toString(revision);
  }

  public static void main(String[] args) {
    PersistentRedBlackTree<Float, String> tree = new PersistentRedBlackTree<Float, String>();

    Float key = 1.0f;

    // doing classic Red/Black stuff

    System.out.println("inserting a:1.0 at revision 1.0");
    tree.put(key, "a", 1.0);
    System.out.println(tree.toString(1.0));

    System.out.println("\ninserting y:0.8 at revision 1.0");
    tree.put(0.8f, "y", 1.0);
    System.out.println(tree.toString(1.0));

    System.out.println("\ninserting x:1.1 at revision 1.0");
    tree.put(1.1f, "x", 1.0);
    System.out.println(tree.toString(1.0));

    System.out.println("\ninserting z:1.2 at revision 1.0");
    tree.put(1.2f, "z", 1.0);
    System.out.println(tree.toString(1.0));

    System.out.println("\ninserting w:0.9 at revision 1.0");
    tree.put(0.9f, "w", 1.0);
    System.out.println(tree.toString(1.0));

    System.out.println("\ninserting r:0.7 at revision 1.0");
    tree.put(0.7f, "r", 1.0);
    System.out.println(tree.toString(1.0));

    System.out.println("\ninserting s:1.3 at revision 1.0");
    tree.put(1.3f, "s", 1.0);
    System.out.println(tree.toString(1.0));

    // Holding multiple revisions

    /*
    System.out.println("\ninserting b at revision 1.1");
    tree.put(key, "b", 1.1);
    System.out.println("inserting c at revision 1.2");
    tree.put(key, "c", 1.2);
    System.out.println("inserting d at revision 1.3");
    tree.put(key, "d", 1.3);
    System.out.println("inserting e at revision 1.4");
    tree.put(key, "e", 1.4);
    System.out.println(tree.toString());

    // getting from revisions

    System.out.print("getting the key at revision 1.0: ");
    System.out.println(tree.get(key, 1.0));
    System.out.print("getting the key at revision 1.1: ");
    System.out.println(tree.get(key, 1.1));
    */
  }
}
