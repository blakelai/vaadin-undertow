import { LitElement, html } from 'lit-element';

class SimpleGreeting extends LitElement {
    static get properties() {
        return {
            name: { type: String }
        }
    }
    render() {
        return html`
            <p>Hello, ${this.name}</p>
            <h3>Select a person</h3>
            <slot name="grid"></slot>
            <button @click="${e => this.$server.morePerson()}">More</button>
        `;
    }
}
customElements.define('simple-greeting', SimpleGreeting);
