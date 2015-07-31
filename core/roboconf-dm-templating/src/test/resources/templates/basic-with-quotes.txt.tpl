{
  "VMs": [
    {{#all 'Vm' }}
    {
      "path":   "{{path}},
      "status": "{{status}}"
    },
    {{/all}}
  ]
}
