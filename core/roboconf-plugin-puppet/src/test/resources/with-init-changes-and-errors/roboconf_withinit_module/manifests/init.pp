class roboconf_withinit_module($runningState = undef, $importDiff = undef, $withoperations = undef) {

  exec{ "hello":
    command => "echo 'Hello!'",
    path    => "/usr/local/bin/:/bin/"
  }

  exec{ "non-zero return code":
  	command => "/bin/bou 'this command does not exist'",
    path    => "/usr/local/bin/:/bin/"
  }
}
