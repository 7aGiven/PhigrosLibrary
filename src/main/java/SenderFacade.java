import given.phigros.PhigrosUser;
import net.mamoe.mirai.console.command.CommandContext;
import net.mamoe.mirai.console.command.CommandSender;
import net.mamoe.mirai.console.command.OtherClientCommandSenderOnMessageSync;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.MessageSyncEvent;
import net.mamoe.mirai.message.data.ForwardMessage;
import net.mamoe.mirai.message.data.QuoteReply;
import net.mamoe.mirai.utils.ExternalResource;

import java.io.IOException;

public class SenderFacade {
    final QuoteReply quoteReply;
    final Contact subject;
    final User user;
    final PhigrosUser myUser;
    public SenderFacade(CommandContext context) throws Exception {
        CommandSender sender = context.getSender();
        if (sender instanceof OtherClientCommandSenderOnMessageSync) {
            MessageSyncEvent event = ((OtherClientCommandSenderOnMessageSync) sender).getFromEvent();
            user = event.getSender();
            subject = event.getSubject();
        } else {
            user = sender.getUser();
            subject = sender.getSubject();
        }
        myUser = DAO.INSTANCE.users.get(user.getId());
        if (myUser == null) throw new Exception("您尚未绑定SessionToken");
        quoteReply = new QuoteReply(context.getOriginalMessage());
    }
    public void sendMessage(String message) {
        subject.sendMessage(quoteReply.plus(message));
    }
    public void sendMessage(ForwardMessage message) {
        subject.sendMessage(message);
    }
    public void sendImage(ExternalResource externalResource) throws IOException {
        subject.sendMessage(subject.uploadImage(externalResource));
        externalResource.close();
    }
}
