class roboconf_withoperations_module::start($runningState = undef, $withinit = undef) {

  file{"/tmp/WithOperationsTemplate.start":
    ensure  => file,
    content => template('roboconf_withoperations_module/WithOperationsTemplate.erb'),
    }

    file{"/tmp/WithOperationsFile.start":
      ensure  => file,
      mode => 755,
      source => "puppet:///modules/roboconf_withoperations_module/WithOperationsFile.txt"
    }

}

