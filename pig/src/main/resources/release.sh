#!/usr/bin/env bash

set -e
# mstodo: 

<#list buildsToPush as build>
pnc brew-push build ${build} --tag-prefix="${brewTag}" --wait
</#list>

/bin/bash ${nvrListScriptLocation} ${repoZipLocation} ${targetPath} ${kojiHubUrl}

pnc product-milestone close ${milestoneId}
