{{#all Vm}}
- {{name}} => {{#data}}{{#is-key 'machine.id'}}{{value}}{{/is-key}}{{/data}}
{{/all}}
