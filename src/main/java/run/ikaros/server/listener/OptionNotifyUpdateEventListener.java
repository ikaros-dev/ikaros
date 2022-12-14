package run.ikaros.server.listener;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import run.ikaros.server.core.service.NotifyService;
import run.ikaros.server.event.OptionNotifyUpdateEvent;
import run.ikaros.server.model.dto.OptionNotifyDTO;

import javax.annotation.Nonnull;

@Component
public class OptionNotifyUpdateEventListener
    implements ApplicationListener<OptionNotifyUpdateEvent> {

    private final NotifyService notifyService;

    public OptionNotifyUpdateEventListener(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    @Override
    public void onApplicationEvent(@Nonnull OptionNotifyUpdateEvent event) {
        OptionNotifyDTO optionNotifyDTO = event.getOptionNotifyDTO();
        notifyService.refreshMailSender(optionNotifyDTO);
    }
}
