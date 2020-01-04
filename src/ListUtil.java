import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ListUtil {

    private ListUtil(){}

    public  static <T> Optional<T> headOption(List<T> xs) {
        return xs.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(xs.get(0));
    }

    public  static <T> Optional<T> lastOption(List<T> xs) {
        return xs.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(xs.get(xs.size() - 1));
    }

    public static <T, R> List<R> mapOptional(
            List<T> xs,
            Function<? super T, Optional<? extends R>> f) {
        return xs.stream()
                .map(f)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static <T, R> List<R> mapOptional2(
            List<T> xs,
            Function<? super T, Optional<? extends R>> f) {
        return zipWithOptional(xs, xs, (x, non_used) -> f.apply(x));
    }

    public static <T> List<T> catOptional(List<Optional<T>> xs) {
        return mapOptional(xs, Function.identity());
    }

    public static <T> List<T> flatten(List<? extends List<T>> xss) {
        return xss.stream().flatMap(List::stream).collect(Collectors.toList());
    }

    public static <T> List<List<T>> toListOfList(List<T> xs) {
        return xs.stream().map(List::of).collect(Collectors.toList());
    }

    public static <T, R> List<R> takeWhileOptional(
            List<T> xs,
            Function<? super T, Optional<? extends R>> f) {
        return xs.stream()
                .map(f)
                .takeWhile(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static <T, R> Optional<List<R>> traverse(
            List<T> xs,
            Function<? super T, Optional<? extends R>> f) {
        List<R> ys = takeWhileOptional(xs, f);
        return xs.size() == ys.size() ? Optional.of(ys) : Optional.empty();
    }

    public static <T> Optional<List<T>> sequence(List<Optional<T>> xs) {
        return traverse(xs, Function.identity());
    }

    public static <T> List<List<T>> groupBy(
            List<T> xs,
            BiPredicate<? super T, ? super T> p) {
        int n = xs.size();
        if (n == 0)
            return List.of();

        List<List<T>> yss = new ArrayList<>(n);
        List<T> ys = new ArrayList<>(n);
        T z = xs.get(0);
        ys.add(z);
        for (T x : xs.subList(1, xs.size())) {
            if (!p.test(x, z)) {
                yss.add(List.copyOf(ys));
                ys = new ArrayList<>(n);
                z = x;
            }
            ys.add(x);
        }
        yss.add(List.copyOf(ys));

        return List.copyOf(yss);
    }

    public static <T> List<List<T>> group(List<T> xs) {
        return groupBy(xs, Object::equals);
    }

    public static <T, R> R reduceWhile(
            List<T> xs,
            R z,
            BiFunction<? super T, ? super R, ? extends R> f,
            Predicate<? super R> p) {
        List<R> ys = scanLeftWhile(xs, z, f, p);
        return ys.get(ys.size() - 1);
    }

    public static <T> Optional<T> reduceWhile(
            List<T> xs,
            BiFunction<? super T, ? super T, ? extends T> f,
            Predicate<? super T> p) {
        List<T> ys = scanLeftWhile(xs, f, p);
        return lastOption(ys);
    }

    public static <T, R> List<R> scanLeft(
            List<T> xs,
            R z,
            BiFunction<? super T, ? super R, ? extends R> f) {
        return scanLeftWhile(xs, z, f, non_used -> true);
    }

    public static <T> List<T> scanLeft(
            List<T> xs,
            BiFunction<? super T, ? super T, ? extends T> f) {
        return scanLeftWhile(xs, f, non_used -> true);
    }

    public static <T, R> List<R> scanLeftWhile(
            List<T> xs,
            R z,
            BiFunction<? super T, ? super R, ? extends R> f,
            Predicate<? super R> p) {
        List<R> ys = new ArrayList<>(xs.size());
        ys.add(z);
        Iterator<T> it = xs.iterator();
        R acc = z;
        while (it.hasNext() && p.test((acc = f.apply(it.next(), acc)))) {
            ys.add(acc);
        }
        return List.copyOf(ys);
    }

    public static <A> List<A> scanLeftWhile(
            List<A> xs,
            BiFunction<? super A, ? super A, ? extends A> f,
            Predicate<? super A> p) {
        return xs.isEmpty()
                ? List.of()
                : scanLeftWhile(xs.subList(1, xs.size()), xs.get(0), f, p);
    }
    public static <A, B, C> List<C> zipWith(
            List<A> xs,
            List<B> ys,
            BiFunction<? super A, ?super B, ? extends C> f) {
        return zipWithOptional(xs, ys, f.andThen(Optional::ofNullable));
    }

    public static <T, U, R> List<R> zipWithOptional(
            List<T> xs,
            List<U> ys,
            BiFunction<? super T, ? super U, Optional<? extends R>> f) {
        Iterator<T> itx = xs.iterator();
        Iterator<U> ity = ys.iterator();
        int n = Math.min(xs.size(), ys.size());
        List<R> zs = new ArrayList<>(n);
        for (int i = 0; i < n; ++i) {
            Optional<? extends R> z = f.apply(itx.next(), ity.next());
            z.ifPresent(zs::add);
        }
        return List.copyOf(zs);
    }

    public static <A, C> List<C> mapWithIndex(
            List<A> xs,
            BiFunction<? super A, Integer, ? extends C> f) {
        return zipWith(xs, range(0, xs.size()), f);
    }

    public static List<Integer> range(int start, int end) {
        return IntStream.range(start, end).boxed().collect(Collectors.toList());
    }
}