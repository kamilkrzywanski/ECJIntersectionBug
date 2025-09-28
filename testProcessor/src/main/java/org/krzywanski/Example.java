package org.krzywanski;

import java.io.Serializable;


@Annot
public class Example<T extends Serializable & Comparable<T>> {
    public <U extends Number & Runnable> void doSomething(U input) { }
}
