import { LitElement, html } from 'lit-element';

class SimpleGreeting extends LitElement {
    static get properties() {
        return {
            name: { type: String }
        }
    }
    render() {
        return html`<p>Hello, ${this.name}</p>`;
    }
}
customElements.define('simple-greeting', SimpleGreeting);
