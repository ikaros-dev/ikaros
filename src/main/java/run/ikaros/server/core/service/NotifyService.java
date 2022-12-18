package run.ikaros.server.core.service;

import org.thymeleaf.context.Context;
import run.ikaros.server.model.dto.OptionNotifyDTO;
import run.ikaros.server.model.request.NotifyMailTestRequest;

import jakarta.annotation.Nonnull;
import jakarta.mail.MessagingException;

public interface NotifyService {
    void mailTest(NotifyMailTestRequest notifyMailTestRequest) throws MessagingException;

    void sendTemplateMail(@Nonnull String targetMailAddress, @Nonnull String subject,
                          @Nonnull String template, @Nonnull Context context);

    void refreshMailSender(@Nonnull OptionNotifyDTO optionNotifyDTO);
}
