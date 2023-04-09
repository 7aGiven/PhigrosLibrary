import javax.imageio.ImageIO;
import java.awt.*;
import java.util.concurrent.Callable;

public class IllustrationCallback implements Callable<Image> {
    private final String id;
    IllustrationCallback(String id) {
        this.id = id;
    }
    @Override
    public Image call() throws Exception {
        final var file = MyPlugin.INSTANCE.resolveDataFile(String.format("illustration/%s.png",id));
        if (!file.exists()) throw new Exception(String.format("曲绘%s不存在",id));
        return ImageIO.read(file).getScaledInstance(300,158,Image.SCALE_SMOOTH);
    }
}
