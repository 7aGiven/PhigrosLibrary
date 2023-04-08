package given.phigros;

import java.io.IOException;

@FunctionalInterface
interface ModifyStrategy {
    byte[] apply(byte[] data) throws IOException;
}
