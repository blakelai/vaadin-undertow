package com.github.blakelai.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

@Tag("simple-greeting")
@JsModule("./simple-greeting.js")
public class SimpleGreetingComponent extends Component {
    public SimpleGreetingComponent() {
        getElement().setProperty("name", "Java");
    }
}
