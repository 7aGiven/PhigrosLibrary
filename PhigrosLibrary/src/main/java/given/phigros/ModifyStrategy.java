package given.phigros;

import java.io.IOException;

@FunctionalInterface
public interface ModifyStrategy<T> {
    void apply(T data) throws IOException;
}
