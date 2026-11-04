package ws.tilda.sentryprotocol.Luvd1s.security;

import ws.tilda.sentryprotocol.Luvd1s.data.Interaction;
import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.data.Tag;
import ws.tilda.sentryprotocol.Luvd1s.data.User;
import ws.tilda.sentryprotocol.Luvd1s.service.UserService;
import org.springframework.stereotype.Component;

@Component("ownership")
public class OwnershipCheck {

    private final UserService userService;

    public OwnershipCheck(UserService userService) {
        this.userService = userService;
    }

    public boolean owns(Person person) {
        if (person == null) return false;
        User current = userService.getCurrentUser();
        if (current == null) return false;
        if (person.getOwner() == null) return false;
        return current.getId().equals(person.getOwner().getId());
    }

    public boolean owns(Interaction interaction) {
        if (interaction == null) return false;
        return owns(interaction.getPerson());
    }

    public boolean owns(Tag tag) {
        if (tag == null) return false;
        User current = userService.getCurrentUser();
        if (current == null) return false;
        if (tag.getOwner() == null) return false;
        return current.getId().equals(tag.getOwner().getId());
    }
}