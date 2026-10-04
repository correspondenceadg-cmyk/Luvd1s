package ws.tilda.sentryprotocol.Luvd1s.api;

import ws.tilda.sentryprotocol.Luvd1s.api.dto.TagDto;
import ws.tilda.sentryprotocol.Luvd1s.service.TagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagApiController {

    private final TagService tagService;

    public TagApiController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public List<TagDto> list() {
        return tagService.findAll().stream()
                .map(TagDto::from)
                .toList();
    }
}