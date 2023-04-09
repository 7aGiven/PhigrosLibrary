import net.mamoe.mirai.Bot;
import net.mamoe.mirai.console.command.*;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;

public final class MyPlugin extends JavaPlugin {
    public static final MyPlugin INSTANCE = new MyPlugin();
    private MyPlugin() {
        super(new JvmPluginDescriptionBuilder("given.PhigrosBot","0.0.4").build());
    }
    @Override
    public void onEnable() {
        getLogger().error("启动");
        CommandManager.INSTANCE.registerCommand(PhigrosCompositeCommand.INSTANCE,true);
        CommandManager.INSTANCE.registerCommand(TestPhigrosCommand.INSTANCE,true);
        EventChannel<BotEvent> channel = GlobalEventChannel.INSTANCE
                .parentScope(INSTANCE)
                .filterIsInstance(BotEvent.class);
        //bind命令
        channel.subscribeAlways(UserMessageEvent.class,event -> {
            MessageChain chain = event.getMessage();
            if (chain.size() != 2) return;
            String session = chain.get(1).contentToString();
            if (session.matches("[a-z0-9]{25}")) {
                event.getSubject().sendMessage("匹配成功");
                try {
                    PhigrosCompositeCommand.INSTANCE.bind(event.getSubject(),session);
                } catch (Exception e) {
                    event.getSubject().sendMessage(e.toString());
                }
            }
        });
        //戳一戳
        channel.filterIsInstance(NudgeEvent.class)
                .filter(event -> event.getTarget().getId() == event.getBot().getId())
                .subscribeAlways(NudgeEvent.class, event -> {
                    if (!(event.getSubject() instanceof Group)) return;
                    Member member;
                    if (event.getFrom() instanceof Bot) {
                        member = ((Group) event.getSubject()).getBotAsMember();
                    } else {
                        member = (Member) event.getFrom();
                    }
                    CommandManager.INSTANCE.executeCommand(CommandSender.of(member),new PlainText("/help"),true);
                });
        channel.subscribeAlways(GroupMessagePostSendEvent.class,event -> {
            if (event.getMessage().contentToString().startsWith("◆ /help     # 查看指令帮助\n◆ /")) {
                event.getReceipt().recallIn(60000);
            }
        });
    }
    @Override
    public void onDisable() {
        DAO.INSTANCE.writeUser();
    }
}