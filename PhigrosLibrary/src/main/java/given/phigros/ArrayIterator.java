package given.phigros;

import java.util.Iterator;

class ArrayIterator<T> implements Iterator<T> {
    private final T[] array;
    private int position;
    ArrayIterator(T[] array) {
        this.array = array;
    }
    @Override
    public boolean hasNext() {
        return position != array.length;
    }
    @Override
    public T next() {
        return array[position++];
    }
}