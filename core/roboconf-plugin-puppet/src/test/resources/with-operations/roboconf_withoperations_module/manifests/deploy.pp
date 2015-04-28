class roboconf_withoperations_module::deploy($runningState = undef, $withinit = undef) {

  file{"/tmp/roboconf-test-for-puppet/WithOperations.tpl.deploy":
    ensure  => file,
    content => template('roboconf_withoperations_module/WithOperationsTemplate.erb'),
  }

  file{"/tmp/roboconf-test-for-puppet/WithOperations.file.deploy":
    ensure  => file,
    mode => "755",
    source => "puppet:///modules/roboconf_withoperations_module/WithOperationsFile.txt"
  }
}
