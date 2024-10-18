package oblitusnumen.calendar.implementation.data;

import androidx.annotation.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

public class SortedList<E> implements Serializable, Iterable<E> {
    @Serial
    private static final long serialVersionUID = 1;
    protected final ArrayList<E> list = new ArrayList<>();
    protected final Comparator<? super E> comparator;

    public SortedList() {
        this((Comparator<? super E>) Comparator.naturalOrder());
    }

    public SortedList(Comparator<? super E> comparator) {
        this.comparator = comparator;
    }

    public SortedList(Collection<? extends E> initial) {
        this(initial, (Comparator<? super E>) Comparator.naturalOrder());
    }

    public SortedList(Collection<? extends E> initial, Comparator<? super E> comparator) {
        this(comparator);
        list.addAll(initial);
        list.sort(comparator);
    }

    public void add(E el) {
        list.add(findAdditionIndex(el), el);
    }

    public int findAdditionIndex(E el) {
        int begin = 0;
        int end = list.size();
        while (true) {
            int center = (begin + end) / 2;
            if (center == end) return center;
            if (comparator.compare(list.get(center), el) > 0) {
                end = center;
            } else {
                if (begin == center) return end;
                begin = center;
            }
        }
    }

    @Override
    public @NotNull String toString() {
        return list.toString();
    }

    @NonNull
    @Override
    public @NotNull Iterator<E> iterator() {
        return list.iterator();
    }
}
