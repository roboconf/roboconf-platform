class roboconf_withoperations_module::update($runningState = undef, $withinit = undef, $importDiff = undef) {

  file{"/tmp/roboconf-test-for-puppet/WithOperations.tpl.update":
    ensure  => file,
    content => template('roboconf_withoperations_module/WithOperationsTemplate.erb'),
  }

  file{"/tmp/roboconf-test-for-puppet/WithOperations.file.update":
    ensure  => file,
    mode => "755",
    source => "puppet:///modules/roboconf_withoperations_module/WithOperationsFile.txt"
  }
}
