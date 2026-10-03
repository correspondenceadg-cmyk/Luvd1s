package ws.tilda.sentryprotocol.Luvd1s.ui;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.repository.PersonRepository;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("")
@PageTitle("People")
public class PeopleView extends VerticalLayout {

    private final PersonRepository repository;
    private final Grid<Person> grid = new Grid<>(Person.class, false);

    public PeopleView(PersonRepository repository) {
        this.repository = repository;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("People"));

        grid.addColumn(Person::getFirstName).setHeader("First name").setAutoWidth(true);
        grid.addColumn(Person::getLastName).setHeader("Last name").setAutoWidth(true);
        grid.addColumn(Person::getEmail).setHeader("Email").setAutoWidth(true);
        grid.addColumn(Person::getCompany).setHeader("Company").setAutoWidth(true);
        grid.addColumn(Person::getJobTitle).setHeader("Role").setAutoWidth(true);
        grid.setSizeFull();

        grid.setItems(repository.findAll());

        add(grid);
    }
}