import given.phigros.PhigrosUser;
import org.apache.thrift.TException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class PhigrosImpl implements Phigros.Iface{
    @Override
    public Summary getSaveUrl(String sessionToken) throws TException {
        try {
            if (sessionToken.length() != 25)
                throw new Exception("SessionToken的长度不为25.");
            final var user = new PhigrosUser(sessionToken);
            final var summary = user.update();
            return new Summary(user.saveUrl.toString(), summary.saveVersion, summary.challengeModeRank, summary.rankingScore, summary.gameVersion, summary.avatar);
        } catch (Exception e) {
            e.printStackTrace();
            throw new TException(e);
        }
    }

    @Override
    public List<SongLevel> best19(String saveUrl) throws TException {
        try {
            List<SongLevel> list = new ArrayList<>();
            for(final var songLevel:new PhigrosUser(URI.create(saveUrl)).getBestN(19)) {
                if (songLevel.id == null) {
                    songLevel.id = "";
                    songLevel.level = given.phigros.Level.EZ;
                }
                list.add(new SongLevel(songLevel.id, Level.findByValue(songLevel.level.ordinal()), songLevel.s, songLevel.a, songLevel.c, songLevel.difficulty, songLevel.rks));
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            throw new TException(e);
        }
    }

    @Override
    public List<SongLevel> bestn(String saveUrl, byte num) throws TException {
        try {
            List<SongLevel> list = new ArrayList<>();
            for(final var songLevel:new PhigrosUser(URI.create(saveUrl)).getBestN(num)) {
                list.add(new SongLevel(songLevel.id, Level.findByValue(songLevel.level.ordinal()), songLevel.s, songLevel.a, songLevel.c, songLevel.difficulty, songLevel.rks));
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            throw new TException(e);
        }
    }

    @Override
    public List<SongExpect> songExpects(String saveUrl) throws TException {
        try {
            List<SongExpect> list = new ArrayList<>();
            for(final var songExpect:new PhigrosUser(URI.create(saveUrl)).getExpects()) {
                list.add(new SongExpect(songExpect.id, Level.findByValue(songExpect.level.ordinal()), songExpect.acc, songExpect.expect));
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            throw new TException(e);
        }
    }
}