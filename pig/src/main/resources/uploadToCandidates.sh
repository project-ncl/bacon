#!/usr/bin/expect

set timeout 1800
spawn ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null $::env(LOGIN)@rcm-guest.app.eng.bos.redhat.com
expect {
  password { send "$::env(PASSWORD)\r" ; exp_continue }
  bash { send "kinit\r" }
}
expect Password { send "$::env(PASSWORD)\r" }
expect bash { send "/mnt/redhat/scripts/rel-eng/utility/bus-clients/stage-mw-release ${productWithVersion}\r" }
expect bash
