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
    private final AuditService auditService;

    public TagService(TagRepository tags, UserService userService, AuditService auditService) {
        this.tags = tags;
        this.userService = userService;
        this.auditService = auditService;
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
            Tag saved = tags.save(t);
            auditService.log("TAG_CREATE", "Tag", saved.getId(), null);
            return saved;
        });
    }

    @PreAuthorize("isAuthenticated() and @ownership.owns(#tag)")
    @Transactional
    public void delete(Tag tag) {
        tags.delete(tag);
        auditService.log("TAG_DELETE", "Tag", tag.getId(), null);
    }
}
