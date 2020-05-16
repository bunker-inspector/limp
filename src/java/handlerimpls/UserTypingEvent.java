package handlerimpls;

import com.slack.api.rtm.RTMEventHandler;
import com.slack.api.model.event.UserTypingEvent;

abstract class UserTypingEventHandler extends RTMEventHandler<UserTypingEvent> {
    public abstract void handle(UserTypingEvent event);
}
