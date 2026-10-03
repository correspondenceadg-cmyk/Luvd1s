package ws.tilda.sentryprotocol.Luvd1s.data;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Reminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Person person;

    private LocalDate dueDate;

    @Column(length = 500)
    private String message;

    private boolean completed;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Person getPerson() { return person; }
    public void setPerson(Person person) { this.person = person; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}