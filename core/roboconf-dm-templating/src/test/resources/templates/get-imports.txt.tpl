{{#all Vm}}
- {{name}}
	{{#each imports}}
	instance = {{instance.path}}
	variables = {{#each variables}}
		{{name}} = {{value}}
	{{/each}}
	{{/each}}
{{/all}}
