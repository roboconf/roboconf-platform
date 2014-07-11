class roboconf_withoperations_module::undeploy($runningState = undef, $withinit = undef) {

  file{"/tmp/WithOperationsTemplate.undeploy":
    ensure  => file,
    content => template('roboconf_withoperations_module/WithOperationsTemplate.erb'),
    }

    file{"/tmp/WithOperationsFile.undeploy":
      ensure  => file,
      mode => 755,
      source => "puppet:///modules/roboconf_withoperations_module/WithOperationsFile.txt"
    }

}

