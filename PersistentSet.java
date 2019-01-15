import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.List;

/**
 * A Persistent Implementation of a Left Leaning RedBlack Tree as a Set.
 *
 * Takes a revision based approach to persistence, where the revision may be
 * any comparable. Parameterized by {@code E}, the element type to store, and
 * {@code R}, the revision. Both must extend {@code java.lang.Comparable}.
 * This set assumes that the tree is built with non-decreasing revisions.
 * Altering past revisions will not cause cascading changes to future
 * revisions.
 *
 * @author Robert Sedgewick
 * @author Kevin Wayne
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

    // bookkeeping for root nodes
    private R revision;

    public E element;
    public Color color;
    private List<SetRecord> setRecords;

    public Node (E element) {
      this.element = element;
      this.setRecords = new ArrayList<SetRecord>(MAX_RECORD_CHANGES);
    }

    public Node (R revision, E element, Color color, int size) {
      this(element);
      this.color = color;
      this.setRecords.add(new SetRecord(revision, null, null, size));
    }

    public Node (E element, SetRecord record) {
      this(element);
      this.setRecords.add(record);
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
      int index = recordIndex(revision);

      return setRecords.get(index < 0 ? -index - 1 : index);
    }

    private int recordIndex (R revision) {
      int begin = 0;
      int end = setRecords.size() - 1;
      int mid = 0;

      while (begin <= end) {
        mid = (begin + end) >> 1;
        int cmp = revision.compareTo(setRecords.get(mid).revision);
        if (cmp == 0)
          return mid;
        else if (cmp > 0)
          begin = mid + 1;
        else
          end = mid - 1;
      }

      return -mid - 1;
    }

    public Node setLeft(R revision, Node left) {
      Node node = whichNode(revision);

      SetRecord current = node.setRecords.get(node.setRecords.size() - 1);

      SetRecord change =
        new SetRecord(
          revision,
          left,
          current.right,
          size(left, current.right, revision)
        );

      node.setRecord(revision, change);

      return node;
    }

    public Node setRight(R revision, Node right) {
      Node node = whichNode(revision);

      SetRecord current = node.setRecords.get(node.setRecords.size() - 1);

      SetRecord change =
        new SetRecord(
          revision,
          current.left,
          right,
          size(current.left, right, revision)
        );

      node.setRecord(revision, change);

      return node;
    }

    private Node whichNode(R revision) {
      // if we've maxed out this node, allocate a new one
      if (setRecords.size() >= MAX_RECORD_CHANGES) {
        Node replacement =
          new Node(this.element, setRecords.get(setRecords.size() - 1));

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

    private void setRecord (R revision, SetRecord record) {
      // will replace the existing entry at `revision`
      int index = recordIndex(revision);

      if (index < 0 || ! setRecords.get(index).revision.equals(revision))
        setRecords.add(record);
      else
        setRecords.set(index, record);
    }
  }

  private ArrayList<Node> rootRecords;
  private int size;

  /**
   * Initializes an empty set.
   */
  public PersistentSet() { rootRecords = new ArrayList<Node>(); }

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
   * Returns the number of elements in the set at a specified revision
   * @param revision the revision of the tree from which to calculate size
   * @return the number of elements in the set
   */
  public int size(R revision) {
    return size(findRoot(revision), revision);
  }

  /**
   * Returns the number of elements in the set across all revisions
   * @return the number of elements in the set
   */
  public int size() { return this.size; }

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

  /**
   * Returns the greatest element smaller than {@code element}.
   *
   * @param element the element to search for
   * @param revision the revision for which to find the element
   * @return the greatest element smaller than {@code element}
   *     and {@code null} if there is none.
   * @throws IllegalArgumentException if {@code key} is {@code null}
   */
  public E predecessor(E element, R revision) {
    if (element == null)
      throw new IllegalArgumentException("argument to get() is null");

    Node root = findRoot(revision);

    return predecessor(root, null, element, revision);
  }

  private E predecessor(Node x, E accumulator, E element, R revision) {
    if (x == null) return accumulator;
    int cmp = element.compareTo(x.element);
    if (cmp < 0)
      return predecessor(x.getLeft(revision), accumulator, element, revision);
    else
      return predecessor(x.getRight(revision), x.element, element, revision);
  }

  /**
   * Returns the smallest element greater than {@code element}.
   *
   * @param element the element to search for
   * @param revision the revision for which to find the element
   * @return the smallest element greater than {@code element}
   *     and {@code null} if there is none.
   * @throws IllegalArgumentException if {@code key} is {@code null}
   */
  public E successor(E element, R revision) {
    if (element == null)
      throw new IllegalArgumentException("argument to get() is null");

    Node root = findRoot(revision);

    return successor(root, null, element, revision);
  }

  private E successor(Node x, E accumulator, E element, R revision) {
    if (x == null) return accumulator;

    if (element.compareTo(x.element) < 0)
      return successor(x.getLeft(revision), x.element, element, revision);
    else
      return successor(x.getRight(revision), accumulator, element, revision);
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
   * @param element the element to search for
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

    if (contains(element, revision)) return false;

    Node root = findRoot(revision);

    if (root == null) {
      setRoot(revision, new Node(revision, element, Color.BLACK, 1));
    } else {
      setRoot(revision, add(root, element, revision));

      root = findRoot(revision);

      root.color = Color.BLACK;
    }

    this.size++;

    return true;
  }

  private Node add(Node h, E element, R revision) {
    if (h == null) return new Node(revision, element, Color.RED, 1);

    int cmp = element.compareTo(h.element);

    if (cmp < 0)
      h = h.setLeft( revision, add(h.getLeft(revision),  element, revision));
    else
      h = h.setRight(revision, add(h.getRight(revision), element, revision));

    // fix-up any right-leaning links
    // if right is red and left is black, rotate left
    if (isRed(h.getRight(revision)) && !isRed(h.getLeft(revision))) {
      h = rotateLeft(h, revision);
      // if left is red and left of left is red, rotate right
    } if (isRed(h.getLeft(revision)) && isRed(h.getLeft(revision).getLeft(revision))) {
      h = rotateRight(h, revision);
      // if left is red and right is red, flip colors
    } if (isRed(h.getLeft(revision)) && isRed(h.getRight(revision))) {
      flipColors(h, revision);
    }

    return h;
  }

  /***************************************************************************
   *  Red-black tree deletion.
   ***************************************************************************/

  // useful because this is a left-leaning tree
  private Node deleteMin(Node h, R revision) {
    if (h.getLeft(revision) == null)
      return null;

    if (!isRed(h.getLeft(revision)) && !isRed(h.getLeft(revision).getLeft(revision)))
      h = moveRedLeft(h, revision);

    h = h.setLeft(revision, deleteMin(h.getLeft(revision), revision));
    return balance(h, revision);
  }

  /**
   * Removes the specified element from the set.
   *
   * @param element the element to add to the set
   * @param revision the revision of the tree from which to delete
   * @return {@code true} if the element was removed, {@code false} otherwise
   * @throws IllegalArgumentException if {@code element} is {@code null}
   */
  public boolean remove(E element, R revision) {
    if (element == null) throw new IllegalArgumentException("argument to remove() is null");

    if (!contains(element, revision)) return false;

    Node root = findRoot(revision);

    // if both children of root are black, set root to red
    if (!isRed(root.getLeft(revision)) && !isRed(root.getRight(revision)))
      root.color = Color.RED;

    setRoot(revision, remove(root, element, revision));

    if (!isEmpty(revision)) findRoot(revision).color = Color.BLACK;

    this.size--;

    return true;
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

    int index = rootIndexOf(revision);

    return rootRecords.get(index < 0 ? -index - 1 : index);
  }

  private void setRoot (R revision, Node node) {
    // will replace the existing entry at `revision`
    int rootIndex = rootIndexOf(revision);

    node.revision = revision;

    if (rootIndex < 0 || ! rootRecords.get(rootIndex).revision.equals(revision))
      rootRecords.add(node);
    else
      rootRecords.set(rootIndex, node);
  }

  private int rootIndexOf(R revision) {
    int begin = 0;
    int end = rootRecords.size() - 1;
    int mid = 0;

    while (begin <= end) {
      mid = (begin + end) >> 1;
      int cmp = revision.compareTo(rootRecords.get(mid).revision);
      if (cmp == 0)
        return mid;
      else if (cmp > 0)
        begin = mid + 1;
      else
        end = mid - 1;
    }

    return -mid - 1;
  }

  /**
   * Stringifies the set for all revisions.
   *
   * @return a string version of the tree with newlines for each revision
   */
  public String toString () {
    String str = "";

    for (Node root : rootRecords)
      str += "revision " + root.revision + ": " +
        toString(root.revision) + "\n";

    return str;
  }

  /**
   * Stringifies the set for the specified revision.
   *
   * @param revision the revision to stringify
   * @return a string version of the tree in the recursive form of
   * (root left-tree right-tree).
   */
  public String toString (R revision) {
    Node root = findRoot(revision);

    return root == null ?
      "No tree exists at revision " + revision : root.toString(revision);
  }
}
