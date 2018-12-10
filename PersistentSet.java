import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.Map;

/**
 * A Persistent Implementation of a Left Leaning RedBlack Tree as a Set.
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

public class PersistentSet<E extends Comparable<E>, R extends Comparable<R>> {

  private enum Color { RED, BLACK }

  // saves the 'state' of a node at a revision
  private class SetRecord {
    public R revision;
    public Node left, right, node;
    public int size;

    public SetRecord (R revision, Node left, Node right, int size) {
      this.revision = revision;
      this.left = left;
      this.right = right;
      this.size = size;
    }

    public SetRecord (R revision, SetRecord previous) {
      this.revision = revision;
      this.left = previous.left;
      this.right = previous.right;
      this.size = previous.size;
    }

    public String toString () {
      return "revision: " + revision + ", " +
        "left: " + (left == null ? "()" : left.toString(revision)) + ", " +
        "right: " + (right == null ? "()" : right.toString(revision)) + ", " +
        "size: " + size;
    }
  }

  // BST helper node data type
  private class Node {
    private static final int MAX_RECORD_CHANGES = 5;

    public E element;
    public Color color;
    private TreeMap<R, SetRecord> setRecords;

    public Node (E element) {
      this.element = element;
      this.setRecords = new TreeMap<R, SetRecord>();
    }

    public Node (R revision, E element, Color color, int size) {
      this(element);
      this.color = color;
      setRecords.put(revision, new SetRecord(revision, null, null, size));
    }

    public Node (E element, SetRecord record) {
      this(element);
      setRecords.put(record.revision, record);
    }

    public String toString (R revision) { return toString(revision, this); }

    private String toString (R revision, Node node) {
      if (node == null) return "()";

      // the element
      return "({" + node.element + ":" +
        // the color
        (node.color == Color.RED ? "red" : "black") + "} " +
        // the left
        toString(revision, node.getLeft(revision)) + " " +
        // the right
        toString(revision, node.getRight(revision)) + ")";
    }

    public Node getLeft (R revision) {
      SetRecord current = findRevision(revision);

      if (current == null)
        throw new NoSuchElementException("Could not find that revision!");

      return current.left;
    }

    public Node getRight (R revision) {
      SetRecord current = findRevision(revision);

      if (current == null)
        throw new NoSuchElementException("Could not find that revision!");

      return current.right;
    }

    public int getSize (R revision) {
      SetRecord current = findRevision(revision);

      if (current == null)
        throw new NoSuchElementException("Could not find that revision!");

      return current.size;
    }

    public SetRecord findRevision (R revision) {
      if (setRecords.isEmpty()) return null;

      Map.Entry<R, SetRecord> floor =
        setRecords.floorEntry(revision);

      return floor == null ? null : floor.getValue();
    }

    public Node setLeft(R revision, Node left) {
      Node node = whichNode(revision);

      SetRecord current = node.setRecords.lastEntry().getValue();

      SetRecord change =
        new SetRecord(
          revision,
          left,
          current.right,
          size(left, current.right, revision)
        );

      node.setRecords.put(revision, change);

      return node;
    }

    public Node setRight(R revision, Node right) {
      Node node = whichNode(revision);

      SetRecord current = node.setRecords.lastEntry().getValue();

      SetRecord change =
        new SetRecord(
          revision,
          current.left,
          right,
          size(current.left, right, revision)
        );

      node.setRecords.put(revision, change);

      return node;
    }

    private Node whichNode(R revision) {
      // if we've maxed out this node, allocate a new one
      if (setRecords.size() >= MAX_RECORD_CHANGES) {
        Node replacement =
          new Node(this.element, setRecords.lastEntry().getValue());

        replacement.color = this.color;

        return replacement;
      }
      return this;
    }

    private int size(Node left, Node right, R revision) {
      int leftSize = left == null ? 0 : left.getSize(revision);
      int rightSize = right == null ? 0 : right.getSize(revision);
      // +1 because of the parent of left & right
      return leftSize + rightSize + 1;
    }
  }

  private TreeMap<R, Node> rootRecords;

  /**
   * Initializes an empty symbol table.
   */
  public PersistentSet() { rootRecords = new TreeMap<R, Node>(); }

  /***************************************************************************
   *  Node helper methods.
   ***************************************************************************/

  private boolean isRed(Node x) {
    return x == null ? false : x.color == Color.RED;
  }

  // number of node in subtree rooted at x; 0 if x is null
  private int size(Node x, R revision) {
    return x == null ? 0 : x.getSize(revision);
  }


  /**
   * Returns the number of elements in the set
   * @param revision the revision of the tree from which to calculate size
   * @return the number of elements in the set
   */
  public int size(R revision) {
    return size(findRoot(revision), revision);
  }

  /**
   * Is this set empty?
   * @param revision the revision of the tree to test for emptiness
   * @return {@code true} if this symbol table is empty and {@code false} otherwise
   */
  public boolean isEmpty(R revision) {
    return findRoot(revision) == null;
  }


  /***************************************************************************
   *  Standard BST search.
   ***************************************************************************/

  // TODO modify this for getting
  /**
   * Returns the value associated with the given element.
   * @param key the key
   * @param revision the revision for which to find the key
   * @return the value associated with the given key if the key is in the symbol table
   *     and {@code null} if the key is not in the symbol table
   * @throws IllegalArgumentException if {@code key} is {@code null}
   */
  public E get(E element, R revision) {
    if (element == null) throw new IllegalArgumentException("argument to get() is null");

    Node root = findRoot(revision);
    return get(root, element, revision);
  }

  private E get(Node x, E element, R revision) {
    while (x != null) {
      int cmp = element.compareTo(x.element);
      if      (cmp < 0) x = x.getLeft(revision);
      else if (cmp > 0) x = x.getRight(revision);
      else              return x.element;
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
  public boolean contains(E element, R revision) {
    Node root = findRoot(revision);
    return get(root, element, revision) != null;
  }

  /***************************************************************************
   *  Red-black tree insertion.
   ***************************************************************************/

  /**
   * Inserts the specified element into the set.
   *
   * @param element the element to add to the set
   * @param revision the tree revision for which to add the element
   * @return {@code true} if the element was inserted, {@code false} otherwise
   * @throws IllegalArgumentException if {@code element} is {@code null}
   */
  public boolean add(E element, R revision) {
    if (element == null) throw new IllegalArgumentException("argument to add() is null");

    Node root = findRoot(revision);

    if (root == null) {
      setRoot(revision, new Node(revision, element, Color.BLACK, 1));
    } else {
      setRoot(revision, add(root, element, revision));
      root.color = Color.BLACK;
    }
  }

  private Node add(Node subtree, E element, R revision) {
    if (subtree == null) return new Node(revision, element, Color.RED, 1);

    int cmp = element.compareTo(subtree.element);

    if        (cmp < 0) {
      subtree =
        subtree.setLeft( revision, add(subtree.getLeft(revision),  element, revision));
    } else if (cmp > 0) {
      subtree =
        subtree.setRight(revision, add(subtree.getRight(revision), element, revision));
    } else {
      // TODO
      // if the element was already here, you can't insert it
      subtree.element = element;
    }

    // fix-up any right-leaning links
    // if right is red and left is black, rotate left
    if (isRed(subtree.getRight(revision)) && !isRed(subtree.getLeft(revision))) {
      subtree = rotateLeft(subtree, revision);
      // if left is red and left of left is red, rotate right
    } if (isRed(subtree.getLeft(revision)) && isRed(subtree.getLeft(revision).getLeft(revision))) {
      subtree = rotateRight(subtree, revision);
      // if left is red and right is red, flip colors
    } if (isRed(subtree.getLeft(revision)) && isRed(subtree.getRight(revision))) {
      flipColors(subtree, revision);
    }

    return subtree;
  }

  /***************************************************************************
   *  Red-black tree deletion.
   ***************************************************************************/

  private void deleteMin(R revision) {
    if (isEmpty(revision)) throw new NoSuchElementException("BST underflow");

    // if both children of root are black, set root to red
    Node root = findRoot(revision);
    if (!isRed(root.getLeft(revision)) && !isRed(root.getRight(revision)))
      root.color = Color.RED;

    setRoot(revision, deleteMin(root, revision));
    if (!isEmpty(revision)) findRoot(revision).color = Color.BLACK;
  }

  private Node deleteMin(Node h, R revision) {
    if (h.getLeft(revision) == null)
      return null;

    if (!isRed(h.getLeft(revision)) && !isRed(h.getLeft(revision).getLeft(revision)))
      h = moveRedLeft(h, revision);

    h = h.setLeft(revision, deleteMin(h.getLeft(revision), revision));
    return balance(h, revision);
  }

  private void deleteMax(R revision) {
    if (isEmpty(revision)) throw new NoSuchElementException("BST underflow");

    // if both children of root are black, set root to red
    Node root = findRoot(revision);
    if (!isRed(root.getLeft(revision)) && !isRed(root.getRight(revision)))
      root.color = Color.RED;

    setRoot(revision, deleteMax(root, revision));
    if (!isEmpty(revision)) findRoot(revision).color = Color.BLACK;
  }

  private Node deleteMax(Node h, R revision) {
    if (isRed(h.getLeft(revision)))
      h = rotateRight(h, revision);

    if (h.getRight(revision) == null)
      return null;

    if (!isRed(h.getRight(revision)) && !isRed(h.getRight(revision).getLeft(revision)))
      h = moveRedRight(h, revision);

    h = h.setRight(revision, deleteMax(h.getRight(revision), revision));

    return balance(h, revision);
  }

  /**
   * Removes the specified element from the set.
   *
   * If the element is not in the set, {@code false} is returned.
   *
   * @param element the element to add to the set
   * @param revision the revision of the tree from which to delete
   * @throws IllegalArgumentException if {@code element} is {@code null}
   */
  public void remove(E element, R revision) {
    if (element == null) throw new IllegalArgumentException("argument to remove() is null");
    if (!contains(element, revision)) return;

    Node root = findRoot(revision);

    // if both children of root are black, set root to red
    if (!isRed(root.getLeft(revision)) && !isRed(root.getRight(revision)))
      root.color = Color.RED;

    setRoot(revision, remove(root, element, revision));
    if (!isEmpty(revision)) findRoot(revision).color = Color.BLACK;
  }

  private Node remove(Node h, E element, R revision) {
    if (element.compareTo(h.element) < 0)  {

      if (!isRed(h.getLeft(revision)) && !isRed(h.getLeft(revision).getLeft(revision)))
        h = moveRedLeft(h, revision);

      h = h.setLeft(revision, remove(h.getLeft(revision), element, revision));

    } else {

      if (isRed(h.getLeft(revision)))
        h = rotateRight(h, revision);

      if (element.compareTo(h.element) == 0 && (h.getRight(revision) == null))
        return null;

      if (!isRed(h.getRight(revision)) && !isRed(h.getRight(revision).getLeft(revision)))
        h = moveRedRight(h, revision);

      // you've found the node you're looking for
      if (element.compareTo(h.element) == 0) {
        // right min is the new root
        Node rightMin = min(h.getRight(revision), revision),
             rightSubtree = deleteMin(h.getRight(revision), revision),
             leftSubtree = h.getLeft(revision);

        h = rightMin
              .setRight(revision, rightSubtree)
              .setLeft (revision, leftSubtree);
      } else {
        h = h.setRight(revision, remove(h.getRight(revision), element, revision));
      }
    }
    return balance(h, revision);
  }

  /***************************************************************************
   *  Red-black tree helper functions.
   ***************************************************************************/

  // make a left-leaning link lean to the right
  private Node rotateRight(Node h, R revision) {
    Node left = h.getLeft(revision);

    h = h.setLeft(revision, left.getRight(revision));
    left = left.setRight(revision, h);
    left.color = left.getRight(revision).color;
    left.getRight(revision).color = Color.RED;

    // note that `left` is now the root of the subtree because of the rotation
    return left;
  }

  // make a right-leaning link lean to the left
  private Node rotateLeft(Node subtree, R revision) {
    Node right = subtree.getRight(revision);
    subtree = subtree.setRight(revision, right.getLeft(revision));
    right = right.setLeft(revision, subtree);
    right.color = right.getLeft(revision).color;
    right.getLeft(revision).color = Color.RED;

    // note that `right` is now the root of the subtree because of the rotation
    return right;
  }

  // flip the colors of a node and its two children
  //
  // can be void because we don't need to add any set records, so we don't risk
  // allocating a new node
  private void flipColors(Node h, R revision) {
    h.color = flipColor(h.color);
    h.getLeft(revision).color = flipColor(h.getLeft(revision).color);
    h.getRight(revision).color = flipColor(h.getRight(revision).color);
  }

  // find the opposite of the current color
  private Color flipColor(Color c) {
    return c == Color.RED ? Color.BLACK : Color.RED;
  }

  // Assuming that h is red and both h.getLeft(revision) and h.getLeft(revision).getLeft(revision)
  // are black, make h.getLeft(revision) or one of its children red.
  private Node moveRedLeft(Node h, R revision) {
    flipColors(h, revision);

    if (isRed(h.getRight(revision).getLeft(revision))) {
      h = h.setRight(revision, rotateRight(h.getRight(revision), revision));
      h = rotateLeft(h, revision);
      flipColors(h, revision);
    }

    return h;
  }

  // Assuming that h is red and both h.getRight(revision) and h.getRight(revision).getLeft(revision)
  // are black, make h.getRight(revision) or one of its children red.
  private Node moveRedRight(Node h, R revision) {
    flipColors(h, revision);

    if (isRed(h.getLeft(revision).getLeft(revision))) {
      h = rotateRight(h, revision);
      flipColors(h, revision);
    }

    return h;
  }

  // restore red-black tree invariant
  private Node balance(Node h, R revision) {
    if (isRed(h.getRight(revision)))
      h = rotateLeft(h, revision);

    if (isRed(h.getLeft(revision)) && isRed(h.getLeft(revision).getLeft(revision)))
      h = rotateRight(h, revision);

    if (isRed(h.getLeft(revision)) && isRed(h.getRight(revision)))
      flipColors(h, revision);

    return h;
  }

  // useful because this is left leaning
  private E min(R revision) {
    Node root = findRoot(revision);

    return min(root, revision).element;
  }

  private Node min(Node x, R revision) {
    return x.getLeft(revision) == null ? x : min(x.getLeft(revision), revision);
  }

  private Node findRoot(R revision) {
    if (rootRecords.isEmpty()) return null;

    Map.Entry<R, Node> entry = rootRecords.floorEntry(revision);

    if (entry == null) return null;

    return entry.getValue();
  }

  private void setRoot (R revision, Node node) {
    // will replace the existing entry at `revision`
    rootRecords.put(revision, node);
  }

  public String toString () {
    String str = "";

    for (R revision : rootRecords.keySet())
      str += "revision " + revision + ": " + toString(revision) + "\n";

    return str;
  }

  public String toString (R revision) {
    Node root = findRoot(revision);

    return root == null ?
      "No tree exists at revision " + revision : root.toString(revision);
  }

  public static void main(String[] args) {
    PersistentSet<Float, Double> tree = new PersistentSet<Float, Double>();

    // doing classic Red/Black stuff

    /*
    System.out.println("inserting a:1.0 at revision 1.0");
    tree.put(1.0f, "a", 1.0);
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

    System.out.println("\ninserting b:1.0 at revision 1.1");
    tree.put(1.0f, "b", 1.1);
    System.out.println("inserting c:1.2 at revision 1.2");
    tree.put(1.2f, "c", 1.2);
    System.out.println("inserting d:1.6 at revision 1.3");
    tree.put(1.6f, "d", 1.3);
    System.out.println("inserting e:0.6 at revision 1.4");
    tree.put(0.6f, "e", 1.4);
    System.out.println(tree);

    // getting from revisions

    System.out.print("getting 1.0 at revision 1.0: ");
    System.out.println(tree.get(1.0f, 1.0));
    System.out.print("getting 1.05 at revision 1.1: ");
    System.out.println(tree.get(1.0f, 1.05));
    System.out.print("getting 1.0 at revision 1.1: ");
    System.out.println(tree.get(1.0f, 1.1));

    // getting an invalid revision. revisions are gotten as the floor entry.
    // if no floor entry exists, the tree will throw a NoSuchElementException
    // when calling get/2

    System.out.println("\ngetting revision 0.9");
    System.out.println(tree.toString(0.9));

    // removing elements

    System.out.println("\nDeleting key 1.1 in revision 1.5");
    tree.delete(1.1f, 1.5);
    System.out.println(tree);

    // removing the root

    System.out.println("Deleting key 1.0 in revision 1.6");
    tree.delete(1.0f, 1.6);
    System.out.println(tree);
    */
  }
}
