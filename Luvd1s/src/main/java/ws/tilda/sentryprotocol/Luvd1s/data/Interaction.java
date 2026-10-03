package ws.tilda.sentryprotocol.Luvd1s.data;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Interaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Person person;

    @Enumerated(EnumType.STRING)
    @NotNull
    private InteractionType type;

    @NotNull
    private LocalDateTime occurredAt;

    @Column(length = 2000)
    private String summary;

    private LocalDate followUpDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Person getPerson() { return person; }
    public void setPerson(Person person) { this.person = person; }
    public InteractionType getType() { return type; }
    public void setType(InteractionType type) { this.type = type; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(LocalDate followUpDate) { this.followUpDate = followUpDate; }
}