{{#all Vm}}
- {{name}}
	{{#each exports}}
{{name}} = {{value}}
	{{/each}}
{{/all}}
