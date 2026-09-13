package com.engine.logic;

import com.data.events.entities.OBOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CustomOrderHeap {
    private final ArrayList<OBOrder> heap;
    private final Comparator<OBOrder> comparator;

    public CustomOrderHeap(Comparator<OBOrder> comparator) {
        this.heap = new ArrayList<>();
        this.comparator = comparator;
    }

    public boolean isEmpty() {
        return heap.isEmpty();
    }

    public OBOrder peek() {
        if (heap.isEmpty()) return null;
        return heap.get(0);
    }

    public void insert(OBOrder order) {
        heap.add(order);
        siftUp(heap.size() - 1);
    }

    public OBOrder extractTop() {
        if (heap.isEmpty()) return null;

        OBOrder top = heap.get(0);
        OBOrder last = heap.remove(heap.size() - 1);

        if (!heap.isEmpty()) {
            heap.set(0, last);
            siftDown(0);
        }
        return top;
    }

    private void siftUp(int index) {
        OBOrder current = heap.get(index);
        while (index > 0) {
            int parentIndex = (index - 1) / 2;
            OBOrder parent = heap.get(parentIndex);

            if (comparator.compare(current, parent) < 0) {
                heap.set(index, parent);
                index = parentIndex;
            } else {
                break;
            }
        }
        heap.set(index, current);
    }

    private void siftDown(int index) {
        int size = heap.size();
        OBOrder current = heap.get(index);

        while (true) {
            int leftChild = 2 * index + 1;
            int rightChild = 2 * index + 2;
            int highestPriorityIndex = index;

            if (leftChild < size && comparator.compare(heap.get(leftChild), heap.get(highestPriorityIndex)) < 0) {
                highestPriorityIndex = leftChild;
            }
            if (rightChild < size && comparator.compare(heap.get(rightChild), heap.get(highestPriorityIndex)) < 0) {
                highestPriorityIndex = rightChild;
            }

            if (highestPriorityIndex != index) {
                heap.set(index, heap.get(highestPriorityIndex));
                index = highestPriorityIndex;
            } else {
                break;
            }
        }
        heap.set(index, current);
    }

    public List<OBOrder> getElements() {
        return new ArrayList<>(this.heap);
    }

}