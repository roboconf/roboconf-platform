class roboconf_withoperations_module::stop($runningState = undef, $withinit = undef) {

  file{"/tmp/roboconf-test-for-puppet/WithOperations.tpl.stop":
    ensure  => file,
    content => template('roboconf_withoperations_module/WithOperationsTemplate.erb'),
  }

  file{"/tmp/roboconf-test-for-puppet/WithOperations.file.stop":
    ensure  => file,
    mode => "755",
    source => "puppet:///modules/roboconf_withoperations_module/WithOperationsFile.txt"
  }
}
