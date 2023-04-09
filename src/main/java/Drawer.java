import java.awt.*;

public class Drawer {
    private final Graphics2D g2d;
    private int x;
    private int y;
    Drawer(Graphics2D g2d) {this.g2d = g2d;}
    public void setPoint(int x,int y) {
        this.x = x;
        this.y = y;
    }
    public void drawImage(Image img,int x,int y) {
        g2d.drawImage(img,this.x+x,this.y+y,null);
    }
    public void drawString(String str,int x,int y) {
        g2d.drawString(str,this.x+x,this.y+y);
    }
    public void fillRect(int x,int y,int width,int height) {
        g2d.fillRect(this.x+x,this.y+y,width,height);
    }
}
