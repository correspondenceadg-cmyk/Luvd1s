package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.Tag;
import ws.tilda.sentryprotocol.Luvd1s.data.User;
import ws.tilda.sentryprotocol.Luvd1s.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagService {

    private final TagRepository tags;
    private final UserService userService;

    public TagService(TagRepository tags, UserService userService) {
        this.tags = tags;
        this.userService = userService;
    }

    public List<Tag> findAll() {
        User owner = userService.getCurrentUser();
        if (owner == null) return List.of();
        return tags.findByOwner(owner);
    }

    public Tag save(Tag tag) {
        User owner = userService.getCurrentUser();
        if (owner == null) {
            throw new IllegalStateException("Not logged in");
        }
        if (tag.getOwner() == null) {
            tag.setOwner(owner);
        }
        return tags.save(tag);
    }
}