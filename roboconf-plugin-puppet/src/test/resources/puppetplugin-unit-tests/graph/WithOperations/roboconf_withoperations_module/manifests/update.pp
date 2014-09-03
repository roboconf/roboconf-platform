class roboconf_withoperations_module::update($runningState = undef, $withinit = undef, $importAdded = undef, $importRemoved = undef, $importComponent = undef) {

  file{"/tmp/WithOperationsTemplate.update":
    ensure  => file,
    content => template('roboconf_withoperations_module/WithOperationsTemplate.erb'),
    }

    file{"/tmp/WithOperationsFile.update":
      ensure  => file,
      mode => 755,
      source => "puppet:///modules/roboconf_withoperations_module/WithOperationsFile.txt"
    }

}

