#!/usr/bin/env bash

set -e
# mstodo: 

<#noparse>
function getPushStatus() {
    pnc brew-push status $1 | grep "status" | cut -d : -f 2 | tr -d '[:space:]' | tr -d '"'
}

function waitForPush() {
    while true
    do
        status=$(getPushStatus $1)
        if [ "${status}" == "SUCCESS" ]
        then
            break;
        fi

        if [ "${status}" == "SYSTEM_ERROR" ] || [ "${status}" == "CANCELED" ] || [ "${status}" == "FAILED" ]
        then
            echo "Failed to push build $1 to Brew"
            exit 1
        fi
        echo "Waiting for build $1 to be pushed to Brew"
        sleep 5
    done
}
</#noparse>

<#list buildsToPush as build>
pnc brew-push build ${build} ${brewTag}
waitForPush ${build}
</#list>

/bin/bash ${nvrListScriptLocation} ${repoZipLocation} ${targetPath} ${kojiHubUrl}

pnc close-milestone ${milestoneId} --wait
