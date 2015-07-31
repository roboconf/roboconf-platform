{{! This line selects all the instances of type "VM" inside the application}}
{{#all Vm}}
    {{! For each instance, the content of this block is output once.}}
name = "{{name}}"
{{#all Tomcat}}

	name = "{{name}}"
	path = {{path}}

{{/all}}
{{/all}}
