class roboconf_withinit_module($runningState = undef, $importAdded = undef, $importRemoved = undef, $importComponent = undef, $withoperations = undef) {

  exec{ "non-zero return code":
  	command => "/bin/bou 'this command does not exist'",
    path    => "/usr/local/bin/:/bin/"
  }
}
