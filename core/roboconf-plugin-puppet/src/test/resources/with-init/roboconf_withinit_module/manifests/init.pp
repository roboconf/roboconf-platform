class roboconf_withinit_module($runningState = undef, $importDiff = undef, $withoperations = undef) {

  # A file generated from a template
  file{"/tmp/roboconf-test-for-puppet/WithInit.tpl.$runningState":
    ensure  => file,
    content => template('roboconf_withinit_module/WithInitTemplate.erb'),
  }

  # The exact copy of a file
  file{"/tmp/roboconf-test-for-puppet/WithInit.file.$runningState":
    ensure  => file,
    mode => "755",
    source => "puppet:///modules/roboconf_withinit_module/WithInitFile.txt"
  }
}
