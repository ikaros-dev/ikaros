package run.ikaros.server.core.service;

import org.thymeleaf.context.Context;
import run.ikaros.server.model.request.NotifyMailTestRequest;

import javax.annotation.Nonnull;
import javax.mail.MessagingException;

public interface NotifyService {
    void mailTest(NotifyMailTestRequest notifyMailTestRequest) throws MessagingException;

    void sendTemplateMail(@Nonnull String targetMailAddress, @Nonnull String subject,
                          @Nonnull String template, @Nonnull Context context);
}
