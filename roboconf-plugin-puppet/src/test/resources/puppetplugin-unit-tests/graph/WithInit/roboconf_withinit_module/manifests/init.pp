class roboconf_withinit_module($runningState = undef, $importAdded = undef, $importRemoved = undef, $importComponent = undef, $withoperations = undef) {

# 'withoperations' is an array of hashes
# It needs to be declared as the following:
# $withoperations = { 
#              'withoperations1' => {'ip' => '127.0.0.1', 'port' => '8009'},
#              'withoperations2' => {'ip' => '127.0.0.2', 'port' => '8010'}
#            }

  file{"/tmp/WithInitTemplate.$runningState":
    ensure  => file,
    content => template('roboconf_withinit_module/WithInitTemplate.erb'),
  }

  file{"/tmp/WithInitFile.$runningState":
    ensure  => file,
    mode => 755,
    source => "puppet:///modules/roboconf_withinit_module/WithInitFile.txt"
  }
}
