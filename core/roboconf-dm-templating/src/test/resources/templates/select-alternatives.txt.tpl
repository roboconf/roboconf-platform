{{! Should display everything except Apache and Tomcats }}
{{#all 'Vm | MySQL | */Tom*/War' }}
- {{path}}
{{/all}}
