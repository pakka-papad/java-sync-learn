package org.example.cowlist;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

public class CoWList<T> implements Iterable<T> {

    private volatile Object[] list = new Object[0];

    public synchronized void add(T item) {
        int newLen = list.length + 1;
        var newList = Arrays.copyOf(list, newLen);
        newList[newLen - 1] = item;
        list = newList;
    }

    public synchronized boolean remove(T item) {
        var currentList = this.list;
        int pos = -1;
        for (int i = 0; i < currentList.length; i++) {
            if (Objects.equals(item, currentList[i])) {
                pos = i;
                break;
            }
        }
        if (pos == -1) {
            return false;
        }
        removeAtLocked(pos);
        return true;
    }

    public synchronized boolean removeAt(int index) {
        if (index < 0 || index >= list.length) {
            throw new IllegalArgumentException("Invalid index");
        }
        removeAtLocked(index);
        return true;
    }

    protected void removeAtLocked(int index) {
        var currentList = list;
        int newLen = currentList.length - 1;
        Object[] newList = new Object[newLen];
        if (index > 0) {
            System.arraycopy(currentList, 0, newList, 0, index);
        }
        if (index + 1 < currentList.length) {
            System.arraycopy(currentList, index + 1, newList, index, currentList.length - index - 1);
        }
        list = newList;
    }

    public T get(int index) {
        return (T) list[index];
    }

    public int size() {
        return list.length;
    }

    @Override
    public Iterator<T> iterator() {
        return new CoWIterator<>(list);
    }

    private class CoWIterator<K> implements Iterator<K> {
        private final Object[] list;
        private int currPos;

        CoWIterator(Object[] list) {
            this.list = list;
            currPos = 0;
        }

        @Override
        public boolean hasNext() {
            return (currPos < list.length);
        }

        @Override
        public K next() {
            if (!hasNext()) {
                throw new RuntimeException("No next available");
            }
            return (K) list[currPos++];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Modifications are not permitted on snapshot iterator");
        }

        @Override
        public void forEachRemaining(Consumer<? super K> action) {
            for (int i = currPos; i < list.length; i++) {
                action.accept((K) list[i]);
            }
            currPos = list.length;
        }
    }
}
