package com.github.blakelai.components;

import com.github.blakelai.models.Person;
import com.github.blakelai.services.PersonService;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag("simple-greeting")
@JsModule("./simple-greeting.js")
public class SimpleGreetingComponent extends VerticalLayout {
    private static final Logger logger = LoggerFactory.getLogger(SimpleGreetingComponent.class);

    private int personIndex = 0;
    private PersonService personService = PersonService.get();
    private Grid<Person> grid;

    public SimpleGreetingComponent() {
        grid = new Grid<>(Person.class);
        grid.getElement().setAttribute("slot", "grid");
        grid.addSelectionListener(event -> event.getFirstSelectedItem()
                .ifPresent(person -> {
                    getElement().setProperty("name", person.getFirstName());
                })
        );
        add(grid);

        morePerson();
    }

    @ClientCallable
    public void morePerson() {
        logger.info("Fetch more person");
        grid.setItems(personService.get(personIndex, 20));
        personIndex += 20;
    }
}
