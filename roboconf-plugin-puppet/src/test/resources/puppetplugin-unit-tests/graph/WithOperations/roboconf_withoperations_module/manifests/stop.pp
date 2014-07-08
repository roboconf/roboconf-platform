class roboconf_withoperations_module::stop($runningState = undef, $withinit = undef) {

  file{"/tmp/WithOperationsTemplate.stop":
    ensure  => file,
    content => template('roboconf_withoperations_module/WithOperationsTemplate.erb'),
    }

    file{"/tmp/WithOperationsFile.stop":
      ensure  => file,
      mode => 755,
      source => "puppet:///modules/roboconf_withoperations_module/WithOperationsFile.txt"
    }

}

