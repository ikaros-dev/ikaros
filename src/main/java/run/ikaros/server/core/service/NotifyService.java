package run.ikaros.server.core.service;

import run.ikaros.server.model.request.NotifyMailTestRequest;

import javax.mail.MessagingException;

public interface NotifyService {
    void mailTest(NotifyMailTestRequest notifyMailTestRequest) throws MessagingException;
}
