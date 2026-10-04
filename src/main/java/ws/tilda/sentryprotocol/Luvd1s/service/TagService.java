package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.Tag;
import ws.tilda.sentryprotocol.Luvd1s.data.User;
import ws.tilda.sentryprotocol.Luvd1s.repository.TagRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagService {

    private final TagRepository tags;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public TagService(TagRepository tags,
                      UserService userService,
                      AuditLogService auditLogService) {
        this.tags = tags;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @PreAuthorize("isAuthenticated()")
    public List<Tag> findAll() {
        User owner = userService.getCurrentUser();
        if (owner == null) return List.of();
        return tags.findByOwner(owner);
    }

    @PreAuthorize("isAuthenticated() and @ownership.owns(#tag)")
    public Tag save(Tag tag) {
        User owner = userService.getCurrentUser();
        if (owner == null) {
            throw new IllegalStateException("Not logged in");
        }

        boolean isNew = tag.getId() == null;
        if (tag.getOwner() == null) {
            tag.setOwner(owner);
        }

        Tag saved = tags.save(tag);

        auditLogService.record(
                isNew ? "TAG_CREATE" : "TAG_UPDATE",
                "Tag",
                saved.getId(),
                (isNew ? "Created tag " : "Updated tag ") + saved.getName()
        );

        return saved;
    }
}