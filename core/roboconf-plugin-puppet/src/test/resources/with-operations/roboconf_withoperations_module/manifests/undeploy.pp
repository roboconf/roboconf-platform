class roboconf_withoperations_module::undeploy($runningState = undef, $withinit = undef) {

  file{"/tmp/roboconf-test-for-puppet/WithOperations.tpl.undeploy":
    ensure  => file,
    content => template('roboconf_withoperations_module/WithOperationsTemplate.erb'),
  }

  file{"/tmp/roboconf-test-for-puppet/WithOperations.file.undeploy":
    ensure  => file,
    mode => "755",
    source => "puppet:///modules/roboconf_withoperations_module/WithOperationsFile.txt"
  }
}
