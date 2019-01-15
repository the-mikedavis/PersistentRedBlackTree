# Persistent Red Black Tree

A revision based persistent red black tree. All methods take a "revision"
`Comparable` as a parameter. This is useful for certain problems in
Computational Geometry, in which a sweep parameter like time or space can be
used in creation of an efficient lookup mechanism.

Based on the original implementation by Robert Sedgewick and Kevin Wayne.

## Usage

```java
// initialization
PersistentSet<Float, Double> set = new PersistentSet<Float, Double>();

// adding elements
set.add(1.0f, 1.0);
set.add(0.8f, 1.0);
set.add(1.1f, 1.0);
set.add(1.2f, 1.0);
set.add(1.9f, 1.0);

// adding duplicates returns false
set.add(1.0f, 1.1);
//=> false

// multple revisions
set.add(1.4f, 1.1);
set.add(1.6f, 1.2);
set.add(0.6f, 1.3);

// removing elements
set.remove(1.0f, 1.6);

// getting neighboring elements
// we can do this because the set is a red-black tree underneath
set.predecessor(1.05f, 1.0);
//=> 0.9f
set.successor(1.05f, 1.0);
//=> 1.1f
```
