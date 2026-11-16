package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.Tag;
import ws.tilda.sentryprotocol.Luvd1s.data.User;
import ws.tilda.sentryprotocol.Luvd1s.repository.TagRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagService {

    private final TagRepository tags;
    private final UserService userService;

    public TagService(TagRepository tags, UserService userService) {
        this.tags = tags;
        this.userService = userService;
    }

    @PreAuthorize("isAuthenticated()")
    public List<Tag> findAll() {
        User current = userService.getCurrentUser();
        if (current == null) return List.of();
        return tags.findByOwner(current);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public Tag findOrCreate(String name, String color) {
        User current = userService.getCurrentUser();
        return tags.findByOwnerAndName(current, name).orElseGet(() -> {
            Tag t = new Tag();
            t.setOwner(current);
            t.setName(name);
            t.setColor(color != null ? color : "#888888");
            return tags.save(t);
        });
    }

    @PreAuthorize("isAuthenticated() and @ownership.owns(#tag)")
    @Transactional
    public void delete(Tag tag) {
        tags.delete(tag);
    }
}
