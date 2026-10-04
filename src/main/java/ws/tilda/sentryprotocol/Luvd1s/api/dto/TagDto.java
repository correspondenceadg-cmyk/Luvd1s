package ws.tilda.sentryprotocol.Luvd1s.api.dto;

import ws.tilda.sentryprotocol.Luvd1s.data.Tag;

public record TagDto(Long id, String name, String color) {

    public static TagDto from(Tag tag) {
        return new TagDto(tag.getId(), tag.getName(), tag.getColor());
    }
}